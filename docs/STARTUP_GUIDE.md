# E-Bookstore — Local Startup Guide

**CR:** CR-004 / TASK-CR004-003  
**Date:** 2026-08-30

---

## Prerequisites

Ensure the following are installed and available on your `PATH` before starting.

| Tool | Minimum Version | Check Command |
|---|---|---|
| Java (JDK) | 21 | `java -version` |
| Node.js | 20 | `node -v` |
| npm | 10 | `npm -v` |
| PostgreSQL | 15+ | `pg_isready` |

> The Maven wrapper (`mvnw`) is bundled in `ebookstore/backend/` — no separate Maven install needed.

---

## Step 1 — Verify PostgreSQL is running

Open a terminal and run:

```powershell
pg_isready
```

Expected output:
```
localhost:5432 - accepting connections
```

If PostgreSQL is not running, start it via Windows Services or:
```powershell
pg_ctl start -D "C:\Program Files\PostgreSQL\<version>\data"
```

---

## Step 2 — Create the database (one-time only)

Run this **once** before the very first backend startup. Skip on subsequent starts.

```powershell
psql -U postgres -c "CREATE DATABASE ebookstore;"
```

Expected output:
```
CREATE DATABASE
```

> If you see `ERROR: database "ebookstore" already exists` — that's fine, skip this step.

**Note:** The `postgres` user password is configured as `root` in  
`ebookstore/backend/src/main/resources/application-local.properties`.  
If your local PostgreSQL `postgres` user has a different password, update  
`spring.datasource.password` in that file before proceeding.

---

## Step 3 — Start the Backend

Open **Terminal 1** and run:

```powershell
cd ebookstore/backend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

### What happens on first boot

Flyway runs migrations V1 → V8 automatically, creating all tables:

```
V1  baseline marker
V2  users + gift_point_balance
V3  addresses
V4  categories (seeds 8 categories)
V5  publishers
V6  books + tsvector search index
V7  cart + cart_items
V8  orders + order_items
```

Then `BookLoader` inserts 113 books from `books.json` into the `books` table.

### Expected startup log (last lines)

```
INFO  --- BookLoader        : Book table empty — loading from books.json
INFO  --- BookLoader        : Inserted 113 books.
INFO  --- BackendApplication: Started BackendApplication in X.XXX seconds
```

**Backend is ready at:** `http://localhost:8081`  
**Health check:** `http://localhost:8081/actuator/health` → `{"status":"UP"}`

---

## Step 4 — Start the Frontend

Open **Terminal 2** and run:

```powershell
cd ebookstore/frontend
npm run dev
```

### Expected output

```
  VITE v8.x.x  ready in XXX ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

**Frontend is ready at: `http://localhost:5173`**

---

## Application URLs

| Service | URL |
|---|---|
| **Frontend (React)** | **http://localhost:5173** |
| Backend API | http://localhost:8081 |
| Backend Health | http://localhost:8081/actuator/health |

---

## Subsequent Starts

On every restart after the first:

1. PostgreSQL must be running (Step 1).
2. Start backend (Step 3) — Flyway and BookLoader skip automatically (no new migrations, table not empty).
3. Start frontend (Step 4).

---

## Troubleshooting

### Backend fails with `Connection refused` on port 5432
PostgreSQL is not running. See Step 1.

### Backend fails with `password authentication failed for user "postgres"`
Your local `postgres` password differs from `root`.  
Edit `ebookstore/backend/src/main/resources/application-local.properties`:
```properties
spring.datasource.password=<your_actual_password>
```

### Backend fails with `database "ebookstore" does not exist`
Run Step 2 to create the database.

### Backend starts on port 8080 instead of 8081
The `local` Spring profile is not active. Ensure you run:
```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Frontend shows CORS errors in the browser console
The backend is not running on port 8081. Start the backend first (Step 3).

### `npm run dev` fails with `Cannot find module`
Run `npm install` once in `ebookstore/frontend/` to restore `node_modules`.

---

## Configuration Reference

| File | Purpose |
|---|---|
| `backend/src/main/resources/application.properties` | Production config (reads from env vars) |
| `backend/src/main/resources/application-local.properties` | Local dev overrides (gitignored) |
| `backend/src/main/resources/application-test.properties` | Test profile (H2 in-memory, Flyway disabled) |
| `frontend/src/api/apiClient.ts` | API base URL (`VITE_API_BASE_URL` env var, defaults to `localhost:8081`) |
| `frontend/vite.config.ts` | Vite config (default port 5173) |
