package org.votingsystem.signature.util;

import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class KeyStoreUtils {

    public static KeyStore getKeyStoreFromBytes(byte[] keyStore, char[] password)
            throws ExceptionVS {
    	try {
    		KeyStore store = KeyStore.getInstance(ContextVS.KEYSTORE_TYPE);
        	store.load(new ByteArrayInputStream(keyStore), password);
            return store;
    	} catch(Exception ex) {
    		throw new ExceptionVS(ex.getMessage(), ex);
    	}
    }
    
    public static byte[] getBytes (KeyStore keyStore, char[] password) throws Exception {
    	ByteArrayOutputStream baos  = new ByteArrayOutputStream();
    	keyStore.store(baos, password);
    	return baos.toByteArray();
    }

}