<#
.SYNOPSIS
Genera un respaldo completo de la base de datos local de MIGAL SmartOps (RNF-12).

.DESCRIPTION
Ejecuta pg_dump dentro del contenedor migal-postgres en formato personalizado y guarda el archivo en respaldos/, con la
fecha y hora en el nombre. La carpeta está excluida de Git porque el respaldo contiene datos del negocio. Para
restaurar, ver database/README.md, sección Respaldos.
#>
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$carpeta = Join-Path $repo 'respaldos'
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
$archivo = Join-Path $carpeta ("migal-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')

docker exec migal-postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/respaldo.dump'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el respaldo. Verificar que el contenedor migal-postgres esté en ejecución.' }
docker cp migal-postgres:/tmp/respaldo.dump $archivo | Out-Null
docker exec migal-postgres rm -f /tmp/respaldo.dump | Out-Null
Write-Host "Respaldo generado: $archivo"
