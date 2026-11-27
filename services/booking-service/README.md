# Booking Service

Port 8066, database `booking_db`. Owns bookings (PNR), their flights, passengers, seats and tickets.

## Flight search (public, no login)

```
GET /api/v1/flight-search?from=1&to=4&date=2026-12-24&adults=2&children=1&infants=0&cabin=ECONOMY&sort=PRICE
```

`from` / `to` are airport ids (from `GET /api/v1/airports`), `date` is the local departure date.
Returns every bookable flight (scheduled, with time zones, room for the whole party), each with its
cabins (`seatsLeft`) and fares priced for this party (`pricePerAdult`, `pricePerChild`,
`pricePerInfant`, `totalPrice`), cheapest first. `sort=DEPARTURE` (default) or `PRICE`.
Book a result with `POST /api/v1/bookings/{id}/segments {flightInstanceId, fareId}`.

## The booking flow

```
1. POST   /api/v1/bookings/draft                          empty PENDING booking, expires in 15 min
2. POST   /api/v1/bookings/{id}/segments                  { flightInstanceId, fareId, journeyNumber? }
3. POST   /api/v1/bookings/{id}/passengers                passenger details (priced automatically)
4. PUT    /api/v1/bookings/{id}/passenger-segments/{psId}/seat   { seatInstanceId }   (optional)
5. POST   /api/v1/payments  (payment-service)             { bookingId, method, ... }
   -> customer pays on ABA PayWay -> callback -> booking CONFIRMED, seats BOOKED, tickets issued
6. GET    /api/v1/bookings/{id}/tickets
```

| Step | Talks to | What happens |
|---|---|---|
| Add segment | flight-ops, pricing | Flight, airports, times and fare are **read from their owners**, never taken from the client. The flight must be SCHEDULED/DELAYED, active, in the future and inside its booking window; the fare must belong to that flight and use the booking's currency. The fare is copied onto the segment (snapshot). |
| Add passenger | (none) | Priced from each segment's snapshot: adult 100 %, child `booking.child-fare-percent`, infant `booking.infant-fare-percent`. |
| Pick seat | seat-service | The seat is **HELD** under the PNR until the booking expires. A seat someone else holds gives 409. Changing or clearing a seat releases the old one; the seat surcharge is added to the price. |
| Pay | payment-service | Payment asks `GET /internal/v1/bookings/{id}/payment-info` for the amount (400 if the booking isn't complete). On success it calls `POST /internal/v1/bookings/{id}/payment-result`. |
| Confirmed | seat-service | HELD seats become BOOKED. A seat whose hold was lost is cleared and the booking still confirms. Tickets are issued, one per passenger per flight. |
| Expire | seat-service | `BookingExpiryJob` runs every minute: unpaid bookings past `expiresAt` become EXPIRED and their seats are released. |
| Cancel | seat-service | Seats released, unused tickets voided. |

All status changes with side effects live in `service/BookingLifecycle`.

## Capacity (no overselling)

Every passenger who needs a seat (adults and children, not infants on a lap) takes a **place** in the
cabin as soon as they're in the booking, seat picked or not: a `CabinReservation` in seat-service, set
under a row lock on the cabin, so two bookings can't take the last place. A full cabin answers **409**.
Places are held until the booking expires, become booked when it's paid (even if the hold lapsed:
a paid passenger keeps their place; overbooking is logged), and are given back on cancel / expiry.
Lapsed holds of abandoned bookings are freed the next time someone reserves in that cabin.
A flight can only be sold once its airline has set up the cabin (`/api/v1/flight-instance-cabins`).

## Messages to other services (outbox)

Seat release, seat booking (after payment) and refund requests are **outbox messages**: saved in the
same transaction as the booking change (`outbox_messages` table), sent right after commit, and retried
every few seconds (backing off to 10 minutes) until the other service accepts them. A seat-service or
payment-service outage only delays them. Handlers are in `service/messaging`; receivers handle repeats.

## Internal endpoints

`/internal/**` (here and in seat-service) is service-to-service only. **The API gateway must never
route it**: anyone who could call it could mark a booking as paid or hold seats.

## Settings (application.yaml)

```yaml
booking:
  hold-minutes: 15
  child-fare-percent: 100
  infant-fare-percent: 10
  expiry-check-millis: 60000
```

## Not done yet

- Refunds: a cancelled paid booking (or a payment that arrived after expiry) is logged as "refund required".
- Meals and ancillaries (`BookingItem` exists, no endpoints yet).
- An owner of the airline can't see bookings on their flights yet (`AccessGuard.canViewBooking`).
