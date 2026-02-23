<template>
  <div class="page">
    <div class="toolbar">
      <div class="filters">
        <label>
          状态
          <select v-model="status">
            <option value="">全部</option>
            <option value="OPEN">待审核</option>
            <option value="CANCELLED">已拒绝</option>
          </select>
        </label>
        <label>
          关键字
          <input v-model="keyword" type="text" placeholder="标题/描述/地点" />
        </label>
        <button class="primary" :disabled="loading" @click="fetchList">查询</button>
      </div>
      <div class="pager">
        <button class="ghost" :disabled="loading || page<=1" @click="prevPage">上一页</button>
        <span>第 {{ page }} 页</span>
        <button class="ghost" :disabled="loading || !hasMore" @click="nextPage">下一页</button>
      </div>
    </div>

    <div class="card">
      <table class="table">
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
            <th style="width: 180px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <div class="title">{{ item.title }}</div>
              <div class="subtitle" v-if="item.description">{{ item.description.substring(0, 20) }}...</div>
            </td>
            <td>{{ item.categoryCode }}</td>
            <td>{{ item.locationText }}</td>
            <td>{{ fmt(item.startTime) }}</td>
            <td>{{ fmt(item.endTime) }}</td>
            <td>
               <span class="badge" :class="['p-'+(item.priority||'').toLowerCase()]">{{ item.priority }}</span>
            </td>
            <td>{{ item.rewardPoints }}</td>
            <td>
              <span class="badge" :class="['s-'+(item.status||'').toLowerCase()]">{{ item.status }}</span>
            </td>
            <td>
              <div class="row-actions">
                <button v-if="item.status === 'OPEN'" class="primary" :disabled="loading" @click="approve(item.id)">通过</button>
                <button v-if="item.status === 'OPEN'" class="danger" :disabled="loading" @click="reject(item.id)">拒绝</button>
                <span v-else class="text-gray">已处理</span>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && items.length===0">
            <td colspan="9" class="empty">暂无数据</td>
          </tr>
          <tr v-if="loading">
            <td colspan="9" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

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
const status = ref<string>('')
const keyword = ref('')

function fmt(v?: string) {
  if (!v) return '-'
  const d = new Date(v)
  if (isNaN(d.getTime())) return v
  return d.toLocaleString()
}

async function fetchList() {
  loading.value = true
  try {
    const qs = new URLSearchParams()
    if (status.value) qs.set('status', status.value)
    if (keyword.value) qs.set('keyword', keyword.value)
    qs.set('page', String(page.value))
    qs.set('size', String(size.value))
    const resp = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews?${qs.toString()}`)
    const data = await resp.json()
    const list: NeighborSupportTask[] = (resp.ok && data?.code === 200 && Array.isArray(data?.data)) ? data.data : []
    items.value = list
    hasMore.value = list.length >= size.value
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  if (!confirm('确认审核通过并发布?')) return
  loading.value = true
  try {
    const resp = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews/${id}/approve`, { method: 'POST' })
    const data = await resp.json()
    if (!resp.ok || data?.code !== 200) throw new Error(data?.message || '审批失败')
    alert('审批通过成功')
    fetchList()
  } catch (e: any) {
    alert(e?.message || '审批失败')
  } finally {
    loading.value = false
  }
}

async function reject(id: number) {
  if (!confirm('确认拒绝该任务?')) return
  loading.value = true
  try {
    const resp = await fetch(`${API_BASE}/api/admin/neighbor-tasks/reviews/${id}/reject`, { method: 'POST' })
    const data = await resp.json()
    if (!resp.ok || data?.code !== 200) throw new Error(data?.message || '拒绝失败')
    alert('已拒绝')
    fetchList()
  } catch (e: any) {
    alert(e?.message || '拒绝失败')
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
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.filters { display: flex; gap: 12px; align-items: center; }
label { display: flex; gap: 8px; align-items: center; font-size: 14px; color: #4b5563; }
select, input[type="text"] { border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px 10px; font-size: 14px; }
.pager { display: flex; gap: 10px; align-items: center; }
.card { background: #fff; border-radius: 12px; padding: 0; box-shadow: 0 10px 30px rgba(0,0,0,0.06); }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { text-align: left; padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
.table thead th { background: #fafafa; font-weight: 600; font-size: 13px; color: #6b7280; }
.title { font-weight: 600; color: #111827; }
.subtitle { font-size: 12px; color: #6b7280; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; }
.s-open { background: #fff7ed; color: #c2410c; }
.s-cancelled { background: #fee2e2; color: #991b1b; }
.p-high { background: #fee2e2; color: #991b1b; }
.p-medium { background: #fff7ed; color: #c2410c; }
.p-low { background: #ecfdf5; color: #065f46; }
.row-actions { display: flex; gap: 8px; }
.primary, .ghost, .danger { border: none; padding: 8px 12px; border-radius: 8px; cursor: pointer; font-weight: 600; }
.primary { background: linear-gradient(120deg, #10b981, #059669); color: #fff; }
.ghost { background: #f3f4f6; color: #111827; }
.danger { background: #ef4444; color: #fff; }
.text-gray { color: #9ca3af; font-size: 13px; }
.empty { text-align: center; color: #6b7280; }
</style>
