# Transaction Processing Service

A Spring Boot REST API for creating, retrieving, updating, and querying customer transactions.

## Technology Stack

- Java 17
- Spring Boot 3.5.5
- Spring Data JPA
- H2 Database
- Maven
- JUnit 5
- Mockito
- Lombok

## Features

The application provides the following transaction operations:

1. Create a transaction
2. Retrieve a transaction by transaction ID
3. Update transaction status
4. Retrieve all transactions for a customer

Additional features include:

- Request validation
- Duplicate transaction ID detection
- Transaction status transition validation
- Centralized exception handling
- Case-insensitive enum input
- Automated unit tests
- Automated controller/API tests

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/example/transactionstarter/
│   │       ├── sample/
│   │       │   └── SampleController.java
│   │       │
│   │       ├── transaction/
│   │       │   ├── controller/
│   │       │   │   ├── TransactionController.java
│   │       │   │   └── CustomerTransactionController.java
│   │       │   │
│   │       │   ├── dto/
│   │       │   │   ├── CreateTransactionRequest.java
│   │       │   │   └── UpdateStatusRequest.java
│   │       │   │
│   │       │   ├── entity/
│   │       │   │   └── Transaction.java
│   │       │   │
│   │       │   ├── enums/
│   │       │   │   ├── Currency.java
│   │       │   │   ├── TransactionStatus.java
│   │       │   │   └── TransactionType.java
│   │       │   │
│   │       │   ├── exception/
│   │       │   │   ├── DuplicateTransactionException.java
│   │       │   │   ├── InvalidStatusTransitionException.java
│   │       │   │   ├── ResourceNotFoundException.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   │
│   │       │   ├── repository/
│   │       │   │   └── TransactionRepository.java
│   │       │   │
│   │       │   └── service/
│   │       │       └── TransactionService.java
│   │       │
│   │       └── TransactionStarterApplication.java
│   │
│   └── resources/
│       └── application.yml
│
└── test/
    └── java/
        └── com/example/transactionstarter/
            ├── TransactionStarterApplicationTests.java
            │
            └── transaction/
                ├── controller/
                │   └── TransactionControllerTest.java
                │
                └── service/
                    └── TransactionServiceTest.java