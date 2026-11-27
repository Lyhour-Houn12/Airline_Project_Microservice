# API Gateway

Port **8080**. The single entry point for clients (web, mobile): `http://localhost:8080/api/v1/...`.
Built on Spring Cloud Gateway (reactive), routing through Eureka (`lb://<service-name>`).

## What it does for every request

1. **Blocks** service-to-service paths: `/internal/**`, plus `gateway.blocked-paths`
   (`/api/v1/flights/by-ids`, `/api/v1/flight-instances/batch`) -> 404.
2. **Strips** any `X-User-Id` / `X-User-Role` the client sent. Nobody can pretend to be someone else.
3. **Authenticates**: `Authorization: Bearer <access token from user-service>` is verified
   (signature with `JWT_SECRET`, issuer `user-service`, expiry, known role).
   - valid -> `X-User-Id` and `X-User-Role` are set from the token and the request is routed
   - invalid / expired -> **401** (same error JSON as the services)
   - no token -> routed anonymously; the service decides (public reads work, the rest answers 401)

**Authorization stays in the services** (`@RequireRole`, `CurrentUser`, booking ownership). The gateway
only guarantees that the identity they receive is real.

## Routes

| Path | Service |
|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**` | user-service |
| `/api/v1/airlines/**`, `/api/v1/aircrafts/**` | airline-core-service |
| `/api/v1/cities/**`, `/api/v1/airports/**` | location-service |
| `/api/v1/flights/**`, `/api/v1/flight-schedules/**`, `/api/v1/flight-instances/**` | flight-ops-service |
| `/api/v1/cabin-classes/**`, `/api/v1/seat-maps/**`, `/api/v1/seats/**`, `/api/v1/seat-instances/**`, `/api/v1/flight-instance-cabins/**` | seat-service |
| `/api/v1/fares/**`, `/api/v1/fare-rules/**`, `/api/v1/baggage-policies/**` | pricing-service |
| `/api/v1/ancillaries/**`, `/api/v1/meals/**`, `/api/v1/insurance-coverages/**`, `/api/v1/flight-cabin-ancillaries/**`, `/api/v1/flight-instance-meals/**` | ancillary-service |
| `/api/v1/bookings/**`, `/api/v1/tickets/**` | booking-service |
| `/api/v1/payments/**` (incl. PayWay's callback) | payment-service |

Adding a controller under a new prefix? Add it to `application.yaml`, or it won't be reachable.

## Run

```bash
cp cloud/api-gateway/.env.example cloud/api-gateway/.env   # JWT_SECRET = the same value as user-service's
mvn -pl cloud/api-gateway spring-boot:run                  # after discovery-server (8761)
```

CORS: browser origins in `CORS_ALLOWED_ORIGINS` (default `http://localhost:3000,http://localhost:5173`).

## Important for deployment

The gateway only protects what goes **through** it. In production only port 8080 may be reachable from
outside; the services (8000-8089, 5004) must be on a private network. Otherwise a caller can skip the
gateway and send `X-User-*` headers directly. A next step is to make that impossible even on a shared
network (a gateway-signed header or mTLS between gateway and services).
