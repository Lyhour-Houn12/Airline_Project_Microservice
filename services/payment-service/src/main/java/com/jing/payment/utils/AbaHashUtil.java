package com.jing.payment.utils;

import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.TreeMap;

public final class AbaHashUtil {

    private AbaHashUtil(){}

    public static String hmacSha512Base64(String data, String key){
        try{
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        }catch (GeneralSecurityException e){
            throw new IllegalStateException("Cannot compute HMAC", e);
        }
    }

    public static String formatAmount(BigDecimal amount, String currency){
        return "KHR".equalsIgnoreCase(currency)
                ? amount.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Callback signing string: sort fields by key, concatenate values (mirrors PayWay's PHP sample). */
    public static String concatSorted(JsonNode body) {
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        body.properties().forEach(e -> sorted.put(e.getKey(), e.getValue()));
        StringBuilder sb = new StringBuilder();
        sorted.values().forEach(v -> sb.append(phpString(v)));
        return sb.toString();
    }


    private static String phpString(JsonNode v) {
        if (v == null || v.isNull()) return "";
        if (v.isBoolean()) return v.asBoolean() ? "1" : "";
        if (v.isNumber()) {
            BigDecimal d = v.decimalValue();
            return d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString();
        }
        if (v.isArray() || v.isObject()) return v.toString();
        return v.asString();
    }
}
