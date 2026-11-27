package com.jing.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jing.payment.service.PaymentService;
import com.jing.payment.service.gateway.AbaSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/aba")
@RequiredArgsConstructor
@Slf4j
public class AbaCallbackController {
    private final AbaSignatureVerifier verifier;
    private final PaymentService paymentService;
    private final ObjectMapper mapper;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(
            @RequestBody String raw,
            @RequestHeader(value = "X-PayWay-HMAC-SHA512", required = false) String signature) {
        JsonNode body = mapper.readTree(raw);
        if (verifier.isValid(body, signature)) {
            paymentService.processAbaCallback(body, raw);
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        // PayWay's docs don't say whether QR notifications are signed. An unsigned (or wrongly signed)
        // body is never trusted: it only tells us WHICH payment to look at, and the answer comes from
        // PayWay's own check-transaction API (server-to-server, signed by us).
        String tranId = body.path("tran_id").asText("");
        log.info("Unsigned PayWay notification for tran_id {}: verifying with check-transaction", tranId);
        paymentService.reconcileByTranId(tranId);
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}