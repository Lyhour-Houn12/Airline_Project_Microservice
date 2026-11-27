# Payment Service

Port 8000, database `payment_db`. Takes payments for bookings through **ABA PayWay**, and refunds them.
The amount always comes from booking-service, never from the client.

## Paying

`POST /api/v1/payments`

```json
{ "bookingId": 7, "method": "ABA_PAYWAY_QR", "paymentOption": "abapay_khqr",
  "firstName": "Test", "lastName": "Adult", "email": "a@example.com", "phone": "012345678" }
```

| `method` | What you get back | How the customer pays |
|---|---|---|
| `ABA_PAYWAY` | `checkout`: an action URL + form fields | Post the fields to PayWay's hosted page (cards, ABA Pay, KHQR) |
| `ABA_PAYWAY_QR` | `qr`: `qrImage` (data URL), `qrString` (the KHQR code itself), `abapayDeeplink`, `appStore`, `playStore` | Show the QR on screen, or on a phone open the deeplink into ABA Mobile |

QR `paymentOption`: `abapay_khqr` (default, USD and KHR), `wechat` and `alipay` (USD only).
The QR lives as long as the payment window (and the booking's hold), at least 3 minutes.

While the customer pays, poll `GET /api/v1/payments/{id}` (or `POST /api/v1/payments/{id}/sync` to ask PayWay now).

## How a result arrives

1. **Callback** `POST /api/v1/payments/aba/callback`, the URL you set as `ABA_RETURN_URL`.
   A correctly signed callback (`X-PayWay-HMAC-SHA512`) is processed, amount and currency are checked,
   then confirmed with PayWay. An unsigned one (PayWay doesn't document whether QR pushes are signed) is
   **never trusted**: it only triggers a server-to-server `check-transaction`.
2. **Reconciliation job** (every `payment.reconcile-every-millis`, 1 min): PENDING payments older than a
   minute are checked with PayWay. Paid ones become SUCCESS; unpaid ones EXPIRE after their window.
3. On SUCCESS booking-service is told (`/internal/v1/bookings/{id}/payment-result`): the booking is
   confirmed, seats booked, tickets issued. If the booking can no longer be confirmed (expired, cancelled,
   already paid), this payment is **refunded automatically**.

## Refunds

| Endpoint | Who |
|---|---|
| `POST /api/v1/payments/{id}/refunds` `{ "amount": 40.00, "reason": "..." }` (omit amount = all that's left) | admin |
| `GET /api/v1/payments/{id}/refunds` | owner, admin |
| `POST /internal/v1/payments/refunds` `{ "bookingId": 7 }` | booking-service, when a paid booking is cancelled |

- PayWay rules: completed payments only, **within 30 days**, partial refunds allowed up to the amount paid.
- `REQUESTED -> SUCCEEDED` (payment becomes PARTIALLY_REFUNDED / REFUNDED; at REFUNDED the booking becomes REFUNDED),
  or `FAILED` for a final PayWay error. Busy / low balance / network errors stay REQUESTED and are retried
  (`payment.refund-retry-millis`, 5 min).
- **Never twice:** the attempt is committed before PayWay is called; a retry first asks `check-transaction`
  how much was already refunded.
- The Refund API needs **`ABA_RSA_PUBLIC_KEY`**, an RSA public key ABA Bank issues for refunds (ask PayWay
  for it). Until it's set, refunds are recorded as REQUESTED with "Waiting: ABA_RSA_PUBLIC_KEY is not configured".

## Settings (`.env`, see `.env.example`)

| Variable | |
|---|---|
| `ABA_MERCHANT_ID`, `ABA_API_KEY` | from ABA |
| `ABA_RSA_PUBLIC_KEY` | refunds (optional; refunds wait without it) |
| `ABA_RETURN_URL` | public URL of the callback (e.g. ngrok when running locally) |
| `ABA_QR_TEMPLATE` | look of the QR image (default `template3_color`) |

`/internal/**` is service-to-service only: the API gateway must never route it.

## Not verified against PayWay yet

The QR and Refund requests follow PayWay's documentation (field names, hash order, RSA chunking) and are
unit-tested, but they have not been sent to the sandbox. Points the docs leave open: how amounts must be
formatted in the hash, whether QR callbacks are signed, and the RSA key size/padding (PKCS#1 v1.5 assumed,
as in PayWay's PHP sample).
