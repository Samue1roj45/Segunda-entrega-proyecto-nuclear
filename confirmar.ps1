# Compila y prueba el backend; SOLO si todo pasa, hace commit con el mensaje de
# .mensaje_commit.txt y lo sube a la rama actual (feature/modulo-1).
$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
$log  = Join-Path $root 'confirmar.log'
$msg  = Join-Path $root '.mensaje_commit.txt'
Set-Location $root
"=== Confirmar $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File $log -Encoding utf8
function Log($m) { $m | Out-File $log -Append -Encoding utf8; Write-Host $m }
function Invocar-Git { $out = & git.exe @args 2>&1 | ForEach-Object { "$_" } | Out-String; $code = $LASTEXITCODE; Log "> git $($args -join ' ')"; if ($out.Trim()) { Log $out.TrimEnd() }; if ($code -ne 0) { throw "git $($args[0]) falló ($code)" } }

try {
    if (-not (Test-Path $msg)) { throw 'No hay .mensaje_commit.txt: nada que confirmar.' }
    $rama = (& git.exe rev-parse --abbrev-ref HEAD).Trim()
    if ($rama -eq 'main') { throw 'Estás en main: no se hace commit aquí.' }
    Log "Rama: $rama"

    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'compilar.ps1') -SinCursor | Out-Null
    $codigo = $LASTEXITCODE
    $resumen = (Get-Content (Join-Path $root 'compilar.log') | Where-Object { $_ -match 'Tests run:|BUILD|ERROR' } | Select-Object -Last 15) -join "`n"
    Log $resumen
    if ($codigo -ne 0) { throw "La compilación o las pruebas fallaron (código $codigo). No se hace commit." }

    Invocar-Git add -A
    Invocar-Git status --short
    Invocar-Git commit -F $msg
    Invocar-Git push -u origin $rama
    Invocar-Git log --oneline -1
    Remove-Item $msg
    Log '=== OK: commit subido ==='
} catch {
    Log "=== ERROR: $($_.Exception.Message) ==="
}
