package com.jing.payment.service.gateway;

import com.jing.common.enumeration.PaymentMethod;
import com.jing.common.exception.BadRequestException;
import com.jing.payment.config.AbaProperties;
import com.jing.payment.model.Payment;
import com.jing.payment.payload.request.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbaQrFieldsTest {

    private final AbaProperties props = new AbaProperties("https://checkout-sandbox.payway.com.kh", "merchant1",
            "api-key", "https://example.com/api/v1/payments/aba/callback", null, null, "template3_color");
    private final AbaPayWayGateway gateway = new AbaPayWayGateway(props, JsonMapper.builder().build());

    @Test
    void fieldsAreInPayWaysHashOrder() {
        Payment p = Payment.builder().id(8L).tranId("1795772616392428").bookingReference("R3D8CC")
                .amount(new BigDecimal("143")).currency("USD").build();
        CreatePaymentRequest r = new CreatePaymentRequest(7L, PaymentMethod.ABA_PAYWAY_QR,
                "Test", "Adult", null, null, null);

        Map<String, String> f = gateway.buildQrFields(p, r, 15);

        // the order documented for the QR API hash
        assertThat(List.copyOf(f.keySet())).containsExactly(
                "req_time", "merchant_id", "tran_id", "amount", "items", "first_name", "last_name",
                "email", "phone", "purchase_type", "payment_option", "callback_url", "return_deeplink",
                "currency", "custom_fields", "return_params", "payout", "lifetime", "qr_image_template");
        assertThat(f.get("amount")).isEqualTo("143.00");
        assertThat(f.get("payment_option")).isEqualTo("abapay_khqr");
        assertThat(f.get("lifetime")).isEqualTo("15");
        assertThat(f.get("return_params")).isEqualTo("8");
    }

    @Test
    void wechatAndAlipayOnlyInUsd() {
        assertThat(AbaPayWayGateway.qrOption("WeChat", "USD")).isEqualTo("wechat");
        assertThat(AbaPayWayGateway.qrOption(null, "KHR")).isEqualTo("abapay_khqr");
        assertThatThrownBy(() -> AbaPayWayGateway.qrOption("alipay", "KHR")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> AbaPayWayGateway.qrOption("cards", "USD")).isInstanceOf(BadRequestException.class);
    }
}
