<template>
  <section class="admin-page task-review-page">
    <AdminPageHeader eyebrow="运营管理" title="任务审核" description="审核社区任务内容，保留原有通过与驳回流程。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="fetchList">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <label class="filter-field">
          <span>状态</span>
          <select v-model="status">
            <option value="">全部</option>
            <option value="OPEN">待审核</option>
            <option value="CANCELLED">已拒绝</option>
          </select>
        </label>
        <label class="filter-field filter-field--search">
          <span>关键字</span>
          <input v-model="keyword" type="text" placeholder="标题 / 描述 / 地点" />
        </label>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="search">
          查询
        </button>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="resetFilters">
          重置
        </button>
      </template>
      <template #actions>
        <div class="toolbar-meta">
          <span class="meta-chip">每页 {{ size }} 条</span>
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
            :disabled="loading || !hasMore"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </template>
    </AdminToolbar>

    <AdminPanel title="待审核任务" description="聚焦任务基础信息、优先级与奖励积分。">
      <div class="table-shell">
        <table class="admin-data-table task-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>分类</th>
              <th>地点</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>优先级</th>
              <th>积分</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>
                <div class="item-title">
                  <strong>{{ item.title }}</strong>
                  <p v-if="item.description">{{ shortDescription(item.description) }}</p>
                </div>
              </td>
              <td>{{ item.categoryCode || '-' }}</td>
              <td>{{ item.locationText || '-' }}</td>
              <td>{{ formatDate(item.startTime) }}</td>
              <td>{{ formatDate(item.endTime) }}</td>
              <td>
                <span class="state-pill" :class="priorityClass(item.priority)">
                  {{ item.priority || '未设置' }}
                </span>
              </td>
              <td>{{ item.rewardPoints ?? 0 }}</td>
              <td>
                <span class="state-pill" :class="statusClass(item.status)">
                  {{ item.status || '未设置' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <template v-if="item.status === 'OPEN'">
                    <button
                      class="admin-button admin-button--primary admin-button--small"
                      type="button"
                      :disabled="loading"
                      @click="approve(item.id)"
                    >
                      通过
                    </button>
                    <button
                      class="admin-button admin-button--danger admin-button--small"
                      type="button"
                      :disabled="loading"
                      @click="reject(item.id)"
                    >
                      驳回
                    </button>
                  </template>
                  <span v-else class="handled-text">已处理</span>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && items.length === 0">
              <td colspan="9" class="empty-state">暂无待处理任务</td>
            </tr>
            <tr v-if="loading">
              <td colspan="9" class="empty-state">正在加载任务列表...</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AdminPanel>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminPageHeader from './admin/AdminPageHeader.vue'
import AdminPanel from './admin/AdminPanel.vue'
import AdminToolbar from './admin/AdminToolbar.vue'

type NeighborSupportTask = {
  id: number
  requesterUserId: number
  title: string
  categoryCode: string
  description: string
  locationText: string
  startTime: string
  endTime: string
  volunteerSlots: number
  filledSlots: number
  priority: string
  rewardPoints: number
  status: string
  createdAt: string
}

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const items = ref<NeighborSupportTask[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const hasMore = ref(false)
const status = ref('')
const keyword = ref('')

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

function shortDescription(value: string) {
  return value.length > 28 ? `${value.slice(0, 28)}...` : value
}

function statusClass(value?: string) {
  switch ((value || '').toUpperCase()) {
    case 'OPEN':
      return 'state-pill--warning'
    case 'CANCELLED':
      return 'state-pill--danger'
    default:
      return 'state-pill--neutral'
  }
}

function priorityClass(value?: string) {
  switch ((value || '').toUpperCase()) {
    case 'HIGH':
      return 'state-pill--danger'
    case 'MEDIUM':
      return 'state-pill--warning'
    case 'LOW':
      return 'state-pill--success'
    default:
      return 'state-pill--neutral'
  }
}

async function fetchList() {
  loading.value = true
  try {
    const searchParams = new URLSearchParams()
    if (status.value) searchParams.set('status', status.value)
    if (keyword.value) searchParams.set('keyword', keyword.value)
    searchParams.set('page', String(page.value))
    searchParams.set('size', String(size.value))

    const response = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews?${searchParams.toString()}`)
    const data = await response.json()
    const list: NeighborSupportTask[] =
      response.ok && data?.code === 200 && Array.isArray(data?.data) ? data.data : []
    items.value = list
    hasMore.value = list.length >= size.value
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchList()
}

function resetFilters() {
  status.value = ''
  keyword.value = ''
  page.value = 1
  fetchList()
}

async function approve(id: number) {
  if (!window.confirm('确认审核通过并发布该任务吗？')) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews/${id}/approve`, {
      method: 'POST',
    })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '审批失败')
    }
    window.alert('任务审核通过')
    fetchList()
  } catch (error: any) {
    window.alert(error?.message || '审批失败')
  } finally {
    loading.value = false
  }
}

async function reject(id: number) {
  if (!window.confirm('确认驳回该任务吗？')) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews/${id}/reject`, {
      method: 'POST',
    })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '驳回失败')
    }
    window.alert('已驳回该任务')
    fetchList()
  } catch (error: any) {
    window.alert(error?.message || '驳回失败')
  } finally {
    loading.value = false
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
.task-review-page {
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

.filter-field select,
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
  min-width: 260px;
}

.filter-field select:focus,
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

.task-table {
  min-width: 1100px;
}

.item-title strong {
  display: block;
  color: var(--admin-text-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.item-title p {
  margin: 4px 0 0;
  color: var(--admin-text-muted);
  font-size: 12px;
  line-height: 1.4;
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

.handled-text {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.empty-state {
  padding: 28px 12px;
  text-align: center;
  color: var(--admin-text-muted);
}
</style>
