# Banking API

[![Build and test](https://github.com/simohamedd/banking-api/actions/workflows/build.yml/badge.svg)](https://github.com/simohamedd/banking-api/actions/workflows/build.yml)

A Spring Boot REST API for a simple bank: open current and savings accounts, deposit, withdraw, transfer, and view statements. It's built the way real ledgers are: money is `BigDecimal`, every movement is an append-only ledger entry, and transfers are all-or-nothing.

## Stack
Spring Boot 3, Spring Web, Spring Data JPA, Bean Validation, H2 in-memory database, JUnit 5 and MockMvc.

## Design
- **Ledger:** each deposit, withdrawal or transfer adds a `LedgerEntry` with the balance after it
- **Concurrency:** accounts are row-locked during a movement, always in account-number order, so two simultaneous transfers can't overspend or deadlock
- **Rules:** current accounts can have an overdraft; savings accounts can't
- **Errors:** `400` with a `fields` map for invalid input, `404` for unknown accounts, `422` for insufficient funds

## Run
```bash
mvn spring-boot:run
```
The API starts on http://localhost:8080. The H2 console is at `/h2-console` (JDBC URL `jdbc:h2:mem:bank`).

## Endpoints
| Method | Path | Description |
|---|---|---|
| POST | `/api/accounts` | Open an account: `{"owner":"Amina","type":"CURRENT","overdraftLimit":100}` |
| GET | `/api/accounts?owner=Amina` | List accounts, oldest first, optionally for one owner |
| GET | `/api/accounts/{number}` | Balance, overdraft and available funds |
| POST | `/api/accounts/{number}/deposits` | `{"amount":250,"reference":"Salary"}` |
| POST | `/api/accounts/{number}/withdrawals` | `{"amount":40}` |
| POST | `/api/transfers` | `{"from":"12345678","to":"87654321","amount":50,"reference":"Rent"}` |
| GET | `/api/accounts/{number}/transactions` | Statement, newest first |

## Test
```bash
mvn test
```

## Licence
MIT. See [LICENSE](LICENSE).
