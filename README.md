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
C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd -Didea.version=2026.2.1 -Dmaven.ext.class.path=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\intellij.maven.rt\maven-event-listener.jar -Djansi.passthrough=true -Dstyle.color=always -Dmaven.repo.local=C:\Users\Anchula Sridhar\.m2\repository clean test -f pom.xml
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< com.example:transaction-starter >-------------------
[INFO] Building transaction-starter 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.4.1:clean (default-clean) @ transaction-starter ---
[INFO] Deleting C:\Users\Anchula Sridhar\Desktop\transaction-assignment\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ transaction-starter ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.14.0:compile (default-compile) @ transaction-starter ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 16 source files with javac [debug parameters release 17] to target\classes
[INFO] Annotation processing is enabled because one or more processors were found
  on the class path. A future release of javac may disable annotation processing
  unless at least one processor is specified by name (-processor), or a search
  path is specified (--processor-path, --processor-module-path), or annotation
  processing is enabled explicitly (-proc:only, -proc:full).
  Use -Xlint:-options to suppress this message.
  Use -proc:none to disable annotation processing.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ transaction-starter ---
[INFO] skip non existing resourceDirectory C:\Users\Anchula Sridhar\Desktop\transaction-assignment\src\test\resources
[INFO] 
[INFO] --- compiler:3.14.0:testCompile (default-testCompile) @ transaction-starter ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 3 source files with javac [debug parameters release 17] to target\test-classes
[INFO] Annotation processing is enabled because one or more processors were found
  on the class path. A future release of javac may disable annotation processing
  unless at least one processor is specified by name (-processor), or a search
  path is specified (--processor-path, --processor-module-path), or annotation
  processing is enabled explicitly (-proc:only, -proc:full).
  Use -Xlint:-options to suppress this message.
  Use -proc:none to disable annotation processing.
[INFO] 
[INFO] --- surefire:3.5.3:test (default-test) @ transaction-starter ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.transactionstarter.transaction.controller.TransactionControllerTest
21:40:28.850 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.example.transactionstarter.transaction.controller.TransactionControllerTest]: TransactionControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
21:40:29.203 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.example.transactionstarter.TransactionStarterApplication for test class com.example.transactionstarter.transaction.controller.TransactionControllerTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.5)

2026-08-30T21:40:30.421+05:30  INFO 23296 --- [           main] c.e.t.t.c.TransactionControllerTest      : Starting TransactionControllerTest using Java 21.0.3 with PID 23296 (started by Anchula Sridhar in C:\Users\Anchula Sridhar\Desktop\transaction-assignment)
2026-08-30T21:40:30.423+05:30  INFO 23296 --- [           main] c.e.t.t.c.TransactionControllerTest      : No active profile set, falling back to 1 default profile: "default"
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (C:\Users\Anchula Sridhar\.m2\repository\net\bytebuddy\byte-buddy-agent\1.17.7\byte-buddy-agent-1.17.7.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2026-08-30T21:40:34.021+05:30  INFO 23296 --- [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-08-30T21:40:34.021+05:30  INFO 23296 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-08-30T21:40:34.024+05:30  INFO 23296 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 2 ms
2026-08-30T21:40:34.070+05:30  INFO 23296 --- [           main] c.e.t.t.c.TransactionControllerTest      : Started TransactionControllerTest in 4.734 seconds (process running for 7.072)
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.331 s -- in com.example.transactionstarter.transaction.controller.TransactionControllerTest
[INFO] Running com.example.transactionstarter.transaction.service.TransactionServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.358 s -- in com.example.transactionstarter.transaction.service.TransactionServiceTest
[INFO] Running com.example.transactionstarter.TransactionStarterApplicationTests
2026-08-30T21:40:35.199+05:30  INFO 23296 --- [           main] t.c.s.AnnotationConfigContextLoaderUtils : Could not detect default configuration classes for test class [com.example.transactionstarter.TransactionStarterApplicationTests]: TransactionStarterApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
2026-08-30T21:40:35.235+05:30  INFO 23296 --- [           main] .b.t.c.SpringBootTestContextBootstrapper : Found @SpringBootConfiguration com.example.transactionstarter.TransactionStarterApplication for test class com.example.transactionstarter.TransactionStarterApplicationTests

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.5)

2026-08-30T21:40:35.298+05:30  INFO 23296 --- [           main] c.e.t.TransactionStarterApplicationTests : Starting TransactionStarterApplicationTests using Java 21.0.3 with PID 23296 (started by Anchula Sridhar in C:\Users\Anchula Sridhar\Desktop\transaction-assignment)
2026-08-30T21:40:35.299+05:30  INFO 23296 --- [           main] c.e.t.TransactionStarterApplicationTests : No active profile set, falling back to 1 default profile: "default"
2026-08-30T21:40:35.923+05:30  INFO 23296 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-08-30T21:40:36.036+05:30  INFO 23296 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 91 ms. Found 1 JPA repository interface.
2026-08-30T21:40:36.649+05:30  INFO 23296 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-08-30T21:40:36.795+05:30  INFO 23296 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.6.26.Final
2026-08-30T21:40:36.879+05:30  INFO 23296 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-08-30T21:40:37.351+05:30  INFO 23296 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-08-30T21:40:37.467+05:30  INFO 23296 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-08-30T21:40:38.087+05:30  INFO 23296 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:transactions user=SA
2026-08-30T21:40:38.091+05:30  INFO 23296 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-08-30T21:40:38.245+05:30  INFO 23296 --- [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [Connecting through datasource 'HikariDataSource (HikariPool-1)']
	Database driver: undefined/unknown
	Database version: 2.3.232
	Autocommit mode: undefined/unknown
	Isolation level: undefined/unknown
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-08-30T21:40:40.221+05:30  INFO 23296 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-08-30T21:40:40.335+05:30  INFO 23296 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-08-30T21:40:41.198+05:30  INFO 23296 --- [           main] o.s.b.a.h2.H2ConsoleAutoConfiguration    : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:transactions'
2026-08-30T21:40:41.293+05:30  INFO 23296 --- [           main] c.e.t.TransactionStarterApplicationTests : Started TransactionStarterApplicationTests in 6.047 seconds (process running for 14.295)
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.109 s -- in com.example.transactionstarter.TransactionStarterApplicationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  25.396 s
[INFO] Finished at: 2026-08-30T21:40:41+05:30
[INFO] ------------------------------------------------------------------------

```