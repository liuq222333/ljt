$ErrorActionPreference = 'Stop'
function Get-Data($path) {
  $resp = Invoke-RestMethod -Uri ("http://127.0.0.1:8080" + $path) -Method Get
  if ($resp.code -ne 200) { throw "GET $path failed: $($resp.message)" }
  return $resp.data
}
function Post-Data($path, $body) {
  $json = if ($null -eq $body) { '{}' } else { $body | ConvertTo-Json -Depth 12 -Compress }
  $resp = Invoke-RestMethod -Uri ("http://127.0.0.1:8080" + $path) -Method Post -ContentType 'application/json' -Body $json
  if ($resp.code -ne 200) { throw "POST $path failed: $($resp.message)" }
  return $resp.data
}
$dashboard = Get-Data '/api/admin/governance/dashboard'
$metricsDaily = @(Get-Data '/api/admin/governance/metrics/daily/list?page=1&size=7')
$errorTrend = @(Get-Data '/api/admin/governance/metrics/error-attribution/trend?days=7')
$replayList = @(Get-Data '/api/admin/governance/replay/list?limit=10')
$candidates = @(Get-Data '/api/admin/governance/replay/candidates?days=7&limit=10&problematicOnly=true&excludeExistingQuery=true')
$evalStats = Get-Data '/api/admin/governance/eval-cases/stats'
$evalCases = @(Get-Data '/api/admin/governance/eval-cases/list?page=1&size=10')
$versions = @(Get-Data '/api/admin/governance/eval-case-versions/list?page=1&size=10')
$evalRuns = @(Get-Data '/api/admin/governance/eval-runs/list?page=1&size=10')
$regressionSets = @(Get-Data '/api/admin/governance/regression-sets/list?page=1&size=10')
$releaseRecords = @(Get-Data '/api/admin/governance/release-records/list?page=1&size=10')
$grayConfigs = @(Get-Data '/api/admin/governance/gray-configs/list?page=1&size=10')
if ($replayList.Count -gt 0) {
  $requestId = [uri]::EscapeDataString($replayList[0].requestId)
  $traceId = [uri]::EscapeDataString($replayList[0].traceId)
  $sessionId = [uri]::EscapeDataString($replayList[0].sessionId)
  $null = Get-Data ("/api/admin/governance/replay/request/$requestId")
  $null = Get-Data ("/api/admin/governance/replay/trace/$traceId")
  $null = Get-Data ("/api/admin/governance/replay/session/$sessionId")
}
if ($versions.Count -gt 0) {
  $versionId = $versions[0].id
  $null = Get-Data ("/api/admin/governance/eval-case-versions/$versionId")
  if ($versions.Count -gt 1) {
    $null = Get-Data ("/api/admin/governance/eval-case-versions/compare?baseVersionId=$($versions[1].id)&targetVersionId=$versionId")
  }
}
if ($evalRuns.Count -gt 0) {
  $runId = $evalRuns[0].id
  $null = Get-Data ("/api/admin/governance/eval-runs/$runId")
  if ($evalRuns.Count -gt 1) {
    $null = Get-Data ("/api/admin/governance/eval-runs/compare?baseRunId=$($evalRuns[1].id)&targetRunId=$runId")
  }
}
if ($regressionSets.Count -gt 0) {
  $null = Get-Data ("/api/admin/governance/regression-sets/$($regressionSets[0].id)")
}
if ($releaseRecords.Count -gt 0) {
  $releaseId = $releaseRecords[0].id
  $null = Get-Data ("/api/admin/governance/release-records/$releaseId")
  $null = Get-Data ("/api/admin/governance/release-records/$releaseId/verification?minEvalPassRate=1&maxRunAgeHours=24")
  $null = Get-Data ("/api/admin/governance/release/preflight?detailLimit=10&taskLimit=10&maxDegradedRate=0.2&evalCaseVersionId=$($releaseRecords[0].evalCaseVersionId)&regressionSetId=$($releaseRecords[0].regressionSetId)&minEvalPassRate=1&maxRunAgeHours=24")
  $null = Get-Data ("/api/admin/governance/release-records/$releaseId/events")
}
if ($grayConfigs.Count -gt 0) {
  $null = Get-Data ("/api/admin/governance/gray-configs/$($grayConfigs[0].id)")
}
$smokeSuffix = [DateTime]::Now.ToString('yyyyMMddHHmmss')
$createdCase = Post-Data '/api/admin/governance/eval-cases/add' @{
  caseName = "governance-live-smoke-$smokeSuffix"
  queryText = "治理联调烟测样本 $smokeSuffix"
  bucket = 'golden'
  riskLevel = 'low'
  enabled = 1
  expectedTaskType = 'governance_replay'
  expectedPlanType = 'replay_diagnosis'
  expectedAnswerType = 'diagnostic_summary'
  notes = 'live smoke'
}
$null = Post-Data "/api/admin/governance/eval-cases/delete/$($createdCase.id)" $null
$createdGray = Post-Data '/api/admin/governance/gray-configs/add' @{
  configName = "governance-live-gray-$smokeSuffix"
  queryBucket = 'golden'
  trafficPercent = 5
  riskLevel = 'low'
  enabled = 1
  targetVersionJson = '{"agent":"live-smoke"}'
  notes = 'live smoke'
}
$null = Post-Data "/api/admin/governance/gray-configs/delete/$($createdGray.id)" $null
[ordered]@{
  dashboardStage = $dashboard.overview.stage
  metricsDailyCount = $metricsDaily.Count
  errorTrendCount = $errorTrend.Count
  replayCount = $replayList.Count
  replayCandidateCount = $candidates.Count
  evalCaseCount = $evalCases.Count
  evalVersionCount = $versions.Count
  evalRunCount = $evalRuns.Count
  regressionSetCount = $regressionSets.Count
  releaseRecordCount = $releaseRecords.Count
  grayConfigCount = $grayConfigs.Count
  writeSmoke = 'ok'
} | ConvertTo-Json -Depth 6
