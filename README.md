# E-Bookstore Capstone Project

This project is developed using a **Controlled Spec-Driven Development** process.

## Development Protocol

See [`../CONTROLLED_SPEC_DRIVEN_DEVELOPMENT_PROTOCOL.md`](../CONTROLLED_SPEC_DRIVEN_DEVELOPMENT_PROTOCOL.md) for the full protocol.

## Current Phase

**PHASE 1 — SPECIFICATION** (Awaiting source specification)

## Project Structure

```
ebookstore/
│
├── specs/
│   ├── 00-source/          # Raw source requirements provided by the product owner
│   ├── 01-specification/   # Formal specification (approval-gated)
│   ├── 02-plan/            # Implementation plan (approval-gated)
│   ├── 03-design/          # Architecture, DB, API, component design (approval-gated)
│   │   └── diagrams/       # Design diagrams
│   └── 04-change-control/  # Change requests and change log
│
├── approvals/              # Signed-off approval records per phase
├── frontend/               # React + TypeScript application
├── backend/                # Java Spring Boot application
├── tests/                  # Integration and end-to-end tests
├── docs/                   # Project documentation
├── README.md
└── .gitignore
```

## Technology Stack (Proposed — subject to specification/design approval)

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript |
| Backend | Java, Spring Boot, Maven |
| Database | PostgreSQL |
| Digital Asset Storage | Railway Storage Bucket |

## Phase Gate Summary

| Phase | Gate Keyword | Status |
|---|---|---|
| Specification | `SPECIFICATION APPROVED` | ⏳ In Progress |
| Plan | `PLAN APPROVED` | 🔒 Locked |
| Design | `DESIGN APPROVED` | 🔒 Locked |
| Implementation | — | 🔒 Locked |
