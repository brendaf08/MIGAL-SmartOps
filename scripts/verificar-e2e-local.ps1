[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$frontend = Join-Path $repo 'frontend'
$runStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$reportDir = Join-Path $repo "artifacts/e2e-local/$runStamp"
$htmlDir = Join-Path $reportDir 'html'
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "migal-e2e-$runStamp-$PID"
$null = New-Item -ItemType Directory -Force -Path $reportDir, $tempDir
$started = Get-Date
$scenarios = [System.Collections.Generic.List[object]]::new()
$failure = $null
$cleanupFailure = $null
$backendProcess = $null
$frontendProcess = $null
$dbStarted = $false
$savedEnvironment = @{}

function Add-Scenario([string]$Name, [string]$Result, [double]$Duration, [string]$Detail = '') {
    $scenarios.Add([pscustomobject]@{ Name = $Name; Result = $Result; Duration = $Duration; Detail = $Detail })
}

function Get-FreePort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port }
    finally { $listener.Stop() }
}

function Set-ChildEnvironment([hashtable]$Values) {
    foreach ($key in $Values.Keys) {
        if (-not $savedEnvironment.ContainsKey($key)) { $savedEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
        [Environment]::SetEnvironmentVariable($key, [string]$Values[$key], 'Process')
    }
}

function Restore-ChildEnvironment {
    foreach ($key in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($key, $savedEnvironment[$key], 'Process')
    }
    $savedEnvironment.Clear()
}

function Wait-Http([string]$Url, [int]$TimeoutSeconds, [string]$ProcessName) {
    $until = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $until) {
        if ($ProcessName -eq 'backend' -and $backendProcess.HasExited) { throw 'El backend terminó antes de responder.' }
        if ($ProcessName -eq 'frontend' -and $frontendProcess.HasExited) { throw 'El frontend terminó antes de responder.' }
        try {
            $response = Invoke-WebRequest -Uri $Url -TimeoutSec 3 -UseBasicParsing
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) { return }
        } catch { Start-Sleep -Milliseconds 750 }
    }
    throw "El servicio local no respondió a tiempo ($ProcessName)."
}

function Stop-ProcessTree($Process) {
    if ($null -ne $Process -and -not $Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F *> $null
    }
}

function Sanitize-PlaywrightEvidence {
    $contextFiles = @()
    $testOutput = Join-Path $reportDir 'test-output'
    $htmlData = Join-Path $htmlDir 'data'
    if (Test-Path $testOutput) { $contextFiles += Get-ChildItem -LiteralPath $testOutput -Filter 'error-context.md' -File -Recurse -ErrorAction SilentlyContinue }
    if (Test-Path $htmlData) { $contextFiles += Get-ChildItem -LiteralPath $htmlData -Filter '*.md' -File -Recurse -ErrorAction SilentlyContinue }
    foreach ($file in $contextFiles) {
        $content = [System.IO.File]::ReadAllText($file.FullName)
        $marker = '# Page snapshot'
        $position = $content.IndexOf($marker, [System.StringComparison]::Ordinal)
        if ($position -ge 0) {
            $match = [regex]::Match($content.Substring($position), 'heading "(?<title>[^"\r\n]+)" \[level=1\]')
            $safeHeading = if ($match.Success) { "- heading `"$($match.Groups['title'].Value)`" [level=1]`r`n" } else { '' }
            $safeContent = $content.Substring(0, $position) + "$marker`r`n$safeHeading[Contenido DOM omitido para evitar incluir datos de pantalla en la evidencia.]`r`n"
            [System.IO.File]::WriteAllText($file.FullName, $safeContent, [System.Text.UTF8Encoding]::new($false))
        }
    }
}

function Write-Reports {
    $elapsed = [Math]::Round(((Get-Date) - $started).TotalSeconds, 1)
    $commit = (& git -C $repo rev-parse HEAD 2>$null | Select-Object -First 1)
    if (-not $commit) { $commit = 'desconocido' }
    $dateText = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')
    $junitPath = Join-Path $reportDir 'junit.xml'
    if (Test-Path $junitPath) {
        try {
            [xml]$junit = Get-Content $junitPath -Raw
            foreach ($case in @($junit.SelectNodes('//testcase'))) {
                $result = if ($case.SelectSingleNode('failure') -or $case.SelectSingleNode('error')) { 'FALLÓ' } else { 'OK' }
                Add-Scenario $case.name $result ([double]$case.time) 'Diagnóstico en el HTML; capturas acotadas al encabezado cuando el flujo llegó a ese punto.'
            }
        } catch { Add-Scenario 'Lectura de resultados Playwright' 'ERROR' 0 'No se pudo interpretar el resumen JUnit.' }
    }
    if ($failure) {
        $failureStage = if (Test-Path $junitPath) { 'Ejecución de Playwright' } else { 'Preparación del entorno' }
        $failureResult = if (Test-Path $junitPath) { 'FALLÓ' } else { 'NO EJECUTADO' }
        Add-Scenario $failureStage $failureResult ([Math]::Round(((Get-Date) - $started).TotalSeconds, 1)) $failure
    }
    if ($cleanupFailure) { Add-Scenario 'Limpieza de PostgreSQL temporal' 'ERROR' 0 $cleanupFailure }
    $overall = if ($scenarios | Where-Object Result -in @('FALLÓ', 'ERROR')) { 'FALLÓ' } elseif ($failure) { 'NO EJECUTADO' } else { 'OK' }
    $rows = foreach ($scenario in $scenarios) {
        "| $($scenario.Name.Replace('|','/')) | $($scenario.Result) | $([Math]::Round([double]$scenario.Duration,1)) s | $($scenario.Detail.Replace('|','/')) |"
    }
    $markdown = @(
        '# Verificación E2E local MIGAL SmartOps', '',
        "- Fecha: $dateText", "- Commit: ``$commit``", "- Resultado general: **$overall**", "- Duración total: $elapsed s", '',
        '| Escenario | Resultado | Duración | Evidencia / detalle |', '|---|---:|---:|---|'
    ) + @($rows) + @('', 'Si Playwright se ejecutó, su reporte detallado está en `html/index.html` y los estados HTTP sin cuerpos están en `api-estados.txt`. Cuando se alcanzan los escenarios, se usa una base PostgreSQL temporal sin volumen persistente. Los escenarios marcados **SIMULADO** alcanzan la API local, pero usan proveedores OCR/IA simulados; no representan integraciones con proveedores reales.', '')
    Set-Content -LiteralPath (Join-Path $reportDir 'resumen.md') -Value ($markdown -join "`n") -Encoding utf8

    $scenarioHtml = foreach ($scenario in $scenarios) {
        $safeName = [System.Net.WebUtility]::HtmlEncode([string]$scenario.Name)
        $safeResult = [System.Net.WebUtility]::HtmlEncode([string]$scenario.Result)
        $safeDetail = [System.Net.WebUtility]::HtmlEncode([string]$scenario.Detail)
        "<tr><td>$safeName</td><td>$safeResult</td><td>$([Math]::Round([double]$scenario.Duration,1)) s</td><td>$safeDetail</td></tr>"
    }
    $playwrightLink = if (Test-Path (Join-Path $htmlDir 'index.html')) { '<p><a href="html/index.html">Abrir el reporte detallado de Playwright</a></p>' } else { '<p>Playwright no llegó a generar su reporte HTML en esta ejecución.</p>' }
    $apiLogLink = if (Test-Path (Join-Path $reportDir 'api-estados.txt')) { '<p><a href="api-estados.txt">Ver estados HTTP de la API local (método, ruta y código; sin cuerpos ni cabeceras)</a></p>' } else { '' }
    $page = @"
<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>E2E local MIGAL SmartOps</title>
<style>body{font:16px system-ui,sans-serif;max-width:1100px;margin:2rem auto;padding:0 1rem;color:#18212b}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccd4dc;padding:.65rem;text-align:left}th{background:#edf2f7}.estado{font-weight:700}</style>
<h1>Verificación E2E local MIGAL SmartOps</h1><p>Fecha: $dateText · Commit: $commit · Resultado: <span class="estado">$overall</span> · Duración: $elapsed s</p>
<table><thead><tr><th>Escenario</th><th>Resultado</th><th>Duración</th><th>Evidencia / detalle</th></tr></thead><tbody>$($scenarioHtml -join "`n")</tbody></table>
$playwrightLink
$apiLogLink
<p>Las pruebas usan PostgreSQL temporal sin volumen cuando alcanzan los escenarios. Los casos <strong>SIMULADO</strong> usan endpoints locales con proveedores OCR/IA simulados.</p>
<p>Reiniciar con <code>pwsh -File scripts/verificar-e2e-local.ps1</code>; el contenedor se detiene al finalizar y no afecta los servicios de demostración.</p>
</html>
"@
    Set-Content -LiteralPath (Join-Path $reportDir 'index.html') -Value $page -Encoding utf8
}

try {
    $nodeCmd = Get-Command node -ErrorAction Stop
    $nodeVersion = [version]((node --version).TrimStart('v'))
    if ($nodeVersion -lt [version]'22.22.0') { throw 'Se requiere Node.js 22.22 o posterior.' }
    $npm = Join-Path (Split-Path $nodeCmd.Source) 'npm.cmd'
    if (-not (Test-Path $npm)) { throw 'No se encontró npm.cmd.' }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'No se encontró Docker.' }
    $dockerCheck = & docker info --format '{{.ServerVersion}}' 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $dockerCheck) { throw 'Docker no está disponible o no está iniciado.' }

    $javaCandidates = [System.Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) { $javaCandidates.Add((Join-Path $env:JAVA_HOME 'bin/java.exe')) }
    if ($env:ProgramFiles) {
        foreach ($jdkRoot in @((Join-Path $env:ProgramFiles 'Java'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'))) {
            if (Test-Path $jdkRoot) {
                Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue |
                    Where-Object Name -Match '^jdk-(21|2[2-9])(?:\.|$)' |
                    ForEach-Object { $javaCandidates.Add((Join-Path $_.FullName 'bin/java.exe')) }
            }
        }
    }
    $pathJava = Get-Command java -ErrorAction SilentlyContinue
    if ($pathJava -and $pathJava.Source) { $javaCandidates.Add($pathJava.Source) }
    $javaExe = $null
    $javaHome = $null
    foreach ($candidate in ($javaCandidates | Select-Object -Unique)) {
        if (-not (Test-Path $candidate)) { continue }
        $versionText = (& $candidate -version 2>&1 | Out-String)
        $versionMatch = [regex]::Match($versionText, 'version "(?<major>\d+)')
        if ($versionMatch.Success -and [int]$versionMatch.Groups['major'].Value -ge 21) {
            $javaExe = $candidate
            $javaHome = Split-Path (Split-Path $candidate -Parent) -Parent
            break
        }
    }
    if (-not $javaExe) { throw 'Se requiere JDK 21 o posterior para el backend.' }
    if (-not (Test-Path (Join-Path $repo 'backend/mvnw.cmd'))) { throw 'No se encontró el Maven Wrapper del backend.' }

    Push-Location $frontend
    try {
        & $npm ls --depth=0 *> (Join-Path $tempDir 'npm-check.log')
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path 'node_modules/@playwright/test')) {
            & npm ci --no-audit --no-fund *> (Join-Path $tempDir 'npm-install.log')
            if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar las dependencias del frontend.' }
        }
        $browserPath = (& node --input-type=module -e "import { chromium } from '@playwright/test'; console.log(chromium.executablePath())").Trim()
        if (-not (Test-Path $browserPath)) {
            & npx playwright install chromium *> (Join-Path $tempDir 'browser-install.log')
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar Chromium para Playwright.' }
        }
    } finally { Pop-Location }

    $runId = [guid]::NewGuid().ToString('N').Substring(0, 8)
    $adminUser = "e2eadmin$runId"
    $password = 'E2e' + [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(12)) + '7'
    $dbUser = "e2e_$runId"
    $dbPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(18))
    $db = "e2e_$runId"
    $dbContainer = "migal-e2e-$PID"
    & docker run --detach --rm --name $dbContainer --env "POSTGRES_USER=$dbUser" --env "POSTGRES_PASSWORD=$dbPassword" --env "POSTGRES_DB=$db" --publish '127.0.0.1::5432' postgres:17.11-alpine *> $null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el PostgreSQL temporal.' }
    $dbStarted = $true
    $healthDeadline = (Get-Date).AddMinutes(2)
    do {
        & docker exec $dbContainer pg_isready -U $dbUser -d $db *> $null
        if ($LASTEXITCODE -eq 0) { break }
        if ((Get-Date) -ge $healthDeadline) { throw 'PostgreSQL temporal no alcanzó estado saludable.' }
        Start-Sleep -Seconds 2
    } while ($true)
    $portInfo = (& docker port $dbContainer '5432/tcp' | Select-Object -First 1)
    $portMatch = [regex]::Match($portInfo, '^127\.0\.0\.1:(?<port>\d+)$')
    if (-not $portMatch.Success) { throw 'No se pudo verificar que PostgreSQL esté publicado solo en loopback.' }
    $dbPort = [int]$portMatch.Groups['port'].Value
    $datasource = "jdbc:postgresql://127.0.0.1:$dbPort/$db"
    if ($datasource -notmatch '^jdbc:postgresql://127\.0\.0\.1:') { throw 'Destino de base de datos no local; se cancela por seguridad.' }

    $backendPort = Get-FreePort
    $frontendPort = Get-FreePort
    $backendUrl = "http://127.0.0.1:$backendPort"
    $frontendUrl = "http://127.0.0.1:$frontendPort"
    $emptyConfig = Join-Path $tempDir 'sin-configuracion-externa.properties'
    $emptyConfigUri = ([System.IO.Path]::GetFullPath($emptyConfig) -replace '\\','/')
    $documentDir = Join-Path $tempDir 'documentos'
    $null = New-Item -ItemType Directory -Force -Path $documentDir
    $backendEnv = @{
        JAVA_HOME = $javaHome; PATH = "$javaHome\bin;$env:PATH"
        SPRING_DATASOURCE_URL = $datasource; SPRING_DATASOURCE_USERNAME = $dbUser; SPRING_DATASOURCE_PASSWORD = $dbPassword
        SPRING_CONFIG_IMPORT = "optional:file:$emptyConfigUri"; SERVER_PORT = [string]$backendPort; SERVER_ADDRESS = '127.0.0.1'
        MIGAL_JWT_SECRETO = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(40))
        MIGAL_ADMIN_USERNAME = $adminUser; MIGAL_ADMIN_CLAVE = $password
        MIGAL_OCR_PROVEEDOR = 'simulado'; MIGAL_AGENTE_PROVEEDOR = 'simulado'
        MIGAL_DOCUMENTOS_DIRECTORIO = $documentDir
        FLYWAY_LOCATIONS = "filesystem:$($repo -replace '\\','/')/database/migraciones"
    }
    Set-ChildEnvironment $backendEnv
    $mvnw = Join-Path $repo 'backend/mvnw.cmd'
    $backendOut = Join-Path $tempDir 'backend.stdout.log'
    $backendErr = Join-Path $tempDir 'backend.stderr.log'
    $backendProcess = Start-Process -FilePath $mvnw -ArgumentList @('-q', 'spring-boot:run') -WorkingDirectory (Join-Path $repo 'backend') -PassThru -WindowStyle Hidden -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr
    Restore-ChildEnvironment
    Wait-Http "$backendUrl/actuator/health" 180 'backend'

    $adminReadyDeadline = (Get-Date).AddSeconds(45)
    do {
        $adminBody = @{ username = $adminUser; clave = $password } | ConvertTo-Json -Compress
        try {
            $session = Invoke-RestMethod -Uri "$backendUrl/api/v1/sesiones" -Method Post -ContentType 'application/json' -Body $adminBody -TimeoutSec 3
            if ($session.token) { break }
        } catch { }
        if ((Get-Date) -ge $adminReadyDeadline) { throw 'El backend inició, pero no quedó disponible el usuario de prueba.' }
        Start-Sleep -Milliseconds 750
    } while ($true)

    $seed = Join-Path $repo 'database/datos-demostracion/demo.sql'
    Get-Content -LiteralPath $seed -Raw | & docker exec -i $dbContainer psql -U $dbUser -d $db -v ON_ERROR_STOP=1 *> $null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar los datos sintéticos del contenedor temporal.' }

    Set-ChildEnvironment @{ VITE_API_REAL = 'true'; VITE_API_DESTINO = $backendUrl }
    $frontendOut = Join-Path $tempDir 'frontend.stdout.log'
    $frontendErr = Join-Path $tempDir 'frontend.stderr.log'
    $frontendProcess = Start-Process -FilePath $npm -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1', '--port', [string]$frontendPort, '--strictPort') -WorkingDirectory $frontend -PassThru -WindowStyle Hidden -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr
    Restore-ChildEnvironment
    Wait-Http $frontendUrl 90 'frontend'

    Set-ChildEnvironment @{
        E2E_REPORT_DIR = $reportDir; E2E_FRONTEND_URL = $frontendUrl; E2E_BACKEND_URL = $backendUrl
        E2E_RUN_ID = $runId; E2E_ADMIN_USER = $adminUser; E2E_ADMIN_PASSWORD = $password
    }
    Push-Location $frontend
    try {
        & npm run e2e *> (Join-Path $reportDir 'runner-output.tmp')
        $testExit = $LASTEXITCODE
        Sanitize-PlaywrightEvidence
        if ($testExit -ne 0) { $failure = 'Uno o más escenarios Playwright fallaron. Revise el reporte y las capturas.' }
    } finally {
        Pop-Location
        Restore-ChildEnvironment
        Remove-Item -LiteralPath (Join-Path $reportDir 'runner-output.tmp') -Force -ErrorAction SilentlyContinue
    }
} catch {
    $failure = $_.Exception.Message
} finally {
    Stop-ProcessTree $frontendProcess
    Stop-ProcessTree $backendProcess
    Restore-ChildEnvironment
    if ($dbStarted) {
        & docker stop $dbContainer *> $null
        if ($LASTEXITCODE -ne 0) { $cleanupFailure = 'No se pudo detener el contenedor PostgreSQL temporal; revise docker ps para cerrarlo manualmente.' }
    }
    $resolvedTemp = [System.IO.Path]::GetFullPath($tempDir).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    $resolvedTempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if ($resolvedTemp.StartsWith($resolvedTempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and (Split-Path $tempDir -Leaf) -like "migal-e2e-$runStamp-*") {
        Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Reports
}

Write-Host "Reporte local: $reportDir"
if ($failure) { Write-Error $failure; exit 1 }
if ($cleanupFailure) { Write-Error $cleanupFailure; exit 1 }
if ($scenarios | Where-Object Result -eq 'FALLÓ') { exit 1 }
exit 0
