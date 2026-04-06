<template>
  <section class="admin-page launch-center">
    <AdminPageHeader eyebrow="????" title="?????" description="??????????Smoke?????????">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" @click="copyCurrentLink">
          ????
        </button>
        <button class="admin-button admin-button--secondary" type="button" @click="exportHandoffSummary">
          ????
        </button>
        <button class="admin-button admin-button--secondary" type="button" @click="exportLaunchPackage">
          ?????
        </button>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="refreshAll">
          {{ loading ? '???...' : '????' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminStateBlock v-if="error" tone="danger" :message="error" />

    <AdminToolbar>
      <template #filters>
        <div class="launch-summary-badges">
          <AdminStatusBadge
            :tone="readiness.overallReady ? 'success' : 'warning'"
            :label="readiness.overallReady ? '?????' : '?????'"
          />
          <AdminStatusBadge
            :tone="finalSummary.finalReady ? 'success' : 'warning'"
            :label="finalSummary.finalReady ? '?????' : '??????'"
          />
          <AdminStatusBadge tone="neutral" :label="`?? ${latestLoadTestStatus}`" />
          <AdminStatusBadge tone="neutral" :label="`?? ${latestDrillStatus}`" />
        </div>
      </template>
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" @click="loadTimeline">
          ?????
        </button>
      </template>
    </AdminToolbar>

    <section class="admin-workbench">
      <div class="admin-workbench__main">
        <AdminPanel title="????">
          <ul class="admin-summary">
            <li v-for="(value, key) in readiness.gates || {}" :key="String(key)">
              <span>{{ key }}</span>
              <strong>{{ yesNo(value) }}</strong>
            </li>
            <li v-if="!Object.keys(readiness.gates || {}).length">
              <span>???????</span>
              <strong>-</strong>
            </li>
          </ul>
        </AdminPanel>

        <AdminPanel title="Checklist">
          <div class="admin-list">
            <article v-for="item in checklist" :key="item.id" class="admin-list-item checklist-row">
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.detail || '-' }}</p>
              </div>
              <span class="check-badge" :class="item.ready ? 'check-badge--good' : 'check-badge--warn'">
                {{ item.ready ? 'PASS' : 'TODO' }}
              </span>
            </article>
            <div v-if="!checklist.length" class="admin-empty">?????</div>
          </div>
        </AdminPanel>

        <AdminPanel title="Smoke" description="?????? Smoke ?????">
          <template #actions>
            <button class="admin-button admin-button--primary" type="button" :disabled="smokeLoading" @click="runSmoke">
              {{ smokeLoading ? '???...' : '?? Smoke' }}
            </button>
          </template>
          <div class="form-grid">
            <label>
              <span>Entity ID</span>
              <input v-model.number="smokeEntityId" type="number" min="1" />
            </label>
            <label>
              <span>Query Type</span>
              <input v-model="smokeQueryType" type="text" />
            </label>
          </div>
          <pre class="json-block">{{ pretty(smokeResult) }}</pre>
        </AdminPanel>

        <AdminPanel title="Realtime ??" description="????? fallback ???">
          <template #actions>
            <button class="admin-button admin-button--primary" type="button" :disabled="chaosLoading" @click="forceFallback">
              {{ chaosLoading ? '???...' : '????' }}
            </button>
            <button class="admin-button admin-button--secondary" type="button" @click="recoverFallback">
              ??
            </button>
          </template>
          <div class="form-grid">
            <label>
              <span>????</span>
              <input v-model.number="chaosDurationSeconds" type="number" min="1" />
            </label>
            <label>
              <span>??</span>
              <input v-model="chaosReason" type="text" />
            </label>
          </div>
          <pre class="json-block">{{ pretty(chaosResult) }}</pre>
        </AdminPanel>

        <AdminPanel title="???">
          <template #actions>
            <button class="admin-button admin-button--secondary" type="button" @click="loadTimeline">
              ??
            </button>
          </template>
          <div class="admin-list">
            <article v-for="item in timeline" :key="`${item.type}-${item.id}`" class="admin-list-item checklist-row">
              <div>
                <strong>{{ item.title || `${item.type}-${item.id}` }}</strong>
                <p>{{ item.summary || '-' }}</p>
                <p>{{ item.timestamp || '-' }}</p>
              </div>
              <span class="check-badge" :class="badgeClass(item.status)">{{ item.status || 'unknown' }}</span>
            </article>
            <div v-if="!timeline.length" class="admin-empty">???????</div>
          </div>
        </AdminPanel>

        <section class="launch-record-grid">
          <AdminPanel title="????">
            <template #actions>
              <button class="admin-button admin-button--primary" type="button" @click="submitLoadTest">
                ??
              </button>
            </template>
            <div class="form-grid">
              <label>
                <span>??</span>
                <input v-model="loadTestForm.name" type="text" />
              </label>
              <label>
                <span>??</span>
                <input v-model="loadTestForm.environment" type="text" />
              </label>
              <label>
                <span>???</span>
                <input v-model="loadTestForm.operator" type="text" />
              </label>
              <label>
                <span>QPS</span>
                <input v-model.number="loadTestForm.qps" type="number" min="0" />
              </label>
              <label>
                <span>P95 (ms)</span>
                <input v-model.number="loadTestForm.p95Ms" type="number" min="0" />
              </label>
              <label class="checkbox-field">
                <input v-model="loadTestForm.passed" type="checkbox" />
                <span>??</span>
              </label>
            </div>
          </AdminPanel>

          <AdminPanel title="????">
            <template #actions>
              <button class="admin-button admin-button--primary" type="button" @click="submitDrill">
                ??
              </button>
            </template>
            <div class="form-grid">
              <label>
                <span>??</span>
                <input v-model="drillForm.name" type="text" />
              </label>
              <label>
                <span>??</span>
                <input v-model="drillForm.environment" type="text" />
              </label>
              <label>
                <span>???</span>
                <input v-model="drillForm.operator" type="text" />
              </label>
              <label>
                <span>????</span>
                <input v-model="drillForm.drillType" type="text" />
              </label>
              <label>
                <span>??</span>
                <input v-model="drillForm.target" type="text" />
              </label>
              <label class="checkbox-field">
                <input v-model="drillForm.passed" type="checkbox" />
                <span>??</span>
              </label>
            </div>
          </AdminPanel>

          <AdminPanel title="????">
            <template #actions>
              <button class="admin-button admin-button--primary" type="button" @click="submitSignoff">
                ??
              </button>
            </template>
            <div class="form-grid">
              <label>
                <span>??</span>
                <input v-model="signoffForm.signoffRole" type="text" />
              </label>
              <label>
                <span>???</span>
                <input v-model="signoffForm.operator" type="text" />
              </label>
              <label class="launch-record-grid__wide">
                <span>??</span>
                <input v-model="signoffForm.notes" type="text" />
              </label>
              <label class="checkbox-field">
                <input v-model="signoffForm.approved" type="checkbox" />
                <span>???</span>
              </label>
            </div>
          </AdminPanel>

          <AdminPanel title="????">
            <template #actions>
              <button class="admin-button admin-button--primary" type="button" @click="submitDependencyCheck">
                ??
              </button>
            </template>
            <div class="form-grid">
              <label>
                <span>???</span>
                <input v-model="dependencyForm.dependencyName" type="text" />
              </label>
              <label>
                <span>???</span>
                <input v-model="dependencyForm.operator" type="text" />
              </label>
              <label class="launch-record-grid__wide">
                <span>??</span>
                <input v-model="dependencyForm.notes" type="text" />
              </label>
              <label class="checkbox-field">
                <input v-model="dependencyForm.ready" type="checkbox" />
                <span>???</span>
              </label>
            </div>
          </AdminPanel>

          <AdminPanel class="launch-record-grid__wide-panel" title="????">
            <template #actions>
              <div class="panel-actions-inline">
                <button class="admin-button admin-button--primary" type="button" @click="submitLaunchWindow">
                  ????
                </button>
                <button class="admin-button admin-button--secondary" type="button" @click="closeCurrentLaunchWindow">
                  ????
                </button>
              </div>
            </template>
            <div class="form-grid">
              <label>
                <span>????</span>
                <input v-model="launchWindowForm.windowName" type="text" />
              </label>
              <label>
                <span>???</span>
                <input v-model="launchWindowForm.operator" type="text" />
              </label>
              <label>
                <span>????</span>
                <input v-model="launchWindowForm.startAt" type="text" placeholder="yyyy-MM-dd HH:mm:ss" />
              </label>
              <label>
                <span>??</span>
                <input v-model="launchWindowForm.notes" type="text" />
              </label>
            </div>
          </AdminPanel>
        </section>
      </div>

      <aside class="admin-workbench__side">
        <AdminPanel title="????">
          <template #actions>
            <button class="admin-button admin-button--secondary" type="button" @click="loadFinalSummary">
              ??
            </button>
          </template>
          <ul class="admin-summary">
            <li><span>????</span><strong>{{ yesNo(finalSummary.finalReady) }}</strong></li>
            <li><span>????</span><strong>{{ yesNo(finalSummary.gates?.readinessGate) }}</strong></li>
            <li><span>?????</span><strong>{{ yesNo(finalSummary.gates?.loadTestReady) }}</strong></li>
            <li><span>?????</span><strong>{{ yesNo(finalSummary.gates?.drillReady) }}</strong></li>
            <li><span>?????</span><strong>{{ yesNo(finalSummary.gates?.checklistReady) }}</strong></li>
          </ul>
        </AdminPanel>

        <AdminPanel title="????">
          <template #actions>
            <button class="admin-button admin-button--secondary" type="button" @click="loadHandoffSummary">
              ??
            </button>
          </template>
          <ul class="admin-summary">
            <li><span>???</span><strong>{{ yesNo(handoffSummary.finalReady) }}</strong></li>
            <li><span>????</span><strong>{{ latestArtifactId('loadTest') }}</strong></li>
            <li><span>????</span><strong>{{ latestArtifactId('drill') }}</strong></li>
            <li><span>????</span><strong>{{ latestArtifactId('checklistSnapshot') }}</strong></li>
          </ul>
          <div class="admin-list">
            <article v-for="item in handoffSummary.ownerActions || []" :key="item" class="admin-list-item">
              <strong>{{ item }}</strong>
            </article>
            <div v-if="!(handoffSummary.ownerActions || []).length" class="admin-empty">??????</div>
          </div>
        </AdminPanel>

        <AdminPanel title="Runbook ??">
          <template #actions>
            <button class="admin-button admin-button--secondary" type="button" @click="loadRunbookBundle">
              ??
            </button>
          </template>
          <div class="doc-list">
            <article v-for="(doc, key) in runbookBundle.documents || {}" :key="key" class="doc-item">
              <strong>{{ doc.title || key }}</strong>
              <p>{{ doc.path || '-' }}</p>
              <pre class="doc-content">{{ doc.content || '????' }}</pre>
            </article>
            <div v-if="!Object.keys(runbookBundle.documents || {}).length" class="admin-empty">?? Runbook ??</div>
          </div>
        </AdminPanel>
      </aside>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { downloadJsonFile } from '../../utils/governanceFile'
import {
  closeLaunchWindow,
  createLaunchWindow,
  fetchLaunchChecklist,
  fetchLaunchFinalSummary,
  fetchLaunchHandoffSummary,
  fetchLaunchPackage,
  fetchLaunchReadiness,
  fetchLaunchRunbookBundle,
  fetchLaunchTimeline,
  forceLaunchRealtimeFallback,
  listLaunchDrills,
  listLaunchLoadTests,
  listLaunchWindows,
  recordLaunchDependencyCheck,
  recordLaunchDrill,
  recordLaunchLoadTest,
  recordLaunchSignoff,
  recoverLaunchRealtimeFallback,
  runLaunchSmoke,
  type LaunchChecklistItem,
  type LaunchFinalSummary,
  type LaunchHandoffSummary,
  type LaunchPackage,
  type LaunchReadinessResult,
  type LaunchRecord,
  type LaunchRunbookBundle,
  type LaunchTimelineItem,
} from '../../api/adminLaunch'
import AdminPageHeader from '../admin/AdminPageHeader.vue'
import AdminPanel from '../admin/AdminPanel.vue'
import AdminStateBlock from '../admin/AdminStateBlock.vue'
import AdminStatusBadge from '../admin/AdminStatusBadge.vue'
import AdminToolbar from '../admin/AdminToolbar.vue'

const loading = ref(false)
const smokeLoading = ref(false)
const chaosLoading = ref(false)
const error = ref('')

const readiness = ref<LaunchReadinessResult>({})
const checklist = ref<LaunchChecklistItem[]>([])
const finalSummary = ref<LaunchFinalSummary>({})
const handoffSummary = ref<LaunchHandoffSummary>({})
const runbookBundle = ref<LaunchRunbookBundle>({})
const launchPackage = ref<LaunchPackage>({})
const timeline = ref<LaunchTimelineItem[]>([])
const loadTests = ref<LaunchRecord[]>([])
const drills = ref<LaunchRecord[]>([])
const smokeResult = ref<Record<string, any>>({})
const chaosResult = ref<Record<string, any>>({})

const smokeEntityId = ref(1)
const smokeQueryType = ref('availability')
const chaosDurationSeconds = ref(60)
const chaosReason = ref('w14_launch_drill')
const loadTestForm = ref({
  name: 'launch-load-test',
  environment: 'staging',
  operator: 'admin',
  qps: 50,
  p95Ms: 1200,
  passed: true,
})
const drillForm = ref({
  name: 'launch-drill',
  environment: 'staging',
  operator: 'admin',
  drillType: 'realtime_gateway_failure',
  target: 'realtime',
  passed: true,
})
const signoffForm = ref({
  signoffRole: 'qa',
  operator: 'admin',
  approved: true,
  notes: '',
})
const dependencyForm = ref({
  dependencyName: 'realtime-gateway',
  operator: 'admin',
  ready: true,
  notes: '',
})
const launchWindowForm = ref({
  windowName: 'go-live-window',
  operator: 'release-manager',
  startAt: '',
  notes: '',
})

const latestLoadTest = computed(() => loadTests.value[0] || finalSummary.value.latestLoadTest)
const latestDrill = computed(() => drills.value[0] || finalSummary.value.latestDrill)
const latestLoadTestStatus = computed(() => (latestLoadTest.value?.passed ? '??' : latestLoadTest.value ? '???' : '??'))
const latestDrillStatus = computed(() => (latestDrill.value?.passed ? '??' : latestDrill.value ? '???' : '??'))

function yesNo(value: unknown) {
  return value ? '?' : '?'
}

function latestArtifactId(key: 'loadTest' | 'drill' | 'checklistSnapshot') {
  return ((handoffSummary.value.latestArtifacts || {})[key] || {}).id || '??'
}

function pretty(value: unknown) {
  return JSON.stringify(value || {}, null, 2)
}

function badgeClass(status?: string) {
  const normalized = String(status || '').toLowerCase()
  if (['passed', 'ready', 'released', 'gray', 'success', 'open', 'approved'].includes(normalized)) return 'check-badge--good'
  if (['failed', 'warning', 'degraded', 'pending', 'blocked', 'closed', 'rejected'].includes(normalized)) return 'check-badge--warn'
  return 'check-badge--info'
}

async function copyCurrentLink() {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(window.location.href)
      return
    }
    error.value = '???????????'
  } catch {
    error.value = '??????'
  }
}

function exportHandoffSummary() {
  downloadJsonFile(`launch-handoff-${Date.now()}.json`, handoffSummary.value)
}

function exportLaunchPackage() {
  downloadJsonFile(`launch-package-${Date.now()}.json`, launchPackage.value)
}

async function loadReadiness() {
  readiness.value = await fetchLaunchReadiness()
}

async function loadChecklist() {
  checklist.value = await fetchLaunchChecklist()
}

async function loadFinalSummary() {
  finalSummary.value = await fetchLaunchFinalSummary()
}

async function loadHandoffSummary() {
  handoffSummary.value = await fetchLaunchHandoffSummary()
}

async function loadRunbookBundle() {
  runbookBundle.value = await fetchLaunchRunbookBundle()
}

async function loadTimeline() {
  timeline.value = await fetchLaunchTimeline(20)
}

async function loadLaunchPackage() {
  launchPackage.value = await fetchLaunchPackage()
}

async function loadLoadTests() {
  loadTests.value = await listLaunchLoadTests(10)
}

async function loadDrills() {
  drills.value = await listLaunchDrills(10)
}

async function refreshAll() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([
      loadReadiness(),
      loadChecklist(),
      loadFinalSummary(),
      loadHandoffSummary(),
      loadRunbookBundle(),
      loadTimeline(),
      loadLaunchPackage(),
      loadLoadTests(),
      loadDrills(),
    ])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '?????????'
  } finally {
    loading.value = false
  }
}

async function runSmoke() {
  smokeLoading.value = true
  error.value = ''
  try {
    smokeResult.value = await runLaunchSmoke({
      realtimeRequest: {
        entityIds: [smokeEntityId.value],
        queryType: smokeQueryType.value,
      },
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Smoke ????'
  } finally {
    smokeLoading.value = false
  }
}

async function forceFallback() {
  chaosLoading.value = true
  error.value = ''
  try {
    chaosResult.value = await forceLaunchRealtimeFallback({
      durationSeconds: chaosDurationSeconds.value,
      reason: chaosReason.value,
      realtimeRequest: {
        entityIds: [smokeEntityId.value],
        queryType: smokeQueryType.value,
      },
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Fallback ????'
  } finally {
    chaosLoading.value = false
  }
}

async function recoverFallback() {
  error.value = ''
  try {
    chaosResult.value = await recoverLaunchRealtimeFallback()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Fallback ????'
  }
}

async function submitLoadTest() {
  error.value = ''
  try {
    await recordLaunchLoadTest({ ...loadTestForm.value })
    await Promise.all([
      loadLoadTests(),
      loadFinalSummary(),
      loadHandoffSummary(),
      loadTimeline(),
      loadLaunchPackage(),
    ])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '????????'
  }
}

async function submitDrill() {
  error.value = ''
  try {
    await recordLaunchDrill({ ...drillForm.value })
    await Promise.all([
      loadDrills(),
      loadFinalSummary(),
      loadHandoffSummary(),
      loadTimeline(),
      loadLaunchPackage(),
    ])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '????????'
  }
}

async function submitSignoff() {
  error.value = ''
  try {
    await recordLaunchSignoff({ ...signoffForm.value })
    await Promise.all([loadHandoffSummary(), loadTimeline(), loadLaunchPackage()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '??????'
  }
}

async function submitDependencyCheck() {
  error.value = ''
  try {
    await recordLaunchDependencyCheck({ ...dependencyForm.value })
    await Promise.all([loadHandoffSummary(), loadTimeline(), loadLaunchPackage()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '????????'
  }
}

async function submitLaunchWindow() {
  error.value = ''
  try {
    await createLaunchWindow({ ...launchWindowForm.value })
    await Promise.all([loadHandoffSummary(), loadTimeline(), loadLaunchPackage()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '????????'
  }
}

async function closeCurrentLaunchWindow() {
  error.value = ''
  try {
    const windows = await listLaunchWindows(10)
    const openWindow = windows.find((item) => String(item.status || '').toLowerCase() === 'open')
    if (!openWindow?.id) {
      error.value = '????????????'
      return
    }
    await closeLaunchWindow({ id: openWindow.id, status: 'closed' })
    await Promise.all([loadHandoffSummary(), loadTimeline(), loadLaunchPackage()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '????????'
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.launch-center {
  gap: 12px;
}

.admin-button {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button--secondary {
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
}

.admin-button--secondary:hover {
  border-color: var(--admin-border-strong);
  color: var(--admin-text-primary);
}

.admin-button--primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #ffffff;
}

.admin-button--primary:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.launch-summary-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.admin-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
  gap: 12px;
}

.admin-workbench__main,
.admin-workbench__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.launch-record-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.launch-record-grid__wide {
  grid-column: 1 / -1;
}

.launch-record-grid__wide-panel {
  grid-column: 1 / -1;
}

.admin-summary {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-summary li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 9px 10px;
}

.admin-summary span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.admin-summary strong {
  color: var(--admin-text-primary);
  font-size: 13px;
}

.admin-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-list-item {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 10px;
}

.admin-list-item strong {
  display: block;
  color: var(--admin-text-primary);
  font-size: 13px;
}

.admin-list-item p {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.checklist-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.checklist-row > div {
  min-width: 0;
}

.check-badge {
  border-radius: var(--admin-radius-control);
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.check-badge--good {
  background: #eaf6ef;
  color: #1f7a4d;
}

.check-badge--warn {
  background: #fcf3e5;
  color: #9b6811;
}

.check-badge--info {
  background: #eaf0f8;
  color: #2d5887;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.form-grid input {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 8px 10px;
  font-size: 12px;
  background: var(--admin-bg-surface);
}

.checkbox-field {
  flex-direction: row !important;
  align-items: center;
  gap: 8px;
}

.json-block {
  margin: 8px 0 0;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: #101922;
  color: #e5edf5;
  padding: 8px;
  max-height: 220px;
  overflow: auto;
  font-size: 12px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 10px;
}

.doc-item p {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.doc-content {
  margin: 8px 0 0;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  padding: 8px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  font-size: 12px;
  color: var(--admin-text-secondary);
}

.panel-actions-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-empty {
  border: 1px dashed var(--admin-border);
  border-radius: var(--admin-radius-control);
  padding: 10px;
  color: var(--admin-text-muted);
  font-size: 12px;
  text-align: center;
}
</style>
