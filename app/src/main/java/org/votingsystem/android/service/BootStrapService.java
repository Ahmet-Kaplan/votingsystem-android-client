package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.voting.AccessControlDto;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BootStrapService extends IntentService {

    public static final String TAG = BootStrapService.class.getSimpleName();

    private AppContextVS contextVS;
    private String serviceCaller;
    private Handler mHandler;

    public BootStrapService() {
        super(TAG);
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        final String accessControlURL = arguments.getString(ContextVS.ACCESS_CONTROL_URL_KEY);
        final String currencyServerURL = arguments.getString(ContextVS.CURRENCY_SERVER_URL);
        LOGD(TAG + ".onHandleIntent", "accessControlURL: " + accessControlURL +
                " - currencyServerURL: " + currencyServerURL);
        ResponseVS responseVS = null;
        if(contextVS.getAccessControl() == null) {
            responseVS = HttpHelper.getData(AccessControlDto.getServerInfoURL(accessControlURL),
                    ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    AccessControlDto accessControl = (AccessControlDto) responseVS.getMessage(AccessControlDto.class);
                    contextVS.setAccessControlVS(accessControl);
                } catch(Exception ex) {ex.printStackTrace();}
            } else {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                                accessControlURL), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        if(contextVS.getCurrencyServer() == null) {
            responseVS = HttpHelper.getData(ActorDto.getServerInfoURL(currencyServerURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    CurrencyServerDto currencyServer = (CurrencyServerDto) responseVS.getMessage(CurrencyServerDto.class);
                    contextVS.setCurrencyServerDto(currencyServer);
                } catch(Exception ex) {ex.printStackTrace();}
            } else {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                                currencyServerURL), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        if(!PrefUtils.isDataBootstrapDone(this)) {
            PrefUtils.markDataBootstrapDone(this);
            /*if(contextVS.getCurrencyServer() == null && contextVS.getAccessControl() == null) {
                intent = new Intent(getBaseContext(), IntentFilterActivity.class);
                responseVS.setCaption(getString(R.string.connection_error_msg));
                if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())
                    responseVS.setNotificationMessage(getString(R.string.conn_timeout_msg));
                intent.putExtra(RESPONSEVS_KEY, responseVS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }*/
        }
        if(responseVS == null) responseVS = new ResponseVS();
        responseVS.setServiceCaller(serviceCaller);
        contextVS.broadcastResponse(responseVS);
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}