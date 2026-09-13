# E-Bookstore — Project Approach

**Project:** AI-Assisted E-Commerce Bookstore (Capstone)
**Methodology:** Controlled Spec-Driven Development
**Last Updated:** 2026-08-24

---

## 1. Philosophy

> Think → Specify → Plan → Design → Code → Test → Verify

This project is built using a **Controlled Spec-Driven Development** process.
No code is written until the corresponding specification, plan, and design
have been produced and explicitly approved by the project owner.

The AI coding agent is an engineering assistant — not an autonomous
decision-maker. Every architectural decision, business rule, and
scope boundary must be confirmed by a human before implementation begins.

---

## 2. The Non-Negotiable Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   BUSINESS REQUIREMENTS                                     │
│          ↓                                                  │
│   PHASE 1 — SPECIFICATION  ──► STOP ► HUMAN APPROVAL       │
│          ↓                                                  │
│   PHASE 2 — PLAN           ──► STOP ► HUMAN APPROVAL       │
│          ↓                                                  │
│   PHASE 3 — DESIGN         ──► STOP ► HUMAN APPROVAL       │
│          ↓                                                  │
│   PHASE 4 — IMPLEMENTATION (task by task)                  │
│          ↓                                                  │
│   PHASE 5 — TESTING + SPEC VALIDATION                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Every phase is gated. The AI cannot advance to the next phase without
an explicit written approval from the project owner.

Valid approval keywords:

```
SPECIFICATION APPROVED
PLAN APPROVED
DESIGN APPROVED
```

---

## 3. Phase Breakdown

---

### PHASE 1 — SPECIFICATION

**Status:** ⏳ In Progress
**Output file:** `ebookstore/specs/01-specification/specification.md`

**Purpose:**
Translate the business requirements into a formal, traceable specification
with unique Requirement IDs, acceptance criteria, and classifications.

**Covers:**

| Module | Requirement IDs |
|---|---|
| Users (Guest + Registered) | REQ-USR-001 → REQ-USR-005 |
| Book Catalogue | REQ-CAT-001 → REQ-CAT-006 |
| Search & Filtering | REQ-SRC-001 → REQ-SRC-002 |
| Shopping Cart | REQ-CRT-001 → REQ-CRT-005 |
| Order Management | REQ-ORD-001 → REQ-ORD-004 |
| Checkout & Delivery | REQ-CHK-001 → REQ-CHK-003 |
| Payment (Simulated) | REQ-PAY-001 → REQ-PAY-005 |
| Gift Points | REQ-GFT-001 → REQ-GFT-003 |
| Recommendations | REQ-REC-001 → REQ-REC-002 |
| Catalogue Seed Pipeline | REQ-SEED-001 → REQ-SEED-002 |
| Non-Functional Requirements | REQ-NFR-001 → REQ-NFR-006 |

**Rules:**
- No application code written in this phase.
- No architecture decisions finalized.
- No database schemas created.
- Open questions recorded, not silently assumed.

**Gate:** `SPECIFICATION APPROVED`

---

### PHASE 2 — PLAN

**Status:** 🔒 Locked until SPECIFICATION APPROVED
**Output file:** `ebookstore/specs/02-plan/implementation-plan.md`

**Purpose:**
Define WHAT will be built and in WHAT ORDER. Break the specification
into implementation milestones and tasks with full requirement traceability.

**Milestones:**

| Milestone | Description |
|---|---|
| M1 — Foundation | Project scaffold, DB setup, Flyway migrations baseline |
| M2 — Auth & Users | Registration, Login (JWT), Token refresh, Logout |
| M3 — Catalogue | Book entity, Category, Publisher, Browse, Search, Filter, Related |
| M4 — Cart | Guest localStorage cart (frontend), Registered DB cart, Merge on login |
| M5 — Orders | Create order, History, Buy Again, Cancel (pre-delivery) |
| M6 — Checkout | Delivery address, Tentative delivery date, Order summary |
| M7 — Payment | Simulated payment flow, Credit/Debit card, Confirmation |
| M8 — Gift Points | Earn (₹50=1pt), Redeem (1pt=₹1), Balance endpoint |
| M9 — Recommendations | Rule-based engine (same category/author), Display in catalogue + basket |
| M10 — Seed Pipeline | Python Open Library script, JSON output, Spring Boot loader |
| M11 — Frontend | All React pages and components |
| M12 — Testing | Unit, integration, API tests per module |

**Task format:**

```
REQ-CAT-001
    ↓
TASK-CAT-001  (implement Book entity + repository)
    ↓
TASK-CAT-002  (implement BookService)
    ↓
TEST-CAT-001  (unit tests for BookService)
```

**Gate:** `PLAN APPROVED`

---

### PHASE 3 — DESIGN

**Status:** 🔒 Locked until PLAN APPROVED
**Output files:** `ebookstore/specs/03-design/`

**Purpose:**
Define HOW the system will be built. Produce the full technical design
derived from the approved specification and plan.

**Documents:**

| File | Contents |
|---|---|
| `architecture.md` | System architecture, module map, deployment topology |
| `database-design.md` | Entity definitions, ERD, table schemas, Flyway strategy |
| `api-design.md` | Full REST API contract — endpoints, request/response, auth |
| `component-design.md` | Backend layers per module + Frontend pages/components/hooks |
| `diagrams/` | ERD, sequence diagrams, component diagram |

**Architectural decisions locked in design:**
- Modular monolith (not microservices)
- JWT-based authentication
- Guest cart in browser localStorage only (never in DB)
- All monetary values in Indian Rupees (₹)
- Simulated payment gateway (no real gateway)
- Catalogue populated by offline seed script (no admin UI)

**Gate:** `DESIGN APPROVED`

---

### PHASE 4 — IMPLEMENTATION

**Status:** 🔒 Locked until DESIGN APPROVED

**Purpose:**
Build the application milestone by milestone, task by task.

**Rules:**
- Every task must reference a Requirement ID and Plan Task ID.
- Tests written alongside implementation — not after.
- If implementation reveals a spec/design gap: STOP, report, wait for decision.
- No feature implemented unless traceable to an approved requirement.

**Execution order:**

```
M1 Foundation
    ↓
M2 Auth & Users
    ↓
M3 Catalogue  ←── M10 Seed Pipeline (parallel)
    ↓
M4 Cart
    ↓
M5 Orders
    ↓
M6 Checkout
    ↓
M7 Payment + M8 Gift Points
    ↓
M9 Recommendations
    ↓
M11 Frontend (per feature alongside backend)
    ↓
M12 Testing + Spec Validation Report
```

---

### PHASE 5 — TESTING & SPEC VALIDATION

**Status:** 🔒 Locked until implementation milestones complete

**Purpose:**
Verify that every approved requirement is correctly implemented and tested.

**Output:** Specification Validation Report

```
REQ-USR-001  | TASK-AUTH-001 | UserServiceTest    | ✅ PASS
REQ-CAT-001  | TASK-CAT-001  | BookServiceTest     | ✅ PASS
REQ-CRT-003  | TASK-CRT-003  | CartServiceTest     | ✅ PASS
...
```

---

## 4. Key Decisions Already Made

| Decision | Value |
|---|---|
| **Platform** | Physical books only (no e-book download/DRM) |
| **Currency** | Indian Rupees (₹) only |
| **Guest Cart** | Browser localStorage — never persisted to DB |
| **Brand** | Publisher (e.g. Penguin, HarperCollins) |
| **Payment** | Simulated — mock flow, no real gateway |
| **Order Cancellation** | Allowed before delivery only (no 48-hr timer) |
| **Gift Points — Earn** | ₹50 spent = 1 gift point |
| **Gift Points — Value** | 1 gift point = ₹1 |
| **Catalogue Source** | Open Library via offline Python seed script |
| **Admin UI** | None — out of scope |
| **Architecture** | Modular monolith (not microservices) |

---

## 5. Open Questions (Unresolved)

These require a decision before their corresponding implementation tasks begin:

| # | Question | Blocks |
|---|---|---|
| OQ-001 | Are delivery charges applicable? If yes, how calculated? | M6 Checkout |
| OQ-002 | Can a user save multiple delivery addresses? | M6 Checkout |
| OQ-003 | Do gift points expire? | M8 Gift Points |
| OQ-004 | What happens to redeemed points if an order is cancelled? | M8 Gift Points |
| OQ-005 | Maximum gift points redeemable per order? | M8 Gift Points |
| OQ-006 | Search scope — title + author only, or also publisher/ISBN/category? | M3 Catalogue |
| OQ-007 | How many recommendations to display? | M9 Recommendations |

---

## 6. Change Control

Any new requirement after implementation has started must follow:

```
NEW REQUIREMENT
    ↓
SPECIFICATION UPDATE (CR-XXX.md in specs/04-change-control/)
    ↓
HUMAN APPROVAL
    ↓
PLAN UPDATE
    ↓
HUMAN APPROVAL
    ↓
DESIGN UPDATE
    ↓
HUMAN APPROVAL
    ↓
IMPLEMENTATION → TESTING → REGRESSION VALIDATION
```

Approved specifications are never silently overwritten.
All versions are preserved with history.

---

## 7. Project Structure

```
Capstone_Project/
├── PROJECT_APPROACH.md                        ← this file
├── ARCHITECTURE_DIAGRAMS.md                   ← all architecture diagrams
├── CONTROLLED_SPEC_DRIVEN_DEVELOPMENT_PROTOCOL.md
└── ebookstore/
    ├── specs/
    │   ├── 00-source/                         ← raw source requirements
    │   ├── 01-specification/                  ← formal specification
    │   ├── 02-plan/                           ← implementation plan
    │   ├── 03-design/                         ← architecture + API + DB design
    │   │   └── diagrams/                      ← design diagrams
    │   └── 04-change-control/                 ← change requests + log
    ├── approvals/                             ← signed-off approval records
    ├── frontend/                              ← React + TypeScript
    ├── backend/                               ← Java + Spring Boot + Maven
    ├── data/seed/books.json                   ← seeded catalogue (113 books)
    ├── script/books_fetch.py                  ← Open Library seed script
    ├── tests/                                 ← integration + e2e tests
    └── docs/                                  ← project documentation
```

---

## 8. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | React | 19 |
| Frontend | TypeScript | 6 |
| Frontend | Vite | 8 |
| Backend | Java | 21 |
| Backend | Spring Boot | 4.1.0 |
| Backend | Maven | (wrapper) |
| Database | PostgreSQL | latest stable |
| Storage | Railway Storage | — |
| Seed Script | Python | 3.10+ |

---

*This document is the single source of truth for project approach and phase status.
Update the status fields as each phase gate is approved.*
