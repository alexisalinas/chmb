# Uso (PowerShell, desde esta carpeta):  .\probar.ps1
# Con la app corriendo y update-offer.ems.dry-run: true, el XML se imprime en la consola de STS.

$url = "http://localhost:8080/sd-campaing-management/v1/update-offer"

$headers = @{
    "Content-Type" = "application/json"
    "Accept"       = "application/json"
    "uuid"         = [guid]::NewGuid().ToString()
    "ChannelId"    = "CAT"
    "sid"          = "sid-prueba-1"
    "countryCode"  = "MX"
    "businessCode" = "COP"
    "Accept-Language" = "es-MX"
}

$body = Get-Content -Raw -Encoding UTF8 ".\request-ejemplo.json"

try {
    $resp = Invoke-RestMethod -Method Post -Uri $url -Headers $headers -Body $body
    $resp | ConvertTo-Json -Depth 5
} catch {
    Write-Host "HTTP $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $_.ErrorDetails.Message
}
