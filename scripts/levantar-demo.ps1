<#
.SYNOPSIS
Levanta el sistema completo de MIGAL SmartOps en contenedores y carga los datos de demostración.

.DESCRIPTION
Solo para el entorno local; no se conecta a Supabase. Pasos:
1. Si no existe .env, lo crea desde .env.example con contraseñas aleatorias y muestra la clave del administrador.
2. Ejecuta docker compose --profile completo up -d --build --wait.
3. Carga database/datos-demostracion/demo.sql (ubicaciones, clientes y proveedores).
4. Carga el catálogo real si existe database/datos-demostracion/local/catalogo-real.sql (se recibe aparte, porque
   contiene los precios del negocio); si no, carga el catálogo de ejemplo catalogo-notas-venta.sql.

Los scripts SQL son idempotentes: el script puede ejecutarse de nuevo sin duplicar datos.

.PARAMETER Excel
Ruta opcional de Catalogo_Migal.xlsx. Si se indica, genera local/catalogo-real.sql antes de cargarlo (requiere
Python con openpyxl).

.EXAMPLE
powershell -ExecutionPolicy Bypass -File scripts/levantar-demo.ps1
#>
[CmdletBinding()]
param(
    [string]$Excel
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$datos = Join-Path $repo 'database/datos-demostracion'
$catalogoReal = Join-Path $datos 'local/catalogo-real.sql'
Set-Location $repo

function Paso([string]$texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }

function Texto-Aleatorio([int]$largo) {
    $caracteres = 'abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'.ToCharArray()
    $bytes = New-Object byte[] $largo
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $caracteres[$_ % $caracteres.Length] })
}

function Ejecutar-Sql([string]$archivo) {
    # Se copia el archivo al contenedor en lugar de enviarlo por la tubería de PowerShell, que en Windows PowerShell
    # 5.1 altera las tildes y la ñ.
    docker cp $archivo migal-postgres:/tmp/carga.sql | Out-Null
    docker exec migal-postgres sh -c 'psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -f /tmp/carga.sql'
    if ($LASTEXITCODE -ne 0) { throw "Falló la carga de $archivo." }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'No se encontró Docker. Instalar Docker Desktop e iniciarlo antes de ejecutar este script.'
}

Paso 'Variables de entorno'
$envArchivo = Join-Path $repo '.env'
if (Test-Path $envArchivo) {
    Write-Host 'Se usa el archivo .env existente.'
} else {
    $claveAdmin = 'Migal' + (Texto-Aleatorio 8) + '7'
    $contenido = Get-Content (Join-Path $repo '.env.example') -Encoding UTF8
    $contenido = $contenido -replace '^POSTGRES_PASSWORD=.*', "POSTGRES_PASSWORD=$(Texto-Aleatorio 24)"
    $contenido += ''
    $contenido += '# Generado por scripts/levantar-demo.ps1'
    $contenido += "MIGAL_JWT_SECRETO=$(Texto-Aleatorio 48)"
    $contenido += "MIGAL_OCR_TOKEN_INTERNO=$(Texto-Aleatorio 32)"
    $contenido += 'MIGAL_ADMIN_USERNAME=admin'
    $contenido += "MIGAL_ADMIN_CLAVE=$claveAdmin"
    $contenido += '# Para usar Gemini real: definir la clave y cambiar ambos proveedores (ver .env.example).'
    $contenido += '# GEMINI_API_KEY=<clave-de-la-api>'
    $contenido += '# MIGAL_OCR_PROVEEDOR=http'
    $contenido += '# MIGAL_AGENTE_PROVEEDOR=gemini'
    [System.IO.File]::WriteAllLines($envArchivo, $contenido, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "Se creó .env. Usuario: admin  Clave: $claveAdmin" -ForegroundColor Yellow
    Write-Host 'La clave queda guardada en .env (MIGAL_ADMIN_CLAVE).'
}
$variables = @{}
Get-Content $envArchivo -Encoding UTF8 | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_.]*=' } | ForEach-Object {
    $partes = $_ -split '=', 2
    $variables[$partes[0]] = $partes[1]
}
if (-not $variables['MIGAL_JWT_SECRETO'] -or -not $variables['MIGAL_ADMIN_CLAVE']) {
    throw '.env debe definir MIGAL_JWT_SECRETO y MIGAL_ADMIN_CLAVE. Borrar .env y ejecutar de nuevo para generarlo.'
}
if ($variables['GEMINI_API_KEY'] -and $variables['MIGAL_OCR_PROVEEDOR'] -eq 'http') {
    Write-Host 'OCR con Gemini real.'
} else {
    Write-Host 'OCR y agente simulados (sin GEMINI_API_KEY o sin MIGAL_OCR_PROVEEDOR=http).'
}

Paso 'Construyendo e iniciando los contenedores (la primera vez tarda varios minutos)'
docker compose --profile completo up -d --build --wait
if ($LASTEXITCODE -ne 0) { throw 'docker compose no pudo iniciar los servicios. Revisar: docker compose --profile completo logs' }

Paso 'Datos de demostración'
Ejecutar-Sql (Join-Path $datos 'demo.sql')

if ($Excel) {
    Paso 'Generando el catálogo real desde el Excel'
    python (Join-Path $datos 'generar_catalogo_real.py') $Excel
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el catálogo. Instalar openpyxl: pip install openpyxl' }
}

if (Test-Path $catalogoReal) {
    Paso 'Catálogo real de MIGAL'
    Ejecutar-Sql $catalogoReal
} else {
    Paso 'Catálogo de ejemplo (no se encontró local/catalogo-real.sql)'
    Ejecutar-Sql (Join-Path $datos 'catalogo-notas-venta.sql')
}

$puerto = if ($variables['FRONTEND_PUERTO']) { $variables['FRONTEND_PUERTO'] } else { '8081' }
Paso "Listo: abrir http://localhost:$puerto e ingresar con el usuario $($variables['MIGAL_ADMIN_USERNAME'])"
Write-Host 'Detener sin borrar datos: docker compose --profile completo down'
