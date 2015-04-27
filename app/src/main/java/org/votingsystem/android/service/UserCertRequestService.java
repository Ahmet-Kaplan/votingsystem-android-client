package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.votingsystem.android.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.ContextVS.CALLER_KEY;
import static org.votingsystem.util.ContextVS.DEVICE_ID_KEY;
import static org.votingsystem.util.ContextVS.EMAIL_KEY;
import static org.votingsystem.util.ContextVS.KEY_SIZE;
import static org.votingsystem.util.ContextVS.NAME_KEY;
import static org.votingsystem.util.ContextVS.NIF_KEY;
import static org.votingsystem.util.ContextVS.PHONE_KEY;
import static org.votingsystem.util.ContextVS.PIN_KEY;
import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.ContextVS.SIG_NAME;
import static org.votingsystem.util.ContextVS.SURNAME_KEY;
import static org.votingsystem.util.ContextVS.State;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserCertRequestService extends IntentService {

    public static final String TAG = UserCertRequestService.class.getSimpleName();

    public UserCertRequestService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        LOGD(TAG + ".onHandleIntent", "arguments: " + arguments);
        AppVS contextVS = (AppVS) getApplicationContext();
        String serviceCaller = arguments.getString(CALLER_KEY);
        ResponseVS responseVS = null;
        try {
            String nif = arguments.getString(NIF_KEY);
            String email = arguments.getString(EMAIL_KEY);
            String phone = arguments.getString(PHONE_KEY);
            String deviceId = arguments.getString(DEVICE_ID_KEY);
            String givenName = arguments.getString(NAME_KEY);
            String surname = arguments.getString(SURNAME_KEY);
            String pin = arguments.getString(PIN_KEY);
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    KEY_SIZE, SIG_NAME, SIGNATURE_ALGORITHM, PROVIDER, nif, email, phone, deviceId,
                    givenName, surname, DeviceVSDto.Type.MOBILE);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            responseVS = HttpHelper.sendData(csrBytes, null,
                    contextVS.getAccessControl().getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                certificationRequest.setHashPin(CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST));
                PrefUtils.putCsrRequest(requestId, certificationRequest, this);
                PrefUtils.putAppCertState(contextVS.getAccessControl().getServerURL(),
                        State.WITH_CSR, null, this);
                responseVS.setCaption(getString(R.string.operation_ok_msg));
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch (Exception ex){
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller);
            contextVS.broadcastResponse(responseVS);
        }
    }

    private void requestDeviceVSId() {

    }
}
