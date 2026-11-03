# Confirma los commits pendientes, en orden:
#   1) el de .mensaje_commit.txt (cambios ya presentes en la carpeta), si existe;
#   2) cada carpeta de .cola\NN_nombre (copia sus archivos sobre el proyecto y usa su mensaje.txt).
# En cada uno compila y prueba el backend; SOLO si todo pasa hace commit y push. Si algo falla, se detiene.
$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
$log  = Join-Path $root 'confirmar.log'
$msg  = Join-Path $root '.mensaje_commit.txt'
$cola = Join-Path $root '.cola'
Set-Location $root
"=== Confirmar $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File $log -Encoding utf8
function Log($m) { $m | Out-File $log -Append -Encoding utf8; Write-Host $m }
function Invocar-Git { $out = & git.exe @args 2>&1 | ForEach-Object { "$_" } | Out-String; $code = $LASTEXITCODE; Log "> git $($args -join ' ')"; if ($out.Trim()) { Log $out.TrimEnd() }; if ($code -ne 0) { throw "git $($args[0]) falló ($code)" } }

function Confirmar-Pendiente {
    $rama = (& git.exe rev-parse --abbrev-ref HEAD).Trim()
    if ($rama -eq 'main') { throw 'Estás en main: no se hace commit aquí.' }
    Log "--- Commit en rama $rama"
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'compilar.ps1') -SinCursor | Out-Null
    $codigo = $LASTEXITCODE
    $resumen = (Get-Content (Join-Path $root 'compilar.log') | Where-Object { $_ -match 'Tests run:|BUILD|ERROR' } | Select-Object -Last 15) -join "`n"
    Log $resumen
    if ($codigo -ne 0) { throw "La compilación o las pruebas fallaron (código $codigo). No se hace commit." }
    Invocar-Git add -A
    if (-not (& git.exe status --porcelain)) { Log 'Sin cambios: este commit ya estaba aplicado, se omite.'; Remove-Item $msg; return }
    Invocar-Git commit -F $msg
    Invocar-Git push -u origin $rama
    Invocar-Git log --oneline -1
    Remove-Item $msg
}

try {
    if (Test-Path $msg) { Confirmar-Pendiente }
    if (Test-Path $cola) {
        foreach ($item in (Get-ChildItem $cola -Directory | Sort-Object Name)) {
            Log "=== Cola: $($item.Name)"
            $archivos = Join-Path $item.FullName 'archivos'
            if (Test-Path $archivos) { Copy-Item -Path (Join-Path $archivos '*') -Destination $root -Recurse -Force }
            Copy-Item (Join-Path $item.FullName 'mensaje.txt') $msg -Force
            Confirmar-Pendiente
            Remove-Item $item.FullName -Recurse -Force
        }
    }
    Log '=== OK: todos los commits pendientes fueron subidos ==='
} catch {
    Log "=== ERROR: $($_.Exception.Message) ==="
}
