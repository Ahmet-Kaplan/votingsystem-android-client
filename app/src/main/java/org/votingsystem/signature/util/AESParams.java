package org.votingsystem.signature.util;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONException;
import org.votingsystem.dto.AESParamsDto;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AESParams {

    private Key key;
    private IvParameterSpec iv;

    public AESParams() throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        iv = new IvParameterSpec(random.generateSeed(16));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(random);
        key = kg.generateKey();
    }

    public Key getKey() {
        return key;
    }

    public IvParameterSpec getIV() {
        return iv;
    }

    public AESParamsDto getDto() throws UnsupportedEncodingException {
        return new AESParamsDto(new String(Base64.encode(key.getEncoded()), "UTF-8"),
                new String(Base64.encode(iv.getIV()), "UTF-8"));
    }

    public static AESParams load(AESParamsDto dto) throws NoSuchAlgorithmException,
            JSONException {
        AESParams aesParams = new AESParams();
        byte[] decodeKeyBytes = Base64.decode(dto.getKey().getBytes());
        aesParams.key = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        byte[] iv = Base64.decode(dto.getIv().getBytes());
        aesParams.iv = new IvParameterSpec(iv);
        return aesParams;
    }

}