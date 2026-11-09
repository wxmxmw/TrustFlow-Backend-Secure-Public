# Document Onchain Verification Script
# Purpose: Verify that document upload, file_hash calculation, and onchain submission work correctly
# Usage: .\test_document_onchain.ps1 -WalletAddress "0xYourAddress" -TestFile "test.pdf"

param(
    [Parameter(Mandatory=$true)][string]$WalletAddress,
    [string]$TestFile = "test_.pdf"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$BaseUrl = "http://localhost:8080/api/v1"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Document Onchain Verification Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Purpose:" -ForegroundColor Yellow
Write-Host "  1. Verify file_hash is calculated and saved" -ForegroundColor White
Write-Host "  2. Verify document is submitted to blockchain" -ForegroundColor White
Write-Host "  3. Verify documents.tx_hash is updated after onchain" -ForegroundColor White
Write-Host "  4. Verify documents.status is updated correctly" -ForegroundColor White
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

# Part 2: Document Upload Test
Write-Host ""
Write-Host "=== Part 2: Document Upload & Onchain Test ===" -ForegroundColor Yellow

Write-Host "[1/5] Checking test file..." -ForegroundColor Yellow
$testFilePath = ".\$TestFile"
if (-not (Test-Path $testFilePath)) {
    Write-Host "[ERROR] Test file not found: $testFilePath" -ForegroundColor Red
    Write-Host "   Please ensure $TestFile exists in project root" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Test file exists: $testFilePath" -ForegroundColor Green

Write-Host ""
Write-Host "[2/5] Uploading document..." -ForegroundColor Yellow
try {
    $response = curl.exe -X POST "$BaseUrl/documents/upload" -H "Authorization: Bearer $token" -F "file=@$TestFile" -s
    $result = $response | ConvertFrom-Json
    $docId = $result.doc_id
    $fileHash = $result.file_hash
    $txHashImmediate = $result.tx_hash
    $statusImmediate = $result.status
    
    Write-Host "[OK] Document uploaded successfully" -ForegroundColor Green
    Write-Host "   Document ID: $docId" -ForegroundColor Gray
    Write-Host "   File Hash: $fileHash" -ForegroundColor Gray
    Write-Host "   TX Hash (immediate): $txHashImmediate" -ForegroundColor $(if ($txHashImmediate) { "Green" } else { "Yellow" })
    Write-Host "   Status (immediate): $statusImmediate" -ForegroundColor Gray
    Write-Host ""
    
    # Verification Point 1: file_hash should be present
    if ($fileHash) {
        Write-Host "   [VERIFY] file_hash is calculated: $fileHash" -ForegroundColor Green
    } else {
        Write-Host "   [FAIL] file_hash is missing!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Document upload failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[3/5] Waiting 30 seconds for async onchain..." -ForegroundColor Yellow
Write-Host "   (Blockchain transaction confirmation may take time)" -ForegroundColor Cyan
for ($i = 30; $i -gt 0; $i--) {
    Write-Host "   Waiting... $i seconds" -ForegroundColor Gray -NoNewline
    Start-Sleep -Seconds 1
    Write-Host "`r" -NoNewline
}
Write-Host "   Wait completed!                    " -ForegroundColor Green

Write-Host ""
Write-Host "[4/5] Database Verification" -ForegroundColor Yellow
Write-Host "Please execute the following queries in PostgreSQL:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   -- Query 1: Check documents table" -ForegroundColor White
Write-Host "   SELECT doc_id, file_hash, tx_hash, status" -ForegroundColor White
Write-Host "   FROM documents" -ForegroundColor White
Write-Host "   WHERE doc_id = $docId;" -ForegroundColor White
Write-Host ""
Write-Host "   -- Query 2: Check onchain_proof table" -ForegroundColor White
Write-Host "   SELECT id, generation_record_id, content_hash, tx_hash, status" -ForegroundColor White
Write-Host "   FROM onchain_proof" -ForegroundColor White
Write-Host "   WHERE content_hash = '0x$fileHash';" -ForegroundColor White
Write-Host ""
Write-Host "Expected Results:" -ForegroundColor Yellow
Write-Host "  ✓ documents.file_hash should match: $fileHash" -ForegroundColor Green
Write-Host "  ✓ documents.tx_hash should NOT be null (after onchain)" -ForegroundColor Green
Write-Host "  ✓ documents.status should be 'completed' or 'processing'" -ForegroundColor Green
Write-Host "  ✓ onchain_proof.content_hash should be '0x$fileHash'" -ForegroundColor Green
Write-Host "  ✓ onchain_proof.tx_hash should NOT be null" -ForegroundColor Green
Write-Host "  ✓ onchain_proof.status should be 'succeeded' or 'pending'" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter after executing the queries and reviewing the results"

Write-Host ""
Write-Host "[5/5] Verification Summary" -ForegroundColor Yellow
Write-Host ""
Write-Host "Please confirm the following:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  [ ] 1. documents.file_hash is filled with SHA-256 hash" -ForegroundColor White
Write-Host "  [ ] 2. onchain_proof table has a record with content_hash = '0x$fileHash'" -ForegroundColor White
Write-Host "  [ ] 3. onchain_proof.tx_hash is filled (transaction sent to blockchain)" -ForegroundColor White
Write-Host "  [ ] 4. documents.tx_hash is filled (updated after transaction confirmation)" -ForegroundColor White
Write-Host "  [ ] 5. documents.status is 'completed' or 'processing'" -ForegroundColor White
Write-Host "  [ ] 6. onchain_proof.status is 'succeeded' (if transaction confirmed)" -ForegroundColor White
Write-Host ""

# Save test info
$testInfo = @{
    timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    test_purpose = "Document Onchain Verification"
    wallet_address = $WalletAddress
    document_id = $docId
    document_file = $TestFile
    file_hash = $fileHash
    tx_hash_immediate = $txHashImmediate
    status_immediate = $statusImmediate
    verification_queries = @{
        documents_query = "SELECT doc_id, file_hash, tx_hash, status FROM documents WHERE doc_id = $docId;"
        onchain_proof_query = "SELECT id, generation_record_id, content_hash, tx_hash, status FROM onchain_proof WHERE content_hash = '0x$fileHash';"
    }
} | ConvertTo-Json -Depth 5

$testInfo | Out-File -FilePath "document_onchain_verification.json" -Encoding UTF8
Write-Host "[OK] Test info saved to document_onchain_verification.json" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Test Completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Key Verification Points:" -ForegroundColor Yellow
Write-Host "  1. file_hash calculation: Should be SHA-256 hash (64 hex chars)" -ForegroundColor Cyan
Write-Host "  2. file_hash storage: Should be saved in documents.file_hash" -ForegroundColor Cyan
Write-Host "  3. Onchain submission: Should create record in onchain_proof table" -ForegroundColor Cyan
Write-Host "  4. tx_hash update: Should update documents.tx_hash after confirmation" -ForegroundColor Cyan
Write-Host ""
Write-Host "If all points pass, the optimization is successful!" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to exit"
