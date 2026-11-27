package com.jing.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * payment.aba.* settings.
 *
 * @param returnUrl       public URL of POST /api/v1/payments/aba/callback (PayWay pushes results there)
 * @param rsaPublicKey    ABA's RSA public key for the Refund API (PEM or bare base64). Without it,
 *                        refunds are recorded as REQUESTED and wait until it is configured.
 * @param qrImageTemplate look of the QR image returned by the QR API
 */
@ConfigurationProperties(prefix = "payment.aba")
public record AbaProperties(
        String baseUrl,
        String merchantId,
        String apiKey,
        String returnUrl,
        String continueSuccessUrl,
        String rsaPublicKey,
        @DefaultValue("template3_color") String qrImageTemplate
) {
    public boolean refundsConfigured() {
        return rsaPublicKey != null && !rsaPublicKey.isBlank();
    }
}
