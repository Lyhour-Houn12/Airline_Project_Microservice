package com.jing.common.enumeration;

public enum PaymentMethod {
    /** ABA PayWay hosted checkout page (cards, ABA Pay, KHQR chosen on PayWay's page). */
    ABA_PAYWAY,
    /** ABA PayWay QR API: we show the QR ourselves (ABA Pay / KHQR, WeChat, Alipay). */
    ABA_PAYWAY_QR,
    /** National Bakong KHQR (not integrated yet). */
    BAKONG_KHQR
}
