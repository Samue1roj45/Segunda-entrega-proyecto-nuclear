# Abre el proyecto en Cursor y compila/prueba el backend con Maven.
# -SinCursor: solo compila (lo usa confirmar.ps1).
param([switch]$SinCursor)
# El resultado queda en compilar.log (en esta misma carpeta).
$root = $PSScriptRoot
$log  = Join-Path $root 'compilar.log'
"=== Compilación $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File $log -Encoding utf8

function Log($m) { $m | Out-File $log -Append -Encoding utf8; Write-Host $m }

# 1. Abrir la carpeta en Cursor
if (-not $SinCursor) {
$cursor = Get-Command cursor -ErrorAction SilentlyContinue
if (-not $cursor) {
    $cand = Join-Path $env:LOCALAPPDATA 'Programs\cursor\Cursor.exe'
    if (Test-Path $cand) { Start-Process $cand -ArgumentList "`"$root`"" ; Log "Cursor: abierto ($cand)" }
    else { Log 'Cursor: no encontrado' }
} else { Start-Process $cursor.Source -ArgumentList "`"$root`"" -WindowStyle Hidden; Log "Cursor: abierto ($($cursor.Source))" }
}

# 2. Buscar un JDK 17 o superior
function VersionJava($exe) {
    $v = & $exe -version 2>&1 | Out-String
    if ($v -match 'version "(\d+)') { return [int]$Matches[1] } return 0
}
$java = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe") -and (VersionJava "$env:JAVA_HOME\bin\java.exe") -ge 17) { $java = $env:JAVA_HOME }
if (-not $java) {
    $dirs = @()
    foreach ($b in @("$HOME\.jdks", 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft', 'C:\Program Files\Amazon Corretto')) {
        if (Test-Path $b) { $dirs += Get-ChildItem $b -Directory }
    }
    foreach ($d in ($dirs | Sort-Object Name -Descending)) {
        $exe = Join-Path $d.FullName 'bin\java.exe'
        if ((Test-Path $exe) -and (VersionJava $exe) -ge 17) { $java = $d.FullName; break }
    }
}
if (-not $java) { Log 'ERROR: no se encontró un JDK 17 o superior.'; exit 1 }
$env:JAVA_HOME = $java
Log "JDK: $java (Java $(VersionJava "$java\bin\java.exe"))"

# 3. Buscar Maven (PATH, IntelliJ, Toolbox o distribuciones del wrapper ya descargadas)
$mvn = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $patrones = @(
        'C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd',
        "$env:LOCALAPPDATA\Programs\IntelliJ*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\*\*\*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\*\*\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
    foreach ($p in $patrones) { $m = Get-ChildItem $p -ErrorAction SilentlyContinue | Select-Object -First 1; if ($m) { $mvn = $m.FullName; break } }
}
if (-not $mvn -and (Test-Path "$HOME\.m2\wrapper\dists")) {
    $m = Get-ChildItem "$HOME\.m2\wrapper\dists" -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($m) { $mvn = $m.FullName }
}
if (-not $mvn) { Log 'ERROR: no se encontró Maven (mvn.cmd).'; exit 1 }
Log "Maven: $mvn"

# 4. Compilar y ejecutar las pruebas
& $mvn -f (Join-Path $root 'backend\pom.xml') -B verify 2>&1 | ForEach-Object { "$_" } | Out-File $log -Append -Encoding utf8
$codigo = $LASTEXITCODE
Log "=== Fin (código $codigo) ==="
exit $codigo
