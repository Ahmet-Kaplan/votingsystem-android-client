package org.votingsystem.signature.util;


import android.util.Base64;
import android.util.Log;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.BERConstructedOctetString;
import org.bouncycastle2.asn1.DERNull;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle2.asn1.cms.ContentInfo;
import org.bouncycastle2.asn1.cms.SignedData;
import org.bouncycastle2.asn1.cms.SignerInfo;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSProcessable;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedGenerator;
import org.bouncycastle2.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.ContextVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PDF_CMSSignedGenerator extends CMSSignedGenerator {
	
	public static final String TAG = "PDF_CMSSignedGenerator";
	
	public static String CERT_STORE_TYPE = "Collection";
               
    private PrivateKey privateKey = null;
    private X509Certificate userCert = null;
    private Certificate[] signerCertChain = null;
    private String signatureMechanism = null;
    private String pdfDigestObjectIdentifier = null;
    private String signatureDigestAlg = null;

    public PDF_CMSSignedGenerator (PrivateKey privateKey,
            Certificate[] signerCertChain, String signatureMechanism, 
            String signatureDigestAlg, String pdfDigestObjectIdentifier) throws Exception {
        this.privateKey = privateKey;
        this.signerCertChain = signerCertChain;
        this.signatureMechanism = signatureMechanism;
        this.pdfDigestObjectIdentifier = pdfDigestObjectIdentifier;
        this.signatureDigestAlg = signatureDigestAlg;
        this.userCert = (X509Certificate) signerCertChain[0];
    }

    private CMSSignedData genCMSSignedData(String eContentType,
            CMSProcessable content, boolean encapsulate, Provider sigProvider,
            boolean addDefaultAttributes, List<SignerInfo> signerInfoList)
            throws NoSuchAlgorithmException, CMSException, Exception {
// TODO if (signerInfs.isEmpty()){
//            /* RFC 3852 5.2
//             * "In the degenerate case where there are no signers, the
//             * EncapsulatedContentInfo value being "signed" is irrelevant.  In this
//             * case, the content type within the EncapsulatedContentInfo value being
//             * "signed" MUST be id-data (as defined in section 4), and the content
//             * field of the EncapsulatedContentInfo value MUST be omitted."
//             */
//            if (encapsulate) {
//                throw new IllegalArgumentException("no signers, encapsulate must be false");
//            } if (!DATA.equals(eContentType)) {
//                throw new IllegalArgumentException("no signers, eContentType must be id-data");
//            }
//        }
//        if (!DATA.equals(eContentType)) {
//            /* RFC 3852 5.3
//             * [The 'signedAttrs']...
//             * field is optional, but it MUST be present if the content type of
//             * the EncapsulatedContentInfo value being signed is not id-data.
//             */
//            // TODO signedAttrs must be present for all signers
//        }
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        digests.clear();  // clear the current preserved digest state
        Iterator it = _signers.iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSUtils.fixAlgID(signer.getDigestAlgorithmID()));
            signerInfos.add(signer.toSignerInfo());
        }
        boolean isCounterSignature = (eContentType == null);
        ASN1ObjectIdentifier contentTypeOID = isCounterSignature ?
            CMSObjectIdentifiers.data : new ASN1ObjectIdentifier(eContentType);
        for(SignerInfo signerInfo : signerInfoList) {
            try {
                digestAlgs.add(signerInfo.getDigestAlgorithm());
                signerInfos.add(signerInfo);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ASN1Set certificates = null;
        if (!certs.isEmpty()) certificates = CMSUtils.createBerSetFromList(certs);
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) certrevlist = CMSUtils.createBerSetFromList(crls);
        ASN1OctetString octs = null;
        if (encapsulate) {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            if (content != null) {
                try {
                    content.write(bOut);
                }
                catch (IOException e) {
                    throw new CMSException("encapsulation error.", e);
                }
            }
            octs = new BERConstructedOctetString(bOut.toByteArray());
        }
        ContentInfo encInfo = new ContentInfo(contentTypeOID, octs);
        SignedData  sd = new SignedData(new DERSet(digestAlgs), encInfo,
            certificates, certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(
            CMSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }

    public CMSSignedData genSignedData(byte[] signatureHash, 
            CMSAttributeTableGenerator unsAttr) throws Exception {
    	
        CMSProcessable content = new CMSProcessableByteArray(signatureHash);
        ByteArrayOutputStream out = null;
        if (content != null) {
        	out = new ByteArrayOutputStream();
            content.write(out);
            out.close();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
        MessageDigest softwareDigestEngine = MessageDigest.getInstance(signatureDigestAlg);
        int bytesRead;
        byte[] dataBuffer = new byte[4096];
        while ((bytesRead = bais.read(dataBuffer)) >= 0) {
          softwareDigestEngine.update(dataBuffer, 0, bytesRead);
        }
        byte[] hash = softwareDigestEngine.digest();
    	
        CertStore certsAndCRLs = CertStore.getInstance(CERT_STORE_TYPE,
                new CollectionCertStoreParameters(Arrays.asList(signerCertChain)), ContextVS.PROVIDER);
        addCertificatesAndCRLs(certsAndCRLs);
    	
    	CMSAttributeTableGenerator sAttr = new DefaultSignedAttributeTableGenerator();
    	
        ASN1ObjectIdentifier contentTypeOID = new ASN1ObjectIdentifier(CMSSignedGenerator.DATA);
        Map parameters = getBaseParameters(contentTypeOID, 
        		new AlgorithmIdentifier(new DERObjectIdentifier(pdfDigestObjectIdentifier), new DERNull()), hash);
        AttributeTable attributeTable = sAttr.getAttributes(Collections.unmodifiableMap(parameters));
    	
        String signatureHashStr = Base64.encodeToString(signatureHash, Base64.DEFAULT);
    	
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(ContextVS.ANDROID_PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(attributeTable);
        jcaSignerInfoGeneratorBuilder.setUnsignedAttributeGenerator(unsAttr);
        SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(
        		signatureMechanism, privateKey, userCert);
    	
        SignerInfo signerInfo = signerInfoGenerator.generate(contentTypeOID);
    	
        List<SignerInfo> signerInfoList = new ArrayList<SignerInfo>();
        signerInfoList.add(signerInfo);

        Log.d(TAG, " -- certificadoUsuario: " + userCert.getSubjectDN().getName());
        CMSSignedData signedData = genCMSSignedData(CMSSignedGenerator.DATA, 
                content, true, CMSUtils.getProvider("BC"), true, signerInfoList);
        //END SIGNED PKCS7
        return signedData;
    }




}
