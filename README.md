# Car Pooling Backend Authentication API

## Sample cURL Requests

```bash
curl --location 'http://localhost:8080/api/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
  "fullName": "Janvi Singh",
  "email": "Janvi@gmail.com",
  "phoneNumber": "6392199459",
  "password": "Google@2026",
  "gender": "FEMALE",
  "role": "PASSENGER"
}'
```

---

```bash
curl --location 'http://localhost:8089/api/auth/password' \
--header 'Content-Type: application/json' \
--data '{
    "email": "janvi@gmail.com",
    "password": "Google@2026",
    "confirm_password": "Google@2026"
}'



curl --location 'http://localhost:8089/api/auth/logout' \
--header 'Content-Type: application/json' \
--data '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYWluQGdtYWlsLmNvbSIsImlhdCI6MTc3OTMxNDcwNiwiZXhwIjoxNzc5OTE5NTA2fQ.u0puPU3BNQ7s2LgPDy9Ql-VgjETEXHxKnPhZEV9fMXs"
}'
```

---

# Login API Documentation

## Base URL

```text
http://localhost:8080/api/auth
```

---

## Login API

**Endpoint**

```http
POST /login
```

---

### Request Headers

| Key | Value |
| --- | ----- |
| Content-Type | application/json |

---

### Request Body

```json
{
  "email": "john@gmail.com",
  "password": "Google@2026"
}
```

---

### Validation Rules

#### Email

- Required
- Must be valid email format

Example:

```text
john@gmail.com
```

#### Password

Password must contain:

- Minimum 8 characters
- 1 uppercase letter
- 1 lowercase letter
- 1 number
- 1 special character

Example:

```text
Google@2026
```

---

### Success Response

**HTTP Status**

```http
202 Accepted
```

**Response Body**

```json
{
  "status": "SUCCESS",
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "name": "John Doe",
      "email": "john@gmail.com",
      "phoneNumber": "9876543210",
      "gender": "MALE",
      "role": "USER",
      "profilePicture": null,
      "createdAt": "2026-05-21T01:10:20",
      "updatedAt": "2026-05-21T01:10:20",
      "deletedAt": null
    }
  }
}
```

---

### User Not Registered

**HTTP Status**

```http
401 Unauthorized
```

**Response Body**

```json
{
  "status": "UNAUTHORIZED",
  "message": "You haven't registered yet, please register first",
  "data": null
}
```

---

### Incorrect Password

**HTTP Status**

```http
400 Bad Request
```

**Response Body**

```json
{
  "status": "INVALID_REQUEST",
  "message": "Password is incorrect",
  "data": null
}
```

---

### Validation Error Response

**HTTP Status**

```http
400 Bad Request
```

**Response Body**

```json
{
  "status": "INVALID_REQUEST",
  "message": "Validation failed",
  "data": [
    "Invalid email format"
  ]
}
```

---

### Example Invalid Password Format

```json
{
  "status": "INVALID_REQUEST",
  "message": "Validation failed",
  "data": [
    "Password must contain minimum 8 characters, 1 uppercase, 1 lowercase, 1 number and 1 special character"
  ]
}
```

---

### Example Multiple Validation Errors

```json
{
  "status": "INVALID_REQUEST",
  "message": "Validation failed",
  "data": [
    "Email is required",
    "Password is required"
  ]
}
```

---

### Internal Server Error

**HTTP Status**

```http
500 Internal Server Error
```

**Response Body**

```json
{
  "status": "FAILED",
  "message": "Something went wrong",
  "data": null
}
```

---

## Authentication Flow

```text
Client Request
↓
Validate Request Body
↓
Check User Exists
↓
Verify Password Using BCrypt
↓
Generate Access Token
↓
Generate Refresh Token
↓
Save Refresh Token
↓
Return Response
```

---

## Technologies Used

- Spring Boot
- Spring Security
- JWT Authentication
- BCrypt Password Encoder
- JPA/Hibernate
- MySQL

---

## Notes

- Password is stored in encoded format using BCrypt.
- JWT Access Token is used for API authentication.
- Refresh Token is stored in database.
- Old refresh tokens are deleted during login.
- Password field is hidden from API response using `@JsonIgnore`.

---

## Sample cURL Request

```bash
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
"email": "john@gmail.com",
"password": "Google@2026"
}'
```

---

## Car Pooling Backend Authentication API (alternate Base URL)

**Base URL**

```text
http://localhost:8089/api/auth
```

---

## Authentication Flow (summary)

```text
Register/Login
      ↓
Receive Access Token + Refresh Token
      ↓
Use Access Token for Protected APIs
      ↓
Access Token Expires
      ↓
Call Refresh API using Refresh Token
      ↓
Receive New Access Token
      ↓
Continue Without Login
```

---

# Refresh Token API Documentation

## Base URL

```text
http://localhost:8080/api/auth
```

---

## Refresh Token Endpoint

**Endpoint**

```http
POST /refresh_token
```

---

### Request Headers

| Key | Value |
| --- | ----- |
| Content-Type | application/json |

---

### Request Body

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Success Response

**HTTP Status**

```http
202 Accepted
```

**Response Body**

```json
{
  "status": "SUCCESS",
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

### Invalid Refresh Token

**HTTP Status**

```http
401 Unauthorized
```

**Response Body**

```json
{
  "status": "UNAUTHORIZED",
  "message": "Invalid or expired refresh token",
  "data": null
}
```

---

### Refresh Token Expired

**HTTP Status**

```http
401 Unauthorized
```

**Response Body**

```json
{
  "status": "UNAUTHORIZED",
  "message": "Refresh token has expired. Please login again",
  "data": null
}
```

---

### Missing Refresh Token

**HTTP Status**

```http
400 Bad Request
```

**Response Body**

```json
{
  "status": "INVALID_REQUEST",
  "message": "Validation failed",
  "data": [
    "Refresh token is required"
  ]
}
```

---

### Refresh Token Not Found in Database

**HTTP Status**

```http
401 Unauthorized
```

**Response Body**

```json
{
  "status": "UNAUTHORIZED",
  "message": "Refresh token not found. Please login again",
  "data": null
}
```

---

### Internal Server Error

**HTTP Status**

```http
500 Internal Server Error
```

**Response Body**

```json
{
  "status": "FAILED",
  "message": "Something went wrong",
  "data": null
}
```

---

### Sample cURL Request

```bash
curl --location 'http://localhost:8080/api/auth/refresh_token' \
--header 'Content-Type: application/json' \
--data-raw '{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}'
```

---

## Token Expiry

| Token Type    | Expiry     |
| ------------- | ---------- |
| Access Token  | 15 Minutes |
| Refresh Token | 7 Days     |





# Spring Exception Handling Flow

```
Request → Controller → Service
                     ↓
              Exception thrown
                     ↓
          DispatcherServlet catches it
                     ↓
     @RestControllerAdvice handler runs
                     ↓
           HTTP Response sent back
```
# Ride Creation Flow — CoRide Backend

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Database Schema](#2-database-schema)
3. [API Contract](#3-api-contract)
4. [Step-by-Step Creation Flow](#4-step-by-step-creation-flow)
5. [Return Ride Sub-Flow](#5-return-ride-sub-flow)
6. [Occurrence Window Management](#6-occurrence-window-management)
7. [Update & Cancel Operations](#7-update--cancel-operations)
8. [Error Scenarios](#8-error-scenarios)
9. [Package Structure](#9-package-structure)

---

## 1. System Overview

### Why Template + Occurrence?

The old design stored N physical copies of a ride entity — one per date.
This model stores **one template** (the recurrence rule) and generates
**occurrence rows** (dated instances) lazily via a rolling 30-day window.

```
POST /rides
      │
      ▼
┌─────────────────┐          ┌──────────────────────┐
│  ride_templates │ 1 ──── N │   ride_occurrences   │
│  (what + rule)  │          │   (when + overrides) │
└─────────────────┘          └──────────────────────┘
         │
         │  if isReturnRide = true
         ▼
┌─────────────────┐          ┌──────────────────────┐
│  ride_templates │ 1 ──── N │   ride_occurrences   │
│  (return ride)  │          │   (return dates)     │
└─────────────────┘          └──────────────────────┘
```

| Concern | Old (N copies) | New (template + occurrence) |
|---|---|---|
| Update price for all future rides | UPDATE N rows | UPDATE 1 template row |
| Cancel a specific date | UPDATE 1 row | UPDATE 1 occurrence row |
| Cancel whole series | UPDATE N rows | UPDATE 1 template status |
| Override one date's seats | Impossible | `available_seats_override` on occurrence |
| Extend recurrence to 60 days | Redesign required | Scheduler handles it automatically |
| Return ride creation | Manual second request | Automatic on `isReturnRide=true` |

---

## 2. Database Schema

### `ride_templates`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `driver_id` | BIGINT FK → users | |
| `vehicle_id` | BIGINT FK → vehicles | |
| `return_template_id` | BIGINT FK → ride_templates | Self-ref; NULL for one-way rides |
| `pickup_lat` | DOUBLE | |
| `pickup_lng` | DOUBLE | |
| `pickup_location` | VARCHAR(255) | |
| `pickup_landmark` | VARCHAR(255) | |
| `pickup_instructions` | TEXT | |
| `flexible_pickup_radius_km` | FLOAT | |
| `destination_lat` | DOUBLE | |
| `destination_lng` | DOUBLE | |
| `destination_location` | VARCHAR(255) | |
| `destination_landmark` | VARCHAR(255) | |
| `route_stops` | JSON | `["Stop A", "Stop B"]` |
| `departure_time` | TIME | |
| `available_seats` | INT | Default for all occurrences |
| `price_per_seat` | DECIMAL(10,2) | Default for all occurrences |
| `share_emergency_contact` | BOOLEAN | |
| `recurring` | BOOLEAN | false = one-time ride |
| `repeat_type` | ENUM | DAILY / WEEKLY / MONTHLY; NULL if not recurring |
| `repeat_start_date` | DATE | First occurrence date |
| `repeat_until` | DATE | NULL = rolling window (no end date) |
| `template_status` | ENUM | ACTIVE / PAUSED / ENDED |
| `created_at` | TIMESTAMP | |
| `updated_at` | TIMESTAMP | |

### `ride_occurrences`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `template_id` | BIGINT FK → ride_templates | |
| `ride_date` | DATE | The specific date of this instance |
| `available_seats_override` | INT | NULL = use template default |
| `price_per_seat_override` | DECIMAL(10,2) | NULL = use template default |
| `status` | ENUM | SCHEDULED / CANCELLED / COMPLETED / FULL |
| `created_at` | TIMESTAMP | |
| `updated_at` | TIMESTAMP | |

**Unique constraint:** `(template_id, ride_date)` — prevents duplicate occurrences.

### `template_preferences`

| Column | Type |
|---|---|
| `template_id` | BIGINT FK |
| `preference_id` | BIGINT FK |

---

## 3. API Contract

### Request — `POST /rides`

```json
{
  "vehicleId": 1,

  "pickupLat": 28.6139, "pickupLng": 77.2090,
  "pickupLocation": "Connaught Place, Delhi",
  "pickupLandmark": "Near Palika Bazaar",
  "pickupInstructions": "Wait near gate 3",
  "flexiblePickupRadiusKm": 0.5,

  "destinationLat": 28.5355, "destinationLng": 77.3910,
  "destinationLocation": "Noida Sector 62",
  "destinationLandmark": "HCL Tower",

  "routeStops": ["Akshardham", "Noida Sector 18"],
  "rideDate": "2026-07-01",
  "departureTime": "08:30:00",

  "isRepeatRide": true,
  "repeatType": "DAILY",
  "repeatUntil": "2026-07-31",

  "isReturnRide": true,
  "returnDepartureTime": "18:00:00",
  "returnRideDate": "2026-07-01",

  "availableSeats": 3,
  "pricePerSeat": 150.00,

  "preferenceIds": [1, 2],
  "shareEmergencyContact": true
}
```

**Validation rules (enforced by `@ValidRepeatRide`):**
- `repeatType` is required when `isRepeatRide=true`
- `returnDepartureTime` is required when `isReturnRide=true`
- `repeatUntil` must be after `rideDate` if provided

### Response

```json
{
  "templateId": 101,
  "occurrenceId": 5001,
  "rideDate": "2026-07-01",
  "departureTime": "08:30:00",
  "availableSeats": 3,
  "pricePerSeat": 150.00,
  "repeatRide": true,
  "repeatType": "DAILY",
  "repeatUntil": "2026-07-31",
  "occurrenceStatus": "SCHEDULED",
  "templateStatus": "ACTIVE",
  "message": "Ride created successfully",

  "pickupLocation": "Connaught Place, Delhi",
  "pickupLat": 28.6139, "pickupLng": 77.2090,
  "destinationLocation": "Noida Sector 62",
  "destinationLat": 28.5355, "destinationLng": 77.3910,

  "driverDetails": { ... },
  "vehicleDetail": { ... },
  "preferences": [ ... ],

  "returnRide": {
    "templateId": 102,
    "occurrenceId": 5032,
    "rideDate": "2026-07-01",
    "departureTime": "18:00:00",
    "pickupLocation": "Noida Sector 62",
    "destinationLocation": "Connaught Place, Delhi",
    "availableSeats": 3,
    "pricePerSeat": 150.00,
    "status": "SCHEDULED"
  }
}
```

---

## 4. Step-by-Step Creation Flow

```
Client
  │
  │  POST /rides  (CreateRideRequest)
  ▼
RideController.createRide()
  │
  │  @Valid — DTO field validation (nulls, sizes, @FutureOrPresent)
  │  @ValidRepeatRide — cross-field: repeatType required if isRepeatRide=true
  │                                  returnDepartureTime required if isReturnRide=true
  ▼
RideServiceImpl.createRideRequest()
  │
  ├─ [1] currentUserService.getCurrentUser()
  │       Extracts driver from JWT. Throws 401 if token invalid.
  │
  ├─ [2] vehicleRepository.findById(req.vehicleId)
  │       Throws ResourceNotFoundException(404) if not found.
  │       (Future: assert vehicle.driver == currentUser)
  │
  ├─ [3] preferenceRepository.findAllById(req.preferenceIds)
  │       Bulk fetch. Size mismatch → ResourceNotFoundException(404).
  │
  ├─ [4] rideMapper.toTemplate(req, user, vehicle, prefSet)
  │       Builds RideTemplate in memory — NO DB call yet.
  │       Sets recurring=false if repeatType is null (safety guard).
  │
  ├─ [5] isReturnRide=true ?
  │   │
  │   ├── YES → createWithReturnRide() (see Section 5)
  │   │
  │   └── NO  → templateRepository.save(forwardTemplate)
  │               └─ 1 INSERT into ride_templates
  │
  ├─ [6] occurrenceGenerator.generateInitialWindow(savedTemplate)
  │       Computes dates from repeatStartDate up to:
  │         min(repeatUntil, today + 30 days)
  │       For one-time ride: returns [ repeatStartDate ] (single item)
  │
  ├─ [7] occurrenceRepository.saveAll(occurrences)
  │       1 batch INSERT for all occurrences.
  │       For DAILY until 30 days: ~30 rows in 1 query.
  │
  └─ [8] rideMapper.toResponse(savedTemplate, occurrences.get(0))
          Maps template + first occurrence → CreateRideResponse.
          getEffectiveSeats() / getEffectivePrice() read override ?? template default.
```

---

## 5. Return Ride Sub-Flow

When `isReturnRide=true`, the service creates **two linked templates** atomically.
The return template is the mirror of the forward template: pickup and destination are swapped,
route stops are reversed, and departure time comes from `returnDepartureTime`.

```
createWithReturnRide()
  │
  ├─ [1] rideMapper.toReturnTemplate(req, user, vehicle, prefSet)
  │         pickup  ← original destinationLat/Lng/Location/Landmark
  │         destination ← original pickupLat/Lng/Location/Landmark
  │         routeStops  ← Collections.reverse(original stops)
  │         departureTime ← req.returnDepartureTime
  │         repeatStartDate ← req.returnRideDate ?? req.rideDate
  │         same: seats, price, recurrence rule, safety
  │
  ├─ [2] templateRepository.save(returnTemplate)
  │         INSERT return template first — no FK pointing to forward yet.
  │         JPA assigns returnTemplate.id (e.g. 102).
  │
  ├─ [3] forwardTemplate.setReturnTemplate(savedReturn)
  │         Sets return_template_id = 102 on forward template.
  │
  ├─ [4] templateRepository.save(forwardTemplate)
  │         INSERT forward template with return_template_id = 102.
  │
  ├─ [5] occurrenceGenerator.generateInitialWindow(savedForward)
  │       occurrenceGenerator.generateInitialWindow(savedReturn)
  │         Both use the same recurrence rule → same number of occurrences.
  │
  ├─ [6] occurrenceRepository.saveAll(forwardOccurrences)
  │       occurrenceRepository.saveAll(returnOccurrences)
  │         2 batch INSERTs.
  │
  └─ [7] rideMapper.toResponse(savedForward, forwardOcc.get(0),
  │                              savedReturn,  returnOcc.get(0))
  │         Response includes full forward ride + nested returnRide summary.
  │
  └─ All inside @Transactional — any failure rolls back both templates + all occurrences.
```

### What the driver sees

```
Forward:  CP Delhi 08:30 → Noida Sector 62   (templateId=101, 30 occurrences)
Return:   Noida Sector 62 18:00 → CP Delhi   (templateId=102, 30 occurrences)
```

Both templates share the same `vehicle`, `preferences`, `availableSeats`, and recurrence rule.
The driver can later cancel or modify each series independently via its own `templateId`.

---

## 6. Occurrence Window Management

### Initial window (on creation)

```
today = 2026-07-01
repeatUntil = 2026-07-31

window_end = min(repeatUntil, today + 30) = 2026-07-31

Generated: 2026-07-01 → 2026-07-31 = 31 occurrences (DAILY)
```

### Rolling window (nightly scheduler)

```
@Scheduled(cron = "0 0 2 * * *")   ← runs every night at 2 AM
OccurrenceWindowExtender.extendWindows()
  │
  ├─ Fetch all templates WHERE templateStatus = ACTIVE AND recurring = true
  │
  └─ For each template:
        window_end = today + 30 days  (or repeatUntil if sooner)
        latest_existing = MAX(ride_date) from ride_occurrences
        generate_from = latest_existing + 1 day

        if generate_from <= window_end:
            generateFrom(template, generate_from, window_end)
            saveAll(new occurrences)
```

**Example:** On 2026-07-02 the scheduler runs and sees the latest occurrence is 2026-07-31.
New window_end = 2026-08-01. It generates 1 new occurrence: 2026-08-01.
Every night it adds exactly 1 row for DAILY rides. Zero wasted work.

If `repeatUntil` is set and the window would exceed it, generation stops at `repeatUntil`.
After the last occurrence passes, nothing is generated and the template status can be set to ENDED.

---

## 7. Update & Cancel Operations

### Cancel a specific date

```
PATCH /rides/occurrences/{occurrenceId}/cancel
  └─ occurrenceRepository: SET status = CANCELLED WHERE id = occurrenceId
     1 UPDATE. All other occurrences unaffected.
```

### Cancel the whole series (all future dates)

```
PATCH /rides/templates/{templateId}/cancel
  ├─ templateRepository: SET templateStatus = ENDED WHERE id = templateId
  └─ occurrenceRepository: SET status = CANCELLED
       WHERE template_id = templateId AND ride_date >= today AND status = SCHEDULED
     2 UPDATEs. Past occurrences (COMPLETED/FULL) are preserved for history.
```

### Change price for all future rides

```
PATCH /rides/templates/{templateId}/price  { "pricePerSeat": 175.00 }
  └─ templateRepository: SET price_per_seat = 175.00 WHERE id = templateId
     1 UPDATE. All future occurrences WITHOUT a price override automatically reflect this.
     Past occurrences are not affected.
```

### Change price for one specific date

```
PATCH /rides/occurrences/{occurrenceId}/price  { "pricePerSeat": 200.00 }
  └─ occurrenceRepository: SET price_per_seat_override = 200.00 WHERE id = occurrenceId
     1 UPDATE. Only this date is affected.
```

### Effective value resolution (always in RideOccurrence)

```java
public Integer getEffectiveSeats() {
    return availableSeatsOverride != null ? availableSeatsOverride : template.getAvailableSeats();
}

public BigDecimal getEffectivePrice() {
    return pricePerSeatOverride != null ? pricePerSeatOverride : template.getPricePerSeat();
}
```

---

## 8. Error Scenarios

| Scenario | Where caught | HTTP status | Message |
|---|---|---|---|
| JWT missing / expired | Spring Security filter | 401 | Unauthorized |
| vehicleId not found | Service | 404 | Vehicle not found |
| preferenceId(s) not found | Service | 404 | One or more preferences not found |
| `isRepeatRide=true` but `repeatType=null` | `@ValidRepeatRide` | 400 | repeatType is required when isRepeatRide is true |
| `isReturnRide=true` but `returnDepartureTime=null` | `@ValidRepeatRide` | 400 | returnDepartureTime is required when isReturnRide is true |
| `rideDate` in the past | `@FutureOrPresent` on DTO | 400 | Ride date cannot be in the past |
| DB failure on save | Service catch block | 500 | Failed to create ride (logged with cause) |
| Duplicate occurrence (same template + date) | DB unique constraint | 409 | Unique constraint violation (shouldn't happen normally) |

---

## 9. Package Structure

```
com.carPooling.backend
│
├── controller
│   └── RideController.java
│
├── service
│   ├── RideService.java              (interface)
│   └── impl
│       └── RideServiceImpl.java      (createRideRequest, createWithReturnRide,
│                                      cancelOccurrence, cancelSeries, updatePrice)
│
├── mapper
│   └── RideMapper.java               (toTemplate, toReturnTemplate, toResponse)
│
├── component
│   ├── OccurrenceGenerator.java      (generateInitialWindow, generateFrom)
│   └── OccurrenceWindowExtender.java (@Scheduled nightly job)
│
├── entity
│   ├── RideTemplate.java             (ride_templates table)
│   └── RideOccurrence.java           (ride_occurrences table)
│
├── repository
│   ├── RideTemplateRepository.java   (findAllActiveRecurring)
│   └── RideOccurrenceRepository.java (findMaxDateByTemplateId,
│                                      cancelFutureOccurrences,
│                                      findScheduledByDate)
│
├── dto
│   ├── request
│   │   └── CreateRideRequest.java    (+ returnDepartureTime, returnRideDate, repeatUntil)
│   └── response
│       ├── CreateRideResponse.java   (templateId, occurrenceId, returnRide)
│       └── ReturnRideSummary.java    (nested object in response)
│
├── enums
│   ├── RepeatType.java               (DAILY, WEEKLY, MONTHLY)
│   ├── TemplateStatus.java           (ACTIVE, PAUSED, ENDED)
│   └── OccurrenceStatus.java         (SCHEDULED, CANCELLED, COMPLETED, FULL)
│
└── validation
    ├── ValidRepeatRide.java           (annotation)
    └── RepeatRideValidator.java       (cross-field logic)
```