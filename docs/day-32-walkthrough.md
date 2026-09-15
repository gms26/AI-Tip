# Day 32 Walkthrough: Smart Tip Feedback & Adaptive Recommendations

## 1. Objective

Day 32 enhances the **Context-Aware Smart Tip Assistant** by enabling the application to learn from the user's **explicit tipping decisions**.

- **Day 31 answered:** *"Based on my history and this current situation, what tip options make sense?"*
- **Day 32 answers:** *"When I receive these recommendations, what do I actually choose?"* and *"Does my repeated behavior suggest that my personalized recommendation should conservatively adapt?"*

> **Mandatory Guarantee:**
> Day 32 behavioral adaptation is deterministic and based only on explicit saved user decisions. It is not machine learning and does not provide financial advice. The system never auto-selects or auto-saves tips, never silently manipulates user choices, and keeps the user in complete control.

---

## 2. Architecture & Progression

The Day 32 workflow follows a strict, unidirectional pipeline:

```text
Day 31 Baseline Recommendation (Historical + Day 18 Optimization + Context)
                             ↓
                Ephemeral Display & Selection
                 (Clicking "Use X%" sets local calculator state only)
                             ↓
                Explicit User Decision
                 (User accepts, modifies, or enters custom tip)
                             ↓
                Explicit Save Action
                 (POST /api/tips commits tip to history)
                             ↓
                Feedback Persistence
                 (POST /api/smart-tip/feedback records decision)
                             ↓
                Deterministic Behavioral Analysis
                 (SmartTipAdaptationService computes counts & differences)
                             ↓
                Bounded Adaptation (±3.00 pp)
                 (Overlaid onto future primary recommendations)
```

---

## 3. Database Migration

### Migration: `V13__create_tip_recommendation_feedback.sql`
Creates the dedicated `tip_recommendation_feedback` table separating recommendation interactions from tip history:

```sql
CREATE TABLE tip_recommendation_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    restaurant_name VARCHAR(255),
    service_quality VARCHAR(30),
    bill_amount NUMERIC(10, 2) NOT NULL,
    suggested_tip_percentage NUMERIC(5, 2),
    chosen_tip_percentage NUMERIC(5, 2) NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    recommendation_type VARCHAR(50),
    difference_percentage_points NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tip_rec_feedback_user_curr_date
ON tip_recommendation_feedback (user_id, currency, created_at);

CREATE INDEX idx_tip_rec_feedback_user_curr_type
ON tip_recommendation_feedback (user_id, currency, feedback_type);
```

---

## 4. Feedback Model & Classification Rules

The backend deterministically classifies each recorded decision using exact `BigDecimal` arithmetic:

| Classification | Condition | Difference Points |
| :--- | :--- | :--- |
| **`ACCEPTED`** | `suggested != null` AND `chosen == suggested` | `0.00` |
| **`MODIFIED`** | `suggested != null` AND `chosen != suggested` | `chosen - suggested` (signed) |
| **`CUSTOM`** | `suggested == null` | `null` |

---

## 5. Behavioral Analytics & Adaptation Algorithm

Implemented in `SmartTipAdaptationService`:

1. **Evidence Threshold**: Requires at least **5 recommendation decisions** (`recommendationDecisionCount >= 5`) before declaring a preference direction. If $< 5$, direction is `INSUFFICIENT_DATA` and no adaptation is applied.
2. **Adaptation Trigger**: Requires $|averageDifference| \ge 2.00$ percentage points.
   - If `averageDifference >= +2.00 pp`: `PREFERS_HIGHER`, `adaptationApplied = true`
   - If `averageDifference <= -2.00 pp`: `PREFERS_LOWER`, `adaptationApplied = true`
   - If between `-2.00` and `+2.00 pp`: `ALIGNED`, `adaptationApplied = false`
3. **Bounded Personalization**: The adjustment is strictly clamped to **$\pm 3.00$ percentage points**:
   $$\text{adjustment} = \min(+3.00, \max(-3.00, \text{averageDifference}))$$
4. **Baseline Preservation**: Day 31 suggestions remain the baseline. `baselinePrimarySuggestion` is always preserved in `SmartTipResponse`, allowing side-by-side comparison with `primarySuggestion`.
5. **Clamping**: Final tip percentages are clamped to $[0.00, 100.00]\%$.

---

## 6. User and Currency Isolation

- **User Isolation**: All queries filter strictly by the authenticated user (`Authentication.getName() -> User`). User A cannot access or influence User B's feedback.
- **Currency Isolation**: Feedback for `USD`, `EUR`, `INR`, and `GBP` are completely isolated. EUR tipping habits never contaminate USD recommendations.

---

## 7. REST API Endpoints

### 1. Record Feedback
```http
POST /api/smart-tip/feedback
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "currency": "USD",
  "billAmount": 50.00,
  "restaurantName": "Luigi Trattoria",
  "serviceQuality": "GOOD",
  "suggestedTipPercentage": 15.00,
  "suggestedRecommendationType": "OPTIMIZED",
  "chosenTipPercentage": 18.00
}
```
**Response:**
```json
{
  "feedbackType": "MODIFIED",
  "suggestedTipPercentage": 15.00,
  "chosenTipPercentage": 18.00,
  "differencePercentagePoints": 3.00
}
```

### 2. Behavioral Feedback Summary
```http
GET /api/smart-tip/feedback?currency=USD
Authorization: Bearer <JWT>
```
**Response:**
```json
{
  "currency": "USD",
  "feedbackCount": 7,
  "acceptedCount": 1,
  "modifiedCount": 5,
  "customCount": 1,
  "averageDifferencePercentagePoints": 2.83,
  "direction": "PREFERS_HIGHER",
  "adaptationApplied": true,
  "adaptationAdjustment": 2.83
}
```

---

## 8. Frontend Integration

1. **Local State**: Tracks `smartTipApplied`, `smartTipOriginalPercentage`, and `smartTipOriginalType`.
2. **Advisory Click**: Clicking `Use X%` sets calculator inputs only. No network request, no auto-save.
3. **Explicit Save Flow**: When `Save Tip` succeeds, `smartTipApi.submitFeedback(...)` is dispatched.
4. **Adaptive UI**: When `adaptationApplied` is true, a subtle chip `Personalized (+2.8 pp)` and explainability message are displayed inside the Smart Tip card.

---

## 9. Verification & Evidence

### A. Automated Backend Regression Suite
- **Baseline (Day 31):** 631 tests
- **Day 32 Added Tests:** 45 tests
  - `SmartTipFeedbackServiceTest`: 16 tests
  - `SmartTipAdaptationServiceTest`: 14 tests
  - `SmartTipFeedbackControllerIntegrationTest`: 11 tests
  - `SmartTipServiceTest`: +4 tests
- **Total Tests:** **676 tests**
- **Failures:** **0**
- **Errors:** **0**
- **Result:** `BUILD SUCCESS`

### B. Live API Verification (`scratch/verify_smart_tip_feedback.ps1`)
All 14 scenarios passed cleanly:
1. `PASS`: New user has 0 feedback records and `INSUFFICIENT_DATA`.
2. `PASS`: Accepted recommendation recorded with diff = 0.00 pp.
3. `PASS`: Modified recommendation recorded with diff = +3.00 pp.
4. `PASS`: Custom tip recorded without recommendation difference.
5. `PASS`: Accumulated counts: total=3, accepted=1, modified=1, custom=1. Direction remains `INSUFFICIENT_DATA`.
6. `PASS`: Created 4 additional higher decisions (+3.50 pp).
7. `PASS`: User preference detected as `PREFERS_HIGHER` (avg diff: 2.83 pp).
8. `PASS`: Bounded adaptation adjustment (2.83 pp) within [2.00, 3.00].
9. `PASS`: EUR feedback successfully recorded.
10. `PASS`: Currency isolation verified: EUR = 1, USD = 7.
11. `PASS`: User isolation verified: User B cannot see User A's feedback.
12. `PASS`: POST /api/smart-tip returns baseline primary (15.00%) and adapted primary (17.83%).
13. `PASS`: Zero auto-save verified: clicking/viewing suggestions does not alter feedback count.
14. `PASS`: Explicit save created exactly one feedback record (7 -> 8).

### C. Frontend Production Build
- `npm run build` -> `✓ built in 3.74s` (0 errors).

---

## 10. Storage & Workspace Hygiene

All unnecessary temporary files and caches were cleaned:
- Deleted 22 JVM crash/replay logs (`hs_err_pid*.log`, `replay_pid*.log`) in `backend/` (~4.5 MB).
- Deleted 24 test `.dumpstream` files in `backend/target/surefire-reports/`.
- Deleted production build artifacts in `frontend/dist/`.
- Deleted pre-bundled cache in `frontend/node_modules/.vite/` (~27.5 MB).
- Deleted 18 temporary browser captures in `.tempmediaStorage/` (~4.4 MB).
- Deleted 5 large video recordings (`.webp`) in artifact storage (~31.0 MB).
- **Total storage cleared:** **~69 MB**.

---

## 11. Known Limitations & Non-Goals

1. **Non-Goal: Machine Learning / Predictive Models**: The adaptation uses bounded, explainable statistical formulas.
2. **Non-Goal: Automatic Tip Application**: The system never pre-selects or auto-commits tips.
3. **Threshold Boundary**: Users with fewer than 5 explicit decisions receive pure Day 31 context-aware recommendations without adaptation.
