# common-lib

Shared code for every service. Every service scans `com.jing.common`, so the
configuration here (exception handler, access control) is active automatically.

```
com.jing.common
├── security/      who is calling, and are they allowed
├── pagination/    PageResponse + PageableFactory
├── exception/     exceptions, ErrorResponse, GlobalExceptionHandler
├── dto/<domain>/  request/response contracts: airline, location, flight, seat, pricing, ancillary, user
├── enumeration/   shared enums (UserRole, BookingStatus, ...)
├── embeddable/    JPA value objects (Address, ContactInfo, ...)
└── feign/         Feign defaults: forwards identity headers, maps remote errors to our exceptions
```

## Access control (`security/`)

The API gateway verifies the JWT and sends `X-User-Id` and `X-User-Role` (see `AuthHeaders`).
**Never read those headers yourself.** Use:

```java
// role check: 401 without identity headers, 403 for the wrong role
@RequireRole({ROLE_SYSTEM_ADMIN, ROLE_AIRLINE_OWNER})
@PostMapping
public FareResponse create(@Valid @RequestBody FareRequest request) { ... }

// who is calling (required: 401 when missing)
@GetMapping("/me")
public AirlineResponse mine(CurrentUser user) { return service.getByOwner(user.id()); }

// public endpoint that behaves differently for staff
@GetMapping
public PageResponse<MealResponse> list(@AuthUser(required = false) CurrentUser user) {
    boolean staffView = user.isStaff();   // anonymous callers get CurrentUser.ANONYMOUS
}
```

`@RequireRole` works on a method or on the whole controller class (a method annotation wins).
Resource rules such as "only the owner of this booking" still belong in the service.

## Paging (`pagination/`)

```java
Pageable pageable = PageableFactory.of(page, size, sortBy, sortDirection,
        Set.of("id", "name", "createdAt"),   // allowed sort fields
        "createdAt", Sort.Direction.DESC);   // defaults
return PageResponse.from(repository.findAll(spec, pageable).map(Mapper::toResponse));
```

Pages are 1-based, size is 1 to 100, unknown sort fields give a 400, and `id` is always added as a tie-breaker.

## What belongs here

Put a class here only when more than one service needs it (a DTO another service calls through Feign,
a shared enum). Entities, repositories, filters and projections stay inside their service.
