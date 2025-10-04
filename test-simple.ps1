Write-Host "=== PROBANDO API DE TRANSACCIONES BANCARIAS ===" -ForegroundColor Green


Write-Host ""

# 1. Crear dos cuentas de ejemplo controladas
Write-Host "1. Creando cuentas de ejemplo..." -ForegroundColor Yellow
try {
    $source = Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method POST -ContentType "application/json" -Body '{"owner":"Source User","initialBalance":2000.00}'
    $target = Invoke-RestMethod -Uri "http://localhost:8080/accounts" -Method POST -ContentType "application/json" -Body '{"owner":"Target User","initialBalance":0.00}'
    Write-Host "✅ Cuentas creadas" -ForegroundColor Green
    $source | ConvertTo-Json -Depth 2
    $target | ConvertTo-Json -Depth 2
    $sourceId = $source.data.id
    $targetId = $target.data.id
    $initialSource = [decimal]$source.data.balance
    $initialTarget = [decimal]$target.data.balance
    Start-Sleep -Milliseconds 200
} catch {
    Write-Host "❌ Error creando cuentas: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Asegúrate de tener la app corriendo: ./gradlew bootRun" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 2. Parámetros de la prueba de concurrencia y expectativas
Write-Host "2. Preparando prueba de concurrencia..." -ForegroundColor Yellow
$numRequests = 10000
$amountPerTx = [decimal]1.00

# Éxitos esperados: limitado por el saldo inicial de la cuenta origen
$maxPossible = [int]([math]::Floor($initialSource / $amountPerTx))
$expectedSuccess = [math]::Min($numRequests, $maxPossible)
$expectedFailure = $numRequests - $expectedSuccess

$expectedSourceFinal = $initialSource - ($expectedSuccess * $amountPerTx)
$expectedTargetFinal = $initialTarget + ($expectedSuccess * $amountPerTx)

Write-Host ("Se enviarán {0} transferencias concurrentes de {1} desde la cuenta {2} hacia la cuenta {3}." -f $numRequests, $amountPerTx, $sourceId, $targetId) -ForegroundColor Yellow
Write-Host ("Éxitos esperados: {0}, Fallos esperados: {1}" -f $expectedSuccess, $expectedFailure) -ForegroundColor Yellow
Write-Host ("Saldos esperados -> Source: {0}, Target: {1}" -f $expectedSourceFinal, $expectedTargetFinal) -ForegroundColor Yellow

Write-Host ""

# 3. Probar concurrencia: 1000 peticiones simultáneas
Write-Host "3. Probando concurrencia (10000 peticiones simultáneas)..." -ForegroundColor Yellow
try {
    # Asegurar que el ensamblado de System.Net.Http esté cargado en Windows PowerShell
    try { Add-Type -AssemblyName System.Net.Http -ErrorAction Stop } catch {}

    [System.Net.ServicePointManager]::DefaultConnectionLimit = 20000

    $baseUrl = "http://localhost:8080"
    $endpoint = "$baseUrl/transactions/transfer"

    $payload = @{ fromAccountId = $sourceId; toAccountId = $targetId; amount = $amountPerTx } | ConvertTo-Json -Compress

    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(120)

    $tasks = New-Object 'System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]'

    for ($i = 0; $i -lt $numRequests; $i++) {
        $content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
        $tasks.Add($client.PostAsync($endpoint, $content))
    }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $responses = [System.Threading.Tasks.Task]::WhenAll($tasks).GetAwaiter().GetResult()
    $sw.Stop()

    # Nota: /transactions/transfer retorna 202 (Accepted) por procesamiento asíncrono.
    # Estos 'éxitos' representan solicitudes aceptadas, no transferencias completadas.
    $accepted = ($responses | Where-Object { $_.IsSuccessStatusCode }).Count
    $failedHttp = $numRequests - $accepted

    Write-Host "✅ Envío concurrente completado en $($sw.ElapsedMilliseconds) ms" -ForegroundColor Green
    Write-Host "HTTP -> Aceptadas: $accepted, Rechazadas: $failedHttp de $numRequests" -ForegroundColor Yellow

    $client.Dispose()
} catch {
    Write-Host "❌ Error en concurrencia: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. Verificar saldos finales vs esperados (esperar un momento a que finalicen tareas asíncronas)
Write-Host "4. Verificando saldos finales y resultados de transacciones..." -ForegroundColor Yellow
try {
    # Esperar activamente a que COMPLETED + FAILED == numRequests o timeout
    $timeoutSec = 60
    $elapsed = 0
    $lastTotal = -1
    do {
        Start-Sleep -Seconds 1
        $completed = Invoke-RestMethod -Uri "http://localhost:8080/transactions/status/COMPLETED" -Method GET
        $failedTx = Invoke-RestMethod -Uri "http://localhost:8080/transactions/status/FAILED" -Method GET
        $completedCount = ($completed.data | Measure-Object).Count
        $failedCount = ($failedTx.data | Measure-Object).Count
        $total = $completedCount + $failedCount
        $elapsed++
        if ($total -ne $lastTotal) {
            $lastTotal = $total
            $elapsed = 0  # reinicia timeout si hay progreso
        }
    } while (($total -lt $numRequests) -and ($elapsed -lt $timeoutSec))
    $sourceAfter = Invoke-RestMethod -Uri ("http://localhost:8080/accounts/{0}" -f $sourceId) -Method GET
    $targetAfter = Invoke-RestMethod -Uri ("http://localhost:8080/accounts/{0}" -f $targetId) -Method GET
    Write-Host ("Source actual: {0} | esperado: {1}" -f $sourceAfter.data.balance, $expectedSourceFinal) -ForegroundColor Yellow
    Write-Host ("Target actual: {0} | esperado: {1}" -f $targetAfter.data.balance, $expectedTargetFinal) -ForegroundColor Yellow

    Write-Host ("Transacciones -> COMPLETED: {0}, FAILED: {1} (esperado éxito: {2})" -f $completedCount, $failedCount, $expectedSuccess) -ForegroundColor Yellow
    Write-Host "✅ Verificación completada (revisa que saldos y conteos coincidan con lo esperado)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error verificando saldos: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "=== PRUEBAS COMPLETADAS ===" -ForegroundColor Green
Write-Host "Revisa los logs de la aplicación para ver el manejo de concurrencia" -ForegroundColor Yellow
