# Claims Platform

A motor and property claims platform for six APAC markets. Claimants report incidents and track them, claims officers review and decide them, and managers see workload and outstanding exposure.

Built as a take-home assessment: **Java 21 / Spring Boot 4** backend, **Angular 21** frontend, **PostgreSQL** and **Kafka** via Docker Compose. Runs locally only.

## Run it

Prerequisites: Java 21, Node 24, Docker.

```bash
# 1. Postgres (port 5433) and Kafka (port 9092)
docker compose up -d

# 2. Backend on http://localhost:8080 (Flyway creates the schema on start)
cd backend && ./mvnw spring-boot:run

# 3. Frontend on http://localhost:4200 (second terminal)
cd frontend && npm install && npx ng serve --proxy-config proxy.conf.json
```

**Try it without the UI:** `./demo.sh` walks one claim through its whole life with curl, including the requests the rules refuse.

**Tests:** `cd backend && ./mvnw test`. Needs Docker running. The tests use their own Postgres schema (`claims_test`) and Kafka topic, so they never touch the app's data.

### Users and roles

There is no login. The user is sent as two headers, `X-User-Id` and `X-User-Role` (`CLAIMANT`, `OFFICER`, `MANAGER`), and the UI has a switcher for them. Every permission check lives in the service layer, so real authentication (OIDC/JWT) can replace the headers without changing any business rules.

Suggested walkthrough in the UI: `alice`/CLAIMANT submits a claim → `officer1`/OFFICER picks it up and requests info → `alice` replies → `officer1` approves and settles → `manager1`/MANAGER checks exposure.

## Design

```
Angular ──REST──► Spring Boot ──JPA──► PostgreSQL
                      │                 (claims, claim_events, notifications)
                      └──Kafka "claim-events"──► notification consumer ──► notifications table
```

One deployable service with clear modules (`claim`, `notify`, `dashboard`, `web`). Microservices would add a lot of operational work for no benefit at this size. The modules don't reach into each other's tables except through the event, so they can be split out later.

### REST vs Kafka

| | Used for | Why |
|---|---|---|
| **REST** | Every user action and query: submit, pick up, request info, approve, reject, settle, view, dashboards | The user is waiting for an answer: the updated claim, or why the action was refused (409, 403, 422). |
| **Kafka** | `ClaimStatusChanged` events on every status change, consumed by the notification service | These are consequences of a change, not part of it. A slow or failed notification must not block or roll back a claim decision. Payments, fraud checks and reporting can subscribe to the same topic later without changing the claims code. |

- **Events are published only after the database commit** (`@TransactionalEventListener(AFTER_COMMIT)`), so consumers never act on a change that was rolled back.
- **Events are keyed by claim ID**, so all events for one claim stay in order.
- **The consumer is idempotent.** Kafka delivers at least once, so each event has an ID and `notifications.event_id` is UNIQUE.

### Claim lifecycle

```
SUBMITTED ──► UNDER_REVIEW ──► APPROVED ──► SETTLED
                 │   ▲     └──► REJECTED
                 ▼   │
            INFO_REQUESTED
```

- The allowed moves are defined once, in `ClaimStatus`, and unit-tested. An illegal move returns **409**.
- Only the officer assigned to a claim can decide it.
- The approved amount can't exceed the claimed amount, and settlement can't exceed the approved amount (**422**).
- A claimant who asks for someone else's claim gets **404**, not 403, so claim IDs can't be probed.
- Every change writes a row to `claim_events`, an append-only audit trail of who did what, when and why. It's shown as the claim's history.
- Optimistic locking (`@Version`) means that if two officers act on the same claim at once, one gets a 409 instead of silently overwriting the other.
- Money is `NUMERIC`/`BigDecimal`, never floating point.

### Dashboards

- **Workload** (officers and managers): per officer, how many claims are under review, waiting on info, waiting to be settled, and closed, plus the average days to close. Also shows the unassigned count.
- **Exposure** (managers only): open claims per market, **in that market's own currency**. A claim counts at its approved amount once approved, and at its claimed amount (the worst case) before that.

## Assumptions

- The brief doesn't name the six markets, so I assumed **MY, SG, HK, TH, ID, PH**, each in its local currency.
- **Exposure is not converted into one currency.** That would need FX rates, and a made-up rate would give managers a wrong number.
- One currency per claim: the market's.
- "Performance" means throughput (claims closed) and speed (average days to close).

## Known gaps and next steps

| Gap | What I'd do with more time |
|---|---|
| An event is lost if the app dies between the commit and the Kafka send | Transactional outbox table plus a relay |
| Mock auth via headers | OIDC/JWT through Spring Security, with roles from token claims |
| Dashboards aggregate in Java | `GROUP BY` queries, or a read model fed from Kafka, as volume grows |
| No document or photo uploads for claims | Object storage with links on the claim |
| No pagination on lists | Page and sort parameters on the list endpoints |
| No frontend tests | Component tests for the action buttons, plus one Playwright end-to-end flow |
| Notifications are stored but not sent | An email or SMS sender as another Kafka consumer |

## AI usage

Built with an AI assistant (Claude). **[AI_JOURNAL.md](AI_JOURNAL.md)** records what I asked, what I accepted, and what I challenged or overrode, and why.
