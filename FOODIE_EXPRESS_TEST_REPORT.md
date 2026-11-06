# Foodie Express QA Report

**Run date:** 2026-11-05  
**Scope:** Repository audit, available automated checks, and safe local runtime checks.  
**Result:** Backend unit/web tests and frontend static/build checks pass. Full end-to-end, database, payment, and visual interaction testing is incomplete because MySQL is unavailable and the browser could not be inspected through the provided environment.

## Summary

| Check | Result | Evidence |
|---|---|---|
| Backend automated tests | **PASS** | `mvn test`: 43 tests, 0 failures, 0 errors, 0 skipped |
| Frontend lint | **PASS** | `npm run lint`: 0 errors, 0 warnings after adding a project ESLint config and correcting hook dependencies |
| Frontend production build | **PASS** | `npm run build`: TypeScript and Vite build succeeded; 1,689 modules transformed |
| Frontend server startup | **PASS** | `npm run dev -- --host 127.0.0.1`: Vite reported ready at port 5173 |
| Frontend page/controls in browser | **BLOCKED** | Local HTTP access timed out even after an escalation attempt; the computer-use helper stopped because it could not determine the active browser URL safely |
| Backend live startup/API | **BLOCKED** | MySQL is not listening on `localhost:3306`; backend API port `8080` was not listening |
| Database CRUD/data integrity | **BLOCKED** | No database connection was available; no data was deleted or reset |
| Razorpay test payment | **BLOCKED** | No live backend/database/payment callback could be exercised safely |
| Responsive visual checks | **BLOCKED** | Browser UI was unavailable to the test run |

## Audited application inventory

### Frontend routes

- **Customer/public:** `/`, `/login`, `/register`, `/restaurants`, `/restaurants/:id`, `/faqs`, `/profile`, `/cart`, `/checkout`, `/orders`, `/order-confirmation/:orderId`, `/orders/:id`, `/bills/:id`, `/support`, `/wishlist`.
- **Admin:** `/admin/login`, `/admin/dashboard`, `/admin/restaurants`, `/admin/food-items`, `/admin/orders`, `/admin/customers`, `/admin/coupons`, `/admin/support`.
- **Fallback:** unknown routes redirect to `/`.

### Functional areas and testable controls

| Area | Inventory | Result |
|---|---|---|
| Authentication | Customer/admin login and registration, logout, protected routes, session persistence | **NOT TESTED** live; related existing security/unit tests passed |
| Discovery | Home, restaurant list/details, food search/category endpoints, food images | **NOT TESTED** live API/UI |
| Customer shopping | Cart add/update/remove/clear, cart count, saved wishlist, coupon apply/remove | **NOT TESTED** live API/UI |
| Checkout | Address/instructions form, price refresh, coupon, Razorpay modal, cancel/fail/success callback | **BLOCKED** live; signature flow has a code-review finding below |
| Order lifecycle | Place order, confirmation, history, detail, cancel, bill view/download | **NOT TESTED** live API/UI |
| Customer support | FAQs, ticket create/list/reply | **NOT TESTED** live API/UI |
| Admin catalog | Restaurant and food item CRUD, availability, image upload, search/filter/pagination | **NOT TESTED** live API/UI |
| Admin orders | Order list, search/filter, status changes | **NOT TESTED** live API/UI |
| Admin customers | Customer list/search/pagination, deactivate/reactivate | **NOT TESTED** live API/UI |
| Admin dashboard | Orders/revenue/customer/item metrics and recent orders | **NOT TESTED** against database totals |
| Admin promotions/support | Coupons CRUD/activation, ticket assignment/status/reply, FAQ CRUD | **NOT TESTED** live API/UI |

### Backend REST endpoint inventory

Routes below are compiled from the controller mappings. Parameter names are retained where present. Admin catalog routes declared under the `/api` base path are shown with that prefix.

| Domain | Routes |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `POST /api/auth/admin/register`; admin auth also maps `POST /api/admin/login` and `POST /api/auth/admin/login`, plus corresponding logout routes |
| Restaurants/menu | `GET /api/restaurants`, `GET /api/restaurants/{id}`, `GET /api/restaurants/{restaurantId}/items`, `GET /api/food-items/search`, `GET /api/food-items/category/{category}`, `GET /api/food-items/{id}` |
| Admin catalog | `POST /api/admin/restaurants`, `PUT /api/admin/restaurants/{id}`, `PATCH /api/admin/restaurants/{id}/active`, `DELETE /api/admin/restaurants/{id}`, `POST /api/admin/restaurants/{restaurantId}/items`, `PUT /api/admin/food-items/{id}`, `DELETE /api/admin/food-items/{id}`, `POST /api/admin/uploads/foods` |
| Customer/profile | `GET /api/customers/me`, `PUT /api/customers/me` |
| Cart | `GET /api/cart`, `POST /api/cart/add`, `POST /api/cart/merge`, `PUT /api/cart/items/{cartItemId}`, `DELETE /api/cart/items/{cartItemId}`, `DELETE /api/cart/clear` |
| Orders/bills | `POST /api/orders/place`, `GET /api/orders`, `GET /api/orders/{orderId}`, `PATCH /api/orders/{orderId}/cancel`, `PATCH /api/admin/orders/{orderId}/status`, `GET /api/bills/order/{orderId}`, `GET /api/bills/my`, `GET /api/bills/{billId}` |
| Payments | `POST /api/payments/create-order`, `POST /api/payments/verify`, `POST /api/payments/failed` |
| Coupons | `GET /api/coupons/offers`, `POST /api/coupons/validate`, `GET /api/admin/coupons`, `POST /api/admin/coupons`, `PUT /api/admin/coupons/{id}`, `PATCH /api/admin/coupons/{id}/active` |
| Wishlist | `GET /api/wishlist`, `POST /api/wishlist/items/{foodItemId}`, `DELETE /api/wishlist/items/{foodItemId}` |
| Support/FAQ | `GET /api/support/faqs`, customer ticket GET/POST/reply routes under `/api/support/tickets`, admin ticket and FAQ operations under `/api/admin/support` (list/detail/reply/status/assignment/admin list/FAQ CRUD) |
| Admin data/dashboard | `GET /api/admin/dashboard`, `GET /api/admin/restaurants`, `GET /api/admin/food-items`, `GET /api/admin/orders`, `GET /api/admin/customers`, `PATCH /api/admin/customers/{id}/active` |

### Database entities and persistence areas

Entities inspected include user/customer, restaurant/food item, cart/cart item, order/order item, bill/payment record, coupons/redemptions, wishlist items, support ticket/message/event, and FAQ entries. Repositories exist for these areas. No schema migration or live database inspection was performed because the local MySQL service was unavailable. Hibernate is configured with `ddl-auto=update`; this report did not start the backend against an unknown database or change database contents.

## Test results

### Executed

- **PASS — Backend:** `mvn test`; 43 tests passed. Coverage includes API authorization, session authentication, auth security, cart, coupons, support authorization/service, wishlist, admin dashboard, and payment signature/idempotency cases.
- **PASS — Frontend lint:** `npm run lint`; clean after adding `.eslintrc.cjs` and correcting the 10 reported React hook dependency warnings.
- **PASS — Frontend build:** `npm run build`; `tsc` and Vite production compilation succeeded.
- **PASS — Frontend server start:** Vite started successfully. A local HTTP smoke request was attempted but blocked/timed out by the environment, so page rendering is not counted as tested.
- **NOT TESTED — Buttons/forms:** Individual button and form interactions were not clicked. Source inspection found seven footer anchors with `href="#"` (About Us, Privacy Policy, Terms of Service, and four social/contact icons); these have no destination and need the project owner’s intended URLs. No destinations were invented or changed.

### Not run / blocked

- Customer and admin end-to-end workflows, including login persistence and direct unauthorized API calls.
- All REST calls against a running Spring Boot API.
- MySQL persistence, uniqueness, ownership isolation, order totals, transactional rollback, and dashboard/database reconciliation.
- Razorpay test-mode order creation, success/failure/cancel/retry callbacks, signature verification, capture checks, duplicate callbacks, UPI selection, and payment status in order history.
- Desktop/tablet/mobile layout, keyboard accessibility, image rendering, dialogs, and browser console/network inspection.
- Frontend component/E2E automation: no frontend test runner or Playwright dependency is configured in `package.json`.

## Bugs and changes

### Fixed

1. **Frontend lint command had no ESLint config.** Added `frontend/.eslintrc.cjs` using dependencies already present in the project.
2. **React effect callbacks omitted changing dependencies.** Memoized affected callbacks and updated effect dependencies in `CouponPanel`, `Checkout`, `Orders`, `Profile`, `SupportTickets`, `AdminFoodItems`, and navigation-using order/bill pages. ESLint now reports no warnings.
3. **Credentials had insecure source defaults.** Changed JWT and Razorpay values in `src/main/resources/application.properties` to read environment variables and default to blank. Database username/password are now environment-only as well; `DB_URL` retains a local development default. Provide `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `RAZORPAY_KEY_ID`, and `RAZORPAY_KEY_SECRET` before starting configured services. Rotate the previously configured Razorpay test secret in the Razorpay Dashboard; this report intentionally does not include it.

### Outstanding findings

1. **Payment verification diagnosis — FIXED in code; live outcome BLOCKED.** The service now distinguishes signature mismatch/check error, Razorpay status lookup error, authorized-but-not-captured payments, payment detail mismatches, and success. It still requires a valid signature and a captured payment matching order ID, currency, and amount before confirming the order. Two regression tests cover invalid-signature handling and repeated verification idempotency. The original demo transaction could not be inspected because this environment has no running backend/database and no Razorpay key environment variables; therefore its specific cause is still **NOT TESTED**.
2. **LOW — Footer placeholder links.** Seven anchors point to `#`, so they do not navigate to About Us, Privacy Policy, Terms of Service, or social/contact pages. The correct destinations are unknown.
3. **MEDIUM — Runtime configuration.** MySQL was not listening on port 3306. The backend cannot be exercised until a safe local/test database and credentials are available. `JWT_SECRET` and Razorpay keys now need to be provided via environment configuration.
4. **LOW — Test logging warnings.** Spring test startup reports duplicate `org.json.JSONObject` classes and Spring Security’s generated test password. Tests pass, but those warnings merit dependency/security test-configuration review.

## Files changed in this QA pass

- `frontend/.eslintrc.cjs` — added lint configuration.
- `frontend/src/components/CouponPanel.tsx`
- `frontend/src/pages/Bill.tsx`
- `frontend/src/pages/Checkout.tsx`
- `frontend/src/pages/OrderConfirmation.tsx`
- `frontend/src/pages/OrderDetails.tsx`
- `frontend/src/pages/Orders.tsx`
- `frontend/src/pages/Profile.tsx`
- `frontend/src/pages/SupportTickets.tsx`
- `frontend/src/pages/admin/AdminFoodItems.tsx`
- `frontend/src/pages/admin/AdminSupport.tsx`
- `src/main/java/com/foodieexpress/service/PaymentService.java` — separate safe outcomes for signature, provider status, capture, and payment-detail failures.
- `src/main/java/com/foodieexpress/controller/PaymentController.java` — return the specific safe verification message.
- `src/test/java/com/foodieexpress/service/PaymentServiceTest.java` — invalid signature and idempotent verification tests.
- `src/main/resources/application.properties` — credentials now externalized.
- `README.md` — local database environment setup documented.
- `FOODIE_EXPRESS_TEST_REPORT.md` — this report.

The workspace has no Git metadata, so tracked/untracked status and a Git diff baseline could not be recorded. No test database data was deleted or reset. No real payment credentials or transactions were used.

## Manual configuration needed to continue

1. Start MySQL with a dedicated local/test database; do not point QA at production data.
2. Set `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 random bytes), and Razorpay **test-mode** key ID/secret in the backend environment.
3. Rotate the previously configured Razorpay test secret.
4. Enable UPI and automatic capture in the Razorpay test account’s checkout/payment settings if required by that account.
5. Rerun API and browser workflows, particularly the captured-payment callback and the high-severity payment verification finding.

## Completion status

**Not fully verified.** Automated backend tests (43), frontend lint, and frontend build pass. The original live payment, database, customer/admin journeys, and visual/manual controls remain blocked and must not be represented as passing.
