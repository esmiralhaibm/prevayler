// Prevayler, The Free-Software Prevalence Layer
// Copyright 2001-2006 by Klaus Wuestefeld
//
// This library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// Prevayler is a trademark of Klaus Wuestefeld.
// See the LICENSE file for license details.

package org.prevayler.foundation.serialization;

import org.prevayler.foundation.Cool;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

public class DESSerializer<T> implements Serializer<T> {

    private ThreadLocal<Cipher> _ciphers = new ThreadLocal<Cipher>() {
        @Override protected Cipher initialValue() {
            try {
                return Cipher.getInstance(_triple ? "DESede" : "DES");
            } catch (GeneralSecurityException e) {
                // The method calling _ciphers.get() throws Exception anyway.
                throw Cool.unchecked(e);
            }
        }
    };

    private final Serializer<T> _delegate;

    private final SecretKey _key;

    private final boolean _triple;

    /**
     * @param key
     *            An 8-byte DES key or a 24-byte 3DES key.
     */
    public DESSerializer(Serializer<T> delegate, byte[] key) throws GeneralSecurityException {
        _delegate = delegate;
        if (key != null && key.length == 8) {
            _triple = false;
        } else if (key != null && key.length == 24) {
            _triple = true;
        } else {
            throw new IllegalArgumentException("Key must be 8 or 24 bytes");
        }
        KeySpec keySpec = _triple ? new DESedeKeySpec(key) : new DESKeySpec(key);
        _key = SecretKeyFactory.getInstance(_triple ? "DESede" : "DES").generateSecret(keySpec);
    }

    public void writeObject(OutputStream stream, T object) throws Exception {
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, _key);
        CipherOutputStream encrypt = new CipherOutputStream(stream, cipher);
        _delegate.writeObject(encrypt, object);
        encrypt.close();
    }

    public T readObject(InputStream stream) throws Exception {
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, _key);
        CipherInputStream decrypt = new CipherInputStream(stream, cipher);
        return _delegate.readObject(decrypt);
    }

    private Cipher getCipher() throws Exception {
        return _ciphers.get();
    }

}