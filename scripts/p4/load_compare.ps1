param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [int]$Rounds = 20,
    [ValidateSet("submitTurn", "chooseOption")]
    [string]$Scenario = "submitTurn",
    [string]$Failpoint = "submitTurn.beforeOrchestrator",
    [string]$Difficulty = "SURVIVOR"
)

$ErrorActionPreference = "Stop"

function New-TraceId {
    return "bench_" + [guid]::NewGuid().ToString("N")
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [object]$Body
    )

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        if ($null -ne $Body) {
            $json = $Body | ConvertTo-Json -Depth 8
            $resp = Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -ContentType "application/json" -Body $json
        } else {
            $resp = Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers
        }
        $sw.Stop()
        return [pscustomobject]@{
            ok = $true
            costMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            http = 200
            body = $resp
        }
    } catch {
        $sw.Stop()
        $status = 0
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        return [pscustomobject]@{
            ok = $false
            costMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            http = $status
            body = $null
        }
    }
}

function Run-OneRound {
    param([string]$Mode, [string]$FailpointValue)

    $trace = New-TraceId
    $commonHeaders = @{ "X-Trace-Id" = $trace }

    $createResp = Invoke-Api -Method "POST" -Url "$BaseUrl/game/sessions" -Headers $commonHeaders -Body @{
        playerId = "bench_user"
        difficulty = $Difficulty
        worldVersion = "world_v1"
        styleProfile = "grim_realism"
    }

    if (-not $createResp.ok) {
        return [pscustomobject]@{ mode = $Mode; step = "createSession"; ok = $false; http = $createResp.http; costMs = $createResp.costMs }
    }

    $sessionId = $createResp.body.data.sessionId
    $version = $createResp.body.data.initialState.version

    $turnHeaders = @{
        "X-Trace-Id" = $trace
        "Idempotency-Key" = [guid]::NewGuid().ToString("N")
    }
    if ($FailpointValue) {
        $turnHeaders["X-Doomsday-Failpoint"] = $FailpointValue
    }

    $turnResp = Invoke-Api -Method "POST" -Url "$BaseUrl/game/sessions/$sessionId/turns" -Headers $turnHeaders -Body @{
        expectedVersion = $version
        playerInput = "我沿着墙角谨慎推进并搜集药品"
        clientTime = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    }

    if ($Scenario -eq "submitTurn") {
        return [pscustomobject]@{
            mode = $Mode
            step = "submitTurn"
            ok = $turnResp.ok
            http = $turnResp.http
            costMs = $turnResp.costMs
        }
    }

    if (-not $turnResp.ok) {
        return [pscustomobject]@{ mode = $Mode; step = "submitTurn"; ok = $false; http = $turnResp.http; costMs = $turnResp.costMs }
    }

    $turn = $turnResp.body.data.turn
    $nextVersion = $turnResp.body.data.newVersion
    $chooseHeaders = @{ "X-Trace-Id" = $trace }
    if ($FailpointValue) {
        $chooseHeaders["X-Doomsday-Failpoint"] = $FailpointValue
    }

    $chooseResp = Invoke-Api -Method "POST" -Url "$BaseUrl/game/sessions/$sessionId/turns/$turn/choose" -Headers $chooseHeaders -Body @{
        expectedVersion = $nextVersion
        optionId = "opt_b"
    }

    return [pscustomobject]@{
        mode = $Mode
        step = "chooseOption"
        ok = $chooseResp.ok
        http = $chooseResp.http
        costMs = $chooseResp.costMs
    }
}

function Summarize {
    param([string]$Mode, [array]$Rows)

    if (-not $Rows -or $Rows.Count -eq 0) {
        return [pscustomobject]@{ mode = $Mode; total = 0; success = 0; successRate = 0; avgMs = 0; p95Ms = 0 }
    }

    $latencies = $Rows | ForEach-Object { $_.costMs } | Sort-Object
    $total = $Rows.Count
    $success = ($Rows | Where-Object { $_.ok }).Count
    $avg = [math]::Round((($latencies | Measure-Object -Average).Average), 2)
    $idx = [math]::Max(0, [math]::Ceiling($latencies.Count * 0.95) - 1)
    $p95 = [math]::Round($latencies[$idx], 2)

    return [pscustomobject]@{
        mode = $Mode
        total = $total
        success = $success
        successRate = [math]::Round(($success * 100.0 / $total), 2)
        avgMs = $avg
        p95Ms = $p95
    }
}

Write-Host "[P4] running normal flow scenario=$Scenario rounds=$Rounds"
$normalRows = @()
for ($i = 1; $i -le $Rounds; $i++) {
    $normalRows += Run-OneRound -Mode "normal" -FailpointValue ""
}

Write-Host "[P4] running failpoint flow scenario=$Scenario rounds=$Rounds failpoint=$Failpoint"
$failRows = @()
for ($i = 1; $i -le $Rounds; $i++) {
    $failRows += Run-OneRound -Mode "failpoint" -FailpointValue $Failpoint
}

$normalSummary = Summarize -Mode "normal" -Rows $normalRows
$failSummary = Summarize -Mode "failpoint" -Rows $failRows

Write-Host "\n=== Round Details (tail 5 normal / failpoint) ==="
$normalRows | Select-Object -Last 5 | Format-Table -AutoSize
$failRows | Select-Object -Last 5 | Format-Table -AutoSize

Write-Host "\n=== Summary ==="
@($normalSummary, $failSummary) | Format-Table -AutoSize

Write-Host "\nTips:"
Write-Host "1) 若 failpoint 模式仍 100% 成功，请确认后端已开启 game.chaos.enabled=true。"
Write-Host "2) submitTurn 场景建议 failpoint=submitTurn.beforeOrchestrator 或 submitTurn.afterOrchestrator。"
Write-Host "3) chooseOption 场景建议 failpoint=chooseOption.beforeApply。"
