$ErrorActionPreference = "Stop"

Write-Host "Waiting for backend on port 8080..."
while (!(Test-NetConnection localhost -Port 8080 -WarningAction SilentlyContinue).TcpTestSucceeded) {
    Start-Sleep -Seconds 2
}
Write-Host "Backend is up!"

$baseUrl = "http://localhost:8080/api"

# 1. Register a user
$email = "test$(Get-Random)@example.com"
Write-Host "Registering $email..."
$registerJson = @{
    name = "Test User"
    email = $email
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $registerJson -ContentType "application/json"
$token = $response.token
Write-Host "Got token."

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Create a tip with EXCELLENT
Write-Host "Creating tip with EXCELLENT..."
$tipJson = @{
    restaurantName = "Test Rest"
    billAmount = 100
    tipPercentage = 20
    currency = "USD"
    serviceQuality = "EXCELLENT"
} | ConvertTo-Json

$tipResponse = Invoke-RestMethod -Uri "$baseUrl/tips" -Method Post -Body $tipJson -ContentType "application/json" -Headers $headers
$tipId = $tipResponse.id
Write-Host "Created tip ID: $tipId with quality: $($tipResponse.serviceQuality)"

# 3. Fetch history
Write-Host "Fetching tip history..."
$historyResponse = Invoke-RestMethod -Uri "$baseUrl/tips?page=0&size=10" -Method Get -Headers $headers
$firstTipQuality = $historyResponse.content[0].serviceQuality
Write-Host "Tip in history has quality: $firstTipQuality"

# 4. PATCH tip to POOR
Write-Host "Patching tip to POOR..."
$patchJson = @{
    serviceQuality = "POOR"
} | ConvertTo-Json

$patchResponse = Invoke-RestMethod -Uri "$baseUrl/tips/$tipId/service-quality" -Method Patch -Body $patchJson -ContentType "application/json" -Headers $headers
Write-Host "Patched tip returned quality: $($patchResponse.serviceQuality)"

# 5. Fetch history again
Write-Host "Fetching tip history again..."
$historyResponse2 = Invoke-RestMethod -Uri "$baseUrl/tips?page=0&size=10" -Method Get -Headers $headers
$updatedTipQuality = $historyResponse2.content[0].serviceQuality
Write-Host "Updated tip in history has quality: $updatedTipQuality"

Write-Host "Verification complete."
