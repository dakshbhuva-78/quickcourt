#!/usr/bin/env bash
# Post-deployment smoke test.  Usage:  bash deploy/smoke-test.sh https://your-domain
# Requires demo data (SEED_DATA=true) for the login step.
BASE="${1:-http://localhost:8080}"
fail=0
check() { # name, expected substring, actual
  if echo "$3" | grep -q "$2"; then echo "PASS  $1"; else echo "FAIL  $1"; fail=1; fi
}
check "health is UP"        '"status":"UP"'   "$(curl -s "$BASE/actuator/health")"
check "home page loads"     'QuickCourt'      "$(curl -s "$BASE/")"
check "venue API responds"  '"name"'          "$(curl -s "$BASE/api/venues")"
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
        -d '{"email":"player1@quickcourt.com","password":"Player@123"}')
check "login works"         '"token"'         "$LOGIN"
check "unauthenticated booking is rejected" '401' "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/bookings" -H 'Content-Type: application/json' -d '{}')"
exit $fail
