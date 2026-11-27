# Airline Project (microservices)

Spring Boot 4.1 · Spring Cloud 2025.1 · Java 21 · PostgreSQL · Maven multi-module (`com.jing`).

| Module | Port | Database | What it does |
|---|---|---|---|
| `cloud/discovery-server` | 8761 | – | Eureka: services find each other by name |
| `cloud/api-gateway` | 8080 | – | The single entry point: checks the JWT, routes, blocks `/internal/**` ([README](cloud/api-gateway/README.md)) |
| `services/user-service` | 8081 | `user_db` | Accounts, login, JWT ([README](services/user-service/README.md)) |
| `services/airline-core-service` | 8002 | `airline_core_db` | Airlines (with their owner), aircraft |
| `services/location-service` | 5004 | `location_service` | Countries, cities, airports (with time zones) |
| `services/flight-ops-service` | 8082 | `flight_ops_db` | Flights, schedules, flight instances; cancelling a flight cancels its bookings |
| `services/pricing-service` | 8085 | `pricing_db` | Fares, fare rules, baggage |
| `services/seat-service` | 8088 | `seat_db` | Cabin classes, seat maps, seats, cabin capacity |
| `services/ancillary-service` | 8089 | `ancillary_db` | Meals, extras, insurance |
| `services/booking-service` | 8066 | `booking_db` | Flight search, bookings, passengers, tickets ([README](services/booking-service/README.md)) |
| `services/payment-service` | 8000 | `payment_db` | ABA PayWay (checkout + QR), refunds ([README](services/payment-service/README.md)) |
| `common-lib` | – | – | Shared DTOs, security, paging, errors ([README](common-lib/README.md)) |

## Secrets

Each service that needs secrets reads its own git-ignored `.env` (copy the `.env.example` next to
it): `services/user-service`, `cloud/api-gateway` (same `JWT_SECRET` as user-service),
`services/payment-service`, `services/ancillary-service`. Never commit a `.env`.

## Run it

**Everything in Docker** (Postgres included, on host port 5433). Images are built by
[Jib](https://github.com/GoogleContainerTools/jib) straight from Maven, no Dockerfile:

```bash
scripts/build-images.sh --parallel                    # airline/<service>:latest, all 11 in ~25 s (Windows: scripts\build-images.cmd)
docker compose -f docker/docker-compose.yml up -d     # API http://localhost:8080, Eureka http://localhost:8761
docker compose -f docker/docker-compose.yml logs -f booking-service
docker compose -f docker/docker-compose.yml down      # add -v to also delete the database volume
```

`scripts/build-images.sh` also builds only some services (`scripts/build-images.sh booking-service`)
and pushes to a registry (`IMAGE_PREFIX=<docker-hub-user> scripts/build-images.sh --push`; add
`JIB_ARCH=amd64` when the images are for ordinary servers and you're on Apple Silicon). See `--help`.

`docker/common-config.yml` holds what services share (network, restart, memory limit, Eureka,
database login); `docker/docker-compose.yml` lists the services, each extending one of those blocks.
Only the gateway, Eureka and Postgres have host ports: the services are reached through the gateway.
Set `PAYMENT_ABA_BASE_URL=http://127.0.0.1:9` to run without calling ABA.

**The whole flow in one command**, once the system is up (Docker or local, through the gateway):

```bash
scripts/e2e-flow.sh                   # admin → airline owner → flight + fares → search → booking → payment → tickets
CANCEL_FLIGHT=1 scripts/e2e-flow.sh   # ... then the owner cancels the flight
```

It creates its own owner, airline, flight and customer each run (sign-up codes are read from the
user-service log), and uses the admin login from `services/user-service/.env`. ABA is not called:
the payment is created for real, and ABA's "paid" callback is simulated.

**Locally from the IDE / Maven** (needs a local Postgres on 5432 with the databases above):

```bash
mvn -DskipTests install
mvn -pl cloud/discovery-server spring-boot:run      # first
mvn -pl services/booking-service spring-boot:run    # then any service, and the gateway
```

Tests: `mvn test -Dtest='*Test' -Dsurefire.failIfNoSpecifiedTests=false` (unit tests; the
`*ApplicationTests` need the databases and secrets). CI (`.github/workflows/ci.yml`) runs the same
on every push and pull request, and builds every Docker image with Jib.

## Database changes (Flyway)

Each service's schema is owned by its migrations in `src/main/resources/db/migration`; Hibernate
only checks it (`ddl-auto: validate`) and the service won't start if an entity and the tables
disagree.

- To change the schema, add the next file, e.g. `V3__add_meal_to_passenger.sql`. **Never edit a
  migration that has already run**: Flyway will refuse to start.
- Adding a value to an enum stored as text needs a migration too: its column has a check constraint
  listing the allowed values (see `payment-service/.../V2__allow_qr_and_partial_refund.sql`).
- Databases created before Flyway are adopted automatically at V1 (`baseline-on-migrate`).
