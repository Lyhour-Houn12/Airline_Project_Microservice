package com.jing.payment.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AbaRsaTest {

    // longer than one RSA block, so it really is split into chunks
    private static final String JSON =
            "{\"mc_id\":\"ec476954\",\"tran_id\":\"1795772616392428\",\"refund_amount\":143.00,"
                    + "\"padding_to_make_it_longer_than_one_block\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}";

    @Test
    void chunksDecryptBackToTheJson_1024bitKey() throws Exception {
        roundTrip(1024);
    }

    @Test
    void chunksDecryptBackToTheJson_2048bitKey() throws Exception {
        roundTrip(2048);
    }

    @Test
    void acceptsPemWithHeadersAndEscapedNewlines() throws Exception {
        KeyPair keys = keyPair(1024);
        String body = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\\n" + body.substring(0, 64) + "\\n" + body.substring(64)
                + "\\n-----END PUBLIC KEY-----";

        assertThat(decrypt(AbaRsa.encryptChunked(JSON, pem), keys, 1024)).isEqualTo(JSON);
    }

    private static void roundTrip(int bits) throws Exception {
        KeyPair keys = keyPair(bits);
        String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

        String merchantAuth = AbaRsa.encryptChunked(JSON, publicKey);

        assertThat(decrypt(merchantAuth, keys, bits)).isEqualTo(JSON);
    }

    private static String decrypt(String base64, KeyPair keys, int bits) throws Exception {
        byte[] all = Base64.getDecoder().decode(base64);
        int block = bits / 8;
        assertThat(all.length % block).isZero();          // whole encrypted blocks, concatenated
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < all.length; i += block) {
            out.writeBytes(cipher.doFinal(all, i, block));
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static KeyPair keyPair(int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }
}
