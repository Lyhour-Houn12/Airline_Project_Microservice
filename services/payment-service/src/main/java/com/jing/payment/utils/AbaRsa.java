package com.jing.payment.utils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA for PayWay's Refund API ({@code merchant_auth}): the JSON is encrypted with ABA's public key in
 * chunks, the encrypted chunks are concatenated and the result is Base64 encoded.
 *
 * <p>Mirrors PayWay's PHP sample, {@code openssl_public_encrypt} with its default PKCS#1 v1.5 padding.
 * PKCS#1 v1.5 can encrypt at most (key size in bytes - 11) per block: 117 bytes for a 1024-bit key,
 * which is the chunk size in the sample. It is derived from the key here so a 2048-bit key works too.
 */
public final class AbaRsa {

    private static final int PKCS1_OVERHEAD = 11;

    private AbaRsa() {}

    public static String encryptChunked(String plaintext, String publicKey) {
        try {
            RSAPublicKey key = parsePublicKey(publicKey);
            int chunk = key.getModulus().bitLength() / 8 - PKCS1_OVERHEAD;
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int offset = 0; offset < data.length; offset += chunk) {
                int length = Math.min(chunk, data.length - offset);
                out.writeBytes(cipher.doFinal(data, offset, length));
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot encrypt the refund request with the ABA RSA key", e);
        }
    }

    /** Accepts a PEM ("-----BEGIN PUBLIC KEY-----...") or the bare Base64 body, with or without line breaks. */
    static RSAPublicKey parsePublicKey(String pemOrBase64) throws GeneralSecurityException {
        String base64 = pemOrBase64
                .replaceAll("-----BEGIN [A-Z ]*-----", "")
                .replaceAll("-----END [A-Z ]*-----", "")
                .replace("\\n", "")          // a key pasted into .env with literal \n
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
