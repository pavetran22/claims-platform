# AI Working Journal

Tool: Claude (Anthropic). Format per entry: what I asked, what the AI suggested,
whether I **accepted / challenged / overrode** it, and why.

---

## 2026-11-25 09:43 Setup and stack

- **Asked:** Which backend stack to use (C#/.NET or Java/Spring Boot) and how to split the time.
- **AI suggested:** Java/Spring Boot, one service with clear modules, REST for user actions, Kafka for claim events, Postgres.
- **Decision:** Accepted Java/Spring Boot. I know Java (diploma and enterprise Java coursework), so I can defend every line; learning C# under a 5-hour cap would be risky.
- **Decision:** Accepted a single modular service rather than microservices, for now — two hours is not enough to run several services well, and the module boundaries let them be split later.

## Domain model and claim lifecycle

- **Asked:** Data model and status rules for claims.
- **AI suggested:** A status state machine in one enum, an append-only audit table, NUMERIC money, optimistic locking.
- **Decision:** Accepted. The brief doesn't name the six markets, so I **assumed** MY, SG, HK, TH, ID, PH, each with its local currency.
- **Decision:** Exposure will be reported **per currency**, not converted into one total. Converting needs FX rates I don't have, and a made-up rate would give managers a wrong number.

## REST vs Kafka, and the service layer

- **Asked:** Which interactions use REST and which use Kafka.
- **AI suggested:** REST for every user action (the user waits for an answer: the new claim, or why it was refused). Kafka for what happens *because of* a change — notifications now; payments, fraud checks and reporting later — so those can fail or lag without blocking the claimant or officer.
- **Decision:** Accepted. Events are published **after** the database commit so consumers never act on a change that was rolled back. Known gap: if the app dies between commit and send, the event is lost; the production fix is a transactional outbox. I've noted it rather than built it, given the time cap.
- **Decision:** Accepted an idempotent consumer (unique event_id), because Kafka can deliver the same message twice.
- **Decision:** Dashboards query the database live rather than via Kafka. It's simpler, and always consistent at this scale.
- **Decision:** Mock auth via headers. Real auth (OIDC/JWT) is out of scope for the time cap; the role checks live in the service, so swapping it in won't change the business rules.

## Challenged: test data leaking into the app

- **Observed:** After running the tests, the demo's workload dashboard listed officers like `officer-05d49e`. The AI's original test setup wrote into the same database and Kafka topic the app uses.
- **Challenged:** Asked for tests to be isolated.
- **Fix:** A `test` profile with its own Postgres schema (`claims_test`) and Kafka topic/consumer group. Verified by re-running the tests and the demo: only demo data appears.

## Summary

- Time spent: ~XX minutes (cap: 5 hours).
- Backend first, as the brief asks; the frontend is a thin client and the backend enforces every rule.
- Known gaps and next steps are listed in README.md.

## Challenged: test data leaking into the app

- **Observed:** After running the tests, the demo's workload dashboard listed officers like `officer-05d49e`. The AI's original test setup wrote into the same database and Kafka topic the app uses.
- **Challenged:** Asked for tests to be isolated.
- **Fix:** A `test` profile with its own Postgres schema (`claims_test`) and Kafka topic/consumer group. Verified by re-running the tests and the demo: only demo data appears.

## Summary

- Time spent: ~2 hours (cap: 5 hours).
- Backend first, as the brief asks; the frontend is a thin client and the backend enforces every rule.
- Known gaps and next steps are listed in README.md.
