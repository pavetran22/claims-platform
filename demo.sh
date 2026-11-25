#!/usr/bin/env bash
# End-to-end walkthrough of the API with curl. Start the backend first: cd backend && ./mvnw spring-boot:run
set -euo pipefail
API=${API:-http://localhost:8080/api}
call() { local user=$1 role=$2; shift 2
  curl -sS -w '  [HTTP %{http_code}]\n' -H 'Content-Type: application/json' -H "X-User-Id: $user" -H "X-User-Role: $role" "$@"; }
step() { printf '\n\033[1m%s\033[0m\n' "$1"; }

step "1. Claimant alice submits a motor claim in Malaysia"
CLAIM=$(call alice CLAIMANT -X POST "$API/claims" \
  -d '{"market":"MY","productType":"MOTOR","incidentDate":"2026-11-20","description":"Rear-ended at a traffic light","claimedAmount":4500.00}')
echo "$CLAIM"
ID=$(echo "$CLAIM" | sed -E 's/.*"id":"([^"]+)".*/\1/')

step "2. Officer sees it in the unassigned queue and picks it up"
call officer1 OFFICER "$API/claims/queue" > /dev/null && echo "  queue OK"
call officer1 OFFICER -X POST "$API/claims/$ID/pick-up"

step "3. Officer asks for more information"
call officer1 OFFICER -X POST "$API/claims/$ID/request-info" -d '{"message":"Please send a photo of the damage and the police report number"}'

step "4. Alice's notifications (delivered via Kafka)"
sleep 2; call alice CLAIMANT "$API/notifications"

step "5. Alice replies, officer approves 4000 and settles"
call alice CLAIMANT -X POST "$API/claims/$ID/info" -d '{"message":"Photo uploaded, police report MY-12345"}'
call officer1 OFFICER -X POST "$API/claims/$ID/approve" -d '{"amount":4000.00,"note":"Approved minus excess"}'
call officer1 OFFICER -X POST "$API/claims/$ID/settle" -d '{"amount":4000.00}'

step "6. Rules are enforced: settling twice (409), another claimant looking (404), claimant on a dashboard (403)"
call officer1 OFFICER -X POST "$API/claims/$ID/settle" -d '{"amount":4000.00}' || true
call bob CLAIMANT "$API/claims/$ID" || true
call alice CLAIMANT "$API/dashboard/exposure" || true

step "7. Full audit trail"
call alice CLAIMANT "$API/claims/$ID/history"

step "8. Dashboards: team workload (officer) and exposure (manager)"
call officer1 OFFICER "$API/dashboard/workload"
call manager1 MANAGER "$API/dashboard/exposure"
