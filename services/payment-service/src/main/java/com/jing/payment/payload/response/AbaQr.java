package com.jing.payment.payload.response;

/**
 * What the customer needs to pay by QR. Show {@code qrImage} on a screen, or {@code qrString} rendered
 * as a QR by the app; on a phone, open {@code abapayDeeplink} to jump straight into ABA Mobile.
 *
 * @param qrString       the KHQR / EMV payload itself (the "code" behind the QR)
 * @param qrImage        ready-made image as a data URL ({@code data:image/png;base64,...})
 * @param abapayDeeplink opens the payment in the ABA Mobile app
 * @param appStore       ABA Mobile on the App Store (when the app isn't installed)
 * @param playStore      ABA Mobile on Google Play
 */
public record AbaQr(
        String qrString,
        String qrImage,
        String abapayDeeplink,
        String appStore,
        String playStore
) {
}
