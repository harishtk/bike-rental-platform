# Bike Rental Platform — Domain Model & Use-Case Specification v1

## 1. Domain Scope

The Bike Rental Platform allows customers to reserve and rent bicycles from stations.

A rental is a time-bound agreement in which:

- A customer rents one bike.
- A customer must have at most one active rental at a time.
- A bike can have at most one active rental at a time.
- A customer may reserve a specific available bike for a limited period.
- An upfront payment is required before a rental becomes active.
- The final rental charge is calculated when the bike is returned.
- The remaining amount is collected after return.
- A bike may be returned to a different station.
- Bikes can be taken out of service for maintenance.
- Rental pricing is initially based on a daily rate.

Geographical coordinates are intentionally excluded from v1.

A station contains:

```text
id
name
address
capacity
status
```

Latitude/longitude can be introduced later without changing the core rental model.

---

# 2. Actors

## Customer

A registered platform user who can:

- Browse bikes and stations
- Reserve a bike
- Cancel a reservation
- Start a rental
- Extend a rental
- Cancel a rental where permitted
- Return a bike
- View rental history
- View payment information

## Operator

Responsible for operational activities:

- Manage bikes
- Manage station inventory
- Move bikes between stations
- Report maintenance
- Start maintenance
- Complete maintenance

## Administrator

Responsible for system-level operations:

- Manage users
- Manage operators
- Manage stations
- Manage pricing configuration
- Manage system configuration

## System

Automated responsibilities include:

- Expiring reservations
- Releasing reserved bikes
- Calculating rental charges
- Processing asynchronous events
- Updating inventory projections
- Sending notifications
- Recording audit information

---

# 3. Core Domain Entities

```text
User
Bike
Station
Reservation
Rental
Payment
MaintenanceRecord
PricingRule
```

Relationships:

```text
User
 ├── Reservation
 └── Rental

Bike
 ├── Reservation
 ├── Rental
 └── MaintenanceRecord

Station
 └── Bike

Rental
 └── Payment
```

---

# 4. User

## Attributes

```text
User
------------------------
id
username
email
status
createdAt
updatedAt
```

Authentication credentials are not part of the business domain.

Identity and authentication are delegated to the identity provider, initially Keycloak.

## User invariant

A user cannot have more than one active rental.

---

# 5. Bike

## Attributes

```text
Bike
------------------------
id
serialNumber
type
status
stationId
createdAt
updatedAt
```

## Bike Status

```text
AVAILABLE
RESERVED
RENTED
MAINTENANCE
RETIRED
```

## Valid state transitions

```text
AVAILABLE
 ├── reserve ──────────────> RESERVED
 ├── start rental ─────────> RENTED
 └── maintenance ──────────> MAINTENANCE

RESERVED
 ├── reservation expires ──> AVAILABLE
 ├── reservation cancelled -> AVAILABLE
 └── rental starts ────────> RENTED

RENTED
 └── return ────────────────> AVAILABLE

MAINTENANCE
 └── maintenance completed -> AVAILABLE

AVAILABLE / MAINTENANCE
 └── retire ────────────────> RETIRED
```

## Bike invariants

1. A bike can have at most one active rental.
2. A bike cannot be rented while reserved by another customer.
3. A bike cannot be rented while in maintenance.
4. A retired bike cannot be rented or reserved.
5. A bike belongs to at most one station at a time.

---

# 6. Station

## Attributes

```text
Station
------------------------
id
name
address
capacity
status
createdAt
updatedAt
```

## Station Status

```text
ACTIVE
INACTIVE
```

A station marked `INACTIVE` cannot receive new rentals/returns unless explicitly permitted by future operational rules.

## Station invariants

1. A station cannot exceed its configured capacity.
2. A bike can belong to only one station at a time.
3. Returning a bike to another station changes its station assignment.

For v1, inventory counts are derived from bike state rather than treated as an independent source of truth.

---

# 7. Reservation

A reservation temporarily holds a specific bike for a specific customer.

## Attributes

```text
Reservation
------------------------
id
userId
bikeId
stationId
status
reservedAt
expiresAt
cancelledAt
createdAt
updatedAt
```

## Reservation Status

```text
ACTIVE
EXPIRED
CANCELLED
CONVERTED
```

## Reservation lifecycle

```text
ACTIVE
 ├── cancel ───────────────> CANCELLED
 ├── expiration reached ───> EXPIRED
 └── rental started ────────> CONVERTED
```

## Reservation rules

1. A reservation targets one specific bike.
2. A bike can have only one active reservation.
3. A reservation remains active for up to seven days in v1.
4. An expired reservation releases the bike.
5. A customer cannot create a reservation for a bike that is unavailable.
6. A customer cannot reserve a bike if the customer already has an active rental.
7. A converted reservation cannot be cancelled as a reservation because it has already become a rental.

---

# 8. Rental

Rental is the central business concept.

## Attributes

```text
Rental
----------------------------
id
userId
bikeId
pickupStationId
returnStationId
status
startTime
expectedEndTime
actualEndTime
dailyRate
estimatedAmount
prepaidAmount
finalAmount
remainingAmount
createdAt
updatedAt
```

## Rental Status

For v1:

```text
PENDING
ACTIVE
PAYMENT_PENDING
COMPLETED
CANCELLED
```

## Rental lifecycle

```text
                PENDING
                   |
          upfront payment
                   |
                   v
                ACTIVE
                   |
                return
                   |
                   v
           PAYMENT_PENDING
             /          \
            /            \
     payment success   payment failure
          |                 |
          v                 |
      COMPLETED <-----------+
```

`PAYMENT_PENDING` allows the platform to represent a physically completed return whose outstanding payment has not yet been settled.

---

# 9. Rental Rules

## Start

A rental can start only when:

- The user is eligible.
- The bike is available/reserved for that user.
- The user has no active rental.
- The bike has no active rental.
- The bike is not in maintenance.
- The required upfront payment succeeds.

## Active rental

While active:

- The bike is considered `RENTED`.
- The user cannot start another rental.
- The bike cannot be rented by another user.
- The customer may extend the rental.

## Extension

The customer may extend an active rental.

No separate payment is required for an extension in v1.

The final charge is based on the actual rental duration.

## Return

A return records:

```text
actualEndTime
returnStationId
```

The bike becomes available at the destination station after successful return processing, subject to any maintenance assessment.

## Cancellation

Cancellation is permitted only in states where the business policy allows it.

For v1:

```text
PENDING  -> CANCELLED
ACTIVE   -> CANCELLED
```

Exact refund rules will be implemented in the payment policy rather than hard-coded into the rental state machine.

---

# 10. Pricing

The initial pricing model is pay-per-day.

```text
dailyRate = configurable amount
```

Rental duration is calculated using 24-hour billing periods.

```text
billableDays =
    ceil(actualDurationInHours / 24)
```

Example:

```text
Start:  2026-09-01 10:00
Return: 2026-09-02 09:00

Duration = 23 hours
Billable = 1 day
```

Another example:

```text
Start:  2026-09-01 10:00
Return: 2026-09-02 11:00

Duration = 25 hours
Billable = 2 days
```

The model intentionally keeps the pricing strategy replaceable.

Future strategies may include:

```text
HOURLY
DAILY
WEEKLY
PROMOTIONAL
DYNAMIC
```

---

# 11. Payment

A rental may require multiple payment transactions.

## Payment concept

```text
Payment
------------------------
id
rentalId
type
amount
status
providerReference
createdAt
updatedAt
```

## Payment Type

```text
UPFRONT
FINAL
REFUND
```

## Payment Status

```text
PENDING
PROCESSING
COMPLETED
FAILED
REFUNDED
```

## Initial payment

Before a rental becomes active:

```text
estimatedAmount
        |
        v
required upfront amount
        |
        v
Payment Service
        |
     success
        |
        v
Rental ACTIVE
```

## Final payment

At return:

```text
finalAmount
   -
prepaidAmount
   =
remainingAmount
```

The remaining amount is processed through the Payment Service.

---

# 12. Payment Failure Rules

## Upfront payment failure

The rental does not become active.

The reservation remains subject to its existing rules, or the operation is rejected if there is no valid reservation.

The exact reservation-release behavior will be defined when we formalize the transaction workflow.

## Final payment failure

The bike may already have physically returned.

Therefore, the rental moves to:

```text
PAYMENT_PENDING
```

The system records the outstanding amount.

The rental is not considered financially settled until the final payment succeeds.

This prevents us from incorrectly marking a rental as completely successful when money is still owed.

---

# 13. Maintenance

Maintenance is centrally managed in v1.

We do not create a separate Maintenance microservice yet.

The Bike domain owns maintenance state.

## MaintenanceRecord

```text
MaintenanceRecord
----------------------------
id
bikeId
reportedBy
reason
status
startedAt
completedAt
notes
createdAt
updatedAt
```

## Maintenance Status

```text
REPORTED
IN_PROGRESS
COMPLETED
CANCELLED
```

## Rules

A bike in maintenance:

```text
cannot be reserved
cannot be rented
cannot be returned into service
```

unless the maintenance workflow has completed.

The bike moves:

```text
AVAILABLE
    |
    v
MAINTENANCE
    |
    v
AVAILABLE
```

---

# 14. Use Case: Register User

## Actor

Customer

## Preconditions

None.

## Input

```text
username
email
```

Authentication account creation will eventually integrate with Keycloak.

## Success

A user account/profile is created.

## Failure cases

```text
username already exists
email already exists
invalid input
```

## Result

```text
UserCreated
```

---

# 15. Use Case: Add Bike

## Actor

Operator/Admin

## Preconditions

- Authenticated user has appropriate permission.
- Station exists if a station assignment is supplied.
- Serial number is unique.

## Input

```text
serialNumber
type
stationId
```

## Success

Bike is created as:

```text
AVAILABLE
```

## Events

```text
BikeCreated
```

---

# 16. Use Case: Create Reservation

## Actor

Customer

## Preconditions

```text
customer is authenticated
customer has no active rental
bike exists
bike is AVAILABLE
bike is associated with requested station
```

## Operation

```text
AVAILABLE
   |
   v
RESERVED
```

Reservation expiration:

```text
expiresAt = reservedAt + 7 days
```

## Events

```text
BikeReserved
ReservationCreated
```

## Failure scenarios

```text
bike not found
bike unavailable
bike already reserved
customer already has active rental
station mismatch
```

---

# 17. Use Case: Cancel Reservation

## Actor

Customer

## Preconditions

```text
reservation belongs to customer
reservation status = ACTIVE
```

## State transition

```text
ACTIVE -> CANCELLED
```

Bike:

```text
RESERVED -> AVAILABLE
```

## Events

```text
ReservationCancelled
BikeReleased
```

---

# 18. Use Case: Expire Reservation

## Actor

System

## Preconditions

```text
reservation.status = ACTIVE
currentTime >= expiresAt
```

## State transition

```text
ACTIVE -> EXPIRED
```

Bike:

```text
RESERVED -> AVAILABLE
```

## Events

```text
ReservationExpired
BikeReleased
```

## Notification

Notification Service consumes:

```text
ReservationExpired
```

and notifies the customer.

---

# 19. Use Case: Start Rental

## Actor

Customer

## Preconditions

```text
customer authenticated
customer has no active rental
bike available OR reserved for customer
bike not in maintenance
bike not retired
```

## Process

1. Validate customer.
2. Validate bike.
3. Validate reservation if one exists.
4. Calculate estimated rental amount.
5. Calculate upfront payment.
6. Process upfront payment.
7. Create/activate rental.
8. Mark bike as `RENTED`.
9. Convert reservation if applicable.

## Important concurrency rule

The operation must guarantee:

```text
one user  -> max one active rental
one bike  -> max one active rental
```

These rules must be enforced transactionally.

## Events

```text
RentalStarted
PaymentRequested
PaymentCompleted
BikeRented
```

The exact event order and transactional boundaries will be finalized during service design.

---

# 20. Use Case: Extend Rental

## Actor

Customer

## Preconditions

```text
rental belongs to customer
rental.status = ACTIVE
```

## Operation

Update the expected end time.

No immediate final payment is required.

Example:

```text
Original expected duration: 3 days
Extension: +2 days

Expected duration: 5 days
```

Final billing still occurs at return.

## Event

```text
RentalExtended
```

---

# 21. Use Case: Return Bike

## Actor

Customer

## Preconditions

```text
rental belongs to customer
rental.status = ACTIVE
bike corresponds to rental
return station exists
return station is active
```

## Process

1. Record actual return time.
2. Record return station.
3. Calculate actual rental duration.
4. Calculate final amount.
5. Calculate remaining amount.
6. Process final payment.
7. Update bike station assignment.
8. Make bike available when operationally valid.
9. Complete rental if payment succeeds.

## Example

```text
Daily rate          ₹100
Actual duration       4 days
Final amount         ₹400
Prepaid amount       ₹100
Remaining amount     ₹300
```

## Successful result

```text
Rental -> COMPLETED
Bike   -> AVAILABLE
Station inventory updated
```

## Final payment failure

```text
Rental -> PAYMENT_PENDING
Bike   -> returned
Outstanding amount recorded
```

The exact bike state after final-payment failure will be finalized when we define the transaction boundary. The physical return and financial settlement must be independently representable.

---

# 22. Use Case: Cancel Rental

## Actor

Customer

## Preconditions

Cancellation must be allowed for the current rental state.

## Operation

```text
PENDING -> CANCELLED
ACTIVE  -> CANCELLED
```

The corresponding bike state is released appropriately.

Payment/refund behavior is delegated to payment policy.

## Event

```text
RentalCancelled
```

---

# 23. Use Case: Put Bike Into Maintenance

## Actor

Operator

## Preconditions

Bike is not retired.

## Operation

```text
AVAILABLE -> MAINTENANCE
```

If operational rules later permit maintenance while a bike is reserved, that workflow will require cancellation/release of the reservation first.

## Event

```text
BikeMaintenanceStarted
```

---

# 24. Use Case: Release Bike From Maintenance

## Actor

Operator

## Preconditions

```text
maintenance exists
maintenance is IN_PROGRESS
```

## Operation

```text
MAINTENANCE -> AVAILABLE
```

## Event

```text
BikeMaintenanceCompleted
BikeAvailable
```

---

# 25. Core Business Invariants

These invariants are critical enough that they should eventually have dedicated integration tests.

### User

```text
A user cannot have more than one ACTIVE rental.
```

### Bike

```text
A bike cannot have more than one ACTIVE rental.

A bike cannot be rented while MAINTENANCE.

A bike cannot be rented while RESERVED by another customer.

A RETIRED bike cannot be rented or reserved.
```

### Reservation

```text
A bike can have at most one ACTIVE reservation.

An expired reservation must release the bike.

A reservation cannot continue after conversion into a rental.
```

### Station

```text
A bike can belong to only one station.

Station capacity cannot be exceeded.
```

### Rental

```text
A rental belongs to exactly one user and one bike.

A rental cannot become ACTIVE before upfront payment succeeds.

The final amount is based on actual rental duration.

Remaining amount = final amount - prepaid amount.
```

---

# 26. Concurrency Requirements

The following operations are concurrency-sensitive:

```text
Reserve bike
Start rental
Cancel reservation
Expire reservation
Return bike
Move bike
Start maintenance
```

The most important race:

```text
Customer A                 Customer B
     |                         |
     | reserve Bike-101        | reserve Bike-101
     |------------------------>|
              concurrent
                    |
                    v
              only ONE succeeds
```

Similarly:

```text
Customer A
   |
   ├── rent Bike-101
   |
   └── rent Bike-102
```

Only one operation may create an active rental.

The database will ultimately enforce the critical uniqueness constraints. Redis may assist with distributed coordination, but it will not be our sole consistency mechanism.

---

# 27. Initial Domain Events

We will evolve this list as implementation begins.

```text
UserCreated

BikeCreated
BikeReserved
BikeReleased
BikeRented
BikeReturned
BikeMaintenanceStarted
BikeMaintenanceCompleted

ReservationCreated
ReservationCancelled
ReservationExpired
ReservationConverted

RentalStarted
RentalExtended
RentalCancelled
RentalCompleted

PaymentRequested
PaymentCompleted
PaymentFailed
RefundRequested
RefundCompleted
```

Some of these events may eventually be consolidated or renamed once Kafka contracts are designed.

---

# 28. Initial Service Ownership

## User Service

Owns:

```text
User
```

## Bike Service

Owns:

```text
Bike
Station
MaintenanceRecord
```

## Rental Service

Owns:

```text
Reservation
Rental
PricingRule
```

## Payment Service

Owns:

```text
Payment
```

## Notification Service

Owns notification delivery state and templates.

The Notification Service does not own business entities such as Rental or Bike.

---

# 29. Source-of-Truth Rule

Each business entity has one authoritative service:

```text
User          -> User Service
Bike          -> Bike Service
Station       -> Bike Service
Reservation   -> Rental Service
Rental        -> Rental Service
PricingRule   -> Rental Service
Payment       -> Payment Service
```

Other services may maintain projections or cached copies, but they must not modify another service's source-of-truth data directly.

---

# 30. v1 Scope

The initial implementation will include:

```text
User registration/profile
Bike management
Station management
Bike availability
Reservations
Reservation expiration
Rental creation/start
Rental cancellation
Rental extension
Bike return
Daily pricing
Upfront payment
Final payment
Maintenance
Kafka domain events
Redis caching/coordination
Keycloak authentication
REST APIs
Integration tests
End-to-end tests
Docker Compose environment
```

Explicitly deferred:

```text
GPS / latitude / longitude
Dynamic pricing
Promotions
Multiple bikes per customer
Subscriptions
Ride telemetry
Mobile-specific features
Real payment provider
Production deployment
Advanced analytics
```

These can be added later without changing the fundamental rental concept.