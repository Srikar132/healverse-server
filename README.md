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
| `DB_DDL_AUTO` | no | `prod` profile only. Default `validate`; use `update` once for an empty database |

## Run locally

```
./mvnw spring-boot:run        # http://localhost:8080, Swagger UI at /swagger-ui.html
./mvnw test                   # H2 in memory, no external services needed
```

## Docker

```
docker compose up --build                       # database from .env
docker compose --profile local-db up --build    # plus a local Postgres (see docker-compose.yml)
```

The image is multi-stage (JDK 17 build, JRE 17 Alpine runtime), runs as a non-root user with the `prod` profile, and
has a health check on `/actuator/health/liveness`. Probes for an orchestrator: liveness `/actuator/health/liveness`,
readiness `/actuator/health/readiness` (includes the database). No other actuator endpoint is public.

## Production notes

- The `prod` profile turns off Swagger/OpenAPI, SQL logging and error details, enables graceful shutdown, and sets
  Hibernate to `validate`. There is no migration tool yet: schema changes need to be applied by hand, or add Flyway.
- Login throttling keys on `request.getRemoteAddr()`. Behind a reverse proxy that is the proxy's address; configure
  `server.forward-headers-strategy` only if the app is reachable exclusively through that proxy.
- The voice assistant (`/api/voice-chat/**`) is off (`app.voice.enabled=false`) and answers 410 Gone.
- Database ER diagrams are generated into `docs/erd/` with `java tools/ERDiagramGenerator.java`.
