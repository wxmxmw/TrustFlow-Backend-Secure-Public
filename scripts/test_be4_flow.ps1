param(
    [Parameter(Mandatory=$true)][string]$WalletAddress,
    [string]$BaseUrl = "http://localhost:8080/api/v1"
)

$nonceResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/nonce" -Method POST -ContentType "application/json" -Body (@{ wallet_address = $WalletAddress } | ConvertTo-Json)
Write-Host "Nonce: $($nonceResponse.nonce)"
Write-Host "Open sign.html in a browser, connect your wallet, and sign this nonce."
$signature = Read-Host "Paste the signature"
$login = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -ContentType "application/json" -Body (@{
    wallet_address = $WalletAddress
    nonce = $nonceResponse.nonce
    signature = $signature
} | ConvertTo-Json)
if (-not $login.access_token) { throw "Login did not return an access token" }
$headers = @{ Authorization = "Bearer $($login.access_token)" }
$session = Invoke-RestMethod -Uri "$BaseUrl/sessions" -Method POST -Headers $headers -ContentType "application/json" -Body (@{ title = "Proof integration test" } | ConvertTo-Json)
$response = Invoke-RestMethod -Uri "$BaseUrl/chat/completions" -Method POST -Headers $headers -ContentType "application/json" -Body (@{
    session_id = $session.session_id
    prompt = "Explain blockchain in one sentence."
    model = "zai-org/GLM-4.5"
    mode = "text"
} | ConvertTo-Json)
Write-Host "Record ID: $($response.record_id)"
Write-Host "Transaction hash (may be pending): $($response.tx_hash)"
