package com.jing.payment.service.gateway;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.jing.common.exception.BadRequestException;
import com.jing.payment.config.AbaProperties;
import com.jing.payment.model.Payment;
import com.jing.payment.payload.request.AbaCheckout;
import com.jing.payment.payload.request.CreatePaymentRequest;
import com.jing.payment.utils.AbaHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AbaPayWayGateway {

    private static final String PURCHASE_PATH = "/api/payment-gateway/v1/payments/purchase";
    private static final DateTimeFormatter REQ_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AbaProperties props;
    private final ObjectMapper mapper;

    public AbaCheckout buildCheckout(Payment p, CreatePaymentRequest r){
        String amount = AbaHashUtil.formatAmount(p.getAmount(), p.getCurrency());

        // Insertion order == hash order. Unused optional fields stay empty, so they add nothing to the string.
        Map<String, String> f = new LinkedHashMap<>();
        f.put("req_time", REQ_TIME.format(Instant.now()));
        f.put("merchant_id", props.merchantId());
        f.put("tran_id", p.getTranId());
        f.put("amount", amount);
        f.put("items", buildItems(p, amount));
        f.put("shipping", "");
        f.put("firstname", clean(r.firstName(), 20));
        f.put("lastname", clean(r.lastName(), 20));
        f.put("email", r.email() == null ? "" : r.email());
        f.put("phone", cleanPhone(r.phone()));
        f.put("type", "purchase");
        f.put("payment_option", r.paymentOption() == null ? "" : r.paymentOption());
        f.put("return_url", b64(props.returnUrl()));
        f.put("cancel_url", "");
        f.put("continue_success_url", props.continueSuccessUrl() == null ? "" : props.continueSuccessUrl());
        f.put("return_deeplink", "");
        f.put("currency", p.getCurrency());
        f.put("custom_fields", "");
        f.put("return_params", String.valueOf(p.getId()));

        String hash = AbaHashUtil.hmacSha512Base64(String.join("", f.values()), props.apiKey());
        f.put("hash", hash);

        // Post only non-empty fields (payment_option is always posted, as in PayWay's sample)
        Map<String, String> out = new LinkedHashMap<>();
        f.forEach((k, v) -> { if (!v.isEmpty() || k.equals("payment_option")) out.put(k, v); });

        // ABA: card transactions need payment_gate = 0
        if ("cards".equalsIgnoreCase(r.paymentOption())) {
            out.put("payment_gate", "0");
        }

        return new AbaCheckout(props.baseUrl() + PURCHASE_PATH, out);
    }


    /** QR payment options and the currencies PayWay accepts for each. */
    private static final Map<String, Set<String>> QR_OPTIONS = Map.of(
            "abapay_khqr", Set.of("USD", "KHR"),
            "wechat", Set.of("USD"),
            "alipay", Set.of("USD"));

    /**
     * Fields of the QR API in PayWay's hash order: req_time, merchant_id, tran_id, amount, items,
     * first_name, last_name, email, phone, purchase_type, payment_option, callback_url,
     * return_deeplink, currency, custom_fields, return_params, payout, lifetime, qr_image_template.
     * Unused optional fields stay empty, so they add nothing to the hash string.
     */
    public Map<String, String> buildQrFields(Payment p, CreatePaymentRequest r, int lifetimeMinutes) {
        String option = qrOption(r.paymentOption(), p.getCurrency());
        String amount = AbaHashUtil.formatAmount(p.getAmount(), p.getCurrency());

        Map<String, String> f = new LinkedHashMap<>();
        f.put("req_time", AbaPayWayClient.now());
        f.put("merchant_id", props.merchantId());
        f.put("tran_id", p.getTranId());
        f.put("amount", amount);
        f.put("items", buildItems(p, amount));
        f.put("first_name", clean(r.firstName(), 20));
        f.put("last_name", clean(r.lastName(), 20));
        f.put("email", r.email() == null ? "" : r.email());
        f.put("phone", cleanPhone(r.phone()));
        f.put("purchase_type", "purchase");
        f.put("payment_option", option);
        f.put("callback_url", props.returnUrl() == null || props.returnUrl().isBlank() ? "" : b64(props.returnUrl()));
        f.put("return_deeplink", "");
        f.put("currency", p.getCurrency());
        f.put("custom_fields", "");
        f.put("return_params", String.valueOf(p.getId() != null ? p.getId() : p.getTranId()));
        f.put("payout", "");
        f.put("lifetime", String.valueOf(lifetimeMinutes));
        f.put("qr_image_template", props.qrImageTemplate());
        return f;
    }

    /** Default ABA Pay / KHQR; WeChat and Alipay only in USD. */
    public static String qrOption(String requested, String currency) {
        String option = requested == null || requested.isBlank() ? "abapay_khqr" : requested.trim().toLowerCase();
        Set<String> currencies = QR_OPTIONS.get(option);
        if (currencies == null) {
            throw new BadRequestException("paymentOption for QR must be one of " + QR_OPTIONS.keySet());
        }
        if (!currencies.contains(currency)) {
            throw new BadRequestException(option + " QR payments are only possible in " + currencies);
        }
        return option;
    }

    private String buildItems(Payment p, String amount) {
        var items = List.of(Map.of("name", "Flight booking " + p.getBookingReference(),
                "quantity", "1", "price", amount));
        return b64(mapper.writeValueAsString(items));
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String clean(String s, int max) {
        if (s == null) return "";
        String c = s.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        return c.length() > max ? c.substring(0, max) : c;
    }

    private static String cleanPhone(String s) {
        if (s == null) return "";
        String d = s.replaceAll("[^0-9]", "");
        return d.length() >= 8 && d.length() <= 20 ? d : "";
    }

}
