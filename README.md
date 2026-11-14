# healverse-server

Spring Boot 3.5 / Java 17 REST API for HealVerse (diet plans, food/water/exercise logging, dashboard, insights, AI chat).
AI features use Groq through its OpenAI-compatible API; data lives in PostgreSQL (Neon).

## Configuration

Copy `.env.example` to `.env` (git-ignored) and fill it in. Real environment variables override `.env`.

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | yes | Base64 HS256 key, at least 32 bytes (`openssl rand -base64 32`) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes | PostgreSQL JDBC URL and credentials |
| `GROQ_API_KEY` | yes | Groq API key. The app fails at startup without it |
| `AI_TEXT_MODEL` | no | Default `openai/gpt-oss-120b` (chat, diet plans) |
| `AI_VISION_MODEL` | no | Default `qwen/qwen3.8-27b` (food photos; a Groq preview model) |
| `FORWARD_HEADERS_STRATEGY` | no | `prod` profile. `none` by default; `framework` behind a trusted reverse proxy (see below) |

## Run locally

```
./mvnw spring-boot:run        # http://localhost:8080, Swagger UI at /swagger-ui.html
./mvnw test                   # H2 in memory, no external services needed

# Optional: check the real Groq API (text, food description, food photo, chat; costs a few cents, ~2.5 minutes)
GROQ_API_KEY=gsk_... ./mvnw test -Dtest=GroqLiveSmokeTests
```

## Database migrations

The schema is managed by Flyway (`src/main/resources/db/migration`). Hibernate only validates it against the entities.

- Change an entity -> add `V2__describe_change.sql` (never edit an applied file). Without it the app fails at startup
  with a schema-validation error, by design.
- An empty database runs `V1__baseline.sql` and everything after it. A database that already has the tables but no
  Flyway history (created by Hibernate before Flyway was added) is baselined at V1 on first start and V1 is skipped.
- On Neon, point `DB_URL` at the direct (non-pooler) host: Flyway takes a PostgreSQL advisory lock, which does not
  work through a transaction-mode pooler.

## Docker

```
docker compose up --build                       # database from .env
docker compose --profile local-db up --build    # plus a local Postgres (see docker-compose.yml)
```

The image is multi-stage (JDK 17 build, JRE 17 Alpine runtime), runs as a non-root user with the `prod` profile, and
has a health check on `/actuator/health/liveness`. Probes for an orchestrator: liveness `/actuator/health/liveness`,
readiness `/actuator/health/readiness` (includes the database). No other actuator endpoint is public.

## Production notes

- The `prod` profile turns off Swagger/OpenAPI, SQL logging and error details and enables graceful shutdown.
- Login throttling keys on the socket's client address. Behind a reverse proxy that is the proxy, so set
  `FORWARD_HEADERS_STRATEGY=framework` (only if the app is reachable exclusively through that proxy: otherwise
  `X-Forwarded-For` can be forged).
- Groq rate limits: a request reserves its full `max_completion_tokens` against the tokens-per-minute limit
  (on-demand `gpt-oss-120b`: 8000 TPM). `app.ai.max-tokens` (3000) and `app.ai.food-max-tokens` (1200) are kept small
  for that reason; a busy app needs a higher Groq tier.
- Food photos are analysed but not stored (no image storage yet), so `imageUrl` is null.
- The voice assistant (`/api/voice-chat/**`) is off (`app.voice.enabled=false`) and answers 410 Gone.
- Database ER diagrams are generated into `docs/erd/` with `java tools/ERDiagramGenerator.java`.
