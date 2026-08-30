# Transaction Starter Project

This is the starter project for the Customer Transactions exercise.

## Before you start

The first thing you should do after cloning the repository is:

### Linux / macOS

```bash
./mvnw clean test
```

### Windows

```bat
mvnw.cmd clean test
```

The sample test should pass before you begin implementing the exercise.

## What is already provided

- Java 17
- Spring Boot
- Maven wrapper
- Spring Web
- Spring Data JPA
- H2 embedded database
- JUnit / Spring Boot Test
- A sample REST endpoint: `GET /api/sample`
- A sample test that loads the Spring context

## Exercise

All four operations have been implemented:

1. Create transaction — `POST /api/transactions`
2. Get transaction — `GET /api/transactions/{transactionId}`
3. Update transaction status — `PATCH /api/transactions/{transactionId}/status`
4. Get all transactions for a customer — `GET /api/customers/{customerId}/transactions`

The surrounding design was extended with a layered structure (`controller` / `service` / `repository` / `dto` / `entity` / `enums` / `exception`) rather than putting everything in one package, so that request handling, business rules, and persistence stay separate and independently testable.

## Transaction fields

Every transaction contains:

- Transaction ID
- Customer ID
- Amount
- Currency
- Transaction Type
- Transaction Status

### Validation rules

The following rules are enforced on every transaction:

| Field | Validation |
|---|---|
| Transaction ID | Required (not blank); must not already exist |
| Customer ID | Required (not blank) |
| Amount | Required; must be greater than zero (no upper bound is enforced) |
| Currency | Required; must be one of the supported values (`INR`, `USD`) |
| Transaction type | Required; must be one of the supported values (`CREDIT`, `DEBIT`) |
| Initial status | Not client-settable — every new transaction is created with status `PENDING` regardless of what (if anything) is sent |

Enum fields (currency, transaction type, and — on status update — status) are accepted case-insensitively: `"inr"`, `"INR"`, and `"InR"` all resolve to the same value. Any value that isn't a valid enum constant is rejected with `400 Bad Request`.

**Business validation beyond the annotations:**

- **Duplicate detection** — a create request whose Transaction ID already exists in the database is rejected with `409 Conflict`, not `400`. This is checked in the service layer (`existsById`), not via a Bean Validation annotation, since it depends on the current database state rather than the shape of the request.
- **Status transition rules** — status can only move `PENDING → SUCCESS` or `PENDING → FAILED`. `SUCCESS` and `FAILED` are terminal: no transition is allowed out of either of them, and `PENDING → PENDING` is also rejected since it isn't one of the two allowed forward transitions. An invalid transition returns `400 Bad Request` with a message naming the current and requested status. We chose two terminal end-states rather than allowing e.g. `FAILED → PENDING` (a retry) because the exercise doesn't define what a retry should mean for the same Transaction ID, and allowing it would need extra fields (retry count, audit trail) that are out of scope here — see Limitations.

## API skeleton

### Create

`POST /api/transactions`

Request body:

```json
{
  "transactionId": "TXN1001",
  "customerId": "CUST001",
  "amount": 5000,
  "currency": "INR",
  "transactionType": "CREDIT"
}
```

Responses:

- `201 Created` — returns the created transaction, with `transactionStatus: "PENDING"`.
- `400 Bad Request` — validation failure (missing/blank field, non-positive amount, invalid enum value).
- `409 Conflict` — a transaction with this Transaction ID already exists.

Example success response:

```json
{
  "transactionId": "TXN1001",
  "customerId": "CUST001",
  "amount": 5000,
  "currency": "INR",
  "transactionType": "CREDIT",
  "transactionStatus": "PENDING"
}
```

### Get

`GET /api/transactions/{transactionId}`

Responses:

- `200 OK` — returns the transaction.
- `404 Not Found` — no transaction with that ID exists.

Example: `GET /api/transactions/TXN1001` → `200 OK` with the same JSON shape as above.

### Update status

`PATCH /api/transactions/{transactionId}/status`

Request body:

```json
{
  "status": "SUCCESS"
}
```

Responses:

- `200 OK` — returns the updated transaction.
- `400 Bad Request` — invalid status value, or a transition that isn't allowed (see Validation rules above).
- `404 Not Found` — no transaction with that ID exists.

Example: `PATCH /api/transactions/TXN1001/status` with `{"status": "SUCCESS"}` → `200 OK`:

```json
{
  "transactionId": "TXN1001",
  "customerId": "CUST001",
  "amount": 5000,
  "currency": "INR",
  "transactionType": "CREDIT",
  "transactionStatus": "SUCCESS"
}
```

### Get customer transactions

`GET /api/customers/{customerId}/transactions`

Responses:

- `200 OK` — returns a JSON array of the customer's transactions, or `[]` if the customer has none. (A customer with no transactions and an unrecognized customer ID are indistinguishable — both return `[]`; see Limitations.)

Example: `GET /api/customers/CUST001/transactions` → `200 OK`:

```json
[
  {
    "transactionId": "TXN1001",
    "customerId": "CUST001",
    "amount": 5000,
    "currency": "INR",
    "transactionType": "CREDIT",
    "transactionStatus": "SUCCESS"
  },
  {
    "transactionId": "TXN1002",
    "customerId": "CUST001",
    "amount": 2000,
    "currency": "INR",
    "transactionType": "DEBIT",
    "transactionStatus": "PENDING"
  }
]
```

## Testing expectations

15 tests in total (7 service-layer, 7 controller-layer, 1 the original sample context-load test), covering more than just startup:

- Transaction created successfully
- Transaction rejected because it fails validation
- Duplicate Transaction ID rejected
- Request for a transaction that does not exist
- Successful status transition (`PENDING → SUCCESS`)
- Invalid status transition rejected (`SUCCESS → FAILED`)
- Customer transaction lookup, including the multi-result case
- Case-insensitive enum input accepted (e.g. `"inr"`, `"credit"`)
- Invalid enum value rejected

**Known gap:** controller-level (MockMvc, full HTTP layer) tests exist for create and get; the status-update and customer-lookup endpoints are currently only exercised at the service layer, not end-to-end through MockMvc. Noted honestly rather than glossed over — see Limitations and What I'd Improve below.

---

## Assumptions

- Transaction IDs are supplied by the client, not generated server-side, and must be unique.
- Every new transaction starts in `PENDING`; the client cannot override this.
- `SUCCESS` and `FAILED` are terminal states.
- `amount` must be strictly greater than zero; no maximum is enforced.
- Only `INR`/`USD` and `CREDIT`/`DEBIT` are supported — this is our own implementation choice, not a Toucan-assigned variant, since this cohort's invitation did not specify a per-candidate variant.
- A customer with zero transactions is a valid state and returns `[]`, not a `404`.
- H2 in-memory storage is acceptable for the scope of this assignment; nothing needs to survive an application restart.
- Authentication, authorization, and external payment processing are out of scope.

## Known Limitations

- H2 is in-memory only — data does not survive a restart, and it isn't a production-suitable datastore.
- No authentication or authorization is implemented.
- No pagination on the customer-transactions endpoint.
- No generic/catch-all exception handler — an unexpected runtime error (e.g. a database failure) would fall through to Spring's default error response instead of this application's structured `{status, message, timestamp}` shape.
- Status-update and customer-lookup endpoints lack MockMvc-level (full HTTP) tests; they're covered at the service layer only.
- No audit trail — updating a status overwrites it in place, so the previous value isn't recoverable.
- No production deployment configuration is included.

## What I Would Improve With More Time

- Add MockMvc tests for the PATCH status and customer-lookup endpoints.
- Add a catch-all exception handler for a consistent error shape on unexpected failures.
- Add pagination to the customer-transactions endpoint.
- Add a simple status-history/audit table.

---

## AI Usage Disclosure

- **Tools used:** ChatGPT.
- **Used for:** discussing Spring Boot layering, reviewing the starter project structure, generating and reviewing boilerplate (DTOs, entity, controllers, exception classes), suggesting test cases, and drafting documentation.
- **What I changed/verified:** all generated code was read and run locally against the starter project before being kept; the validation rules, status-transition logic, and API contract described in this README were checked against the actual source files rather than taken on faith from the AI's summary of them.
- **Something the AI got wrong that I had to fix:** an earlier documentation draft stated the H2 JDBC URL as `jdbc:h2:mem:testdb`; the URL actually configured in `application.yml` is `jdbc:h2:mem:transactions`. An earlier draft also implied full controller-level test coverage for every endpoint, which wasn't accurate — corrected above.
- **How I checked the final result works:** ran `./mvnw clean test` locally and confirmed all tests pass; exercised each endpoint manually.

## Test Run Output

```text
<paste your actual `./mvnw clean test` console output here before submitting —
specifically the final "Tests run: X, Failures: 0, Errors: 0" line and
the BUILD SUCCESS line.>
```