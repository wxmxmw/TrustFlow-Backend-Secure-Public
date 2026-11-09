# TrustFlow Full Test Script
# Usage: .\full_test.ps1 -WalletAddress "0xYourAddress"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

param([Parameter(Mandatory=$true)][string]$WalletAddress)
$BaseUrl = "http://localhost:8080/api/v1"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   TrustFlow Full Function Test" -ForegroundColor Cyan
Write-Host "   Chat Onchain + Document Onchain" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Part 1: Authentication
Write-Host "=== Part 1: Authentication ===" -ForegroundColor Yellow

Write-Host "[1/3] Getting nonce..." -ForegroundColor Yellow
try {
    $nonceResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/nonce" -Method POST -ContentType "application/json" -Body (@{wallet_address = $WalletAddress} | ConvertTo-Json)
    $nonce = $nonceResponse.nonce
    Write-Host "[OK] Nonce: $nonce" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get nonce: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/3] Please sign nonce in browser..." -ForegroundColor Yellow
Write-Host "   1. Open browser: http://localhost:8000/sign.html" -ForegroundColor Cyan
Write-Host "   2. Enter wallet address: $WalletAddress" -ForegroundColor Cyan
Write-Host "   3. Enter nonce: $nonce" -ForegroundColor Cyan
Write-Host "   4. Click '1. Connect MetaMask' button" -ForegroundColor Cyan
Write-Host "   5. Click '2. Sign Message' button" -ForegroundColor Cyan
Write-Host "   6. Confirm in MetaMask" -ForegroundColor Cyan
Write-Host "   7. Note: if 404 error, try to run "python -m http.server 8000" in project root, and use chrome browser" -ForegroundColor Cyan
Write-Host ""
$signature = Read-Host "Enter signature result (0x..., or Ctrl+C to cancel)"

if (-not $signature -or -not $signature.StartsWith("0x")) {
    Write-Host "[ERROR] Invalid signature format" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[3/3] Logging in to get token..." -ForegroundColor Yellow
try {
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -ContentType "application/json" -Body (@{
        wallet_address = $WalletAddress
        nonce = $nonce
        signature = $signature
    } | ConvertTo-Json)
    $token = $loginResponse.access_token
    Write-Host "[OK] Token obtained successfully" -ForegroundColor Green

} catch {
    Write-Host "[ERROR] Login failed: $_" -ForegroundColor Red
    exit 1
}

# Part 2: Chat Onchain Test
Write-Host ""
Write-Host "=== Part 2: Chat Onchain Test ===" -ForegroundColor Yellow

Write-Host "[1/4] Creating session..." -ForegroundColor Yellow
try {
    $sessionResponse = Invoke-RestMethod -Uri "$BaseUrl/sessions" -Method POST -Headers @{Authorization = "Bearer $token"} -ContentType "application/json" -Body (@{title = "Full Test Session - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"} | ConvertTo-Json)
    $sessionId = $sessionResponse.session_id
    Write-Host "[OK] Session ID: $sessionId" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to create session: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/4] Sending chat request (testing async onchain)..." -ForegroundColor Yellow
Write-Host "   Note: If not response, try to refresh backend service" -ForegroundColor Cyan
try {
    $startTime = Get-Date
    $chatResponse = Invoke-RestMethod -Uri "$BaseUrl/chat/completions" -Method POST -Headers @{Authorization = "Bearer $token"} -ContentType "application/json" -Body (@{
        session_id = $sessionId
        prompt = "Test async onchain, please briefly answer: What is blockchain?"
        model = "zai-org/GLM-4.5"
        mode = "text"
    } | ConvertTo-Json)
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalSeconds
    
    $recordId = $chatResponse.record_id
    Write-Host "[OK] Chat request successful (response time: $([Math]::Round($responseTime, 2)) seconds)" -ForegroundColor Green
    Write-Host "   Record ID: $recordId" -ForegroundColor Gray
    Write-Host "   TX Hash (immediate): $($chatResponse.tx_hash)" -ForegroundColor $(if ($chatResponse.tx_hash) { "Green" } else { "Yellow" })
    Write-Host "   Content (first 100 chars): $($chatResponse.content.Substring(0, [Math]::Min(100, $chatResponse.content.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Chat request failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[3/4] Waiting 20 seconds for chat async onchain..." -ForegroundColor Yellow
for ($i = 20; $i -gt 0; $i--) {
    Write-Host "   Waiting... $i seconds" -ForegroundColor Gray -NoNewline
    Start-Sleep -Seconds 1
    Write-Host "`r" -NoNewline
}
Write-Host "   Wait completed!                    " -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] Chat onchain verification" -ForegroundColor Yellow
Write-Host "Please execute the following query in PostgreSQL:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   -- Query chat record tx_hash" -ForegroundColor White
Write-Host "   SELECT record_id, tx_hash, content_hash, created_at" -ForegroundColor White
Write-Host "   FROM generation_records" -ForegroundColor White
Write-Host "   WHERE record_id = $recordId;" -ForegroundColor White
Write-Host ""
Write-Host "   -- Query onchain proof record" -ForegroundColor White
Write-Host "   SELECT id, generation_record_id, content_hash, tx_hash, status" -ForegroundColor White
Write-Host "   FROM onchain_proof" -ForegroundColor White
Write-Host "   WHERE generation_record_id = $recordId;" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to continue after executing the queries above"

# Part 3: Document Onchain Test
Write-Host "=== Part 3: Document Onchain Test ===" -ForegroundColor Yellow

# Test Document: test_new.pdf
Write-Host "[Document 1/1] Testing test_new.pdf..." -ForegroundColor Yellow

Write-Host "[1/4] Checking test_new.pdf file..." -ForegroundColor Yellow
$testFile = ".\test_new.pdf"
if (-not (Test-Path $testFile)) {
    Write-Host "[ERROR] Test file not found: $testFile" -ForegroundColor Red
    Write-Host "   Please ensure test_new.pdf exists in project root" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Test file exists: $testFile" -ForegroundColor Green

Write-Host ""
Write-Host "[2/4] Uploading test_new.pdf (testing async onchain)..." -ForegroundColor Yellow
try {
    $response = curl.exe -X POST "$BaseUrl/documents/upload" -H "Authorization: Bearer $token" -F "file=@test_new.pdf" -s
    $result = $response | ConvertFrom-Json
    $docId = $result.doc_id
    Write-Host "[OK] Document uploaded successfully" -ForegroundColor Green
    Write-Host "   Document ID: $docId" -ForegroundColor Gray
    Write-Host "   File Hash: $($result.file_hash)" -ForegroundColor Gray
    Write-Host "   TX Hash (immediate): $($result.tx_hash)" -ForegroundColor $(if ($result.tx_hash) { "Green" } else { "Yellow" })
    Write-Host "   Status: $($result.status)" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Document upload failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[3/4] Waiting 20 seconds for test_new.pdf async onchain..." -ForegroundColor Yellow
for ($i = 20; $i -gt 0; $i--) {
    Write-Host "   Waiting... $i seconds" -ForegroundColor Gray -NoNewline
    Start-Sleep -Seconds 1
    Write-Host "`r" -NoNewline
}
Write-Host "   Wait completed!                    " -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] test_new.pdf onchain verification" -ForegroundColor Yellow
Write-Host "Please execute the following query in PostgreSQL:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   -- Query test_new.pdf document record tx_hash" -ForegroundColor White
Write-Host "   SELECT doc_id, file_hash, tx_hash, status" -ForegroundColor White
Write-Host "   FROM documents" -ForegroundColor White
Write-Host "   WHERE doc_id = $docId;" -ForegroundColor White
Write-Host ""
Write-Host "   -- Query test_new.pdf onchain proof record" -ForegroundColor White
Write-Host "   SELECT id, generation_record_id, content_hash, tx_hash, status" -ForegroundColor White
Write-Host "   FROM onchain_proof" -ForegroundColor White
Write-Host "   WHERE content_hash = '0x$($result.file_hash)';" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to continue after executing the queries above"

Write-Host ""
Write-Host "Document onchain test summary" -ForegroundColor Yellow
Write-Host "   test_new.pdf - Document ID: $docId, File Hash: $($result.file_hash)" -ForegroundColor Cyan
Write-Host ""

# Save test info
$testInfo = @{
    timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    wallet_address = $WalletAddress
    session_id = $sessionId
    chat_record_id = $recordId
    document_id = $docId
    document_file = "test_new.pdf"
    document_hash = $result.file_hash
    token = $token
} | ConvertTo-Json

$testInfo | Out-File -FilePath "full_test_info.json" -Encoding UTF8
Write-Host "[OK] Test info saved to full_test_info.json" -ForegroundColor Green

# Test summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Full Test Completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Results:" -ForegroundColor Yellow
Write-Host "  [OK] Chat onchain test - Record ID: $recordId" -ForegroundColor Green
Write-Host "  [OK] Document (test_new.pdf) onchain test - Document ID: $docId" -ForegroundColor Green
Write-Host ""
Write-Host "Final Verification Steps:" -ForegroundColor Yellow
Write-Host "  1. Query database to verify all tx_hash are filled" -ForegroundColor Cyan
Write-Host "  2. Check onchain_proof table status is 'succeeded' for all records" -ForegroundColor Cyan
Write-Host "  3. If all tx_hash have values, async onchain succeeded!" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to exit"
