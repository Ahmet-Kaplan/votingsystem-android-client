package org.votingsystem.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.votingsystem.android.R;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PaymentFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.QRGeneratorFormFragment;
import org.votingsystem.util.Utils;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRCodesActivity extends ActivityBase {

	public static final String TAG = QRCodesActivity.class.getSimpleName();

    Button read_qr_btn;
    Button gen_qr_btn;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_codes_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        read_qr_btn = (Button) findViewById(R.id.read_qr_btn);
        read_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Utils.launchQRScanner(QRCodesActivity.this);
            }
        });
        gen_qr_btn = (Button) findViewById(R.id.gen_qr_btn);
        gen_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(QRCodesActivity.this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, QRGeneratorFormFragment.class.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
        getSupportActionBar().setTitle(getString(R.string.qr_codes_lbl));
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null && result.getContents() != null) {
            if(result.getContents().toLowerCase().contains("http://") ||
                    result.getContents().toLowerCase().contains("https://")) {
                new GetDataTask(null).execute(result.getContents());
            } else {
                LOGD(TAG, "QR reader - onActivityResult - socket operation UUID: " + result.getContents());
            }
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Representatives mode.
        return NAVDRAWER_ITEM_QR_CODES;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        public final String TAG = GetDataTask.class.getSimpleName();

        private ContentTypeVS contentType = null;
        private String infoURL;

        public GetDataTask(ContentTypeVS contentType) {
            this.contentType = contentType;
        }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true);
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "url: " + urls[0]);
            infoURL = urls[0];
            return  HttpHelper.getData(urls[0], contentType);
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            LOGD(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            setProgressDialogVisible(false);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    TransactionVSDto dto = (TransactionVSDto) responseVS.getMessage(TransactionVSDto.class);
                    dto.setInfoURL(infoURL);
                    switch (dto.getOperation()) {
                        case PAYMENT:
                        case PAYMENT_REQUEST:
                        case DELIVERY_WITHOUT_PAYMENT:
                        case DELIVERY_WITH_PAYMENT:
                        case REQUEST_FORM:
                            Intent intent = new Intent(QRCodesActivity.this,
                                    FragmentContainerActivity.class);
                            intent.putExtra(ContextVS.FRAGMENT_KEY, PaymentFragment.class.getName());
                            intent.putExtra(ContextVS.TRANSACTION_KEY,
                                    JSON.getMapper().writeValueAsString(dto));
                            startActivity(intent);
                            break;
                    }
                } catch (Exception e) { e.printStackTrace();}
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    responseVS.getMessage(), getSupportFragmentManager());
        }
    }

}