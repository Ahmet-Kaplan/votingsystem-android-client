package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.RepresentationStateFragment;
import org.votingsystem.fragment.RepresentativeGridFragment;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativesMainActivity extends ActivityBase {

	public static final String TAG = RepresentativesMainActivity.class.getSimpleName();

    private AppVS appVS = null;
    private String broadCastId = RepresentativesMainActivity.class.getSimpleName();
    private WeakReference<RepresentativeGridFragment> representativeGridRef;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null)
            launchRepresentativeService(responseVS.getTypeVS());
        else {
            setProgressDialogVisible(null, null, false);
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION == responseVS.getTypeVS()) {
                MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
            } else if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
            }
        }
        }
    };

    private void launchRepresentativeService(TypeVS operationType) {
        LOGD(TAG + ".revokeRepresentative", "operationType: " + operationType.toString());
        Intent startIntent = new Intent(this, RepresentativeService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        String caption = null;
        switch(operationType) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                caption = getString(R.string.cancel_anonymouys_representation_lbl);
                break;
        }
        setProgressDialogVisible(caption, getString(R.string.wait_msg), true);
        startService(startIntent);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        appVS = (AppVS) getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.representatives_drop_down_lbl));
        RepresentationStateFragment fragment = new RepresentationStateFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                fragment, RepresentationStateFragment.TAG).commit();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        AlertDialog.Builder builder = null;
        switch (item.getItemId()) {
            case R.id.cancel_anonymouys_representation:
                builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.cancel_anonymouys_representation_lbl),
                        getString(R.string.cancel_anonymouys_representation_msg), this);
                builder.setPositiveButton(getString(R.string.continue_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                PinDialogFragment.showPinScreen(getSupportFragmentManager(),
                                        broadCastId, getString(R.string.enter_signature_pin_msg),
                                        false, TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION);
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl), null);
                UIUtils.showMessageDialog(builder);
                return true;
            case R.id.representative_list:
                Intent intent = new Intent(this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, RepresentativeGridFragment.class.getName());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.removeGroup(R.id.general_items);
        inflater.inflate(R.menu.representative_main, menu);
        RepresentationStateDto representation = PrefUtils.getRepresentationState(this);
        if(representation != null) {
            switch(representation.getState()) {
                case REPRESENTATIVE:
                    menu.removeGroup(R.id.options_for_uservs);
                    break;
                case WITH_PUBLIC_REPRESENTATION:
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
                case WITHOUT_REPRESENTATION:
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
            }
        } else {
            menu.removeGroup(R.id.options_for_uservs);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if (isVisible) ProgressDialogFragment.showDialog(
                caption, message, getSupportFragmentManager());
        else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Representatives mode.
        return NAVDRAWER_ITEM_REPRESENTATIVES;
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
        RepresentativeGridFragment fragment = representativeGridRef.get();
        fragment.fetchItems(fragment.getOffset());
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}