# AuthLib - Java Authentication Library

A scalable, framework-agnostic Java authentication library with JWT token management, user registration, login, and password reset functionality. Works with Spring Boot, Jakarta EE, Quarkus, and vanilla Java.

## Features

- User registration and login with email/password
- JWT-based access and refresh tokens
- Password reset flow with email verification
- Token blacklisting for logout and revocation
- User account management (activation/deactivation)
- Password strength validation and bcrypt/argon2 hashing
- Framework-agnostic: Works with Spring Boot, Jakarta EE, Quarkus
- Database-agnostic: PostgreSQL, MySQL, H2 support via Hibernate
- Async-ready: Compatible with reactive frameworks
- Comprehensive error handling with custom exceptions
- Type-safe with Java 11+

## Requirements

- Java 11 or higher
- Maven 3.6+ or Gradle 7.0+

## Installation

### Using Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.authlib</groupId>
    <artifactId>authlib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Using Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'dev.authlib:authlib:1.0.0'
}
```

### From Source

```bash
git clone https://github.com/authlib/authlib-java.git
cd authlib-java
mvn clean install
```

## Quick Start

### 1. Set up environment

Create `.env` file:

```env
JWT_SECRET_KEY=your-super-secret-key-change-this
JWT_ALGORITHM=HS256
JWT_ACCESS_TOKEN_EXPIRY_MINUTES=15
JWT_REFRESH_TOKEN_EXPIRY_DAYS=7

DATABASE_URL=jdbc:postgresql://localhost:5432/authlib_db
DATABASE_USER=postgres
DATABASE_PASSWORD=password

SMTP_SERVER=smtp.gmail.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
```

### 2. Initialize database

```java
import dev.authlib.database.DatabaseManager;

DatabaseManager dbManager = new DatabaseManager();
dbManager.createTables();
```

### 3. Use in your application

```java
import dev.authlib.services.AuthService;
import dev.authlib.config.Config;

Config config = new Config();
AuthService authService = new AuthService(config);

// Register user
AuthResponse registerResult = authService.register(
    new RegisterRequest(
        "user@example.com",
        "SecurePassword123!",
        "John",
        "Doe"
    )
);

System.out.println("Access Token: " + registerResult.getAccessToken());

// Login user
AuthResponse loginResult = authService.login(
    new LoginRequest("user@example.com", "SecurePassword123!")
);

// Verify token
TokenPayload payload = authService.verifyToken(loginResult.getAccessToken());
System.out.println("User ID: " + payload.getUserId());
```

## Publishing to Maven Central

1. Create Sonatype account: https://issues.sonatype.org
2. Configure GPG signing
3. Setup Maven credentials
4. Deploy: `mvn clean deploy`

Full guide: [MAVEN_PUBLISHING.md](./MAVEN_PUBLISHING.md)

## License

MIT
