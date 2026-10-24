# Uso (PowerShell, desde cualquier carpeta):  .\probar.ps1
# Muestra el codigo HTTP, los headers y el CUERPO de la respuesta (tambien cuando es 400).
# Con la app corriendo y update-offer.ems.dry-run: true, el XML se imprime en la consola de STS.

$url  = "http://localhost:8080/sd-campaing-management/v1/update-offer"
$uuid = [guid]::NewGuid().ToString()
$json = Join-Path $PSScriptRoot "request-ejemplo.json"

curl.exe -s -i -X POST $url `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -H "uuid: $uuid" `
  -H "ChannelId: CAT" `
  -H "sid: sid-prueba-1" `
  -H "countryCode: MX" `
  -H "businessCode: COP" `
  -H "Accept-Language: es-MX" `
  --data-binary "@$json"

Write-Host ""
