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
