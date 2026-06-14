# Problems & Learnings Log

This document contains real issues encountered during development and the lessons learned from them.

---

# Problem 1: Request DTO Field Becoming Null

## Problem

A request was sent using:

```json
{
    "preference_name": "Quiet"
}
```

DTO:

```java
@Getter
@Setter
@AllArgsConstructor
public class CreatePreferenceRequest {

    @NotBlank(message = "Preference Type cant be blank")
    private String preference_name;
}
```

However, `preference_name` was always `null` inside the controller/service.

---

## Root Cause

Jackson (used by Spring Boot for JSON deserialization) needs a no-argument constructor to create the DTO object.

The DTO only had:

```java
@AllArgsConstructor
```

which generated:

```java
public CreatePreferenceRequest(String preference_name)
```

but no default constructor.

As a result, Jackson could not properly instantiate the object.

---

## Fix

Add:

```java
@NoArgsConstructor
```

Example:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreferenceRequest {

    @NotBlank(message = "Preference Type cant be blank")
    private String preference_name;
}
```

---

## Additional Learning

`@NotBlank` already checks for:

* null
* empty string
* whitespace-only string

Therefore:

```java
@NotBlank
@NotNull
```

is redundant.

Use only:

```java
@NotBlank
```

for String fields.

---

## Best Practice

Use camelCase in Java:

```java
private String preferenceName;
```

and map JSON if needed using:

```java
@JsonProperty("preference_name")
private String preferenceName;
```

---

# Problem 2: Understanding Database Transactions

## Problem

Confusion about what happens when multiple database operations are performed in a service method and one operation fails.

Example:

```java
userRepository.save(user);

preferenceRepository.save(preference);

rideRepository.save(ride);
```

What happens if the third operation fails?

---

## Without Transaction

```java
userRepository.save(user);

preferenceRepository.save(preference);

rideRepository.save(ride); // Exception
```

Result:

```text
User saved
Preference saved
Ride not saved
```

Database becomes partially updated.

This can lead to inconsistent data.

---

## With Transaction

```java
@Transactional
public void createRide() {

    userRepository.save(user);

    preferenceRepository.save(preference);

    rideRepository.save(ride);
}
```

If any operation fails:

```text
Rollback everything
```

Result:

```text
User not saved
Preference not saved
Ride not saved
```

Database remains consistent.

---

## Key Concept

A transaction follows ACID principles:

### Atomicity

Either everything succeeds or everything fails.

### Consistency

Database remains valid before and after the transaction.

### Isolation

Concurrent transactions do not interfere with each other.

### Durability

Once committed, changes are permanently stored.

---

## When to Use @Transactional

Use when a service performs multiple database operations that should succeed or fail together.

Examples:

* Creating a ride and ride participants
* Booking a seat and updating seat count
* Payment and order creation
* User creation and role assignment

---

## Personal Rule

If a service method performs more than one write operation and all operations depend on each other, consider using:

```java
@Transactional
```

to avoid partial updates.
