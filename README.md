# be-interview-prep

Backend interview prep assignment: five Spring Boot features, each shipped as its own pull request.

**Stack:** Java 21, Spring Boot 3.5, Maven (wrapper included).

## Run

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me-please
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. `JWT_SECRET` (at least 32 characters) is required to sign login tokens.
`ADMIN_EMAIL` and `ADMIN_PASSWORD` are optional and create an ADMIN account on startup.

## Test

```bash
./mvnw test
```

## Questions

| # | Question | PR link |
|---|---|---|
| 1 | Task Manager API | [#1](https://github.com/jis-edstem/be-interview-prep/pull/1), [#2](https://github.com/jis-edstem/be-interview-prep/pull/2) |
| 2 | URL Shortener | [#3](https://github.com/jis-edstem/be-interview-prep/pull/3) |
| 3 | Authentication & Roles | [#4](https://github.com/jis-edstem/be-interview-prep/pull/4) |
| 4 | Product Catalog | [#5](https://github.com/jis-edstem/be-interview-prep/pull/5) |
| 5 | Order Service | |

Video:
