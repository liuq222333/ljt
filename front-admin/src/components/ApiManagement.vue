<template>
  <section class="admin-page api-management-page">
    <AdminPageHeader eyebrow="运营管理" title="接口管理" description="维护资源路由、HTTP 方法、启停状态与接口描述。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="fetchList">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
        <button class="admin-button admin-button--primary" type="button" @click="openModal()">
          新增接口
        </button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <label class="filter-field filter-field--search">
          <span>搜索</span>
          <input v-model="keyword" type="text" placeholder="按资源、路径或描述检索" />
        </label>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="search">
          查询
        </button>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="resetFilters">
          重置
        </button>
        <span class="meta-chip">共 {{ items.length }} 条</span>
        <span class="meta-chip">启用 {{ enabledCount }}</span>
        <span class="meta-chip">停用 {{ disabledCount }}</span>
      </template>
      <template #actions>
        <div class="toolbar-meta">
          <span class="meta-chip">第 {{ page }} 页</span>
          <button
            class="admin-button admin-button--secondary admin-button--small"
            type="button"
            :disabled="loading || page <= 1"
            @click="prevPage"
          >
            上一页
          </button>
          <button
            class="admin-button admin-button--secondary admin-button--small"
            type="button"
            :disabled="loading || items.length < size"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </template>
    </AdminToolbar>

    <AdminPanel title="接口路由清单" description="支持编辑路由、配置查询体和切换启停状态。">
      <div class="table-shell">
        <table class="admin-data-table api-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>资源</th>
              <th>动作</th>
              <th>方法</th>
              <th>路径模板</th>
              <th>操作类型</th>
              <th>描述</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.resource }}</td>
              <td>{{ item.action }}</td>
              <td>
                <span class="state-pill" :class="methodClass(item.httpMethod)">
                  {{ item.httpMethod }}
                </span>
              </td>
              <td class="path-cell">{{ item.pathTemplate }}</td>
              <td>{{ item.operationType || '-' }}</td>
              <td class="desc-cell">{{ item.description || '-' }}</td>
              <td>
                <span class="state-pill" :class="item.enabled === 1 ? 'state-pill--success' : 'state-pill--neutral'">
                  {{ item.enabled === 1 ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTimestamp(item.updatedAt || item.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button
                    class="admin-button admin-button--secondary admin-button--small"
                    type="button"
                    @click="openModal(item)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="item.enabled === 1"
                    class="admin-button admin-button--warning admin-button--small"
                    type="button"
                    @click="toggleStatus(item, 0)"
                  >
                    停用
                  </button>
                  <button
                    v-else
                    class="admin-button admin-button--success admin-button--small"
                    type="button"
                    @click="toggleStatus(item, 1)"
                  >
                    启用
                  </button>
                  <button
                    class="admin-button admin-button--danger admin-button--small"
                    type="button"
                    @click="deleteItem(item)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && items.length === 0">
              <td colspan="10" class="empty-state">暂无接口配置</td>
            </tr>
            <tr v-if="loading">
              <td colspan="10" class="empty-state">正在加载接口数据...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AdminPanel>

    <div v-if="showModal" class="dialog-mask" @click.self="closeModal">
      <div class="dialog admin-panel">
        <div class="dialog__header">
          <div>
            <p class="dialog__eyebrow">接口编辑</p>
            <h3>{{ isEdit ? '编辑接口' : '新增接口' }}</h3>
          </div>
          <button class="dialog__close" type="button" :disabled="saving" @click="closeModal">×</button>
        </div>

        <div class="dialog__body">
          <div class="form-grid">
            <label class="form-field">
              <span>资源</span>
              <input v-model="form.resource" type="text" placeholder="例如 user" />
            </label>
            <label class="form-field">
              <span>动作</span>
              <input v-model="form.action" type="text" placeholder="例如 create" />
            </label>
            <label class="form-field">
              <span>HTTP 方法</span>
              <select v-model="form.httpMethod">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="PATCH">PATCH</option>
              </select>
            </label>
            <label class="form-field">
              <span>操作类型</span>
              <input v-model="form.operationType" type="text" placeholder="例如 READ / WRITE" />
            </label>
            <label class="form-field form-field--full">
              <span>路径模板</span>
              <input v-model="form.pathTemplate" type="text" placeholder="例如 /api/users/{id}" />
            </label>
            <label class="form-field form-field--full">
              <span>描述</span>
              <input v-model="form.description" type="text" placeholder="简要说明该接口用途" />
            </label>
            <label class="form-field form-field--full">
              <span>Query Schema（JSON）</span>
              <textarea v-model="form.querySchema" rows="4" placeholder='{"keyword":"string"}'></textarea>
            </label>
            <label class="form-field form-field--full">
              <span>Body Schema（JSON）</span>
              <textarea v-model="form.bodySchema" rows="5" placeholder='{"name":"string"}'></textarea>
            </label>
          </div>
        </div>

        <div class="dialog__footer">
          <button class="admin-button admin-button--secondary" type="button" :disabled="saving" @click="closeModal">
            取消
          </button>
          <button class="admin-button admin-button--primary" type="button" :disabled="saving" @click="submitForm">
            {{ saving ? '保存中...' : '保存接口' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminPageHeader from './admin/AdminPageHeader.vue'
import AdminPanel from './admin/AdminPanel.vue'
import AdminToolbar from './admin/AdminToolbar.vue'

type ApiRoute = {
  id?: number
  resource: string
  action: string
  httpMethod: string
  pathTemplate: string
  pathParams?: string
  operationType?: string
  description?: string
  enabled?: number
  querySchema?: string
  bodySchema?: string
  createdAt?: string
  updatedAt?: string
}

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const items = ref<ApiRoute[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const showModal = ref(false)
const isEdit = ref(false)

const createEmptyForm = (): ApiRoute => ({
  resource: '',
  action: '',
  httpMethod: 'GET',
  pathTemplate: '',
  pathParams: '',
  operationType: '',
  description: '',
  enabled: 1,
  querySchema: '',
  bodySchema: '',
})

const form = reactive<ApiRoute>(createEmptyForm())

const enabledCount = computed(() => items.value.filter((item) => item.enabled === 1).length)
const disabledCount = computed(() => items.value.filter((item) => item.enabled !== 1).length)

function methodClass(method?: string) {
  switch ((method || '').toUpperCase()) {
    case 'GET':
      return 'state-pill--info'
    case 'POST':
      return 'state-pill--success'
    case 'PUT':
      return 'state-pill--warning'
    case 'DELETE':
      return 'state-pill--danger'
    default:
      return 'state-pill--neutral'
  }
}

function formatTimestamp(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

function applyForm(payload?: ApiRoute) {
  Object.assign(form, createEmptyForm(), payload ?? {})
}

async function fetchList() {
  loading.value = true
  try {
    const searchParams = new URLSearchParams()
    if (keyword.value) searchParams.set('keyword', keyword.value)
    searchParams.set('page', String(page.value))
    searchParams.set('size', String(size.value))

    const response = await fetch(`${API_BASE}/api/admin/api-management/list?${searchParams.toString()}`)
    const data = await response.json()
    items.value = response.ok && data?.code === 200 && Array.isArray(data?.data) ? data.data : []
  } catch (error) {
    console.error(error)
    window.alert('加载接口列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchList()
}

function resetFilters() {
  keyword.value = ''
  page.value = 1
  fetchList()
}

function openModal(item?: ApiRoute) {
  isEdit.value = Boolean(item)
  applyForm(item)
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitForm() {
  saving.value = true
  try {
    const url = isEdit.value ? `${API_BASE}/api/admin/api-management/update` : `${API_BASE}/api/admin/api-management/add`
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '保存失败')
    }
    window.alert('接口保存成功')
    closeModal()
    fetchList()
  } catch (error: any) {
    window.alert(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item: ApiRoute, status: number) {
  const action = status === 1 ? 'enable' : 'disable'
  const actionLabel = status === 1 ? '启用' : '停用'
  if (!window.confirm(`确认要${actionLabel}接口 ${item.resource}/${item.action} 吗？`)) return

  try {
    const response = await fetch(`${API_BASE}/api/admin/api-management/${action}/${item.id}`, { method: 'POST' })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '状态切换失败')
    }
    fetchList()
  } catch (error: any) {
    window.alert(error?.message || '状态切换失败')
  }
}

async function deleteItem(item: ApiRoute) {
  if (!window.confirm(`确认删除接口 ${item.resource}/${item.action} 吗？删除后不可恢复。`)) return

  try {
    const response = await fetch(`${API_BASE}/api/admin/api-management/delete/${item.id}`, { method: 'POST' })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '删除失败')
    }
    fetchList()
  } catch (error: any) {
    window.alert(error?.message || '删除失败')
  }
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1
    fetchList()
  }
}

function nextPage() {
  page.value += 1
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.api-management-page {
  gap: 12px;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.filter-field span {
  white-space: nowrap;
}

.filter-field input {
  min-width: 0;
  height: 32px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  padding: 0 10px;
  font-size: 13px;
  outline: none;
}

.filter-field--search {
  min-width: 280px;
}

.filter-field input:focus {
  border-color: var(--admin-border-strong);
  box-shadow: 0 0 0 3px rgba(37, 50, 68, 0.08);
}

.toolbar-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  color: var(--admin-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.admin-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button:hover {
  border-color: var(--admin-border-strong);
}

.admin-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.admin-button--primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #fff;
}

.admin-button--secondary {
  background: var(--admin-bg-subtle);
}

.admin-button--success {
  border-color: #b8dfcb;
  background: #ebf8f1;
  color: var(--admin-success);
}

.admin-button--warning {
  border-color: #edd8a5;
  background: #fcf8ec;
  color: var(--admin-warning);
}

.admin-button--danger {
  border-color: #e5b3ab;
  background: #fbeeed;
  color: var(--admin-danger);
}

.admin-button--small {
  height: 30px;
  padding: 0 10px;
}

.table-shell {
  overflow: auto;
}

.api-table {
  min-width: 1320px;
}

.path-cell {
  min-width: 220px;
  color: var(--admin-accent);
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
  word-break: break-all;
}

.desc-cell {
  min-width: 220px;
  line-height: 1.5;
}

.state-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.state-pill--neutral {
  background: var(--admin-accent-soft);
  border-color: #d9e1ec;
  color: var(--admin-accent);
}

.state-pill--info {
  background: #eef4fb;
  border-color: #c8d7eb;
  color: #315d87;
}

.state-pill--warning {
  background: #fcf8ec;
  border-color: #edd8a5;
  color: var(--admin-warning);
}

.state-pill--success {
  background: #ebf8f1;
  border-color: #b8dfcb;
  color: var(--admin-success);
}

.state-pill--danger {
  background: #fbeeed;
  border-color: #efc3bc;
  color: var(--admin-danger);
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-state {
  padding: 28px 12px;
  text-align: center;
  color: var(--admin-text-muted);
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.36);
}

.dialog {
  width: min(960px, 100%);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--admin-border);
}

.dialog__eyebrow {
  margin: 0 0 4px;
  color: var(--admin-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.dialog__header h3 {
  margin: 0;
  color: var(--admin-text-primary);
  font-size: 18px;
}

.dialog__close {
  border: none;
  background: transparent;
  color: var(--admin-text-muted);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.dialog__body {
  padding: 16px;
  overflow: auto;
}

.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid var(--admin-border);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
}

.form-field textarea {
  resize: vertical;
  min-height: 108px;
  line-height: 1.55;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  border-color: var(--admin-border-strong);
  box-shadow: 0 0 0 3px rgba(37, 50, 68, 0.08);
}
</style>
