# Reset Rate Limits Script for DayQuest
# This script clears all rate limit entries in Redis

Write-Host " ____  _____ __ __ _____ _____ _____ _____ _____" -ForegroundColor Cyan
Write-Host "|    \|  _  |  |  |     |  |  |   __|   __|_   _|" -ForegroundColor Cyan
Write-Host "|  |  |     |_   _|  |  |  |  |   __|__   | | |  " -ForegroundColor Cyan
Write-Host "|____/|__|__| |_| |__  _|_____|_____|_____| |_|  " -ForegroundColor Cyan
Write-Host "                     |__|               by AgentP" -ForegroundColor Yellow
Write-Host ""

Write-Host "Resetting all rate limits in Redis..." -ForegroundColor Yellow

# Get all rate limit keys
$keys = docker exec redis_container redis-cli KEYS "ratelimit:*"

if ($keys -and $keys.Trim()) {
    # Split keys by newline
    $keyArray = $keys -split "`n" | Where-Object { $_ -and $_.Trim() }

    foreach ($key in $keyArray) {
        $key = $key.Trim()
        if ($key) {
            Write-Host "Deleting key: $key" -ForegroundColor Gray
            docker exec redis_container redis-cli DEL $key | Out-Null
        }
    }

    Write-Host ""
    Write-Host "Rate limits cleared! ($($keyArray.Count) keys deleted)" -ForegroundColor Green
} else {
    Write-Host "No rate limit keys found in Redis." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "You can now retry your requests." -ForegroundColor Green

