# 🔍 Code Review — Carpooling Backend

> **Scope**: Full codebase review of `com.carPooling.backend`  
> **Date**: 2026-06-14  
> **Severity Legend**: 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Low

---

## 1. 🔴 Critical — Security Vulnerabilities

### 1.1 Hardcoded Database Password in `application.properties`
[application.properties](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L3-L4)
```properties
spring.datasource.username=root
spring.datasource.password=Gate@2026
```
- **Risk**: Credentials are committed to version control. Anyone with repo access has full DB access.
- **Fix**: Use environment variables (`${DB_PASSWORD}`) or Spring Vault / `.env` file (gitignored).

### 1.2 Hardcoded JWT Secret in `application.properties`
[application.properties:14](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L14)
```properties
app.jwt.secret=VGhpc0lzQVN1cGVyU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMTIz
```
- This Base64-decodes to `ThisIsASuperSecretKeyForJWTTokenGeneration123` — weak and predictable.
- **Fix**: Generate a cryptographically random 256-bit key, inject via env var.

### 1.3 Access Token Logged in Plaintext
[AuthController.java:67-70](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/AuthController.java#L67-L70)
```java
log.debug(
    "Token refreshed for user: {}, new access token: {}",
    response.getAccessToken()   // ← only 1 arg for 2 placeholders
);
```
- Even if the format bug is fixed, **never log tokens**. Log files are a common attack surface.

### 1.4 Full `User` Entity Returned in Login Response (Includes Password Hash)
[LogInResponse.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/dto/response/LogInResponse.java#L12)
```java
public class LogInResponse extends BaseAuthResponse {
    private User user;  // ← entire JPA entity, including @JsonIgnore'd password
}
```
- `@JsonIgnore` on `password` may help at serialization time, but returning the raw entity is a **leaky abstraction**. Any future field addition (e.g., `isAdmin`, internal notes) auto-leaks.
- **Fix**: Return a dedicated `UserDTO` with only the fields the client needs.

### 1.5 `createPassword` Doubles as Registration — No Auth Required
[AuthServiceImplements.java:53-58](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/AuthServiceImplements.java#L53-L58)
```java
if (optionalUser.isEmpty()) {
    user = new User();            // creates a brand-new user
    user.setEmail(request.getEmail());
}
```
- Anyone can hit `POST /api/auth/password` with any email and a new account is silently created with no email verification.
- **Risk**: Account squatting, spam user creation.

### 1.6 Stale Text `del` in `application.properties`
[application.properties:13](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L13)
```
del
```
- This orphaned line will cause a **startup warning** and may confuse property parsing. It should be removed.

---

## 2. 🟠 High — Bugs

### 2.1 Broken Role Authority — Empty `ROLE_` String
[CustomUserDetailsService.java:27](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/security/CustomUserDetailsService.java#L27)
```java
List.of(new SimpleGrantedAuthority("ROLE_" ))   // ← no role name appended!
```
- Every user gets authority `"ROLE_"` (literally). Any role-based `@PreAuthorize` or `hasRole()` check will **never match**.
- The `Role` enum (`DRIVER`, `PASSENGER`, `USER`) exists but is never used.
- **Fix**: Add a `role` field to `User` entity and use `"ROLE_" + user.getRole().name()`.

### 2.2 `GenricDTO` Constructor Self-Assignment Bug
[GenricDTO.java:25-30](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/dto/GenricDTO.java#L25-L30)
```java
public GenricDTO(boolean status, String message, T data) {
    this.status = status;
    this.message = message;
    this.error = error;    // ← assigns field to itself (always null)
    this.data = data;
}
```
- `this.error = error` references the field, not a parameter. The `error` field stays `null` regardless.
- **Fix**: Remove that line or pass `error` as a parameter.

### 2.3 `log.debug` Format String Misuse in `JwtAuthFilter`
[JwtAuthFilter.java:63-65](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/security/JwtAuthFilter.java#L63-L65)
```java
logger.debug(
    "User not found for email extracted from JWT: {}. Error: {}" + e.getMessage()
);
```
- Uses **string concatenation** instead of SLF4J's placeholder `{}`. The `{}` are never substituted — they appear literally in the log. The message is concatenated to the format string, breaking the pattern.
- **Fix**: `logger.debug("User not found … Error: {}", e.getMessage());`

### 2.4 `AuthController.refreshToken` — Log Placeholder Count Mismatch
[AuthController.java:67-70](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/AuthController.java#L67-L70)
```java
log.debug(
    "Token refreshed for user: {}, new access token: {}",
    response.getAccessToken()   // 1 arg, 2 placeholders → second {} stays literal
);
```

### 2.5 Refresh Token Expiry Mismatch
| Location | Expiry Set |
|---|---|
| [AuthServiceImplements.java:72](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/AuthServiceImplements.java#L72) (`createPassword`) | `plusDays(7)` |
| [AuthServiceImplements.java:109](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/AuthServiceImplements.java#L109) (`login`) | `plusDays(7)` |
| [AuthServiceImplements.java:157](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/AuthServiceImplements.java#L157) (`refreshToken`) | **`plusSeconds(120)`** ← 2 minutes! |
| `JwtUtil.REFRESH_TOKEN_EXPIRATION` | 7 days (in JWT claims) |

- After the first token rotation, the refresh token in the DB expires in **2 minutes** while the JWT claims say 7 days. The DB check (`isBefore(now)`) will reject the token after 2 min.
- Likely a debugging leftover that was never reverted.

### 2.6 `createPassword` Missing `@Transactional`
[AuthServiceImplements.java:43](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/AuthServiceImplements.java#L43)
- `createPassword()` does: `save(user)` → `deleteByUser(user)` → `save(refreshToken)` — three writes that should be atomic. If `save(refreshToken)` fails, the old tokens are already deleted and the user has no valid refresh token.

### 2.7 `getProfile` — Unguarded `.orElseThrow()` with No Exception
[UserController.java:34-35](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/UserController.java#L34-L35)
```java
User user = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow();   // ← throws NoSuchElementException with no message
```
- Client gets a raw 500 error with no context. Should use a custom exception.

### 2.8 `ProfileResponse` Extends `User` Entity Directly
[ProfileResponse.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/dto/response/ProfileResponse.java#L5-L6)
```java
public class ProfileResponse extends User { }
```
- A DTO should **never** extend a JPA entity. This leaks the entire entity schema including `password`, `createdAt`, `deletedAt`, etc. It's also unused — dead code.

---

## 3. 🟡 Medium — Architectural & Design Issues

### 3.1 Repository Injected Directly into Controller
[UserController.java:27](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/UserController.java#L27)
```java
private final UserRepository userRepository;
```
- The `getProfile` method bypasses the service layer entirely and calls `userRepository` directly from the controller. This breaks the layered architecture pattern used everywhere else.

### 3.2 No CORS Configuration
- [SecurityConfig.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/config/SecurityConfig.java) has no CORS configuration.
- Mobile apps might work (they don't enforce CORS), but any web frontend will be blocked by the browser.

### 3.3 `GenricDTO` Annotated with `@Component`
[GenricDTO.java:9](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/dto/GenricDTO.java#L9)
```java
@Component   // ← DTOs should NOT be Spring beans
```
- A DTO is created per-request via `new GenricDTO<>(...)`. Making it a `@Component` registers a singleton in the application context for no reason. This will also fail due to the generic type parameter.

### 3.4 `RideService` Imports `javax.swing.*`
[RideService.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/RideService.java#L6)
```java
import javax.swing.*;   // ← Swing UI library in a backend API!
```
- IDE auto-import accident. Should be removed.

### 3.5 `AuthService` Interface Has Unused Imports
[AuthService.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/AuthService.java#L6-L12)
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
```
- Interfaces should not import `@Service`, `@Transactional`, or `LocalDateTime` when they don't use them.

### 3.6 `UserService` Interface Has Unused Imports
[UserService.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/UserService.java#L3-L12)
- Imports `LogoutRequest`, `UpdateProfileRequest`, `RefreshToken`, `InvalidTokenException`, `UnauthorizedException`, `@Transactional`, `LocalDateTime` — none of which are used.

### 3.7 `UploadService` Is a Dead Interface
[UploadService.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/UploadService.java) — No implementation exists. Dead code.

### 3.8 Duplicate/Overlapping DTOs
| DTO | Also exists as |
|---|---|
| `UpdateProfileRequest` | `ProfileRequest` (both serve profile updates) |
| `AuthResponse` | `BaseAuthResponse`, `LogInResponse`, `CreatePasswordResponse` (overlapping fields) |
| `ErrorResponse` | `GenricDTO` (both try to standardize error responses) |
| `LogoutRequest` | Never used (controller `logout()` takes no body) |

### 3.9 `@Data` on JPA Entities Can Break `equals`/`hashCode`
[User.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/entity/User.java#L16) uses `@Data` which generates `equals`/`hashCode` from all fields, including lazy-loaded collections. This can trigger unexpected lazy loading and infinite recursion with bidirectional relationships. Use `@Getter`/`@Setter` instead and implement `equals`/`hashCode` on `id` only.

### 3.10 `User.password` Marked `nullable = false` but `createPassword` Creates Users Without One Initially
[User.java:45-46](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/entity/User.java#L45-L46)
```java
@Column(nullable = false)
private String password;
```
- When `createPassword()` creates a new `User`, it sets email and password in the same save, so this works. But if any other code path creates a user without a password, it will hit a DB constraint violation.

---

## 4. 🟡 Medium — Code Quality

### 4.1 Typo: `GenricDTO` → Should be `GenericDTO`
Used across the entire codebase. Renaming now would touch every file — but it should be fixed sooner rather than later.

### 4.2 Debug Garbage in Production Logs
[UserServiceImplementation.java:39](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/UserServiceImplementation.java#L39)
```java
log.debug("Updating profile for user asdfghjkl: {}", email);
```
- `asdfghjkl` is clearly a test string left in.

[UserServiceImplementation.java:43](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/service/impl/UserServiceImplementation.java#L43)
```java
log.debug("______________________________________________");
```
- Separator lines in logs are noise.

### 4.3 Inconsistent Naming Conventions
| Pattern | Examples |
|---|---|
| Snake case in Java | `preference_name` field in `CreatePreferenceRequest` |
| Mixed exception messages | `"Preference Already Exist"` vs `"Preference already exist"` |
| Inconsistent class naming | `AuthServiceImplements` vs `UserServiceImplementation` vs `RideServiceImpl` |

### 4.4 `ValidationUtil` Is Never Used
[ValidationUtil.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/utils/ValidationUtil.java) — All validation is done via Bean Validation annotations. This utility class is dead code.

### 4.5 Duplicate `Pattern` Import
[ValidationUtil.java:3,6](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/utils/ValidationUtil.java#L3-L6)
```java
import java.util.regex.Pattern;
import java.util.regex.Pattern;   // ← duplicate
```

### 4.6 `StringConstant` Uses Non-Final Fields
[StringConstant.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/utils/StringConstant.java#L4-L14)
```java
public static String SUCCESS = "success";   // ← should be `static final`
```
- Without `final`, these can be reassigned at runtime. All constants should be `public static final`.

### 4.7 Global Exception Handler Leaks Internal Error Messages
[GlobalExceptionHandler.java:101](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/exception/GlobalExceptionHandler.java#L91-L106)
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<GenricDTO<Void>> handleGeneric(Exception ex) {
    response.setMessage(ex.getMessage());  // ← stack trace details to client
```
- Internal exception messages (e.g., SQL errors, NPEs) go straight to the API response. This can leak DB schema, query details, etc.
- **Fix**: Return a generic message; log the real error at `ERROR` level (currently using `DEBUG`).

### 4.8 `@Valid` Missing on `RefreshTokenRequest`
[AuthController.java:63](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/AuthController.java#L63)
```java
public ResponseEntity<...> refreshToken(@RequestBody RefreshTokenRequest request)
```
- `RefreshTokenRequest` has `@NotBlank` on `refreshToken`, but `@Valid` is not on the parameter — the validation will **never fire**.

---

## 5. 🟢 Low — Minor / Cosmetic

| # | Issue | File |
|---|---|---|
| 5.1 | `Role` enum defined but never assigned to any entity | [Role.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/enums/Role.java) |
| 5.2 | `UserAlreadyExistsException` and `ResourceNotFoundException` declared but never thrown | [custom_exception/](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/exception/custom_exception) |
| 5.3 | `UserController.register` method name misleading — it's actually "update profile" | [UserController.java:49](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/controller/UserController.java#L49) |
| 5.4 | `JwtUtil.app.jwt.expiration` property read from config but never used (hardcoded constants used instead) | [application.properties:15](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L15) |
| 5.5 | `Preference` not linked to `User` — no `@ManyToMany` or join table, so preferences are global/shared | [Preference.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/entity/Preference.java) |
| 5.6 | `JwtAuthFilter.sendError` uses unescaped string formatting — if `message` contains `"` or `%`, JSON breaks or format throws | [JwtAuthFilter.java:88-91](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/security/JwtAuthFilter.java#L88-L91) |
| 5.7 | `spring.jpa.hibernate.ddl-auto=update` — should be `validate` or `none` in production | [application.properties:7](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L7) |
| 5.8 | DEBUG-level logging enabled for Spring Security in config — very noisy in production | [application.properties:26](file:///home/shivanshkumar/projects/backend/backend/src/main/resources/application.properties#L26) |
| 5.9 | `RefreshTokenResponse extends BaseAuthResponse` with `@AllArgsConstructor` but no `@NoArgsConstructor` — deserialization will fail if Jackson tries to create one | [RefreshTokenResponse.java](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/dto/response/RefreshTokenResponse.java) |
| 5.10 | `profilePicture` stored as `String` with no `@Column(length)` or `@Lob` — will truncate Base64 images in MySQL's default VARCHAR(255) | [User.java:37](file:///home/shivanshkumar/projects/backend/backend/src/main/java/com/carPooling/backend/entity/User.java#L37) |

---

## 📊 Summary

| Category | Count | Top Priority Fix |
|---|---|---|
| 🔴 Critical Security | 6 | Externalize credentials + stop logging tokens |
| 🟠 High Bugs | 8 | Fix `ROLE_` authority + refresh token expiry mismatch |
| 🟡 Architecture | 10 | Remove `@Component` from DTO + fix layering |
| 🟡 Code Quality | 8 | Add `@Valid`, fix GenricDTO constructor, clean debug logs |
| 🟢 Low / Cosmetic | 10 | Dead code cleanup |
| **Total** | **42** | |
