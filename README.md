# Foodie Express

A Spring Boot 3 food delivery REST API.

---

## Tech Stack

| Layer | Tech |
|---|---|
| Runtime | Java 17 / Spring Boot 3.2.5 |
| Data | MySQL + Spring Data JPA (Hibernate) |
| Auth | Signed JWT bearer tokens (24-hour expiry) |
| Docs | SpringDoc OpenAPI → Swagger UI |
| Utility | Lombok |

---

## Quick Start

### 1. Prerequisites
* JDK 17+
* MySQL 8 running locally (or adjust `application.properties`)
* Maven 3.9+

### 2. Create the database

```sql
CREATE DATABASE IF NOT EXISTS foodie_express;
```

### 3. Run the app

```bash
cd foodie-express
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

Swagger UI is available at **http://localhost:8080/swagger-ui.html**.

### JWT secret

Set a private, random secret of at least 32 bytes before starting the backend. Do not commit it or expose it in the frontend:

```powershell
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
mvn spring-boot:run
```

Tokens expire after 24 hours by default. `JWT_EXPIRATION_MS` can override the lifetime. Requests use `Authorization: Bearer <token>`; `X-Session-Token` remains supported for compatibility. The backend stores only a SHA-256 fingerprint so logout can revoke the active token. Existing UUID sessions must sign in again after upgrading.

---

## Database settings

Set the database connection in the backend environment. `DB_URL` defaults to the local `foodie_express` database URL; set `DB_USERNAME` and `DB_PASSWORD` to credentials for a least-privilege local application account. Do not put database credentials in source control. Hibernate will create/update tables automatically (`ddl-auto=update`), so use a dedicated local database and do not point development at production data.

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/foodie_express?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "your local database user"
$env:DB_PASSWORD = "your local database password"
```

### Razorpay checkout

Checkout uses Razorpay's test or live mode from backend environment variables. Configure both values before starting Spring Boot; do not put the secret in the frontend or commit it:

```powershell
$env:RAZORPAY_KEY_ID = "your Razorpay key id"
$env:RAZORPAY_KEY_SECRET = "your Razorpay key secret"
mvn spring-boot:run
```

Without both values, the app keeps cart and order browsing available but disables payment order creation. A bill can only become paid after the backend verifies the Razorpay signature and confirms the captured payment amount matches the server-generated bill.

---

## API Overview

### Public (no token needed)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a customer account |
| POST | `/api/auth/login` | Login → returns session token |
| GET | `/api/restaurants` | List active restaurants |
| GET | `/api/restaurants/{id}` | Restaurant details |
| GET | `/api/restaurants/{id}/items` | Menu items of a restaurant |
| GET | `/api/food-items/{id}` | Single food item |

### Authenticated (pass `Authorization: Bearer <token>` header)

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/logout` | any | Invalidate session |
| GET | `/api/customers/me` | customer | Get own profile |
| PUT | `/api/customers/me` | customer | Update own profile |
| GET | `/api/cart` | customer | View cart |
| POST | `/api/cart/add` | customer | Add item to cart |
| PUT | `/api/cart/items/{id}` | customer | Update item quantity |
| DELETE | `/api/cart/items/{id}` | customer | Remove item |
| DELETE | `/api/cart/clear` | customer | Clear cart |
| POST | `/api/orders/place` | customer | Place order |
| GET | `/api/orders` | customer | My orders |
| GET | `/api/orders/{id}` | customer | Order details |
| PATCH | `/api/orders/{id}/cancel` | customer | Cancel order |
| GET | `/api/bills/order/{orderId}` | customer | Bill for an order |
| GET | `/api/bills/my` | customer | My bills |
| GET | `/api/bills/{id}` | customer | Bill details |
| POST | `/api/payments/create-order` | customer | Create a Razorpay order for an owned order |
| POST | `/api/payments/verify` | customer | Verify captured payment signature |

### Admin (pass `Authorization: Bearer <token>` from admin login)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/admin/register` | Register admin account |
| GET | `/api/admin/dashboard` | Dashboard counts, revenue, and recent orders |
| POST | `/api/admin/restaurants` | Add restaurant |
| PUT | `/api/admin/restaurants/{id}` | Update restaurant |
| DELETE | `/api/admin/restaurants/{id}` | Delete restaurant |
| POST | `/api/admin/restaurants/{id}/items` | Add food item |
| PUT | `/api/admin/food-items/{id}` | Update food item |
| DELETE | `/api/admin/food-items/{id}` | Delete food item |

---

## Sample Flow (test in Swagger UI)

1. **Register a customer**: `POST /api/auth/register`
2. **Register an admin**: `POST /api/auth/admin/register`
3. **Login as admin**: `POST /api/auth/login` → copy `sessionToken`
4. **Add a restaurant**: `POST /api/admin/restaurants` with admin token
5. **Add a menu item**: `POST /api/admin/restaurants/{id}/items` with admin token
6. **Login as customer**: `POST /api/auth/login` → copy `sessionToken`
7. **Browse restaurants**: `GET /api/restaurants`
8. **Add to cart**: `POST /api/cart/add` with customer token
9. **Place order**: `POST /api/orders/place`
10. **View bill**: `GET /api/bills/my`

---

## Project Structure

```
com.foodieexpress
├── FoodieExpressApplication.java   # Entry point
├── config
│   ├── SecurityConfig.java         # CSRF disabled, all endpoints open
│   └── SwaggerConfig.java          # OpenAPI + API key scheme
├── model                            # JPA entities
├── repository                       # Spring Data JPA repos
├── service                          # Business logic
├── controller                       # REST endpoints
├── dto                              # Request/Response DTOs
└── exception                        # Global exception handler
```
