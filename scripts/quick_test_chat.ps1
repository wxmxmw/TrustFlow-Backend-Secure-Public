param(
    [Parameter(Mandatory=$true)][string]$Token,
    [Parameter(Mandatory=$true)][int]$SessionId,
    [string]$Prompt = "Explain blockchain in one sentence.",
    [string]$BaseUrl = "http://localhost:8080/api/v1"
)

$body = @{ session_id = $SessionId; prompt = $Prompt; model = "zai-org/GLM-4.5"; mode = "text" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$BaseUrl/chat/completions" -Method POST -ContentType "application/json" -Headers @{ Authorization = "Bearer $Token" } -Body $body
$response | ConvertTo-Json -Depth 10
Write-Host "The transaction hash may be empty while proof submission is pending."
