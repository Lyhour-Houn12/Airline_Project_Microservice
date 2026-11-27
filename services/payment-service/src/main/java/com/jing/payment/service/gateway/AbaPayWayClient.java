package com.jing.payment.service.gateway;

import com.jing.payment.config.AbaProperties;
import com.jing.payment.payload.request.AbaTxn;
import com.jing.payment.utils.AbaHashUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class AbaPayWayClient {


    private static final DateTimeFormatter REQ_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AbaProperties props;
    private final RestClient rest;

    public AbaPayWayClient(AbaProperties props) {
        this.props = props;
        this.rest = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    public AbaTxn checkTransaction(String tranId) {
        String reqTime = REQ_TIME.format(Instant.now());
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
            return new AbaTxn(true, data.get("payment_status_code").asInt(),
                    data.path("total_amount").decimalValue(), data.path("apv").asText(null), res.toString());
        }
        if ("6".equals(code)) {                       // transaction not found
            return new AbaTxn(false, null, null, null, res.toString());
        }
        throw new IllegalStateException("PayWay check-transaction error: " + res);
    }
}
