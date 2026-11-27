#!/usr/bin/env bash
# End-to-end flow through the API gateway, against the Docker setup:
#
#   admin      -> cities, airports
#   owner      -> signs up, becomes an airline owner, creates the airline, aircraft, cabin, seats,
#                 a flight PNH -> NRT in 30 days, its cabin and seat inventory, two fares
#   (anyone)   -> searches flights with prices
#   customer   -> signs up, books 2 adults + 1 child, picks seats, starts an ABA PayWay payment
#   (simulated)-> ABA confirms the payment  ->  booking CONFIRMED, seats BOOKED, tickets issued
#   CANCEL_FLIGHT=1 -> the owner cancels the flight -> the booking is cancelled, places freed
#
# Usage (from the project root, with the system running):
#   mvn -DskipTests package jib:dockerBuild
#   PAYMENT_ABA_BASE_URL=http://127.0.0.1:9 docker compose -f docker/docker-compose.yml up -d
#   scripts/e2e-flow.sh                  # or: CANCEL_FLIGHT=1 scripts/e2e-flow.sh
#
# Every run creates its own owner, airline, flight and customer, so it can be run again and again.
# Admin login: ADMIN_EMAIL / ADMIN_PASSWORD from the environment, else services/user-service/.env.
#
# Payment: ABA isn't called. The payment is created for real (a PayWay checkout is built inside
# payment-service), but the "customer paid" callback from ABA is simulated by sending
# payment-service's notification to booking-service from inside the Docker network.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GATEWAY="${GATEWAY:-http://localhost:8080}"
CANCEL_FLIGHT="${CANCEL_FLIGHT:-0}"
RUN="$(date +%s)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
BODY="$TMP/body.json"

# ---------- output ----------
if [ -t 1 ]; then B=$'\e[1m'; G=$'\e[32m'; R=$'\e[31m'; D=$'\e[2m'; N=$'\e[0m'; else B=; G=; R=; D=; N=; fi
step() { printf '\n%s== %s%s\n' "$B" "$1" "$N"; }
ok()   { printf '  %s✓%s %s\n' "$G" "$N" "$1"; }
info() { printf '  %s%s%s\n' "$D" "$1" "$N"; }
die()  { printf '  %s✗ %s%s\n' "$R" "$1" "$N" >&2; [ -s "$BODY" ] && printf '    %s\n' "$(head -c 600 "$BODY")" >&2; exit 1; }

dc() { docker compose -f "$ROOT/docker/docker-compose.yml" "$@"; }

# ---------- HTTP + JSON helpers ----------
# call METHOD PATH [TOKEN] [JSON]  -> response body in $BODY, status code in $CODE
call() {
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-s -o "$BODY" -w '%{http_code}' -X "$method" -H 'Accept: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' --data "$data")
  CODE="$(curl "${args[@]}" "$GATEWAY$path" || echo 000)"
}
# must METHOD PATH TOKEN JSON "what"  -> like call, but stops the script unless 2xx
must() { call "$@"; case "$CODE" in 2*) ;; *) die "${5:-$1 $2} -> HTTP $CODE";; esac; }
# j 'python expression on d'   (d = the last response body)
j() { python3 -I -c 'import json,sys; d=json.load(open(sys.argv[1])); r=eval(sys.argv[2]); print("" if r is None else r)' "$BODY" "$1"; }
# json key=value ...  -> a JSON object; values that look like numbers / true / false / null / [..] / {..} stay raw
json() { python3 -I -c '
import json,sys
out={}
for kv in sys.argv[1:]:
    k,v=kv.split("=",1)
    try: out[k]=json.loads(v)
    except ValueError: out[k]=v
print(json.dumps(out))' "$@"; }

# ---------- users ----------
env_value() {  # env_value KEY FILE  (reads KEY=value from a .env without executing it)
  python3 -I -c '
import sys
for line in open(sys.argv[2]):
    line=line.strip()
    if line.startswith(sys.argv[1]+"="):
        print(line.split("=",1)[1].strip().strip("\"'"'"'")); break' "$1" "$2" 2>/dev/null || true
}
signin() {  # signin EMAIL PASSWORD -> prints the JWT
  must POST /api/v1/auth/signin "" "$(json email="$1" password="$2")" "sign in $1"
  j 'd["jwt"]'
}
signup_verified() {  # signup_verified NAME EMAIL PASSWORD  (reads the OTP from user-service's log)
  must POST /api/v1/auth/signup "" "$(json fullname="$1" email="$2" password="$3")" "sign up $2"
  local code="" i
  for i in 1 2 3 4 5 6 7 8 9 10; do
    code="$(dc logs --no-log-prefix --since 5m user-service 2>/dev/null | grep "\[DEV EMAIL\] to=$2 " | tail -1 | sed -nE 's/.*code=([0-9]{6}).*/\1/p')"
    [ -n "$code" ] && break; sleep 1
  done
  [ -n "$code" ] || die "no verification code for $2 in the user-service log (is app.mail.enabled true? then check that inbox)"
  must POST /api/v1/auth/verify-email "" "$(json email="$2" code="$code")" "verify $2"
}

# ---------- reference data (created once, found on later runs) ----------
find_or_create_city() {  # CODE NAME COUNTRY_CODE COUNTRY_NAME -> city id
  must GET "/api/v1/cities?size=100" "$ADMIN" "" "list cities"
  local id; id="$(j "next((c['id'] for c in d['content'] if c['cityCode']=='$1'), '')")"
  if [ -z "$id" ]; then
    must POST /api/v1/cities "$ADMIN" "$(json cityName="$2" cityCode="$1" countryCode="$3" countryName="$4")" "create city $1"
    id="$(j 'd["id"]')"
  fi
  echo "$id"
}
find_or_create_airport() {  # IATA NAME CITY_ID ZONE -> airport id
  must GET "/api/v1/airports?size=100" "" "" "list airports"
  local id; id="$(j "next((a['id'] for a in d['content'] if a['iataCode']=='$1'), '')")"
  if [ -z "$id" ]; then
    must POST /api/v1/airports "$ADMIN" "$(json iataCode="$1" airportName="$2" cityId="$3" timeZone="$4")" "create airport $1"
    id="$(j 'd["id"]')"
  fi
  echo "$id"
}

# =====================================================================================
step "0. Is the system up?"
command -v python3 >/dev/null || die "python3 is needed"
call GET /api/v1/airports
[ "$CODE" = 200 ] || die "the gateway at $GATEWAY doesn't answer (HTTP $CODE). Start it: docker compose -f docker/docker-compose.yml up -d"
ok "gateway answers at $GATEWAY"

ADMIN_EMAIL="${ADMIN_EMAIL:-$(env_value ADMIN_EMAIL "$ROOT/services/user-service/.env")}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-$(env_value ADMIN_PASSWORD "$ROOT/services/user-service/.env")}"
[ -n "$ADMIN_EMAIL" ] && [ -n "$ADMIN_PASSWORD" ] || die "set ADMIN_EMAIL and ADMIN_PASSWORD (or put them in services/user-service/.env)"
ADMIN="$(signin "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"
ok "admin signed in"

# =====================================================================================
step "1. Admin: cities and airports"
PNH_CITY="$(find_or_create_city PNH "Phnom Penh" KH Cambodia)"
TYO_CITY="$(find_or_create_city TYO "Tokyo" JP Japan)"
PNH="$(find_or_create_airport PNH "Phnom Penh International Airport" "$PNH_CITY" Asia/Phnom_Penh)"
NRT="$(find_or_create_airport NRT "Narita International Airport" "$TYO_CITY" Asia/Tokyo)"
ok "PNH airport id $PNH (Asia/Phnom_Penh), NRT airport id $NRT (Asia/Tokyo)"

# =====================================================================================
step "2. A new user becomes an airline owner"
OWNER_EMAIL="owner.$RUN@example.com"; OWNER_PASSWORD="Owner-$RUN!"
signup_verified "Sky Owner $RUN" "$OWNER_EMAIL" "$OWNER_PASSWORD"
signin "$OWNER_EMAIL" "$OWNER_PASSWORD" >/dev/null
OWNER_ID="$(j 'd["user"]["id"]')"
must PUT "/api/v1/users/$OWNER_ID/role" "$ADMIN" "$(json role=ROLE_AIRLINE_OWNER)" "admin makes user $OWNER_ID an owner"
OWNER="$(signin "$OWNER_EMAIL" "$OWNER_PASSWORD")"          # a new token carries the new role
ok "user $OWNER_ID ($OWNER_EMAIL) is an airline owner"

# =====================================================================================
step "3. Owner: airline, aircraft, cabin and seats"
for attempt in 1 2 3 4 5; do   # IATA / ICAO codes are unique: pick random ones
  IATA="$(python3 -I -c 'import random,string; print("".join(random.choices(string.ascii_uppercase, k=2)))')"
  ICAO="$(python3 -I -c 'import random,string; print("".join(random.choices(string.ascii_uppercase, k=3)))')"
  call POST /api/v1/airlines "$OWNER" "$(json iataCode="$IATA" icaoCode="$ICAO" airlineName="Sky $IATA Airways" country=Cambodia supportEmail="help@sky$RUN.example.com")"
  [ "$CODE" = 409 ] || break
done
case "$CODE" in 2*) ;; *) die "create airline -> HTTP $CODE";; esac
AIRLINE="$(j 'd["id"]')"
ok "airline $AIRLINE: Sky $IATA Airways ($IATA/$ICAO)"

must POST /api/v1/aircrafts "$OWNER" "$(json code="A320-$RUN" model="A320neo" manufacturer=Airbus seatingCapacity=60 economySeats=60 status=ACTIVE isAvailable=true)" "create aircraft"
AIRCRAFT="$(j 'd["id"]')"
must POST /api/v1/cabin-classes "$OWNER" "$(json name=Economy code=Y cabinClassType=ECONOMY aircraftId="$AIRCRAFT" isActive=true isBookable=true)" "create cabin class"
CABIN="$(j 'd["id"]')"
must POST /api/v1/seat-maps "$OWNER" "$(json name="A320 Economy" totalRows=10 leftSeatsPerRow=3 rightSeatsPerRow=3 airlineId="$AIRLINE" cabinClassId="$CABIN")" "create seat map"
SEATMAP="$(j 'd["id"]')"
must POST /api/v1/seats/generate "$OWNER" "$(json seatMapId="$SEATMAP" startRow=1)" "generate seats"
ok "aircraft $AIRCRAFT, Economy cabin $CABIN, seat map $SEATMAP: 10 rows x 6 seats (1A-10F)"

# =====================================================================================
step "4. Owner: flight PNH -> NRT, its inventory and fares"
DAY="$(python3 -I -c 'import datetime; print(datetime.date.today()+datetime.timedelta(days=30))')"
FLIGHT_NO="${IATA}801"
must POST /api/v1/flights "$OWNER" "$(json flightNumber="$FLIGHT_NO" aircraftId="$AIRCRAFT" departureAirportId="$PNH" arrivalAirportId="$NRT")" "create flight"
FLIGHT="$(j 'd["id"]')"
# a schedule for that one day, then its dated instance
# local times: 09:00 in Phnom Penh (UTC+7) -> 17:00 in Tokyo (UTC+9) = 6 hours flying
must POST /api/v1/flight-schedules "$OWNER" "$(json flightId="$FLIGHT" departureTime=09:00:00 arrivalTime=17:00:00 startDate="$DAY" endDate="$DAY")" "create schedule"
SCHEDULE="$(j 'd["id"]')"
must POST "/api/v1/flight-schedules/$SCHEDULE/generate-instances" "$OWNER" "$(json totalSeats=60)" "generate instances"
must GET "/api/v1/flight-instances?flightId=$FLIGHT&onDate=$DAY" "$OWNER" "" "find the instance"
INSTANCE="$(j 'd["content"][0]["id"]')"
info "schedule $SCHEDULE -> instance $INSTANCE: departs $(j 'd["content"][0].get("departureAt")') UTC, arrives $(j 'd["content"][0].get("arrivalAt")') UTC"
# (JSON values go through a variable: Bash 3.2 brace-expands a quoted {..,..} inside $(..))
CABINS="[{\"cabinClassId\": $CABIN}]"
# and a one-off extra flight the next evening, created directly, without a schedule
NEXT_DAY="$(python3 -I -c 'import datetime; print(datetime.date.today()+datetime.timedelta(days=31))')"
AFTER="$(python3 -I -c 'import datetime; print(datetime.date.today()+datetime.timedelta(days=32))')"
must POST /api/v1/flight-instances "$OWNER" "$(json flightId="$FLIGHT" departureDateTime="${NEXT_DAY}T23:00:00" arrivalDateTime="${AFTER}T07:00:00" totalSeats=60)" "create a one-off flight instance"
[ -z "$(j 'd.get("scheduleId")')" ] || die "the one-off flight should have no schedule"
info "one-off extra flight: instance $(j 'd["id"]') on $NEXT_DAY 23:00, no schedule"
must POST /api/v1/flight-instance-cabins "$OWNER" "$(json flightInstanceId="$INSTANCE" cabins="$CABINS")" "open the cabin for sale"
must POST /api/v1/seat-instances/generate "$OWNER" "$(json flightInstanceId="$INSTANCE")" "generate seat inventory"
must POST /api/v1/fares "$OWNER" "$(json fareName="Economy Saver" fareLabel=Saver rbdCode=V airlineId="$AIRLINE" flightId="$FLIGHT" cabinClassId="$CABIN" cabinClass=ECONOMY baseFare=250.00 taxesAndFees=35.00 airlineFees=0 currency=USD)" "create Saver fare"
must POST /api/v1/fares "$OWNER" "$(json fareName="Economy Flex" fareLabel=Flex rbdCode=Y airlineId="$AIRLINE" flightId="$FLIGHT" cabinClassId="$CABIN" cabinClass=ECONOMY baseFare=320.00 taxesAndFees=35.00 airlineFees=0 currency=USD freeDateChange=true fullRefund=true)" "create Flex fare"
ok "flight $FLIGHT_NO on $DAY (instance $INSTANCE): 60 seats on sale, Saver \$285 and Flex \$355 per adult"

# =====================================================================================
step "5. Anyone: search PNH -> NRT on $DAY for 2 adults + 1 child (no login)"
must GET "/api/v1/flight-search?from=$PNH&to=$NRT&date=$DAY&adults=2&children=1&sort=PRICE" "" "" "flight search"
python3 -I - "$BODY" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1]))
for f in d["flights"]:
    print("  %s  %s %s -> %s %s (%s)" % (f["flightNumber"], f["departureTime"], f["departureTimeZone"],
                                        f["arrivalTime"], f["arrivalTimeZone"], f["duration"]))
    for c in f["cabins"]:
        for x in c["fares"]:
            print("     %-8s %-14s adult %7s  child %7s  = %s %s for the party  (%d seats left)" % (
                c["name"], x["fareName"], x["pricePerAdult"], x["pricePerChild"], x["totalPrice"], x["currency"], c["seatsLeft"]))
EOF
FARE="$(j "next(x['fareId'] for f in d['flights'] if f['flightInstanceId']==$INSTANCE for c in f['cabins'] for x in c['fares'])")"
[ -n "$FARE" ] || die "our flight is not in the search results"
ok "picked the cheapest fare on $FLIGHT_NO: fare $FARE"

# =====================================================================================
step "6. Customer: signs up and books"
CUST_EMAIL="customer.$RUN@example.com"; CUST_PASSWORD="Customer-$RUN!"
signup_verified "Dara Sok" "$CUST_EMAIL" "$CUST_PASSWORD"
CUSTOMER="$(signin "$CUST_EMAIL" "$CUST_PASSWORD")"
ok "customer $CUST_EMAIL signed in"

CONTACT="{\"email\": \"$CUST_EMAIL\", \"phone\": \"+85512345678\"}"
must POST /api/v1/bookings/draft "$CUSTOMER" "$(json tripType=ONE_WAY currency=USD flexibleTicket=false contactInfo="$CONTACT")" "create draft booking"
BOOKING="$(j 'd["id"]')"; REF="$(j 'd["bookingReference"]')"
EXPIRY="$(python3 -I -c 'import datetime; print((datetime.date.today()+datetime.timedelta(days=3650)).strftime("%d/%m/%Y"))')"
add_passenger() {  # TYPE FIRST LAST DOB(dd/MM/yyyy) GENDER PASSPORT
  must POST "/api/v1/bookings/$BOOKING/passengers" "$CUSTOMER" "$(json passengerType="$1" firstName="$2" lastName="$3" dateOfBirth="$4" gender="$5" passportNumber="$6" passportExpiry="$EXPIRY" passportIssuingCountry=KH nationality=KH)" "add passenger $2"
}
add_passenger ADULT Dara  Sok 14/02/1988 MALE   N01234567
add_passenger ADULT Sreyneang Sok 03/09/1990 FEMALE N07654321
add_passenger CHILD Visal Sok 21/06/2018 MALE   N05551234
must POST "/api/v1/bookings/$BOOKING/segments" "$CUSTOMER" "$(json flightInstanceId="$INSTANCE" fareId="$FARE")" "add flight $FLIGHT_NO"
PSEGS="$(j '" ".join(str(p["id"]) for p in d["passengerSegments"])')"
ok "booking $REF (id $BOOKING): 3 passengers on $FLIGHT_NO, places held in the cabin"

must GET "/api/v1/seat-instances/flight-instance/$INSTANCE" "" "" "seat map"
SEATS="$(python3 -I - "$BODY" <<'EOF'
import json, sys
def seats(o):
    if isinstance(o, dict):
        if "seatNumber" in o and "status" in o: yield o
        for v in o.values(): yield from seats(v)
    elif isinstance(o, list):
        for v in o: yield from seats(v)
free = sorted((s for s in seats(json.load(open(sys.argv[1]))) if s["status"] == "AVAILABLE"),
              key=lambda s: (int(s["seatNumber"][:-1]), s["seatNumber"][-1]))
row = [s for s in free if s["seatNumber"].startswith("3")][:3] or free[:3]   # a row near the front
print(" ".join("%s:%s" % (s["seatInstanceId"], s["seatNumber"]) for s in row))
EOF
)"
set -- $SEATS; picked=""
for ps in $PSEGS; do
  seat="$1"; shift
  must PUT "/api/v1/bookings/$BOOKING/passenger-segments/$ps/seat" "$CUSTOMER" "$(json seatInstanceId="${seat%%:*}")" "seat ${seat#*:}"
  picked="$picked ${seat#*:}"
done
must GET "/api/v1/bookings/$BOOKING" "$CUSTOMER" "" "booking"
TOTAL="$(j 'd["totalAmount"]')"
ok "seats$picked held; total $TOTAL USD; status $(j 'd["status"]'), pay before $(j 'd["expiredAt"]')"

# =====================================================================================
step "7. Customer: pays with ABA PayWay"
must POST /api/v1/payments "$CUSTOMER" "$(json bookingId="$BOOKING" method=ABA_PAYWAY firstName=Dara lastName=Sok email="$CUST_EMAIL")" "start payment"
PAYMENT="$(j 'd["id"]')"
ok "payment $PAYMENT created: $(j 'd["status"]') $(j 'd["amount"]') $(j 'd["currency"]') (the PayWay checkout form is ready for the customer)"

info "SIMULATED: ABA reports the payment as paid (ABA is not connected in this setup)."
info "payment-service would then tell booking-service; the script sends that message from inside Docker."
RESULT="$(dc exec -T booking-service curl -s -w '\n%{http_code}' -X POST "http://localhost:8066/internal/v1/bookings/$BOOKING/payment-result" \
  -H 'Content-Type: application/json' --data "$(json paymentId="$PAYMENT" status=SUCCESS)")" || die "could not reach booking-service inside Docker"
echo "$RESULT" | sed '$d' > "$BODY"; CODE="$(echo "$RESULT" | tail -1)"
case "$CODE" in 2*) ;; *) die "booking-service refused the payment result -> HTTP $CODE (see: docker compose -f docker/docker-compose.yml logs booking-service)";; esac
[ "$(j 'd.get("status")')" = CONFIRMED ] || die "booking is $(j 'd.get("status")') after payment: $(j 'd.get("message")')"
ok "booking-service: $(j 'd.get("message")')"

# =====================================================================================
step "8. Result"
for i in $(seq 1 20); do   # seat confirmation goes through the outbox: give it a moment
  must GET "/api/v1/bookings/$BOOKING/tickets" "$CUSTOMER" "" "tickets"
  [ "$(j 'len(d)')" -ge 3 ] && break; sleep 1
done
[ "$(j 'len(d)')" -ge 3 ] || die "expected 3 tickets, got $(j 'len(d)')"
python3 -I -c '
import json,sys
for t in json.load(open(sys.argv[1])):
    print("  ticket %s  %s  (passenger seat %s)" % (t["ticketNumber"], t["status"], t["passengerSegmentId"]))' "$BODY"
must GET "/api/v1/bookings/$BOOKING" "$CUSTOMER" "" "booking"
ok "booking $REF is $(j 'd["status"]') (paid by payment $(j 'd["paymentId"]'))"
for i in $(seq 1 20); do
  must GET "/api/v1/flight-instance-cabins/flight-instance/$INSTANCE" "" "" "cabin"
  [ "$(j 'd[0]["bookedSeats"]')" -ge 3 ] && break; sleep 1
done
[ "$(j 'd[0]["bookedSeats"]')" -ge 3 ] || die "the 3 places are still not booked"
ok "Economy on $FLIGHT_NO: $(j 'd[0]["bookedSeats"]') booked, $(j 'd[0]["heldSeats"]') held, $(j 'd[0]["availableSeats"]') of $(j 'd[0]["totalSeats"]') left"

# =====================================================================================
if [ "$CANCEL_FLIGHT" = 1 ]; then
  step "9. Owner cancels the flight"
  must PATCH "/api/v1/flight-instances/$INSTANCE/status?status=CANCELLED&reason=Typhoon%20warning" "$OWNER" "" "cancel flight"
  for i in $(seq 1 30); do
    must GET "/api/v1/bookings/$BOOKING" "$CUSTOMER" "" "booking"
    [ "$(j 'd["status"]')" = CANCELLED ] && break; sleep 1
  done
  ok "booking $REF is $(j 'd["status"]')"
  must GET "/api/v1/bookings/$BOOKING/history" "$CUSTOMER" "" "history"
  info "$(j 'd[-1]["changedBy"] + ": " + d[-1]["reason"]')"
  for i in $(seq 1 20); do
    must GET "/api/v1/flight-instance-cabins/flight-instance/$INSTANCE" "" "" "cabin"
    [ "$(j 'd[0]["bookedSeats"]')" = 0 ] && break; sleep 1
  done
  ok "places freed: $(j 'd[0]["availableSeats"]') of $(j 'd[0]["totalSeats"]') left"
  info "refund: requested from payment-service (it waits, since this payment never really reached ABA)"
fi

step "Done"
cat <<EOF
  owner      $OWNER_EMAIL / $OWNER_PASSWORD   (airline $AIRLINE, $IATA)
  customer   $CUST_EMAIL / $CUST_PASSWORD
  flight     $FLIGHT_NO on $DAY, instance $INSTANCE
  booking    $REF (id $BOOKING), payment $PAYMENT
EOF
