# E-Bookstore — Architecture Diagrams

**Project:** AI-Assisted E-Commerce Bookstore (Capstone)
**Last Updated:** 2026-08-24
**Status:** Pre-implementation — diagrams reflect the intended architecture before DESIGN APPROVED

---

## Table of Contents

1. [Context Diagram (Level 0 DFD)](#1-context-diagram-level-0-dfd)
2. [Level 1 DFD — Major Processes](#2-level-1-dfd--major-processes)
3. [Level 2 DFD — Book Catalogue](#3-level-2-dfd--book-catalogue)
4. [Level 2 DFD — Cart Management](#4-level-2-dfd--cart-management)
5. [Level 2 DFD — Order Management](#5-level-2-dfd--order-management)
6. [Level 2 DFD — Checkout & Payment](#6-level-2-dfd--checkout--payment)
7. [Level 2 DFD — Recommendations](#7-level-2-dfd--recommendations)
8. [System Architecture Overview](#8-system-architecture-overview)
9. [Backend Module Map](#9-backend-module-map)
10. [Frontend Page Map](#10-frontend-page-map)
11. [Guest Cart Flow](#11-guest-cart-flow)
12. [Purchase Flow — Sequence Diagram](#12-purchase-flow--sequence-diagram)
13. [Order Cancellation Flow](#13-order-cancellation-flow)
14. [Gift Points Flow](#14-gift-points-flow)
15. [Authentication Flow](#15-authentication-flow)
16. [Seed Pipeline Flow](#16-seed-pipeline-flow)

---

## 1. Context Diagram (Level 0 DFD)

The system boundary and all external actors.

```mermaid
flowchart LR
    GU([Guest User])
    RU([Registered User])
    OL([Open Library\nSeed Script])
    PG([Simulated\nPayment Gateway])

    GU -->|"Browse, Search, Add to Cart"| SYS[E-Bookstore System]
    RU -->|"Login, Browse, Order, Pay, Cancel"| SYS
    OL -->|"Book Metadata JSON"| SYS
    SYS -->|"Payment Request"| PG
    PG -->|"Payment Result"| SYS
    SYS -->|"Catalogue, Recommendations, Confirmation"| RU
    SYS -->|"Catalogue, Search Results"| GU
```

---

## 2. Level 1 DFD — Major Processes

The six top-level processes and their data stores.

```mermaid
flowchart TD
    GU([Guest User])
    RU([Registered User])
    SEED([Open Library\nSeed Script])
    PG([Simulated\nPayment Gateway])

    P1[1. User Management\nAuth & Registration]
    P2[2. Book Catalogue\nBrowse & Search]
    P3[3. Cart Management]
    P4[4. Order Management]
    P5[5. Checkout & Payment]
    P6[6. Recommendations]

    DS1[(Users DB\nPostgreSQL)]
    DS2[(Books DB\nPostgreSQL)]
    DS3[(Orders DB\nPostgreSQL)]
    DS4[Browser\nLocalStorage]

    GU -->|Browse / Search| P2
    GU -->|Add to Cart| P3
    P3 -->|Guest Cart| DS4

    RU -->|Login / Register| P1
    P1 <-->|User Records| DS1
    RU -->|Browse / Search| P2
    RU -->|Add to Cart| P3
    P3 <-->|Registered Cart| DS1

    SEED -->|Book Metadata| DS2
    P2 <-->|Book Data| DS2

    RU -->|Place Order| P4
    P4 <-->|Order Records| DS3
    P4 -->|Order Summary| P5

    P5 -->|Payment Request| PG
    PG -->|Payment Result| P5
    P5 -->|Confirmation| RU
    P5 <-->|Gift Points| DS1

    DS3 -->|Order History| P6
    P6 -->|Recommendations| RU
```

---

## 3. Level 2 DFD — Book Catalogue

```mermaid
flowchart TD
    U([User])
    DS2[(Books DB)]

    P2_1[2.1 Browse All Books]
    P2_2[2.2 Browse by Category]
    P2_3["2.3 Browse by Publisher\n('Brand')"]
    P2_4[2.4 Search Books]
    P2_5[2.5 View Book Details]
    P2_6[2.6 View Related Books]

    U -->|Open Catalogue| P2_1
    U -->|Select Category| P2_2
    U -->|Select Publisher| P2_3
    U -->|Enter Search Term| P2_4
    U -->|Select Book| P2_5
    P2_5 --> P2_6

    P2_1 <-->|All Books| DS2
    P2_2 <-->|Books by Category| DS2
    P2_3 <-->|Books by Publisher| DS2
    P2_4 <-->|Matching Books| DS2
    P2_5 <-->|Book Details| DS2
    P2_6 <-->|Same Category / Author| DS2
```

---

## 4. Level 2 DFD — Cart Management

```mermaid
flowchart TD
    GU([Guest User])
    RU([Registered User])
    LS[Browser LocalStorage]
    DS1[(Users DB\nSession Cart)]

    P3_1[3.1 Add Book to Cart]
    P3_2[3.2 View Cart]
    P3_3["3.3 Update Cart\n(Qty / Remove)"]
    P3_4[3.4 Merge Guest Cart\non Login]

    GU -->|Add Book| P3_1
    P3_1 -->|Store Guest Cart| LS

    RU -->|Add Book| P3_1
    P3_1 -->|Store Registered Cart| DS1

    GU -->|View Cart| P3_2
    P3_2 -->|Read| LS

    RU -->|View Cart| P3_2
    P3_2 -->|Read| DS1

    RU -->|Remove / Change Qty| P3_3
    P3_3 <-->|Update| DS1

    GU -->|Logs In| P3_4
    LS -->|Guest Items| P3_4
    P3_4 -->|Merged Cart| DS1
```

---

## 5. Level 2 DFD — Order Management

```mermaid
flowchart TD
    RU([Registered User])
    DS3[(Orders DB)]

    P4_1[4.1 Create Order\nfrom Cart]
    P4_2[4.2 View Order History]
    P4_3["4.3 Buy Again\n(Re-add to Cart)"]
    P4_4["4.4 Cancel Order\n(Before Delivery)"]

    RU -->|Proceed to Checkout| P4_1
    P4_1 -->|Save Order| DS3

    RU -->|View History| P4_2
    P4_2 <-->|Past Orders| DS3

    RU -->|Select Past Order| P4_3
    P4_3 -->|Items → Cart| DS3

    RU -->|Cancel Request| P4_4
    P4_4 <-->|"Check Status\n(Not Yet Delivered)"| DS3
    P4_4 -->|Update Status = CANCELLED| DS3
```

---

## 6. Level 2 DFD — Checkout & Payment

```mermaid
flowchart TD
    RU([Registered User])
    PG([Simulated Payment Gateway])
    DS1[(Users DB)]
    DS3[(Orders DB)]

    P5_1[5.1 Select Delivery Address]
    P5_2["5.2 View Order Summary\n+ Delivery Date"]
    P5_3[5.3 Redeem Gift Points]
    P5_4["5.4 Select Payment Method\n(Credit / Debit Card)"]
    P5_5["5.5 Process Payment\n(Simulated)"]
    P5_6[5.6 Payment Confirmation]
    P5_7[5.7 Purchase Confirmation]

    RU --> P5_1
    P5_1 <-->|Saved Addresses| DS1
    P5_1 --> P5_2
    P5_2 --> P5_3
    P5_3 <-->|Gift Point Balance| DS1
    P5_3 -->|Adjusted Total ₹| P5_4
    P5_4 --> P5_5
    P5_5 -->|Simulated Request| PG
    PG -->|Success / Fail| P5_6
    P5_6 -->|Order Confirmed| DS3
    P5_6 -->|"Award Gift Points\n(₹50 spent = 1 pt)"| DS1
    P5_6 --> P5_7
    P5_7 -->|Show Confirmation| RU
```

---

## 7. Level 2 DFD — Recommendations

```mermaid
flowchart TD
    RU([Registered User])
    DS2[(Books DB)]
    DS3[(Orders DB)]

    P6_1[6.1 Fetch Order History]
    P6_2["6.2 Identify Categories\n& Authors from History"]
    P6_3[6.3 Query Similar Books]
    P6_4["6.4 Display Recommendations\n(Catalogue & Basket)"]

    RU -->|Load Page / Basket| P6_1
    P6_1 <-->|Past Orders| DS3
    P6_1 --> P6_2
    P6_2 --> P6_3
    P6_3 <-->|"Books: same category\nor author"| DS2
    P6_3 --> P6_4
    P6_4 -->|Recommended Books| RU
```

---

## 8. System Architecture Overview

High-level technology stack and communication paths.

```mermaid
flowchart TD
    subgraph Browser
        FE["React 19 + TypeScript\nVite 8\nGuest Cart → localStorage"]
    end

    subgraph Server ["Spring Boot 4.1 — Modular Monolith (Port 8080)"]
        AUTH[auth module]
        USER[user module]
        BOOK[book module]
        CAT[category module]
        PUB[publisher module]
        CART[cart module]
        ORD[order module]
        PAY[payment module]
        REC[recommendations]
        ACCESS[access module]
    end

    subgraph Storage
        PG[(PostgreSQL\nMetadata · Orders · Users)]
        RS[(Railway Storage\nCover Images · Assets)]
    end

    subgraph External
        OL[Open Library\nSeed Script\none-time offline]
        SPG[Simulated\nPayment Gateway]
    end

    FE -->|HTTPS REST JSON| Server
    Server <-->|JPA / JDBC| PG
    Server <-->|Object Store API| RS
    OL -->|books.json at startup| BOOK
    PAY <-->|Mock Request/Response| SPG
```

---

## 9. Backend Module Map

Internal structure of the Spring Boot modular monolith.

```mermaid
flowchart LR
    subgraph auth ["auth module"]
        AC[AuthController]
        AS[AuthService]
        JF[JwtFilter]
    end

    subgraph user ["user module"]
        UC[UserController]
        US[UserService]
        UR[UserRepository]
    end

    subgraph book ["book module"]
        BC[BookController]
        BS[BookService]
        BR[BookRepository]
        BL[BookLoader\nstartup seed]
    end

    subgraph category ["category module"]
        CC[CategoryController]
        CS[CategoryService]
        CR[CategoryRepository]
    end

    subgraph cart ["cart module"]
        CTC[CartController]
        CTS[CartService]
        CTR[CartRepository]
    end

    subgraph order ["order module"]
        OC[OrderController]
        OS[OrderService]
        OR[OrderRepository]
    end

    subgraph payment ["payment module"]
        PC[PaymentController]
        PS[PaymentService\nSimulated]
    end

    subgraph giftp ["gift points — inside user module"]
        GPS[GiftPointService]
    end

    subgraph rec ["recommendations module"]
        RC[RecommendationController]
        RS2[RecommendationService]
    end

    AC --> AS
    AS --> UR
    UC --> US --> UR
    BC --> BS --> BR
    CC --> CS --> CR
    CTC --> CTS --> CTR
    OC --> OS --> OR
    PC --> PS
    PS --> GPS
    RC --> RS2
    RS2 --> OR
    RS2 --> BR
```

---

## 10. Frontend Page Map

React page and component tree (planned).

```mermaid
flowchart TD
    APP[App.tsx\nRouter]

    APP --> HOME[HomePage\nCatalogue + Recommendations]
    APP --> LOGIN[LoginPage]
    APP --> REGISTER[RegisterPage]
    APP --> CAT[CategoryPage\nBooks by Category]
    APP --> PUB[PublisherPage\nBooks by Publisher]
    APP --> SEARCH[SearchPage\nSearch + Filter Results]
    APP --> DETAIL[BookDetailPage\nBook Info + Related]
    APP --> CART[CartPage\nBasket Review]
    APP --> CHECKOUT[CheckoutPage\nAddress + Summary]
    APP --> PAYMENT[PaymentPage\nCard + Gift Points]
    APP --> CONFIRM[ConfirmationPage\nPurchase Confirmed]
    APP --> HISTORY[OrderHistoryPage\nPast Orders + Buy Again]
    APP --> PROFILE[ProfilePage\nUser Info + Gift Point Balance]

    CART -->|Guest: localStorage| LS[Browser LocalStorage]
    CART -->|Registered: API| API[Spring Boot API]
```

---

## 11. Guest Cart Flow

How a guest's cart is handled through login and merge.

```mermaid
sequenceDiagram
    participant GU as Guest User
    participant FE as React Frontend
    participant LS as localStorage
    participant API as Spring Boot API
    participant DB as PostgreSQL

    GU->>FE: Add book to cart
    FE->>LS: Save cart item (no API call)

    GU->>FE: Click Login
    FE->>API: POST /auth/login
    API->>DB: Validate credentials
    DB-->>API: User record
    API-->>FE: JWT token

    FE->>LS: Read guest cart items
    FE->>API: POST /cart/merge (items + JWT)
    API->>DB: Merge guest items into user cart
    DB-->>API: Merged cart
    API-->>FE: Merged cart response
    FE->>LS: Clear guest cart
    FE->>GU: Show merged cart
```

---

## 12. Purchase Flow — Sequence Diagram

End-to-end flow from cart to purchase confirmation.

```mermaid
sequenceDiagram
    participant U as Registered User
    participant FE as React Frontend
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant SPG as Simulated Payment

    U->>FE: Proceed to Checkout
    FE->>API: GET /user/addresses
    API-->>FE: Saved addresses
    FE->>U: Show address selection

    U->>FE: Select address
    FE->>API: GET /cart
    API-->>FE: Cart items + total ₹
    FE->>U: Show order summary

    U->>FE: Apply gift points (optional)
    FE->>API: GET /user/giftpoints
    API-->>FE: Point balance
    FE->>U: Show adjusted total ₹

    U->>FE: Select card (Credit/Debit)
    U->>FE: Confirm payment
    FE->>API: POST /payment/initiate
    API->>SPG: Simulate payment request
    SPG-->>API: Payment SUCCESS

    API->>DB: Create Order (status=CONFIRMED)
    API->>DB: Deduct gift points used
    API->>DB: Award new gift points (₹50=1pt)
    API->>DB: Clear cart
    API-->>FE: Order confirmation + order ID

    FE->>U: Show Purchase Confirmation screen
```

---

## 13. Order Cancellation Flow

```mermaid
flowchart TD
    U([Registered User])
    A[Select Order from History]
    B{Order Status?}
    C[DELIVERED or\nOUT FOR DELIVERY]
    D[CONFIRMED or\nPROCESSING]
    E[Show: Cannot Cancel\nOrder Already Delivered]
    F[Confirm Cancellation]
    G[Update Status = CANCELLED]
    H[Restore Gift Points\nif redeemed]
    I[Show Cancellation Confirmed]

    U --> A --> B
    B -->|Too late| C --> E
    B -->|Cancellable| D --> F --> G --> H --> I
```

---

## 14. Gift Points Flow

```mermaid
flowchart LR
    subgraph Earn
        PAY[Purchase Completed]
        CALC["Calculate Points\n⌊Total ₹ ÷ 50⌋"]
        ADD[Add to User Balance]
        PAY --> CALC --> ADD
    end

    subgraph Redeem
        CHK[Checkout — Apply Points]
        VAL["Validate: balance ≥ points\nto redeem"]
        DISC["Deduct from Order Total\n(1pt = ₹1)"]
        DED[Deduct from Balance]
        CHK --> VAL --> DISC --> DED
    end

    subgraph Cancel
        CAN[Order Cancelled]
        RST[Restore Redeemed Points\nto Balance]
        CAN --> RST
    end
```

---

## 15. Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as React Frontend
    participant API as Spring Boot API
    participant DB as PostgreSQL

    U->>FE: Enter email + password
    FE->>API: POST /auth/login
    API->>DB: Find user by email
    DB-->>API: User record (hashed password)
    API->>API: BCrypt verify password
    alt Credentials valid
        API->>API: Generate JWT (access token)
        API-->>FE: 200 OK + JWT token
        FE->>FE: Store token (memory / httpOnly cookie)
        FE->>U: Redirect to catalogue
    else Credentials invalid
        API-->>FE: 401 Unauthorized
        FE->>U: Show generic error message
    end

    Note over FE,API: Every subsequent request includes JWT in Authorization header
    FE->>API: GET /orders (Authorization: Bearer <token>)
    API->>API: Validate JWT signature + expiry
    API-->>FE: 200 OK + order data
```

---

## 16. Seed Pipeline Flow

How the catalogue is populated before the application starts.

```mermaid
flowchart TD
    DEV([Developer])
    SCRIPT["books_fetch.py\n(Python 3.10+)"]
    OL["Open Library API\nopenlibrary.org"]
    JSON["data/seed/books.json\n113 books"]
    BOOT["Spring Boot Startup\nBookLoader.java"]
    DB[(PostgreSQL\nbooks table)]

    DEV -->|"python script/books_fetch.py\n(one-time offline)"| SCRIPT
    SCRIPT -->|"GET /search.json\n(8 subjects × 15 books)"| OL
    SCRIPT -->|"GET /works/{key}.json\n(per book — descriptions)"| OL
    OL -->|Raw book data| SCRIPT
    SCRIPT -->|Transforms + deduplicates| JSON
    BOOT -->|"Reads JSON on startup\n(if DB is empty)"| JSON
    BOOT -->|Inserts book records| DB

    style JSON fill:#f0f7ff,stroke:#3b82d4
    style DB fill:#f0fff0,stroke:#22c55e
```

---

## Data Store Summary

| Store | Technology | Contains |
|---|---|---|
| `Users DB` | PostgreSQL | User accounts, addresses, gift point balances, registered cart |
| `Books DB` | PostgreSQL | Book metadata, categories, publishers |
| `Orders DB` | PostgreSQL | Orders, order items, order status |
| `Railway Storage` | Object Store | Cover images, future digital assets |
| `Browser LocalStorage` | Browser | Guest cart only — never sent to DB until login |

---

## External Entity Summary

| Entity | Role | Direction |
|---|---|---|
| Guest User | Browse, search, add to local cart | Inbound |
| Registered User | Full purchase flow | Inbound/Outbound |
| Open Library Seed Script | One-time offline catalogue population | Inbound |
| Simulated Payment Gateway | Mock payment success/fail response | Outbound/Inbound |

---

*All diagrams use [Mermaid](https://mermaid.js.org/) syntax and render natively in GitHub, VS Code, and most modern markdown viewers.*
