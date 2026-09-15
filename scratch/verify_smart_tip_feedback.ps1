$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

Write-Host "=== DAY 32: LIVE API VERIFICATION ===" -ForegroundColor Cyan

# Setup unique test users
$rand = Get-Random -Minimum 10000 -Maximum 99999
$emailA = "day32_userA_$rand@example.com"
$emailB = "day32_userB_$rand@example.com"
$password = "Secret123!"

function Get-Token($email, $pwd) {
    try {
        $regBody = @{ name = "Day 32 User"; email = $email; password = $pwd } | ConvertTo-Json
        Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null
    } catch {
        # Already registered
    }
    $loginBody = @{ email = $email; password = $pwd } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    return $res.token
}

$tokenA = Get-Token $emailA $password
$tokenB = Get-Token $emailB $password
$headersA = @{ Authorization = "Bearer $tokenA" }
$headersB = @{ Authorization = "Bearer $tokenB" }

# Scenario 1: New user has no feedback
Write-Host "[1/14] Verify New User Has No Feedback" -ForegroundColor Yellow
$summary1 = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA
if ($summary1.feedbackCount -eq 0 -and $summary1.direction -eq "INSUFFICIENT_DATA" -and $summary1.adaptationApplied -eq $false) {
    Write-Host "PASS: New user has 0 feedback records and INSUFFICIENT_DATA direction." -ForegroundColor Green
} else {
    Write-Host "FAIL: Unexpected initial summary: $($summary1 | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 2: Save an accepted recommendation
Write-Host "[2/14] Save an Accepted Recommendation" -ForegroundColor Yellow
$reqAccepted = @{
    currency = "USD"
    billAmount = 50.00
    restaurantName = "Trattoria Romana"
    serviceQuality = "GOOD"
    suggestedTipPercentage = 15.00
    suggestedRecommendationType = "OPTIMIZED"
    chosenTipPercentage = 15.00
} | ConvertTo-Json
$resAccepted = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $reqAccepted -ContentType "application/json"
if ($resAccepted.feedbackType -eq "ACCEPTED" -and $resAccepted.differencePercentagePoints -eq 0.00) {
    Write-Host "PASS: Accepted recommendation recorded with diff = 0.00 pp." -ForegroundColor Green
} else {
    Write-Host "FAIL: Unexpected accepted response: $($resAccepted | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 3: Save a modified recommendation
Write-Host "[3/14] Save a Modified Recommendation (+3.00 pp)" -ForegroundColor Yellow
$reqModified = @{
    currency = "USD"
    billAmount = 60.00
    restaurantName = "Bistro Modern"
    serviceQuality = "EXCELLENT"
    suggestedTipPercentage = 15.00
    suggestedRecommendationType = "OPTIMIZED"
    chosenTipPercentage = 18.00
} | ConvertTo-Json
$resModified = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $reqModified -ContentType "application/json"
if ($resModified.feedbackType -eq "MODIFIED" -and $resModified.differencePercentagePoints -eq 3.00) {
    Write-Host "PASS: Modified recommendation recorded with diff = +3.00 pp." -ForegroundColor Green
} else {
    Write-Host "FAIL: Unexpected modified response: $($resModified | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 4: Save a custom tip
Write-Host "[4/14] Save a Custom Tip (no suggestion applied)" -ForegroundColor Yellow
$reqCustom = @{
    currency = "USD"
    billAmount = 40.00
    restaurantName = "Quick Diner"
    serviceQuality = "GOOD"
    suggestedTipPercentage = $null
    suggestedRecommendationType = $null
    chosenTipPercentage = 20.00
} | ConvertTo-Json
$resCustom = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $reqCustom -ContentType "application/json"
if ($resCustom.feedbackType -eq "CUSTOM" -and $resCustom.differencePercentagePoints -eq $null) {
    Write-Host "PASS: Custom tip recorded without recommendation difference." -ForegroundColor Green
} else {
    Write-Host "FAIL: Unexpected custom response: $($resCustom | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 5: Verify feedback counts
Write-Host "[5/14] Verify Accumulated Feedback Counts" -ForegroundColor Yellow
$summary5 = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA
if ($summary5.feedbackCount -eq 3 -and $summary5.acceptedCount -eq 1 -and $summary5.modifiedCount -eq 1 -and $summary5.customCount -eq 1 -and $summary5.direction -eq "INSUFFICIENT_DATA") {
    Write-Host "PASS: Feedback counts: total=3, accepted=1, modified=1, custom=1. Direction remains INSUFFICIENT_DATA (< 5 records)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Feedback count mismatch: $($summary5 | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 6: Create five or more higher-than-baseline decisions
Write-Host "[6/14] Record Additional Higher Decisions to Reach Minimum Evidence" -ForegroundColor Yellow
for ($i = 1; $i -le 4; $i++) {
    $req = @{
        currency = "USD"
        billAmount = 50.00 + ($i * 10)
        restaurantName = "Restaurant $i"
        serviceQuality = "GOOD"
        suggestedTipPercentage = 15.00
        suggestedRecommendationType = "OPTIMIZED"
        chosenTipPercentage = 18.50
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $req -ContentType "application/json" | Out-Null
}
Write-Host "PASS: Created 4 additional decisions with +3.50 pp difference." -ForegroundColor Green

# Scenario 7: Verify PREFERS_HIGHER
Write-Host "[7/14] Verify Direction Detects PREFERS_HIGHER" -ForegroundColor Yellow
$summary7 = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA
if ($summary7.direction -eq "PREFERS_HIGHER" -and $summary7.adaptationApplied -eq $true) {
    Write-Host "PASS: User preference detected as PREFERS_HIGHER (avg diff: $($summary7.averageDifferencePercentagePoints) pp)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected PREFERS_HIGHER, got: $($summary7 | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 8: Verify bounded adaptation (maximum +3.00 pp)
Write-Host "[8/14] Verify Bounded Adaptation Clamped to <= +3.00 pp" -ForegroundColor Yellow
if ($summary7.adaptationAdjustment -le 3.00 -and $summary7.adaptationAdjustment -ge 2.00) {
    Write-Host "PASS: Adaptation adjustment ($($summary7.adaptationAdjustment) pp) is strictly within bounded threshold [2.00, 3.00]." -ForegroundColor Green
} else {
    Write-Host "FAIL: Adaptation adjustment exceeded bounds: $($summary7.adaptationAdjustment)" -ForegroundColor Red
    exit 1
}

# Scenario 9: Create EUR behavior
Write-Host "[9/14] Record Feedback for EUR" -ForegroundColor Yellow
$reqEur = @{
    currency = "EUR"
    billAmount = 45.00
    restaurantName = "Cafe de Paris"
    serviceQuality = "GOOD"
    suggestedTipPercentage = 10.00
    suggestedRecommendationType = "OPTIMIZED"
    chosenTipPercentage = 10.00
} | ConvertTo-Json
$resEur = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $reqEur -ContentType "application/json"
if ($resEur.feedbackType -eq "ACCEPTED") {
    Write-Host "PASS: EUR feedback successfully recorded." -ForegroundColor Green
} else {
    Write-Host "FAIL: EUR feedback failed." -ForegroundColor Red
    exit 1
}

# Scenario 10: Verify EUR does not affect USD
Write-Host "[10/14] Verify Currency Isolation (EUR != USD)" -ForegroundColor Yellow
$summaryEur = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=EUR" -Method Get -Headers $headersA
$summaryUsd = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA
if ($summaryEur.feedbackCount -eq 1 -and $summaryEur.acceptedCount -eq 1 -and $summaryUsd.feedbackCount -eq 7) {
    Write-Host "PASS: Currency isolation verified: EUR feedback count = 1, USD feedback count = 7." -ForegroundColor Green
} else {
    Write-Host "FAIL: Currency contamination detected! EUR: $($summaryEur.feedbackCount), USD: $($summaryUsd.feedbackCount)" -ForegroundColor Red
    exit 1
}

# Scenario 11: Verify User B cannot see User A's feedback
Write-Host "[11/14] Verify User Isolation (User B cannot see User A's feedback)" -ForegroundColor Yellow
$summaryB = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersB
if ($summaryB.feedbackCount -eq 0 -and $summaryB.direction -eq "INSUFFICIENT_DATA") {
    Write-Host "PASS: User isolation verified: User B has 0 feedback records." -ForegroundColor Green
} else {
    Write-Host "FAIL: User isolation breached! User B saw: $($summaryB | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 12: Verify ordinary Smart Tip calculation includes Day 32 adaptation
Write-Host "[12/14] Verify POST /api/smart-tip Returns Adapted Primary & Baseline Suggestion" -ForegroundColor Yellow
$calcReq = @{
    currency = "USD"
    billAmount = 100.00
} | ConvertTo-Json
$calcRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $calcReq -ContentType "application/json"
if ($calcRes.adaptationApplied -eq $true -and $calcRes.baselinePrimarySuggestion -ne $null -and $calcRes.primarySuggestion -ne $null) {
    Write-Host "PASS: Smart Tip calculation returns baseline primary ($($calcRes.baselinePrimarySuggestion.tipPercentage)%) and adapted primary ($($calcRes.primarySuggestion.tipPercentage)%)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Smart Tip response missing adaptation fields: $($calcRes | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# Scenario 13: Verify suggestion click without saving does not create feedback
Write-Host "[13/14] Verify No Auto-Save: Suggestion Click without Save Tip does NOT create feedback" -ForegroundColor Yellow
$initialCount = (Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA).feedbackCount
# Simulate client querying smart-tip suggestion (what happens when clicking Use X% or viewing suggestions)
Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $calcReq -ContentType "application/json" | Out-Null
$afterCount = (Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA).feedbackCount
if ($initialCount -eq $afterCount) {
    Write-Host "PASS: Zero auto-save verified: viewing/requesting suggestions does not alter feedback count ($afterCount)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Feedback count changed without save! Before: $initialCount, After: $afterCount" -ForegroundColor Red
    exit 1
}

# Scenario 14: Verify explicit save creates exactly one feedback record
Write-Host "[14/14] Verify Explicit Save Flow Creates Exactly One Feedback Record" -ForegroundColor Yellow
$beforeSaveCount = (Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA).feedbackCount
# Simulate user explicitly saving tip and submitting feedback
$explicitTip = @{
    billAmount = 85.00
    tipPercentage = 18.00
    restaurantName = "Corner Bakery"
    currency = "USD"
    serviceQuality = "GOOD"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/tips" -Method Post -Headers $headersA -Body $explicitTip -ContentType "application/json" | Out-Null

$explicitFeedback = @{
    currency = "USD"
    billAmount = 85.00
    restaurantName = "Corner Bakery"
    serviceQuality = "GOOD"
    suggestedTipPercentage = 15.00
    suggestedRecommendationType = "OPTIMIZED"
    chosenTipPercentage = 18.00
} | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback" -Method Post -Headers $headersA -Body $explicitFeedback -ContentType "application/json" | Out-Null

$afterSaveCount = (Invoke-RestMethod -Uri "$baseUrl/api/smart-tip/feedback?currency=USD" -Method Get -Headers $headersA).feedbackCount
if ($afterSaveCount -eq ($beforeSaveCount + 1)) {
    Write-Host "PASS: Explicit save created exactly one feedback record (Count: $beforeSaveCount -> $afterSaveCount)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected feedback count $($beforeSaveCount + 1), got $afterSaveCount" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== ALL 14 DAY 32 LIVE SCENARIOS PASSED SUCCESSFULLY ===" -ForegroundColor Green
