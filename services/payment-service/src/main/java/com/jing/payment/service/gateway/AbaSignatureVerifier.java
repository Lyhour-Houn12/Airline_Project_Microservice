package com.jing.payment.service.gateway;

import com.jing.payment.config.AbaProperties;
import com.jing.payment.utils.AbaHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class AbaSignatureVerifier {
    private final AbaProperties props;

    public boolean isValid(JsonNode body, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isBlank()) return false;
        String expected = AbaHashUtil.hmacSha512Base64(AbaHashUtil.concatSorted(body), props.apiKey());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receivedSignature.trim().getBytes(StandardCharsets.UTF_8));
    }
}