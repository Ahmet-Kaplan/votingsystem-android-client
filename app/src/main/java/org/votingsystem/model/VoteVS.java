package org.votingsystem.model;

import android.util.Log;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.FieldEventVSDto;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VoteVS extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

	public static final String TAG = VoteVS.class.getSimpleName();

    public enum State{OK, CANCELLED, ERROR}

    private Long localId = -1L;
    private transient SMIMEMessage voteReceipt;
    private transient SMIMEMessage cancelVoteReceipt;
    private transient TimeStampToken timeStampToken;
    private X509Certificate x509Certificate;
    private byte[] encryptedKey = null;
    private transient CertificationRequestVS certificationRequest;
    private PrivateKey certVotePrivateKey;
    private EventVSDto eventVS;
    private FieldEventVSDto optionSelected;
    private String voteUUID;
    private String representativeURL;
    private String accessControlURL;
    private String originHashCertVote;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private Date dateCreated;
    private Date dateUpdated;
    private Set<X509Certificate> serverCerts = new HashSet<X509Certificate>();

    public VoteVS () {}

    public VoteVS (EventVSDto eventVS, FieldEventVSDto optionSelected) {
        this.eventVS = eventVS;
        this.optionSelected = optionSelected;
    }

    public void genVote() throws NoSuchAlgorithmException {
        Log.d(TAG + ".genVote", "genVote");
        originHashAccessRequest = UUID.randomUUID().toString();
        setHashAccessRequestBase64(CMSUtils.getHashBase64(originHashAccessRequest,
                ContextVS.VOTING_DATA_DIGEST));
        originHashCertVote = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST);
    }

    public HashMap getVoteDataMap() {
        Log.d(TAG + ".getVoteDataMap", "getVoteDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.SEND_VOTE.toString());
        map.put("eventURL", eventVS.getURL());
        HashMap optionSelectedMap = new HashMap();
        optionSelectedMap.put("id", optionSelected.getId());
        optionSelectedMap.put("content", optionSelected.getContent());
        map.put("optionSelected", optionSelectedMap);
        map.put("UUID", UUID.randomUUID().toString());
        return new HashMap(map);
    }

    public AccessRequestDto getAccessRequest() {
        Log.d(TAG + ".getAccessRequestDto", "getAccessRequestDto");
        AccessRequestDto dto = new AccessRequestDto();
        dto.setHashAccessRequestBase64(hashAccessRequestBase64);
        dto.setEventId(eventVS.getId());
        dto.setEventURL(eventVS.getURL());
        dto.setUUID(UUID.randomUUID().toString());
        return dto;
    }

    public HashMap getCancelVoteDataMap() {
        Log.d(TAG + ".getCancelVoteDataMap", "getCancelVoteDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.CANCEL_VOTE.toString());
        map.put("originHashCertVote", originHashCertVote);
        map.put("hashCertVSBase64", getHashCertVSBase64());
        map.put("originHashAccessRequest", originHashAccessRequest);
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("eventURL", eventVS.getURL());
        HashMap dataMap = new HashMap(map);
        return dataMap;
    }

    public Map getDataMap() {
        Log.d(TAG + ".getDataMap", "getDataMap");
        Map resultMap = new HashMap();
        try {
            if(optionSelected != null) {
                HashMap opcionHashMap = new HashMap();
                opcionHashMap.put("id", optionSelected.getId());
                opcionHashMap.put("content", optionSelected.getContent());
                resultMap.put("optionSelected", opcionHashMap);
            }
            if(getHashCertVSBase64() != null) {
                resultMap.put("hashCertVSBase64", getHashCertVSBase64());
                resultMap.put("hashCertVoteHex", StringUtils.toHex(getHashCertVSBase64()));
            }
            if(getHashAccessRequestBase64() != null) {
                resultMap.put("hashAccessRequestBase64", getHashAccessRequestBase64());
                resultMap.put("hashSolicitudAccesoHex", StringUtils.toHex(getHashAccessRequestBase64()));
            }

            if (eventVS != null) resultMap.put("eventId", eventVS.getId());
            //map.put("UUID", UUID.randomUUID().toString());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return resultMap;
    }

    public void setOptionSelected(FieldEventVSDto optionSelected) {
        this.optionSelected = optionSelected;
    }

    public FieldEventVSDto getOptionSelected() {
        return optionSelected;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

	public SMIMEMessage getCancelVoteReceipt() {
        return cancelVoteReceipt;
	}

	public void setCancelVoteReceipt(SMIMEMessage cancelVoteReceipt) {
		this.cancelVoteReceipt = cancelVoteReceipt;
	}

    public SMIMEMessage getVoteReceipt() {
        return voteReceipt;
    }

    public void setVoteReceipt(SMIMEMessage voteReceipt) throws Exception {
        voteReceipt.isValidSignature();
        JSONObject receiptContentJSON = new JSONObject(voteReceipt.getSignedContent());
        JSONObject receiptOptionSelected = receiptContentJSON.getJSONObject("optionSelected");
        if(optionSelected.getId() != receiptOptionSelected.getLong("id") ||
                !optionSelected.getContent().equals(receiptOptionSelected.getString("content"))) {
            throw new Exception("Receipt option doesn't match vote option !!!");
        }
        if (!voteReceipt.isValidSignature()) {
            throw new Exception("Receipt with signature errors!!!");
        }
        this.voteReceipt = voteReceipt;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
	}

    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    @Override public Date getDateFrom() {
        return eventVS.getDateBegin();
    }

    @Override public Date getDateTo() {
        return eventVS.getDateFinish();
    }

    @Override public String getSubject() {
        return eventVS.getSubject();
    }

	public byte[] getEncryptedKey() {
		return encryptedKey;
	}

	public void setEncryptedKey(byte[] encryptedKey) {
		this.encryptedKey = encryptedKey;
	}

	public CertificationRequestVS getPkcs10WrapperClient() {
		return certificationRequest;
	}

	public void setCertificationRequest(CertificationRequestVS certificationRequest) {
		this.certificationRequest = certificationRequest;
	}

	public PrivateKey getCertVotePrivateKey() {
		return certVotePrivateKey;
	}

	public void setCertVotePrivateKey(PrivateKey certVotePrivateKey) {
		this.certVotePrivateKey = certVotePrivateKey;
	}

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public static VoteVS parse (Map eventMap) {
        VoteVS voteVS = null;
        try {
            voteVS = new VoteVS();
            EventVSDto eventVS = new EventVSDto();
            if(eventMap.containsKey("eventId")) {
                eventVS.setId(((Integer) eventMap.get("eventId")).longValue());
            }
            if(eventMap.containsKey("UUID")) {
                voteVS.setVoteUUID((String) eventMap.get("UUID"));
            }
            if(eventMap.containsKey("eventURL")) eventVS.setURL((String) eventMap.get("eventURL"));
            if(eventMap.containsKey("hashAccessRequestBase64")) voteVS.setHashAccessRequestBase64(
                    (String) eventMap.get("hashAccessRequestBase64"));
            if(eventMap.containsKey("optionSelectedId")) {
                FieldEventVSDto optionSelected = new FieldEventVSDto();
                optionSelected.setId(((Integer) eventMap.get("optionSelectedId")).longValue());
                if(eventMap.containsKey("optionSelectedContent")) {
                    optionSelected.setContent((String) eventMap.get("optionSelectedContent"));
                }
                voteVS.setOptionSelected(optionSelected);
            }
            if(eventMap.containsKey("optionSelected")) {
                voteVS.setOptionSelected(FieldEventVSDto.parse((Map) eventMap.get("optionSelected")));
            }
            voteVS.setEventVS(eventVS);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return voteVS;
    }

    public static VoteVS parse (JSONObject voteJSON) {
        VoteVS voteVS = null;
        try {
            voteVS = new VoteVS();
            EventVSDto eventVS = new EventVSDto();
            if(voteJSON.has("eventId")) {
                eventVS.setId(((Integer) voteJSON.get("eventId")).longValue());
            }
            if(voteJSON.has("UUID")) {
                voteVS.setVoteUUID((String) voteJSON.get("UUID"));
            }
            if(voteJSON.has("eventURL")) eventVS.setURL((String) voteJSON.get("eventURL"));
            if(voteJSON.has("hashAccessRequestBase64")) voteVS.setHashAccessRequestBase64(
                    (String) voteJSON.get("hashAccessRequestBase64"));
            if(voteJSON.has("optionSelectedId")) {
                FieldEventVSDto optionSelected = new FieldEventVSDto();
                optionSelected.setId(((Integer) voteJSON.get("optionSelectedId")).longValue());
                if(voteJSON.has("optionSelectedContent")) {
                    optionSelected.setContent((String) voteJSON.get("optionSelectedContent"));
                }
                voteVS.setOptionSelected(optionSelected);
            }
            if(voteJSON.has("optionSelected")) {
                voteVS.setOptionSelected(FieldEventVSDto.parse((Map) voteJSON.get("optionSelected")));
            }
            voteVS.setEventVS(eventVS);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return voteVS;
    }

    private void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    private void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public X509Certificate getX509Certificate() { return x509Certificate; }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getVoteUUID() {
        return voteUUID;
    }

    public void setVoteUUID(String voteUUID) {
        this.voteUUID = voteUUID;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(voteReceipt != null) s.writeObject(voteReceipt.getBytes());
            else s.writeObject(null);
            if(cancelVoteReceipt != null) s.writeObject(cancelVoteReceipt.getBytes());
            else s.writeObject(null);
            s.writeObject(eventVS);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] voteReceiptBytes = (byte[]) s.readObject();
        if(voteReceiptBytes != null) voteReceipt = new SMIMEMessage(
                new ByteArrayInputStream(voteReceiptBytes));
        byte[] cancelVoteReceiptBytes = (byte[]) s.readObject();
        if(cancelVoteReceiptBytes != null) cancelVoteReceipt = new SMIMEMessage(
                new ByteArrayInputStream(cancelVoteReceiptBytes));
    }


    public static VoteVS getInstance(Map contentMap, X509Certificate x509Certificate,
            TimeStampToken timeStampToken) throws IOException, JSONException {
        VoteVS voteVS = VoteVS.parse(contentMap);
        voteVS.setTimeStampToken(timeStampToken);
        voteVS.setX509Certificate(x509Certificate);
        byte[] voteExtensionValue = x509Certificate.getExtensionValue(ContextVS.VOTE_OID);
        if(voteExtensionValue != null) {
            DERTaggedObject voteCertDataDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(voteExtensionValue);
            JSONObject voteCertData = new JSONObject(((DERUTF8String) voteCertDataDER.getObject()).toString());
            EventVSDto eventVS = new EventVSDto();
            eventVS.setId(Long.valueOf(voteCertData.getString("eventId")));
            voteVS.setEventVS(eventVS);
            voteVS.setAccessControlURL(voteCertData.getString("accessControlURL"));
            voteVS.setHashCertVSBase64(voteCertData.getString("hashCertVS"));
        }
        byte[] representativeURLExtensionValue = x509Certificate.getExtensionValue(ContextVS.REPRESENTATIVE_VOTE_OID);
        if(representativeURLExtensionValue != null) {
            DERTaggedObject representativeURL_DER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    representativeURLExtensionValue);
            voteVS.setRepresentativeURL(((DERUTF8String)representativeURL_DER.getObject()).toString());
        }
        return voteVS;
    }

    public SMIMEMessage getReceipt() throws Exception {
        switch(getTypeVS()) {
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                cancelVoteReceipt.isValidSignature();
                return cancelVoteReceipt;
            case VOTEVS:
                voteReceipt.isValidSignature();
                return voteReceipt;
            default: return null;
        }
    }

    public String getMessageId() {
        String result = null;
        try {
            SMIMEMessage receipt = getReceipt();
            if(receipt != null) {
                String[] headers = receipt.getHeader("Message-ID");
                if(headers != null && headers.length >0) return headers[0];
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public Set<X509Certificate> getServerCerts() {
        return serverCerts;
    }

    public void setServerCerts(Set<X509Certificate> serverCerts) {
        this.serverCerts = serverCerts;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }
}