# BITE CART

**Your Cravings. One Cart Away.**

BiteCart is a food-ordering and food e-commerce web application built with Java, Spring Boot, Thymeleaf, and MySQL.

## Run locally

Prerequisite: Java 17 or newer and Maven 3.9+.

```powershell
mvn spring-boot:run
```

The application starts on port `8080` by default.

Maven is configured to use a project-local dependency cache (`.m2`) so the project can build in restricted environments.

## Production deployment

Build the executable JAR with Java 17+ and Maven 3.9+:

```powershell
mvn clean install
```

The packaged application is `target/bitecart-0.0.1-SNAPSHOT.jar`. Production uses the `prod` Spring profile and MySQL. Copy `.env.example` to an environment manager of your choice and set real values outside the repository; never commit a `.env` file.

For a PowerShell session:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://db-host:3306/bitecart?useSSL=true&serverTimezone=UTC"
$env:DB_USERNAME = "bitecart_app"
$env:DB_PASSWORD = "use-a-secret-manager-or-host-environment-variable"
$env:JPA_DDL_AUTO = "validate"
java -jar target/bitecart-0.0.1-SNAPSHOT.jar
```

Deploy behind HTTPS. The production profile marks session cookies as secure, so plaintext HTTP is not suitable for signed-in traffic. Ensure the MySQL user has only the permissions BiteCart needs. `JPA_DDL_AUTO` defaults to `validate`; run schema changes through an approved operational process before deployment.

Before release, confirm that `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are supplied by the host or secret manager, templates/static assets are included in the JAR, and no local credential files are tracked by Git.
