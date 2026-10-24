# 1. Definir la ruta de tu archivo JSON
$json = "C:\Users\AS82906\request-ejemplo.json"

# 2. Definir tus otras variables (cambia estos valores por los reales)
$url = "https://api.tu-servicio.com/endpoint" 
$uuid = "tu-uuid-aqui"

# 3. Ejecutar el comando curl.exe
curl.exe -s -i -X POST $url `
  -H "Content-Type: application/json" `
  -H "Accept: application/json" `
  -H "uuid: $uuid" `
  -H "ChannelId: CAT" `
  -H "sid: sid-prueba-1" `
  -H "countryCode: MX" `
  -H "businessCode: COP" `
  -H "Accept-Language: es-MX" `
  --data-binary ('@' + $json)