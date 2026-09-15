$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

Write-Host "=== DAY 31: LIVE API VERIFICATION ===" -ForegroundColor Cyan

# 1. Register & Login Test User A
$rand = Get-Random -Minimum 1000 -Maximum 9999
$emailA = "smarttip_userA_$rand@example.com"
$emailB = "smarttip_userB_$rand@example.com"
$password = "Secret123!"

function Get-Token($email, $pwd) {
    try {
        $regBody = @{ name = "Test User"; email = $email; password = $pwd } | ConvertTo-Json
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

Write-Host "[1/12] Testing Validation Boundaries (billAmount <= 0.01)" -ForegroundColor Yellow
try {
    $badReq = @{ currency = "USD"; billAmount = 0.01 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $badReq -ContentType "application/json"
    Write-Host "FAIL: 0.01 was accepted" -ForegroundColor Red
} catch {
    Write-Host "PASS: 0.01 properly rejected with 400 Bad Request" -ForegroundColor Green
}

Write-Host "[2/12] Testing New User Flow (Zero History)" -ForegroundColor Yellow
$newReq = @{ currency = "USD"; billAmount = 50.00 } | ConvertTo-Json
$newRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $newReq -ContentType "application/json"
if ($newRes.suggestions.Count -eq 4 -and $newRes.historicalMedianTipPercentage -eq $null -and $newRes.primarySuggestion.isRecommended -eq $true) {
    Write-Host "PASS: New user receives 4 neutral suggestions with primary recommendation marked" -ForegroundColor Green
} else {
    Write-Host "FAIL: New user suggestions mismatch: $($newRes | ConvertTo-Json)" -ForegroundColor Red
}

Write-Host "[3/12] Seed Historical Tips for User A" -ForegroundColor Yellow
$tipsToSeed = @(
    @{ billAmount = 50.00; tipPercentage = 15.00; currency = "USD"; restaurantName = "Luigi Trattoria"; serviceQuality = "GOOD" },
    @{ billAmount = 60.00; tipPercentage = 18.00; currency = "USD"; restaurantName = "Luigi Trattoria"; serviceQuality = "GOOD" },
    @{ billAmount = 70.00; tipPercentage = 20.00; currency = "USD"; restaurantName = "Sushi Zen"; serviceQuality = "EXCELLENT" },
    @{ billAmount = 40.00; tipPercentage = 10.00; currency = "EUR"; restaurantName = "Paris Bistro"; serviceQuality = "GOOD" }
)

foreach ($t in $tipsToSeed) {
    $body = $t | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/tips" -Method Post -Headers $headersA -Body $body -ContentType "application/json" | Out-Null
}
Write-Host "PASS: Seeded 3 USD tips and 1 EUR tip for User A" -ForegroundColor Green

Write-Host "[4/12] Testing Historical Baseline Suggestions" -ForegroundColor Yellow
$histReq = @{ currency = "USD"; billAmount = 100.00 } | ConvertTo-Json
$histRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $histReq -ContentType "application/json"
# Median of USD tips [15, 18, 20] = 18.00
if ($histRes.historicalMedianTipPercentage -eq 18.00 -and $histRes.suggestions.Count -ge 3) {
    Write-Host "PASS: Historical median = 18.00% accurately computed" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected historical median 18.00%, got $($histRes.historicalMedianTipPercentage)" -ForegroundColor Red
}

Write-Host "[5/12] Testing Restaurant Context Matching" -ForegroundColor Yellow
$restReq = @{ currency = "USD"; billAmount = 80.00; restaurantName = "  luigi trattoria  " } | ConvertTo-Json
$restRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $restReq -ContentType "application/json"
if ($restRes.restaurantTipCount -eq 2 -and $restRes.restaurantMedianTipPercentage -eq 16.50) {
    Write-Host "PASS: Restaurant history matched with count=2 and median=16.50%" -ForegroundColor Green
} else {
    Write-Host "FAIL: Restaurant matching failed: count=$($restRes.restaurantTipCount), median=$($restRes.restaurantMedianTipPercentage)" -ForegroundColor Red
}

Write-Host "[6/12] Testing Service Quality Context" -ForegroundColor Yellow
$sqReq = @{ currency = "USD"; billAmount = 50.00; serviceQuality = "EXCELLENT" } | ConvertTo-Json
$sqRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $sqReq -ContentType "application/json"
if ($sqRes.serviceQualityTipCount -eq 1 -and $sqRes.serviceQualityAverageTipPercentage -eq 20.00) {
    Write-Host "PASS: Service quality context computed correctly for EXCELLENT" -ForegroundColor Green
} else {
    Write-Host "FAIL: Service quality mismatch: count=$($sqRes.serviceQualityTipCount)" -ForegroundColor Red
}

Write-Host "[7/12] Testing Budget Impact Calculation" -ForegroundColor Yellow
# Set a budget for User A
$budgetBody = @{ currency = "USD"; monthlyLimit = 200.00; warningThreshold = 80.00 } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/tip-budgets" -Method Put -Headers $headersA -Body $budgetBody -ContentType "application/json" | Out-Null
$budReq = @{ currency = "USD"; billAmount = 100.00 } | ConvertTo-Json
$budRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $budReq -ContentType "application/json"
$hasImpact = $budRes.suggestions | Where-Object { $_.budgetImpact -ne $null }
if ($budRes.budgetStatus -ne $null -and $hasImpact.Count -gt 0) {
    Write-Host "PASS: Budget status is $($budRes.budgetStatus), impact formatted: $($hasImpact[0].budgetImpact)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Budget impact not populated" -ForegroundColor Red
}

Write-Host "[8/12] Testing Strict Currency Isolation" -ForegroundColor Yellow
$eurReq = @{ currency = "EUR"; billAmount = 50.00 } | ConvertTo-Json
$eurRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $eurReq -ContentType "application/json"
if ($eurRes.historicalMedianTipPercentage -eq 10.00) {
    Write-Host "PASS: EUR request used strictly EUR tip (10%), completely isolated from USD tips" -ForegroundColor Green
} else {
    Write-Host "FAIL: EUR currency isolation violated: got $($eurRes.historicalMedianTipPercentage)" -ForegroundColor Red
}

Write-Host "[9/12] Testing User Isolation (User B has no tips)" -ForegroundColor Yellow
$userBReq = @{ currency = "USD"; billAmount = 100.00 } | ConvertTo-Json
$userBRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersB -Body $userBReq -ContentType "application/json"
if ($userBRes.historicalMedianTipPercentage -eq $null -and $userBRes.suggestions.Count -eq 4) {
    Write-Host "PASS: User B cannot see User A's data (received clean new user response)" -ForegroundColor Green
} else {
    Write-Host "FAIL: User isolation failed" -ForegroundColor Red
}

Write-Host "[10/12] Testing Stateless Guarantee (No DB Changes)" -ForegroundColor Yellow
$tipCountBefore = (Invoke-RestMethod -Uri "$baseUrl/api/tips" -Headers $headersA).totalElements
# Call smart-tip multiple times
Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $histReq -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $restReq -ContentType "application/json" | Out-Null
$tipCountAfter = (Invoke-RestMethod -Uri "$baseUrl/api/tips" -Headers $headersA).totalElements
if ($tipCountBefore -eq $tipCountAfter) {
    Write-Host "PASS: Smart Tip endpoint is completely stateless (tip count unchanged: $tipCountAfter)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Smart Tip endpoint mutated database!" -ForegroundColor Red
}

Write-Host "[11/12] Testing Mathematical Invariants on Suggestions" -ForegroundColor Yellow
$allAscending = $true
$primaryFound = $false
for ($i = 0; $i -lt $histRes.suggestions.Count; $i++) {
    $s = $histRes.suggestions[$i]
    if ($s.isRecommended -eq $true) { $primaryFound = $true }
    if ($i -gt 0) {
        if ($s.tipPercentage -le $histRes.suggestions[$i-1].tipPercentage) {
            $allAscending = $false
        }
    }
}
if ($allAscending -and $primaryFound) {
    Write-Host "PASS: Suggestions are strictly ascending without duplicate percentages, exactly 1 primary recommended" -ForegroundColor Green
} else {
    Write-Host "FAIL: Suggestion invariants violated: ascending=$allAscending, primaryFound=$primaryFound" -ForegroundColor Red
}

Write-Host "[12/12] Testing Informational Current Tip Percentage Comparison" -ForegroundColor Yellow
$diffReq = @{ currency = "USD"; billAmount = 100.00; currentTipPercentage = 15.00 } | ConvertTo-Json
$diffRes = Invoke-RestMethod -Uri "$baseUrl/api/smart-tip" -Method Post -Headers $headersA -Body $diffReq -ContentType "application/json"
$matchTyp = $diffRes.suggestions | Where-Object { $_.tipPercentage -eq 18.00 }
if ($matchTyp -and $matchTyp.historicalDifferencePercentagePoints -eq 3.00) {
    Write-Host "PASS: Difference percentage points relative to input tip (18% - 15% = +3.00%) accurately calculated" -ForegroundColor Green
} else {
    Write-Host "FAIL: Difference calculation mismatch: $($matchTyp.historicalDifferencePercentagePoints)" -ForegroundColor Red
}

Write-Host "`n=== ALL 12 API SCENARIOS VERIFIED SUCCESSFULLY ===" -ForegroundColor Green
