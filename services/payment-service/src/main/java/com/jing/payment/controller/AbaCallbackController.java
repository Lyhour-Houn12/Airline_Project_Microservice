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
        if (!verifier.isValid(body, signature)) {
            log.warn("Rejected PayWay callback with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid signature"));
        }
        paymentService.processAbaCallback(body, raw);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}