# DineHub Backend

This repository contains the Spring Boot API for DineHub, a full-stack food-ordering application. It provides JWT authentication, public restaurant and menu discovery, carts, saved addresses, orders, Stripe payments, favourites, and protected restaurant-owner operations.

## Live application

- Frontend: [dinehub-good-food-delivered.vercel.app](https://dinehub-good-food-delivered.vercel.app)
- API: [food-ordering-app-e13n.onrender.com](https://food-ordering-app-e13n.onrender.com)
- Frontend repository: [Food-Ordering-App-Frontend](https://github.com/ShantanuSutar/Food-Ordering-App-Frontend)

The Render service may need a short cold-start period before the first request completes.

## Main capabilities

- Customer and restaurant-owner registration and login
- Stateless JWT authentication with BCrypt password hashing
- Role-protected restaurant-owner endpoints
- Server-side ownership checks for restaurants, foods, categories, ingredients, orders, and addresses
- Public restaurant browsing, Top Meals, menu filters, and food/category search
- Single-restaurant cart validation and authoritative server-side totals
- Saved address CRUD scoped to the authenticated user
- Immutable delivery-address and order-item information for order history
- Restaurant favourites resolved against current restaurant data
- Stripe Checkout Session creation and secure payment verification
- Signed Stripe webhook processing with idempotent payment finalization
- Persistent cart clearing only after verified payment
- Customer order history, order details, cancellation rules, and payment history
- Restaurant order management with validated status transitions
- Menu, category, ingredient, and stock management
- PostgreSQL/Neon connection-string support
- Central API error responses and CORS configuration

## Technology

- Java 17
- Spring Boot 4.1
- Spring Web MVC
- Spring Security
- Spring Data JPA and Hibernate
- PostgreSQL in production
- H2 for automated tests
- JSON Web Tokens with JJWT
- Stripe Java SDK
- Maven Wrapper
- Docker multi-stage builds

## Getting started

### Prerequisites

- JDK 17 or newer
- PostgreSQL 14 or newer, or a Neon PostgreSQL database
- Stripe test-mode API keys for checkout testing

### Database setup

Create a local database:

```sql
CREATE DATABASE food_ordering_app;
```

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so the application creates or updates its tables when it connects successfully.

### Environment configuration

Copy the template to a local `.env` file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Configure the following values:

```dotenv
JWT_SECRET=replace-with-a-random-secret-of-at-least-32-bytes
PORT=9090
DB_URL=jdbc:postgresql://localhost:5432/food_ordering_app
DB_USERNAME=postgres
DB_PASSWORD=your-local-password
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
PAYMENT_CURRENCY=inr
```

Configuration notes:

- `JWT_SECRET` must be a strong secret of at least 32 bytes.
- `DB_URL` accepts a JDBC PostgreSQL URL.
- `DATABASE_URL` can alternatively contain a complete Neon or Render `postgresql://` connection string, including credentials and `sslmode=require`.
- `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` must remain server-side.
- `FRONTEND_URL` controls Stripe success/cancel redirects and is also trusted by CORS.
- `CORS_ALLOWED_ORIGINS` accepts additional comma-separated browser origins.
- `PAYMENT_CURRENCY` defaults to `inr`.
- `JPA_SHOW_SQL=true` can optionally enable SQL logging during development.

Do not commit `.env` or live credentials.

### Run locally

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at [http://localhost:9090](http://localhost:9090) by default.

## Demo data

`seed/neon-demo-data.sql` provides a realistic PostgreSQL dataset with users, restaurant owners, restaurants, addresses, categories, foods, ingredients, carts, favourites, paid/pending orders, and order items.

To use it:

1. Start the backend once so Hibernate creates the current schema.
2. Open the Neon SQL Editor or another PostgreSQL client.
3. Run the complete `seed/neon-demo-data.sql` file once.

All seeded demo accounts use the password `Demo@123`. Example accounts include:

- Customer: `priya@dinehub.demo`
- Restaurant owner: `owner.spice@dinehub.demo`

The script refuses to run again when its marker account already exists. Use it only for demonstration or development databases, never for a sensitive production database.

## API overview

### Public endpoints

- `POST /auth/signup`
- `POST /auth/signin`
- `GET /api/restaurants`
- `GET /api/restaurants/{id}`
- `GET /api/restaurants/search?keyword=...`
- `GET /api/food/top`
- `GET /api/food/search?name=...`
- `GET /api/food/restaurant/{restaurantId}`
- `GET /api/category/restaurant/{restaurantId}`
- `POST /api/payment/webhook`

### Authenticated customer endpoints

- `GET /api/users/profile`
- `GET /api/users/favourites`
- `GET|POST /api/users/addresses`
- `PUT|DELETE /api/users/addresses/{id}`
- `PUT /api/restaurants/{id}/add-favourites`
- `GET /api/cart`
- `PUT /api/cart/add`
- `PUT /api/cart-item/update`
- `DELETE /api/cart-item/{id}/remove`
- `PUT /api/cart/clear`
- `POST /api/order`
- `GET /api/order/user`
- `GET /api/order/{id}`
- `PUT /api/order/{id}/cancel`
- `POST /api/payment/verify`
- `GET /api/users/payments`

### Restaurant-owner endpoints

- `/api/admin/restaurants/**` — restaurant setup, editing, status, and owner lookup
- `/api/admin/food/**` — menu creation, editing, availability, and archival
- `/api/admin/category/**` — food-category management
- `/api/admin/ingredients/**` — ingredient categories, ingredients, and stock
- `/api/admin/order/**` — restaurant orders and status changes

Send authenticated requests with:

```http
Authorization: Bearer <jwt>
```

## Order and payment lifecycle

New orders begin with `PENDING_PAYMENT`. Creating Stripe Checkout does not clear the cart and does not make the order visible as a paid restaurant order.

Payment completion is accepted only when one of these server-side paths validates Stripe data:

- A signed `checkout.session.completed` or `checkout.session.async_payment_succeeded` webhook
- The authenticated `/api/payment/verify` fallback using the Checkout Session ID from Stripe's success redirect

Verification checks the internal order ID, authenticated user, stored Stripe Session ID, metadata, amount, and currency. Successful processing is idempotent, marks the order `PAID`, records non-sensitive transaction details, and clears the persistent cart.

Payment states:

- `PENDING_PAYMENT`
- `PAID`
- `PAYMENT_FAILED`
- `PAYMENT_CANCELLED`

Fulfilment transitions are validated on the backend:

```text
PENDING → CONFIRMED → PREPARING → READY → OUT_FOR_DELIVERY → DELIVERED
```

Cancellation is allowed only from eligible states; terminal states cannot move backwards.

## Stripe webhook setup

For local development with the Stripe CLI:

```bash
stripe listen --forward-to localhost:9090/api/payment/webhook
```

Copy the generated `whsec_...` value into `STRIPE_WEBHOOK_SECRET`, then restart the backend.

For production, create a Stripe webhook endpoint pointing to:

```text
https://your-api.example.com/api/payment/webhook
```

Subscribe to Checkout Session completion, asynchronous success/failure, and expiration events handled by the service.

## Tests

The test suite uses an isolated in-memory H2 database and test-only Stripe/JWT configuration.

macOS/Linux:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Tests cover authentication, public-route security, database URL conversion, serialization contracts, ownership authorization, cart operations, addresses, menu/categories/ingredients, order transitions, payment verification, and restaurant management.

## Docker

Build the image from the backend repository root:

```bash
docker build -t dinehub-backend .
```

Run it using your local environment file:

```bash
docker run --rm -p 9090:9090 --env-file .env dinehub-backend
```

The Dockerfile builds the JAR in a separate Java 17 stage and runs the application as a non-root user. Secrets are supplied only at runtime.

## Render and Neon deployment

1. Create a Render web service from this repository.
2. Use the included Dockerfile.
3. Add the full Neon connection string as `DATABASE_URL`.
4. Add `JWT_SECRET`, Stripe variables, and `FRONTEND_URL`.
5. Set `FRONTEND_URL` to the exact Vercel production origin without a path.
6. Add any additional trusted frontend origins to `CORS_ALLOWED_ORIGINS`.

Keep `sslmode=require` in the Neon URL and do not wrap environment-variable values in quotes. See `DEPLOYMENT.md` for additional deployment notes.

## Project structure

```text
src/main/java/com/shantanu/
├── config/       Security, JWT, database, CORS, and startup support
├── controller/   REST endpoints and centralized exception handling
├── dto/          Lightweight persisted/API data structures
├── model/        JPA entities and enums
├── repository/   Spring Data repositories and queries
├── request/      Validated request contracts
├── response/     Purpose-built API response contracts
└── service/      Business logic and authorization checks

src/test/         Unit and Spring integration tests
seed/             Optional PostgreSQL demo dataset
```

## Security notes

- Never expose Stripe, JWT, or database secrets to the frontend.
- Mutation authorization is enforced server-side; client-supplied IDs are not treated as proof of ownership.
- Stripe success URLs alone are not proof of payment.
- Restaurant and menu records referenced by historical orders are archived or protected instead of blindly hard-deleted.
- Use separate credentials and Stripe test mode for development.
