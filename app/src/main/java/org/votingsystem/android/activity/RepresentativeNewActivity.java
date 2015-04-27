package org.votingsystem.android.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.votingsystem.android.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.fragment.EditorFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.NewFieldDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.ProgressDialogFragment;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.FileDescriptor;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeNewActivity extends ActivityBase {
	
	public static final String TAG = RepresentativeNewActivity.class.getSimpleName();

    private static final int SELECT_PICTURE   = 1;
    private static final int CONFIRM_PICTURE  = 2;

    private TypeVS operationType;
    private EditorFragment editorFragment;
    private AppVS contextVS;
    private TextView imageCaption;
    private UserVSDto representative;
    private String broadCastId = RepresentativeNewActivity.class.getSimpleName();
    private String representativeImageName = null;
    private String editorContent = null;
    private Uri representativeImageUri = null;
    private byte[] imageBytes;
    private Menu menu;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) newRepresentative();
        else {
            if(TypeVS.NIF_REQUEST == responseVS.getTypeVS()) {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    try {
                        String representativeNif = NifUtils.validate(
                                message, RepresentativeNewActivity.this);
                        loadRepresentativeData(representativeNif);
                    }
                    catch(Exception ex) {
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(),
                                getSupportFragmentManager());
                    }
                } else RepresentativeNewActivity.this.onBackPressed();
            } else if(TypeVS.NEW_REPRESENTATIVE == responseVS.getTypeVS()) {
                setProgressDialogVisible(false);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    editorFragment.setEditable(true);
                    MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
                } else {
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            responseVS, RepresentativeNewActivity.this).
                            setPositiveButton(getString(R.string.accept_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            RepresentativeNewActivity.this.finish();
                                        }
                                    });
                    UIUtils.showMessageDialog(builder);
                }
            } else if(TypeVS.ITEM_REQUEST == responseVS.getTypeVS()) {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Uri representativeURI = intent.getParcelableExtra(ContextVS.URI_KEY);
                    Cursor cursor = RepresentativeNewActivity.this.getApplicationContext().
                            getContentResolver().query(representativeURI,
                            null, null, null, null);
                    cursor.moveToFirst();
                    UserVSDto representative = (UserVSDto) ObjectUtils.deSerializeObject(cursor.getBlob(
                            cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                    setRepresentativeData(representative);
                }
                setProgressDialogVisible(false);
            }
        }
        }
    };

    private void newRepresentative() {
        LOGD(TAG + ".newRepresentative", "");
        editorFragment.setEditable(false);
        Intent startIntent = new Intent(getApplicationContext(), RepresentativeService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.NEW_REPRESENTATIVE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.IMAGE_KEY, imageBytes);
        startIntent.putExtra(ContextVS.MESSAGE_KEY, editorContent);
        startIntent.putExtra(ContextVS.URI_KEY, representativeImageUri);
        setProgressDialogVisible(true);
        startService(startIntent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        contextVS = (AppVS) getApplicationContext();
        operationType = (TypeVS) getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        LOGD(TAG + ".onCreate", "operationType: " + operationType +
                " - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.representative_new);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        editorFragment = (EditorFragment) getSupportFragmentManager().findFragmentByTag(
                EditorFragment.TAG);
        imageCaption = (TextView) findViewById(R.id.representative_image_caption);
        imageCaption.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFileChooser();
            }
        });
        if(operationType != null && TypeVS.REPRESENTATIVE == operationType) {
            RepresentationStateDto representation = PrefUtils.getRepresentationState(this);
            if(representation == null) showNifDialog();
            else setRepresentativeData(representation.getRepresentative());
            getSupportActionBar().setTitle(getString(R.string.edit_representative_lbl));
        }
        if(savedInstanceState != null) {
            representativeImageUri = (Uri) savedInstanceState.getParcelable(ContextVS.URI_KEY);
            representativeImageName = savedInstanceState.getString(ContextVS.ICON_KEY);
            imageBytes = savedInstanceState.getByteArray(ContextVS.IMAGE_KEY);
            if(representativeImageUri != null) {
                setRepresentativeImage(representativeImageUri, representativeImageName);
            }
        }
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.sending_data_lbl),
                    getString(R.string.representative_new_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    private void showNifDialog() {
        NewFieldDialogFragment newFieldDialog = NewFieldDialogFragment.newInstance(
                getString(R.string.edit_representative_lbl), getString(
                R.string.representative_nif_lbl), broadCastId,  TypeVS.NIF_REQUEST);
        newFieldDialog.show(getSupportFragmentManager(), NewFieldDialogFragment.TAG);
    }

    private void setRepresentativeData(UserVSDto representativeData) {
        this.representative = representativeData;
        editorFragment.setEditorData(representativeData.getDescription());
        setRepresentativeImage(representativeData.getImageBytes(), null);
    }

    private void loadRepresentativeData(String representativeNif) {
        Toast.makeText(this, getString(R.string.loading_data_msg), Toast.LENGTH_SHORT).show();
        setProgressDialogVisible(true);
        Intent startIntent = new Intent(this, RepresentativeService.class);
        startIntent.putExtra(ContextVS.NIF_KEY, representativeNif);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.NIF_REQUEST);
        startService(startIntent);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.save_editor:
                if(validateForm()) {
                    editorContent = editorFragment.getEditorData();
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                            getString(R.string.enter_signature_pin_msg), false, null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); -> To select multiple images
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.select_img_lbl)), SELECT_PICTURE);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu", "");
        getMenuInflater().inflate(R.menu.text_editor, menu);
        this.menu = menu;
        if(operationType == TypeVS.REPRESENTATIVE && representative == null)
            this.menu.setGroupVisible(R.id.general_items, false);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean validateForm () {
        LOGD(TAG + ".validateForm", "validateForm");
        if(editorFragment == null || editorFragment.isEditorDataEmpty()) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.editor_empty_error_lbl), getSupportFragmentManager());
            return false;
        }
        if(representativeImageUri == null && operationType != TypeVS.REPRESENTATIVE) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.missing_representative_img_error_msg),
                    getSupportFragmentManager());
            return false;
        }
        return true;
    }

    //https://developer.android.com/guide/topics/providers/document-provider.html
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode); //Activity.RESULT_OK;
        if(SELECT_PICTURE == requestCode) {
            if(data != null && data.getData() != null) {
                Intent intent = new Intent(this, ConfirmImageActivity.class);
                intent.putExtra(ContextVS.URI_KEY, data.getData());
                startActivityForResult(intent, CONFIRM_PICTURE);
            }
        } else if(CONFIRM_PICTURE == requestCode) {
            if(Activity.RESULT_OK == resultCode) {
                representativeImageUri = data.getData();
                Cursor cursor = getContentResolver().query(
                        representativeImageUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    representativeImageName = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
                setRepresentativeImage(data.getByteArrayExtra(ContextVS.IMAGE_KEY), representativeImageName);
            }
        }
    }

    private void setRepresentativeImage(byte[] imageBytes, String imageName) {
        try {
            this.imageBytes = imageBytes;
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ImageView image = (ImageView)findViewById(R.id.representative_image);
            image.setImageBitmap(bitmap);
            TextView imagePathTextView = (TextView) findViewById(R.id.representative_image_path);
            ((TextView) findViewById(R.id.representative_image_caption)).setText(getString(
                    R.string.representative_image_lbl));
            imagePathTextView.setText(imageName);
            LinearLayout imageContainer = (LinearLayout) findViewById(R.id.imageContainer);
            imageContainer.setVisibility(View.VISIBLE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setRepresentativeImage(Uri imageUri, String imageName) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            ImageView image = (ImageView)findViewById(R.id.representative_image);
            image.setImageBitmap(bitmap);
            TextView imagePathTextView = (TextView) findViewById(R.id.representative_image_path);
            ((TextView) findViewById(R.id.representative_image_caption)).setText(getString(
                    R.string.representative_image_lbl));
            imagePathTextView.setText(imageName);
            LinearLayout imageContainer = (LinearLayout) findViewById(R.id.imageContainer);
            imageContainer.setVisibility(View.VISIBLE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ContextVS.URI_KEY, representativeImageUri);
        outState.putSerializable(ContextVS.ICON_KEY, representativeImageName);
        outState.putByteArray(ContextVS.IMAGE_KEY, imageBytes);
        LOGD(TAG + ".onSaveInstanceState", "outState: " + outState);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}