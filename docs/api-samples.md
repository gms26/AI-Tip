# AI Tip Assistant — API Reference (Day 1)

## Base URL

```
http://localhost:8080
```

---

## 1. Health Check

**Public** — No authentication required.

```bash
curl http://localhost:8080/health
```

**Expected Response** (200 OK):
```json
{
  "status": "UP",
  "application": "AI Tip Assistant",
  "timestamp": "2025-01-15T10:30:00.123456"
}
```

---

## 2. Register User

**Public** — Creates a new account and returns JWT.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Expected Response** (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA1MzEyMjAwLCJleHAiOjE3MDUzOTg2MDB9.xxxxx",
  "tokenType": "Bearer",
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Validation Error** (400 Bad Request):
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "not-an-email",
    "password": "short"
  }'
```

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "email": "Email must be a valid email address",
    "password": "Password must be between 8 and 100 characters"
  },
  "timestamp": "2025-01-15T10:30:00.123456",
  "path": "/api/auth/register"
}
```

**Duplicate Email** (409 Conflict):
```json
{
  "status": 409,
  "message": "Email 'john@example.com' is already registered",
  "timestamp": "2025-01-15T10:30:00.123456",
  "path": "/api/auth/register"
}
```

---

## 3. Login User

**Public** — Authenticates and returns JWT.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Expected Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Bad Credentials** (401 Unauthorized):
```json
{
  "status": 401,
  "message": "Invalid email or password",
  "timestamp": "2025-01-15T10:30:00.123456",
  "path": "/api/auth/login"
}
```

---

## 4. Get Current User (Protected)

**Requires JWT** — Returns authenticated user's profile.

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Expected Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2025-01-15T10:30:00.123456"
}
```

**No Token / Expired** (401/403):
```
HTTP 401 Unauthorized
```

---

## 5. Get AI Tip Suggestion (Protected)

**Requires JWT** — Returns a smart tip recommendation based on context.

```bash
curl -X POST http://localhost:8080/api/ai/suggest \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "billAmount": 85.00,
    "restaurantType": "FINE_DINING",
    "serviceQuality": "EXCELLENT",
    "country": "USA",
    "currency": "USD",
    "occasion": "DINNER"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recommendedPercentage": 20.00,
  "minimumPercentage": 18.00,
  "maximumPercentage": 22.00,
  "tipAmount": 17.00,
  "totalAmount": 102.00,
  "reason": "Excellent service at a fine dining restaurant generally supports a tip near the upper end of the customary range.",
  "confidence": "HIGH",
  "personalizationSource": "RESTAURANT_AND_SERVICE"
}
```

**Validation Error** (400 Bad Request):
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "billAmount": "Bill amount must be greater than 0",
    "serviceQuality": "Service quality is required"
  },
  "timestamp": "2025-01-15T10:30:00.123456",
  "path": "/api/ai/suggest"
}
```

**AI Failure / Malformed Gemini Output** (503 Service Unavailable):
```json
{
  "status": 503,
  "message": "AI recommendation is temporarily unavailable. You can still calculate your tip manually.",
  "timestamp": "2025-01-15T10:30:00.123456",
  "path": "/api/ai/suggest"
}
```

---

## 6. Get Personalization Summary (Protected)

**Requires JWT** — Returns the authenticated user's overall tipping patterns based on actual database history.

```bash
curl http://localhost:8080/api/personalization/summary \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Expected Response** (200 OK):
```json
{
  "tipCount": 24,
  "averageTipPercentage": 18.47,
  "medianTipPercentage": 18.50,
  "minimumTipPercentage": 15.00,
  "maximumTipPercentage": 22.00,
  "averageTipAmount": 8.50,
  "personalizedPercentage": 18.5,
  "message": "You typically tip around 18.5% (range: 15.00%–22.00%, based on 24 tips).",
  "restaurantInsight": null
}
```

**Empty History Response** (200 OK):
```json
{
  "tipCount": 0,
  "message": "No tipping history yet."
}
```

---

## 7. Get Restaurant Personalization (Protected)

**Requires JWT** — Returns the user's overall tipping patterns plus specific insights for a named restaurant.

```bash
curl "http://localhost:8080/api/personalization/restaurant?name=Italian%20Place" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Expected Response** (200 OK):
```json
{
  "tipCount": 24,
  "averageTipPercentage": 18.47,
  "medianTipPercentage": 18.50,
  "minimumTipPercentage": 15.00,
  "maximumTipPercentage": 22.00,
  "averageTipAmount": 8.50,
  "personalizedPercentage": 18.5,
  "message": "You typically tip around 18.5% (range: 15.00%–22.00%, based on 24 tips).",
  "restaurantInsight": {
    "restaurantName": "Italian Place",
    "visitCount": 3,
    "averageTipPercentage": 20.00,
    "medianTipPercentage": 20.00,
    "lastTipPercentage": 20.00,
    "lastTipAmount": 10.00,
    "lastVisitDate": "2025-06-15T18:30:00"
  }
}
```

---

---

## 8. Currency Conversion (Protected)

**Requires JWT** — Fetches the exchange rate between two currencies. Based on current ECB reference rates via the Frankfurter API.

```bash
curl "http://localhost:8080/api/currency/rate?from=USD&to=INR" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Expected Response** (200 OK):
```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "INR",
  "originalAmount": null,
  "convertedAmount": null,
  "exchangeRate": 83.35,
  "rateTimestamp": "2025-01-15T12:00:00.123456",
  "provider": "Frankfurter (ECB Reference)"
}
```

---

## 9. Convert Amount (Protected)

**Requires JWT** — Converts a specific monetary amount. Handles correct decimal rounding dynamically based on the target currency (e.g., JPY uses 0 decimals, USD uses 2).

```bash
curl -X POST http://localhost:8080/api/currency/convert \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 85.00,
    "sourceCurrency": "USD",
    "targetCurrency": "INR"
  }'
```

**Expected Response** (200 OK):
```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "INR",
  "originalAmount": 85.00,
  "convertedAmount": 7084.75,
  "exchangeRate": 83.35,
  "rateTimestamp": "2025-01-15T12:00:00.123456",
  "provider": "Frankfurter (ECB Reference)"
}
```

**Unsupported Currency** (400 Bad Request):
```json
{
  "status": 400,
  "message": "Unsupported or invalid currency code: XYZ",
  "timestamp": "2025-01-15T12:00:00.123456",
  "path": "/api/currency/convert"
}
```

**Provider Failure** (503 Service Unavailable):
```json
{
  "status": 503,
  "message": "Currency conversion is temporarily unavailable.",
  "timestamp": "2025-01-15T12:00:00.123456",
  "path": "/api/currency/convert"
}
```

---

## Quick Test Workflow

```bash
# 1. Health check
curl http://localhost:8080/health

# 2. Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane","email":"jane@test.com","password":"securepass123"}' \
  | python -c "import sys, json; print(json.load(sys.stdin)['token'])")

echo "JWT: $TOKEN"

# 3. Access protected endpoint
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# 4. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@test.com","password":"securepass123"}'
```

---

## Git Commits (Suggested)

```bash
git init
git add .
git commit -m "feat(project): initialize Spring Boot + React project structure"

# After backend completion
git commit -m "feat(auth): implement user registration with JWT"
git commit -m "feat(auth): implement user login with JWT"
git commit -m "feat(security): add JWT filter and protected endpoints"
git commit -m "feat(db): add Flyway migration for users table"

# After frontend completion
git commit -m "feat(frontend): create React project with MUI theme"
git commit -m "feat(frontend): implement login and register pages"
git commit -m "feat(frontend): add dashboard with auth context"
git commit -m "feat(frontend): add protected routes and 404 page"

# Final
git commit -m "docs: add Postman collection and API documentation"
```

---

## Day 6 — Service Quality Logging

### Service Quality Enum

Valid values for `serviceQuality` are exactly:

| Value | Display | Stars |
|---|---|---|
| `POOR` | Poor | ★ |
| `AVERAGE` | Average | ★★ |
| `GOOD` | Good | ★★★ |
| `EXCELLENT` | Excellent | ★★★★ |

Any other string (e.g. `"SUPERB"`, `"OK"`) returns **400 Bad Request**.

---

### Save a New Tip with Service Quality

**POST** `/api/tips` — Requires JWT.

`serviceQuality` is **required** for all new tips created after Day 6.

```bash
curl -X POST http://localhost:8080/api/tips \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "billAmount": 85.00,
    "tipPercentage": 20.00,
    "restaurantName": "Italian Place",
    "currency": "USD",
    "serviceQuality": "EXCELLENT"
  }'
```

**Expected Response** (201 Created):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "restaurantName": "Italian Place",
  "billAmount": 85.00,
  "tipPercentage": 20.00,
  "tipAmount": 17.00,
  "totalAmount": 102.00,
  "currency": "USD",
  "createdAt": "2025-08-11T10:30:00",
  "serviceQuality": "EXCELLENT"
}
```

**Historical tips** (created before Day 6) return `"serviceQuality": null`. This is expected — not an error.

---

### Update Service Quality

**PATCH** `/api/tips/{id}/service-quality` — Requires JWT. User must own the tip.

```bash
curl -X PATCH http://localhost:8080/api/tips/3fa85f64-5717-4562-b3fc-2c963f66afa6/service-quality \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceQuality": "GOOD"
  }'
```

**Expected Response** (200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "restaurantName": "Italian Place",
  "billAmount": 85.00,
  "tipPercentage": 20.00,
  "tipAmount": 17.00,
  "totalAmount": 102.00,
  "currency": "USD",
  "createdAt": "2025-08-11T10:30:00",
  "serviceQuality": "GOOD"
}
```

**Invalid value** (400 Bad Request):
```bash
-d '{ "serviceQuality": "SUPERB" }'
```

**Access denied** (404 Not Found — no enumeration leak):
```bash
# User B attempting to PATCH User A's tip:
# → 404 Not Found (same as if tip doesn't exist)
```

---

### Service Quality Summary Statistics

**GET** `/api/service-quality/summary` — Requires JWT. Statistics are scoped exclusively to the authenticated user.

```bash
curl http://localhost:8080/api/service-quality/summary \
  -H "Authorization: Bearer <token>"
```

**Expected Response** (200 OK) — with rated tips:
```json
{
  "totalRatedTips": 10,
  "poorCount": 1,
  "averageCount": 2,
  "goodCount": 4,
  "excellentCount": 3,
  "mostCommon": "GOOD",
  "averageTipPercentageByQuality": {
    "POOR": 10.00,
    "AVERAGE": 14.50,
    "GOOD": 17.25,
    "EXCELLENT": 20.00
  }
}
```

**Empty state** (no rated tips — not an error):
```json
{
  "totalRatedTips": 0,
  "poorCount": 0,
  "averageCount": 0,
  "goodCount": 0,
  "excellentCount": 0,
  "mostCommon": null,
  "averageTipPercentageByQuality": {}
}
```

**Security notes:**
- `userId` is NEVER accepted from the client. It is resolved from the JWT token.
- Historical NULL tips are excluded from all statistics (treated as NOT_RATED).
- Qualities with no rated tips are absent from `averageTipPercentageByQuality`.

**Statistics algorithm — deterministic rules:**
- `mostCommon`: the quality with the highest count. On a tie, the higher quality wins (POOR < AVERAGE < GOOD < EXCELLENT). Example: GOOD=2, EXCELLENT=2 → `mostCommon = "EXCELLENT"`.
- All averages use `BigDecimal.divide(count, 2, RoundingMode.HALF_UP)`. No floating point.

---

### Git Commit Suggestions (Day 6)

```bash
git commit -m "feat(db): V4 add service_quality to tips table"
git commit -m "feat(db): V5 add composite index on user_id, service_quality"
git commit -m "feat(entity): add serviceQuality field with EnumType.STRING"
git commit -m "feat(dto): add serviceQuality to CreateTipRequest and TipResponse"
git commit -m "feat(service): add updateServiceQuality to TipService"
git commit -m "feat(service): add ServiceQualityService with deterministic stats"
git commit -m "feat(api): PATCH /api/tips/{id}/service-quality endpoint"
git commit -m "feat(api): GET /api/service-quality/summary endpoint"
git commit -m "test: ServiceQualityServiceTest (11 cases)"
git commit -m "test: ServiceQualityControllerIntegrationTest"
git commit -m "feat(frontend): service quality selector, history badge, stats card"
git commit -m "docs: update api-samples.md and postman-collection for Day 6"
```

---

## Day 8 — Generosity Score

### Get Generosity Score

**GET** `/api/generosity/score` — Requires JWT. Statistics are scoped exclusively to the authenticated user.

```bash
curl http://localhost:8080/api/generosity/score \
  -H "Authorization: Bearer <token>"
```

**Expected Response** (200 OK) — with history:
```json
{
  "score": 78,
  "category": "GENEROUS",
  "medianTipPercentage": 19.50,
  "meanTipPercentage": 19.33,
  "totalTips": 12,
  "ratedTips": 12,
  "confidence": "HIGH",
  "message": "Your tipping pattern is generally generous."
}
```

**Empty state** (no tips — not an error):
```json
{
  "score": null,
  "category": null,
  "medianTipPercentage": null,
  "meanTipPercentage": null,
  "totalTips": 0,
  "ratedTips": 0,
  "confidence": "LOW",
  "message": "Not enough tipping history to calculate a score."
}
```

**Score Details:**
- **Formula:** `medianTipPercentage * 4` clamped to [0, 100].
- **Median:** Used instead of mean to resist outlier bias.
- **Confidence thresholds:** 0-1 tips (LOW), 2-4 tips (MEDIUM), 5+ tips (HIGH).
- **Categories:** CONSERVATIVE (0-39), MODERATE (40-59), GENEROUS (60-79), VERY_GENEROUS (80-100).
- **User isolation:** Exclusively uses `Authentication.getName()` from JWT, guaranteeing isolation.
