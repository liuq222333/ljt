$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$base = 'http://127.0.0.1:8080/api/admin/launch'
$outPath = 'D:\code\aaaaljt\.codex\tmp\launch-w14-20260412-local-rehearsal.json'

function Get-Launch([string]$path) {
    Invoke-RestMethod -Uri ($base + $path) -Method Get
}

function Post-Launch([string]$path, $body) {
    $json = $body | ConvertTo-Json -Depth 30 -Compress
    Invoke-RestMethod -Uri ($base + $path) -Method Post -ContentType 'application/json; charset=utf-8' -Body $json
}

function Capture-Step([string]$name, [scriptblock]$action) {
    try {
        $value = & $action
        return [ordered]@{
            ok = $true
            value = $value
        }
    } catch {
        return [ordered]@{
            ok = $false
            error = $_.Exception.Message
        }
    }
}

$result = [ordered]@{}
$result.startedAt = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$result.readinessBefore = Capture-Step 'readinessBefore' { Get-Launch '/readiness/overview?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24' }
$result.gatewayConfig = Capture-Step 'gatewayConfig' { Get-Launch '/realtime/gateway-config' }
$result.checklistBefore = Capture-Step 'checklistBefore' { Get-Launch '/checklist?maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24' }

$window = Capture-Step 'createWindow' {
    Post-Launch '/windows/create' ([ordered]@{
        windowName = 'w14-local-rehearsal-20260412'
        operator = 'codex'
        notes = 'Local W14 rehearsal window for launch artifact generation.'
    })
}
$result.createWindow = $window

$result.smokeRun = Capture-Step 'smokeRun' {
    Post-Launch '/smoke/run' ([ordered]@{
        detailLimit = 10
        taskLimit = 10
        maxDegradedRate = 0.2
        minEvalPassRate = 1
        maxRunAgeHours = 24
        realtimeRequest = [ordered]@{
            entityType = 'product'
            entityIds = @(283)
            queryType = 'availability'
            userId = 'launch-smoke'
        }
    })
}

$result.loadTestRun = Capture-Step 'loadTestRun' {
    Post-Launch '/load-tests/run' ([ordered]@{
        name = 'w14-local-load-test-20260412'
        environment = 'local'
        operator = 'codex'
        iterations = 3
        concurrency = 2
        maxDegradedRate = 0.2
        realtimeRequest = [ordered]@{
            entityType = 'product'
            entityIds = @(283)
            queryType = 'availability'
            userId = 'launch-load'
        }
    })
}

$result.drillRun = Capture-Step 'drillRun' {
    Post-Launch '/drills/run' ([ordered]@{
        name = 'w14-realtime-fallback-drill-20260412'
        environment = 'local'
        operator = 'codex'
        drillType = 'realtime_fallback'
        target = 'realtime'
        durationSeconds = 15
        notes = 'Force fallback and verify circuit recovery in local environment.'
        detailLimit = 10
        taskLimit = 10
        maxDegradedRate = 0.2
        minEvalPassRate = 1
        maxRunAgeHours = 24
        realtimeRequest = [ordered]@{
            entityType = 'product'
            entityIds = @(283)
            queryType = 'availability'
            userId = 'launch-drill'
        }
    })
}

$result.checklistSnapshotCreate = Capture-Step 'checklistSnapshotCreate' {
    Post-Launch '/checklist-snapshots/create' ([ordered]@{
        name = 'w14-local-checklist-20260412'
        environment = 'local'
        operator = 'codex'
        summary = 'Local launch checklist snapshot generated after rehearsal run.'
        notes = 'Snapshot reflects current local blockers and available evidence.'
        maxDegradedRate = 0.2
        minEvalPassRate = 1
        maxRunAgeHours = 24
    })
}

$dependencyBodies = @(
    [ordered]@{ dependencyName = 'python-sidecar'; operator = 'codex'; ready = $true; notes = '127.0.0.1:9001 is reachable and Python agent sidecar smoke passed.' },
    [ordered]@{ dependencyName = 'spring-backend'; operator = 'codex'; ready = $true; notes = '127.0.0.1:8080 admin and agent APIs are reachable.' },
    [ordered]@{ dependencyName = 'internal-realtime-gateway'; operator = 'codex'; ready = $true; notes = 'Gateway health is UP, product provider ready, circuit CLOSED.' },
    [ordered]@{ dependencyName = 'elasticsearch-sync-baseline'; operator = 'codex'; ready = $false; notes = 'Readiness overview reports failed to read ES alias product_search_read.' },
    [ordered]@{ dependencyName = 'governance-preflight-freshness'; operator = 'codex'; ready = $false; notes = 'Latest bound regression run is stale in readiness overview.' },
    [ordered]@{ dependencyName = 'rollback-candidate-index'; operator = 'codex'; ready = $false; notes = 'No usable rollback candidate index is available in local environment.' },
    [ordered]@{ dependencyName = 'rabbitmq-local'; operator = 'codex'; ready = $false; notes = 'Backend log shows localhost:5672 connection refused during local startup.' }
)
$result.dependencyRecords = @()
foreach ($body in $dependencyBodies) {
    $result.dependencyRecords += ,(Capture-Step ('dependency-' + $body.dependencyName) { Post-Launch '/dependencies/record' $body })
}

$signoffBodies = @(
    [ordered]@{ signoffRole = 'backend'; operator = 'codex'; approved = $true; notes = 'Java backend, Python sidecar, and launch APIs passed local smoke.' },
    [ordered]@{ signoffRole = 'governance'; operator = 'codex'; approved = $false; notes = 'Governance preflight is stale and checklist is not green.' },
    [ordered]@{ signoffRole = 'release-manager'; operator = 'codex'; approved = $false; notes = 'Overall readiness remains blocked by sync baseline, rollback, and governance freshness.' }
)
$result.signoffRecords = @()
foreach ($body in $signoffBodies) {
    $result.signoffRecords += ,(Capture-Step ('signoff-' + $body.signoffRole) { Post-Launch '/signoffs/record' $body })
}

if ($window.ok -and $window.value -and $window.value.data -and $window.value.data.id) {
    $windowId = [string]$window.value.data.id
    $result.closeWindow = Capture-Step 'closeWindow' {
        Post-Launch '/windows/close' ([ordered]@{
            id = $windowId
            status = 'closed'
            notes = 'Local W14 rehearsal window closed after artifact generation.'
        })
    }
} else {
    $result.closeWindow = [ordered]@{ ok = $false; error = 'window id unavailable' }
}

$result.finalSummary = Capture-Step 'finalSummary' { Get-Launch '/final-summary?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24&loadTestFreshnessHours=72&drillFreshnessHours=72&checklistFreshnessHours=24' }
$result.handoffSummary = Capture-Step 'handoffSummary' { Get-Launch '/handoff-summary?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24' }
$result.timeline = Capture-Step 'timeline' { Get-Launch '/timeline?limit=30' }
$result.exportPackage = Capture-Step 'exportPackage' { Get-Launch '/export/package?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24&timelineLimit=30' }
$result.listLoadTests = Capture-Step 'listLoadTests' { Get-Launch '/load-tests/list?limit=5' }
$result.listDrills = Capture-Step 'listDrills' { Get-Launch '/drills/list?limit=5' }
$result.listChecklistSnapshots = Capture-Step 'listChecklistSnapshots' { Get-Launch '/checklist-snapshots/list?limit=5' }
$result.listDependencies = Capture-Step 'listDependencies' { Get-Launch '/dependencies/list?limit=10' }
$result.listSignoffs = Capture-Step 'listSignoffs' { Get-Launch '/signoffs/list?limit=10' }
$result.listWindows = Capture-Step 'listWindows' { Get-Launch '/windows/list?limit=5' }
$result.listCloseoutsBeforeCreate = Capture-Step 'listCloseoutsBeforeCreate' { Get-Launch '/closeouts/list?limit=5' }

$result.closeoutCreate = Capture-Step 'closeoutCreate' {
    Post-Launch '/closeouts/create' ([ordered]@{
        name = 'w14-local-closeout-20260412'
        environment = 'local'
        operator = 'codex'
        notes = 'Local launch rehearsal package generated after smoke, load, drill, dependency, and signoff recording.'
        detailLimit = 10
        taskLimit = 10
        maxDegradedRate = 0.2
        minEvalPassRate = 1
        maxRunAgeHours = 24
        timelineLimit = 30
    })
}

$result.listCloseoutsAfterCreate = Capture-Step 'listCloseoutsAfterCreate' { Get-Launch '/closeouts/list?limit=5' }
$result.finalSummaryAfterCloseout = Capture-Step 'finalSummaryAfterCloseout' { Get-Launch '/final-summary?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24&loadTestFreshnessHours=72&drillFreshnessHours=72&checklistFreshnessHours=24' }
$result.handoffSummaryAfterCloseout = Capture-Step 'handoffSummaryAfterCloseout' { Get-Launch '/handoff-summary?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&minEvalPassRate=1&maxRunAgeHours=24' }
$result.finishedAt = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

$result | ConvertTo-Json -Depth 50 | Set-Content -Path $outPath -Encoding UTF8
$result | ConvertTo-Json -Depth 12
