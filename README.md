# MyShelf

MyShelf (MediaWebApp) is a media catalog and personal tracking app for movies, TV shows, anime, and games. It keeps one shared catalog, lets each user maintain a private shelf of what they plan to consume, are consuming, finished, or dropped, and recommends titles from that history.

The backend is a Spring Boot API. The frontend is a React single-page app. In production both ship as one container: the UI is built into the API’s static resources and served from the same origin.

## Features

- **Discover** — browse the local catalog by type, genre, year, and title, sorted by Bayesian quality, year, or title.
- **Search and import** — look up titles on TMDB, Jikan, or RAWG and import them into the catalog. Repeat imports of the same external id return the existing row.
- **Shelf** — track a title as planned, watching, completed, or dropped, with an optional 1–10 rating and a review of up to 2,000 characters.
- **For You** — personalized recommendations from titles the user rated 7 or higher, blended with genre overlap and public rating quality.
- **Similar titles** — nearest catalog neighbors of any title, grouped by media type.
- **Guest browsing** — catalog reads, genre lists, and similar-title results do not require an account. The shelf and personalized recommendations do.

## Architecture

Requests stay in one direction: controller, service, repository, entity. Controllers return DTO records. JPA entities never leave the service layer.

```mermaid
flowchart LR
  Browser["React SPA"] --> API["Spring Boot API"]
  API --> DB[("PostgreSQL + pgvector")]
  API --> TMDB["TMDB"]
  API --> Jikan["Jikan"]
  API --> RAWG["RAWG"]
  API --> OpenAI["OpenAI embeddings"]
```

| Layer | Responsibility |
| --- | --- |
| `controller` | HTTP routing, status codes, request validation |
| `service` | Catalog, shelf, auth, import, recommendations |
| `external` | Provider HTTP adapters and response mappers |
| `repository` | Spring Data and native SQL, including vector search |
| `security` | Stateless JWT filter, catalog-admin checks, search rate limit |
| `schedule` | Startup catalog seed and daily rating refresh |

External HTTP calls run outside database write transactions. Import persists the catalog row and the provider id mapping in one transaction after the provider response is already in memory.

## How it works

### Catalog

Four media types are seeded: Movie, TV Show, Anime, and Game. A title has a description, release year (1800–2100), genres, a poster URL, and a public rating copied from the source provider. Ratings are stored on a 0–10 scale. RAWG’s 0–5 scores are converted at map time.

Discover (`GET /api/media`) filters by type, genre, year, and a case-insensitive title substring, then pages the result. Default sort is **quality**: a Bayesian average that pulls low-vote titles toward the catalog-wide mean so a single 10.0 with a handful of votes does not outrank a widely rated title. The prior strength is 50 votes (`m = 50`). Unrated titles sort last. Page size defaults to 20 and cannot exceed 50.

Direct catalog create, update, and delete are limited to one configured admin user. Everyone else adds titles through import.

### External providers

Search always hits one provider, chosen by type. Results are capped at 10 and are not merged across providers.

| Type | Provider | External id |
| --- | --- | --- |
| `MOVIE` | TMDB | `movie:{tmdbId}` |
| `TV` | TMDB | `tv:{tmdbId}` |
| `ANIME` | Jikan (MyAnimeList) | numeric MAL id |
| `GAME` | RAWG | numeric RAWG id |

`GET /api/media/external/{provider}/{externalId}` fetches one title and does not save it. `POST /api/media/import` saves it. If that provider id is already in `media_external_ids`, the existing catalog row is returned with HTTP 200. A new row returns HTTP 201. A unique-constraint race between two imports of the same id resolves to the existing row.

Search is rate limited per authenticated user, or per client IP when the caller is anonymous: 20 requests per minute and 300 per day. The counters live in memory and reset when the process restarts. Each app instance counts separately.

Provider responses are cached in-process with Caffeine:

| Cache | TTL | Max entries |
| --- | --- | --- |
| External search | 15 minutes | 1,000 |
| External details | 12 hours | 1,000 |
| Catalog item by id | 30 minutes | 1,000 |

The daily rating job bypasses the details cache so it does not write a stale score back into the database.

### Personal shelf

`user_media` is one row per user and catalog title. Status is `PLANNED`, `WATCHING`, `COMPLETED`, or `DROPPED`. `POST /api/user-media` creates the row or updates it. The user id always comes from the JWT. Clients do not send it.

The shelf list defaults to `COMPLETED`, sorted by when the title was added (newest first). It can also sort by the user’s own rating. Status counts on the page ignore the status filter and the rating bounds, so the tabs stay stable while the user switches them. Genre and title filters still apply to those counts.

### Recommendations

On create and import, the app builds a short text from the title, description, and genre names, then asks OpenAI `text-embedding-3-small` for a 1,536-dimension vector. Rating, vote count, and media type are left out of that text so they cannot dominate similarity. The vector is stored in `media_embeddings` and indexed with pgvector HNSW using cosine distance. If the embedding call fails, the catalog row is still saved. That title is simply skipped by similarity search until an embedding exists.

**Similar** (`GET /api/recommendations/similar/{mediaId}`) returns the 20 nearest catalog items, grouped into movies, TV, anime, and games. Public. A title with no embedding returns 404.

**For You** (`GET /api/recommendations/user?type=MOVIE`) is authenticated. The `type` parameter is required and receives a larger result set than the other three.

1. Load shelf rows the user rated 7 or higher.
2. Build a preference vector: the rating-weighted average of those embeddings.
3. For each media type, pull the 50 nearest catalog items the user does not already have on their shelf.
4. Score each candidate and keep the top results: 20 for the requested type, 5 for each of the others.

The score is:

```
0.50 × cosine similarity
+ 0.35 × genre overlap
+ 0.15 × Bayesian quality
```

Genre overlap is the share of the candidate’s genres that appear on titles the user rated highly **in that same media type**. If the user has no highly rated history for that type, the genre term is dropped and its weight is added to similarity. If the user has no highly rated titles at all, For You falls back to the Bayesian top-rated list for each type, still excluding titles already on the shelf.

### Authentication

Registration stores a BCrypt password hash. Login is email plus password and returns a JWT signed with HMAC-SHA256. The token lasts 24 hours and carries the user id, email, and a `ver` claim equal to `users.token_version`. Logout increments that version, which invalidates every outstanding token for the account. The signing secret must be at least 32 characters.

The API is stateless. There is no server session and no CSRF token. The browser stores the JWT and sends it as `Authorization: Bearer`.

### Background work

**Catalog seed.** When `catalog.seed.on-startup` is true, a daemon thread imports top-rated lists from TMDB, Jikan, and RAWG after boot. It does not block health checks. It skips the run when the catalog already has at least `per-type × 4` rows. Imports are idempotent. Provider calls are spaced (about 80 ms for TMDB and RAWG, 800 ms for Jikan) so the seed stays inside public rate limits. Production turns this on; tests leave it off. The default target is 250 titles per type.

**Rating refresh.** A cron job (default 03:00 UTC) re-fetches external ratings for rows whose `rating_last_updated_at` is older than three months. Each row is fetched with up to four attempts and exponential backoff (1s, 2s, 4s). A failure on one title does not stop the rest. HTTP stays outside the write transaction. After a successful update the by-id cache entry for that title is evicted.

## Tech stack

| Area | Choice |
| --- | --- |
| API | Java 17, Spring Boot 4, Spring Web MVC, Spring Security, Spring Data JPA |
| Database | PostgreSQL 15+ with `pgcrypto` and `pgvector` |
| Migrations | Flyway (`ddl-auto=validate`; the schema is owned by SQL migrations) |
| Auth | JJWT 0.12, BCrypt, stateless sessions |
| Cache | Caffeine |
| Frontend | React 19, TypeScript, Vite 7, React Router 7, TanStack Query, Zustand, React Hook Form, Zod, Tailwind CSS 4 |
| Tests | JUnit, Spring Boot test, Testcontainers PostgreSQL |
| Delivery | Multi-stage Docker image, GitHub Actions, Fly.io |

## Repository layout

```
.
├── src/main/java/com/mediawebapp
│   ├── controller          REST and SPA forward
│   ├── service             application logic
│   ├── repository          JPA and native queries
│   ├── entity              persistence model
│   ├── dto                 request and response records
│   ├── mapper              entity ↔ DTO
│   ├── external            TMDB, Jikan, RAWG, OpenAI
│   ├── security            JWT, CORS, rate limit
│   ├── schedule            seed and rating refresh
│   ├── config              typed properties and cache
│   └── exception           API error translation
├── src/main/resources
│   ├── application.properties
│   └── db/migration        Flyway V1–V9
├── src/test                unit and Testcontainers integration tests
├── frontend                Vite React app
├── docker                  container entrypoint and Postgres image
├── Dockerfile              frontend build + API jar
└── fly.toml                app deploy config
```

Frontend routes:

| Path | Access | Screen |
| --- | --- | --- |
| `/` | Public | Landing, with a quality preview of the catalog |
| `/discover` | Public | Filterable catalog |
| `/search` | Public | Provider search and import |
| `/media/:id` | Public | Title detail and similar titles |
| `/login`, `/register` | Public | Account |
| `/shelf` | Signed in | Personal library (`/library` redirects here) |
| `/for-you` | Signed in | Personalized recommendations |

In development, Vite proxies `/api` to `http://localhost:8080`. In production, `SpaController` forwards the routes above to `index.html` so client-side routing works on refresh. Anything under `/api` stays on the API.

## Data model

```mermaid
erDiagram
  users ||--o{ user_media : tracks
  media ||--o{ user_media : "on shelves"
  media ||--o{ media_genres : has
  genres ||--o{ media_genres : labels
  media_types ||--o{ media : classifies
  media ||--o| media_embeddings : embeds
  media ||--o{ media_external_ids : "maps to"
```

| Table | Role |
| --- | --- |
| `users` | Email (login), unique username (display name), password hash, `token_version` |
| `media_types` | Seeded names: Movie, TV Show, Anime, Game |
| `media` | Catalog title, year, description, poster, external rating and vote count |
| `genres`, `media_genres` | Shared genre vocabulary |
| `media_external_ids` | One provider id per source per title (`TMDB`, `JIKAN`, `RAWG`) |
| `media_embeddings` | 1,536-d vector, HNSW cosine index |
| `user_media` | Shelf status, rating, review; unique per user and title |

`updated_at` is maintained by a PostgreSQL trigger. Migrations are additive from `V1` through `V9`. Do not baseline-skip versions on an empty database.

`V2` inserts a seed account (`devuser`, id `00000000-0000-0000-0000-000000000001`) with a placeholder password hash. That id is the default catalog admin. The placeholder hash is not a usable password.

## API

Base path: `/api`. Errors use one JSON shape:

```json
{
  "error": "Validation failed",
  "status": 400,
  "details": { "email": "Email is required" }
}
```

`details` is omitted when the failure is not field-specific.

| Status | When |
| --- | --- |
| 400 | Validation, bad query values, malformed JSON |
| 401 | Missing, invalid, or expired token; wrong login |
| 403 | Authenticated caller who is not the catalog admin |
| 404 | Unknown id, or a title that has no embedding yet |
| 409 | Email or username already taken; unique-constraint race |
| 429 | Search rate limit |
| 503 | Upstream provider failure (response text does not include the provider payload) |

Health: `GET /actuator/health` (liveness and readiness probes enabled, details hidden).

### Auth

| Method | Path | Auth | Body / result |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | `{ email, username, password }` → `201` empty. Password at least 8 characters. |
| `POST` | `/api/auth/login` | Public | `{ email, password }` → `{ "token": "..." }` |
| `POST` | `/api/auth/logout` | Bearer | `204`. Bumps `token_version`. |

### Catalog

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| `GET` | `/api/media` | Public | Discover. Query: `type`, `genre`, `year`, `q`, `sort` (`QUALITY`, `YEAR`, `TITLE`), `direction`, `page`, `size`. |
| `GET` | `/api/media/{id}` | Public | One title. `inLibrary` is set when a Bearer token is present. |
| `GET` | `/api/media/search` | Public | `query`, `type` (`MOVIE`, `TV`, `ANIME`, `GAME`). Up to 10 results. Rate limited. |
| `GET` | `/api/media/external/{provider}/{externalId}` | Public | `provider` is `tmdb`, `jikan`, or `rawg`. TMDB ids keep the `movie:` / `tv:` prefix. The path pattern allows the colon. |
| `POST` | `/api/media/import` | Bearer | `{ "provider": "tmdb", "externalId": "movie:550" }` → `201` created or `200` already imported. |
| `POST` | `/api/media` | Catalog admin | Direct create. Normal clients use import. |

Discover and library list responses are page envelopes, not bare arrays:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

The shelf list adds a `counts` object: `planned`, `watching`, `completed`, `dropped`.

### Shelf

All routes require a Bearer token. `{mediaId}` is the catalog id.

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/user-media` | Upsert `{ mediaId, status, rating?, review? }`. `201` created, `200` updated. |
| `GET` | `/api/user-media` | `status` (default `COMPLETED`), `type`, `genre`, `minRating`, `maxRating`, `q`, `sort` (`ADDED`, `RATING`), `direction`, `page`, `size`. |
| `GET` | `/api/user-media/genres` | Distinct genre names on the caller’s shelf, all statuses. |
| `GET` | `/api/user-media/{mediaId}` | One shelf row. |
| `DELETE` | `/api/user-media/{mediaId}` | `204`. |

### Recommendations and genres

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| `GET` | `/api/genres` | Public | Distinct catalog genre names. |
| `GET` | `/api/recommendations/similar/{mediaId}` | Public | `{ movies, tvShows, anime, games }`. |
| `GET` | `/api/recommendations/user` | Bearer | Required `type`. Same grouping. Requested type has up to 20 items; each other type has up to 5. |

## Local development

Requirements: JDK 17, Maven (or the included wrapper), Node.js 22, and PostgreSQL 15 or newer with the `vector` and `pgcrypto` extensions.

Create the database, then enable the extensions if your server does not allow them from the migration role:

```sql
CREATE DATABASE "MediaWebAppDB";
\c MediaWebAppDB
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;
```

Flyway applies `src/main/resources/db/migration` on startup.

### Backend

Copy secrets into a `.env` file in the project root (`KEY=value`, no `export`). Spring loads it optionally; real environment variables override it. Minimum for a working API:

```properties
JWT_SECRET=replace-with-at-least-32-random-characters
TMDB_API_KEY=
RAWG_API_KEY=
OPENAI_API_KEY=
DB_PASSWORD=
```

Defaults if unset: JDBC URL `jdbc:postgresql://localhost:5432/MediaWebAppDB`, user `postgres`, port `8080`. Jikan does not use an API key.

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

The dev server runs on port 5173 and proxies `/api` to the backend. Leave `VITE_API_BASE_URL` empty for that proxy. Set it only when the API is on another origin. See `frontend/.env.example`.

Open `http://localhost:5173`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port. Fly.io sets this. |
| `SPRING_DATASOURCE_URL` | local JDBC URL above | JDBC URL. If unset, a `postgres://` `DATABASE_URL` is converted at startup. |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` | empty | Database password |
| `JWT_SECRET` | empty (startup fails if shorter than 32 characters) | HMAC signing key |
| `TMDB_API_KEY` | empty | TMDB v3 key |
| `RAWG_API_KEY` | empty | RAWG key |
| `OPENAI_API_KEY` | empty | Embeddings. Imports still succeed without it; similarity will 404 until vectors exist. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated browser origins |
| `CATALOG_ADMIN_USER_ID` | seed user UUID | Only this user may `POST`/`PUT`/`DELETE` `/api/media` directly |
| `CATALOG_SEED_ON_STARTUP` | `false` | Background top-list import. `true` on Fly. |
| `CATALOG_SEED_PER_TYPE` | `250` | Target titles per type before the seed no-ops |
| `VITE_API_BASE_URL` | empty | Frontend API origin. Empty uses the Vite proxy or same-origin `/api`. |

Rating refresh is on unless `ratings.refresh.enabled=false` (the test profile sets that). Cron and staleness: `ratings.refresh.cron` (`0 0 3 * * *`) and `ratings.refresh.stale-months` (`3`).

## Testing

Backend tests use Testcontainers and a real PostgreSQL database. Docker must be running.

```bash
./mvnw test
```

Frontend CI checks a production typecheck and Vite build:

```bash
cd frontend
npm ci
npm run build
```

GitHub Actions (`.github/workflows/ci.yml`) runs both jobs on pushes and pull requests to `main`. A green `main` push then deploys to Fly.io.

## Deployment

The app image is a three-stage build:

1. Node 22 compiles the frontend.
2. Maven packages the jar with `frontend/dist` copied into `src/main/resources/static`.
3. A Temurin 17 JRE image runs that jar as a non-root user.

`docker/entrypoint.sh` turns Fly’s `DATABASE_URL=postgres://user:pass@host:port/db` into `SPRING_DATASOURCE_*` when those are not already set, then starts the JVM with a 75% RAM cap.

```bash
docker build -t mediawebapp .
docker run --env-file .env -p 8080:8080 mediawebapp
```

Production app config is `fly.toml` (`myshelf-app`, region `lhr`). The app listens on 8080 inside the machine, forces HTTPS, and keeps one machine running. CORS is locked to `https://myshelf-app.fly.dev`. Catalog seed runs on startup.

PostgreSQL is a separate Fly Postgres Flex 17 image (`docker/postgres`) with pgvector 0.8.1 compiled in. Its app config is `docker/postgres/fly.toml`.

Deploy from CI uses `flyctl deploy --remote-only`. Locally, with a Fly token:

```bash
flyctl deploy --remote-only --app myshelf-app
```

Set `JWT_SECRET`, `TMDB_API_KEY`, `RAWG_API_KEY`, `OPENAI_API_KEY`, and the database URL as Fly secrets. Do not commit them.

## Security notes

- Passwords are hashed with BCrypt. The raw password does not leave `AuthService.register`.
- JWTs are rejected when `ver` does not match `token_version`, so logout is server-side.
- Catalog mutation is admin-only. Import is the supported way for signed-in users to add titles.
- Provider error responses returned to clients are written by the adapters. They do not echo upstream bodies or request URLs.
- Actuator exposure is health only, with details disabled.
- Search limits are per process. They reduce accidental provider hammering; they are not a global quota across machines.
