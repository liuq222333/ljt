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
$versions = @(Get-Data '/api/admin/governance/eval-case-versions/list?page=1&size=5')
$regressionSets = @(Get-Data '/api/admin/governance/regression-sets/list?page=1&size=5')
$grayConfigs = @(Get-Data '/api/admin/governance/gray-configs/list?page=1&size=5')
if ($versions.Count -eq 0 -or $regressionSets.Count -eq 0 -or $grayConfigs.Count -eq 0) { throw '缺少种子治理数据，无法继续联调 smoke' }
$versionRun = Post-Data "/api/admin/governance/eval-case-versions/$($versions[0].id)/run?limit=1" $null
$regressionRun = Post-Data "/api/admin/governance/regression-sets/$($regressionSets[0].id)/run?limit=1" $null
$releaseName = 'governance-live-release-' + [DateTime]::Now.ToString('yyyyMMddHHmmss')
$release = Post-Data '/api/admin/governance/release-records/add' @{
  releaseName = $releaseName
  targetScope = 'governance/live-smoke'
  evalCaseVersionId = $versions[0].id
  regressionSetId = $regressionSets[0].id
  notes = 'live smoke release'
}
$releaseUpdated = Post-Data '/api/admin/governance/release-records/update' @{
  id = $release.id
  releaseName = $release.releaseName
  targetScope = 'governance/live-smoke'
  evalCaseVersionId = $versions[0].id
  regressionSetId = $regressionSets[0].id
  notes = 'live smoke release updated'
}
$releaseEval = Post-Data "/api/admin/governance/release-records/$($release.id)/run-eval?limit=1&setAsBaseline=true" $null
$releaseAfterEval = Get-Data "/api/admin/governance/release-records/$($release.id)"
$verification = Get-Data "/api/admin/governance/release-records/$($release.id)/verification?minEvalPassRate=0.5&maxRunAgeHours=24"
$applied = Post-Data "/api/admin/governance/release-records/$($release.id)/apply-gray-config/$($grayConfigs[0].id)" $null
$ready = Post-Data "/api/admin/governance/release-records/$($release.id)/transition?targetStatus=ready&minEvalPassRate=0.5&maxRunAgeHours=24" $null
$gray = Post-Data "/api/admin/governance/release-records/$($release.id)/transition?targetStatus=gray&grayConfigId=$($grayConfigs[0].id)&minEvalPassRate=0.5&maxRunAgeHours=24" $null
$released = Post-Data "/api/admin/governance/release-records/$($release.id)/transition?targetStatus=released&grayConfigId=$($grayConfigs[0].id)&minEvalPassRate=0.5&maxRunAgeHours=24" $null
$events = @(Get-Data "/api/admin/governance/release-records/$($release.id)/events")
[ordered]@{
  versionRunId = $versionRun.id
  regressionRunId = $regressionRun.id
  releaseId = $release.id
  latestEvalRunId = $releaseAfterEval.latestEvalRunId
  baselineEvalRunId = $releaseAfterEval.baselineEvalRunId
  verificationReady = $verification.ready
  finalReleaseStatus = $released.releaseStatus
  releaseEventCount = $events.Count
  smoke = 'ok'
} | ConvertTo-Json -Depth 8
