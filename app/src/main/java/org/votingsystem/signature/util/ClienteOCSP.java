package org.votingsystem.signature.util;

import org.bouncycastle2.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle2.ocsp.BasicOCSPResp;
import org.bouncycastle2.ocsp.CertificateID;
import org.bouncycastle2.ocsp.CertificateStatus;
import org.bouncycastle2.ocsp.OCSPReq;
import org.bouncycastle2.ocsp.OCSPReqGenerator;
import org.bouncycastle2.ocsp.OCSPResp;
import org.bouncycastle2.ocsp.RevokedStatus;
import org.bouncycastle2.ocsp.SingleResp;
import org.bouncycastle2.ocsp.UnknownStatus;
import org.votingsystem.dto.CertificateVSDto;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ClienteOCSP {

    public static CertificateVSDto.State validateCert(X509Certificate certificate, BigInteger serialNumber,
                 Date checkDate) throws Exception {
        OCSPReqGenerator ocspReqGen = new OCSPReqGenerator();
        ocspReqGen.addRequest(new CertificateID(CertificateID.HASH_SHA1, certificate, serialNumber));
        OCSPReq ocspReq = ocspReqGen.generate();
        URL url = new URL(ContextVS.OCSP_DNIE_URL);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", ContentTypeVS.OCSP_REQUEST.getName());
        con.setRequestProperty("Accept", ContentTypeVS.OCSP_RESPONSE.getName());
        con.setDoOutput(true);
        OutputStream out = con.getOutputStream();
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
        dataOut.write(ocspReq.getEncoded());
        dataOut.flush();
        dataOut.close();
        InputStream in = (InputStream) con.getContent();
        OCSPResp ocspResponse = new OCSPResp(in);
        BasicOCSPResp basicOCSPResp ;
        if(ocspResponse.getStatus() == OCSPResponseStatus.SUCCESSFUL) {
            CertificateVSDto.State certificateState = null;
            basicOCSPResp = (BasicOCSPResp) ocspResponse.getResponseObject();
            for(SingleResp singleResponse : basicOCSPResp.getResponses()){
                Object stat = singleResponse.getCertStatus();
                if (stat == CertificateStatus.GOOD) {
                    certificateState = CertificateVSDto.State.OK;
                }else if (stat instanceof RevokedStatus) {
                    Date fechaRevocacion = ((RevokedStatus)stat).getRevocationTime();
                    if (checkDate.after(fechaRevocacion)) 
                        certificateState = CertificateVSDto.State.CANCELLED;
                    else certificateState = CertificateVSDto.State.OK;
                }else if (stat instanceof UnknownStatus) {
                    certificateState = CertificateVSDto.State.UNKNOWN;
                }
            }
            return certificateState;
        } else return null;
    }

}
