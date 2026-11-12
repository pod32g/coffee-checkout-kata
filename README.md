# Coffee Checkout: a refactoring kata

**Little Bean** delivers coffee. This small Spring Boot service prices a delivery order: it applies
discounts and promo codes, works out the delivery fee and tax, and awards loyalty points.

It works, and it's tested. But it grew in a hurry and it's hard to change. Your job is to clean it
up, then add two features.

## Getting started

You need Java 17 or newer installed. The Gradle wrapper downloads everything else, including
Java 21 for the build if you don't have it.

```sh
./gradlew test       # should end with: 22 tests: 22 passed, 0 failed, 0 skipped
./gradlew bootRun    # starts the API on http://localhost:8080
```

To work in an IDE, open the folder in IntelliJ IDEA; it imports as a Gradle project.

Try an order while the app runs:

```sh
curl -s localhost:8080/api/checkout -H 'Content-Type: application/json' -d '{
  "customerId": "c-gold", "distanceKm": 4, "deliveryTime": "17:45", "promoCode": "SAVE5",
  "items": [
    {"name": "Latte",    "category": "DRINK", "price": 4.50, "quantity": 3},
    {"name": "Sandwich", "category": "FOOD",  "price": 8.50, "quantity": 3}
  ]}'
```

```json
{"subtotal":39.0,"discount":6.95,"deliveryFee":4.49,"tax":2.56,"total":39.1,
 "loyaltyPointsEarned":78,"messages":["Peak time delivery surcharge applied"]}
```

`GET /api/customers/{id}` shows a customer's points. The app starts with four customers:
`c-regular`, `c-gold`, `c-platinum`, and `c-new` (who hasn't ordered yet).

## The rules

This is what the code does today. The tests check every rule.

- **Subtotal:** price × quantity, added up over the items.
- **Tier discount:** GOLD customers get 5% off drinks and food (not merch). PLATINUM customers get
  10% off everything. REGULAR customers get no tier discount.
- **Promo codes** (one per order; case and spaces don't matter):
  - `WELCOME10`: 10% off what's left after the tier discount, on a customer's first order only.
  - `SAVE5`: $5 off when the subtotal is $30 or more.
  - `FREEDELIVERY`: no delivery fee.
  - Any other code is ignored, and the response says so in `messages`.
- **Delivery fee:** $2.99, plus $0.50 for every started kilometre beyond 3 km, plus $1.00 between
  17:00 and 19:00. Delivery is free for PLATINUM customers, with `FREEDELIVERY`, or when the order
  after discounts is $40 or more. Addresses beyond 15 km are refused.
- **Tax:** 8% of the order after discounts. Delivery isn't taxed.
- **Loyalty points:** 1 point per whole dollar of the total. GOLD customers earn double and
  PLATINUM customers triple. Every checkout also counts as an order for the customer.
- All amounts are rounded to cents.

## Your task

1. **Run the tests.** All 22 pass. They only talk to the HTTP API, so you can change anything
   inside the app and they will still tell you whether the behaviour stayed the same.
2. **Refactor.** Make the code easy to read and easy to change. Take small steps, run the tests
   after each one, and commit often. Don't change behaviour: if you think a rule is wrong, write it
   down instead of fixing it.
3. **Add two features**, writing a test for each first:
   - A new **DIAMOND** tier: 15% off everything, free delivery, and 4× loyalty points.
   - A **`POST /api/quote`** endpoint that takes the same request as checkout and returns the same
     price breakdown, but doesn't award points or count an order.

Plan on about an hour and a half. If you run short of time, a well-refactored service matters more
than the features.

## Project layout

Everything is in `src/main/java/com/example/checkout`:

| File | What it is |
| --- | --- |
| `CheckoutController` | The HTTP endpoints. |
| `CheckoutService` | The pricing logic. Most of the work is here. |
| `CustomerRepository` | Customers, kept in memory. |
| `Customer`, `CheckoutRequest`, `OrderItem`, `CheckoutResult` | The data classes. |

The tests are in `src/test/java/com/example/checkout/CheckoutApiTest.java`.
