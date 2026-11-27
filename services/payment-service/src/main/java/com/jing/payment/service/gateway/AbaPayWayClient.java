package com.jing.payment.service.gateway;

import com.jing.payment.config.AbaProperties;
import com.jing.payment.payload.request.AbaTxn;
import com.jing.payment.payload.response.AbaQr;
import com.jing.payment.payload.response.AbaRefundResult;
import com.jing.payment.utils.AbaHashUtil;
import com.jing.payment.utils.AbaRsa;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server-to-server calls to ABA PayWay. Every request is signed with HMAC-SHA512 (the API key). */
@Component
public class AbaPayWayClient {

    private static final DateTimeFormatter REQ_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AbaProperties props;
    private final RestClient rest;
    private final ObjectMapper mapper;

    public AbaPayWayClient(AbaProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.rest = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    public static String now() {
        return REQ_TIME.format(Instant.now());
    }

    public AbaTxn checkTransaction(String tranId) {
        String reqTime = now();
        String hash = AbaHashUtil.hmacSha512Base64(reqTime + props.merchantId() + tranId, props.apiKey());
        JsonNode res = rest.post()
                .uri("/api/payment-gateway/v1/payments/check-transaction-2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("req_time", reqTime, "merchant_id", props.merchantId(),
                        "tran_id", tranId, "hash", hash))
                .retrieve()
                .body(JsonNode.class);
        if (res == null) throw new IllegalStateException("Empty response from PayWay");

        String code = res.path("status").path("code").asText("");
        JsonNode data = res.path("data");
        if ("00".equals(code) && data.hasNonNull("payment_status_code")) {
            BigDecimal refunded = data.hasNonNull("refund_amount") ? data.get("refund_amount").decimalValue() : BigDecimal.ZERO;
            return new AbaTxn(true, data.get("payment_status_code").asInt(),
                    data.path("total_amount").decimalValue(), data.path("apv").asText(null), refunded, res.toString());
        }
        if ("6".equals(code)) {                       // transaction not found
            return new AbaTxn(false, null, null, null, BigDecimal.ZERO, res.toString());
        }
        throw new IllegalStateException("PayWay check-transaction error: " + res);
    }

    /**
     * QR API. {@code fields} must already be in PayWay's hash order (see AbaPayWayGateway); the hash
     * is added here. Fields that are empty are left out of the body, they add nothing to the hash.
     */
    public AbaQr generateQr(Map<String, String> fields, BigDecimal amount, int lifetimeMinutes) {
        String hash = AbaHashUtil.hmacSha512Base64(String.join("", fields.values()), props.apiKey());

        Map<String, Object> body = new LinkedHashMap<>();
        fields.forEach((k, v) -> { if (!v.isEmpty()) body.put(k, v); });
        body.put("amount", amount);                  // a JSON number, as the API expects
        body.put("lifetime", lifetimeMinutes);       // a JSON integer
        body.put("hash", hash);

        JsonNode res = rest.post()
                .uri("/api/payment-gateway/v1/payments/generate-qr")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (res == null) throw new IllegalStateException("Empty response from PayWay");

        String code = res.path("status").path("code").asText("");
        if (!"0".equals(code)) {
            throw new AbaApiException(code, res.path("status").path("message").asText("QR request rejected"));
        }
        return new AbaQr(res.path("qrString").asText(null), res.path("qrImage").asText(null),
                res.path("abapay_deeplink").asText(null), res.path("app_store").asText(null),
                res.path("play_store").asText(null));
    }

    /** Refund API: full or partial refund of a completed transaction (within 30 days). */
    public AbaRefundResult refund(String tranId, BigDecimal refundAmount) {
        if (!props.refundsConfigured()) {
            throw new IllegalStateException("ABA_RSA_PUBLIC_KEY is not configured");
        }
        String requestTime = now();
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("mc_id", props.merchantId());
        auth.put("tran_id", tranId);
        auth.put("refund_amount", refundAmount);
        String merchantAuth = AbaRsa.encryptChunked(mapper.writeValueAsString(auth), props.rsaPublicKey());
        String hash = AbaHashUtil.hmacSha512Base64(requestTime + props.merchantId() + merchantAuth, props.apiKey());

        JsonNode res = rest.post()
                .uri("/api/merchant-portal/merchant-access/online-transaction/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("request_time", requestTime, "merchant_id", props.merchantId(),
                        "merchant_auth", merchantAuth, "hash", hash))
                .retrieve()
                .body(JsonNode.class);
        if (res == null) throw new IllegalStateException("Empty response from PayWay");

        // the docs list the totals next to "status"; accept them inside "data" too
        JsonNode d = res.hasNonNull("data") ? res.get("data") : res;
        return new AbaRefundResult(
                res.path("status").path("code").asText(""),
                res.path("status").path("message").asText(null),
                d.hasNonNull("total_refunded") ? d.get("total_refunded").decimalValue() : null,
                d.path("transaction_status").asText(null),
                res.toString());
    }

    /** PayWay answered, but with an error status code. */
    public static class AbaApiException extends RuntimeException {
        private final String code;

        public AbaApiException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
