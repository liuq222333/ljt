<template>
  <div class="page">
    <div class="toolbar">
      <div class="filters">
        <label>
          关键字
          <input v-model="keyword" type="text" placeholder="Resource/Path/Desc" />
        </label>
        <button class="primary" :disabled="loading" @click="fetchList">查询</button>
      </div>
      <div class="actions">
        <button class="primary" @click="openModal()">新增接口</button>
      </div>
    </div>

    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 20px;">ID</th>
            <th style="width: 40px;">资源 (Resource)</th>
            <th style="width: 40px;">动作 (Action)</th>
            <th style="width: 20px;">方法</th>
            <th style="width: 60px;">路径模板</th>
            <th style="width: 30px;">类型</th>
            <th style="width: 80px;">描述</th>
            <th style="width: 20px;">状态</th>
            <th style="width: 50px;" class="sticky-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.resource }}</td>
            <td>{{ item.action }}</td>
            <td>
              <span class="badge" :class="'method-' + item.httpMethod.toLowerCase()">{{ item.httpMethod }}</span>
            </td>
            <td class="code-font">{{ item.pathTemplate }}</td>
            <td>{{ item.operationType }}</td>
            <td>{{ item.description }}</td>
            <td>
              <span class="badge" :class="item.enabled === 1 ? 's-enabled' : 's-disabled'">
                {{ item.enabled === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td class="sticky-col">
              <div class="row-actions">
                <button class="ghost" @click="openModal(item)">编辑</button>
                <button v-if="item.enabled === 1" class="warning" @click="toggleStatus(item, 0)">停用</button>
                <button v-else class="success" @click="toggleStatus(item, 1)">启用</button>
                <button class="danger" @click="deleteItem(item)">删除</button>
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

    <div class="pager" v-if="items.length > 0 || page > 1">
      <button class="ghost" :disabled="loading || page<=1" @click="prevPage">上一页</button>
      <span>第 {{ page }} 页</span>
      <button class="ghost" :disabled="loading || items.length < size" @click="nextPage">下一页</button>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑接口' : '新增接口' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Resource</label>
            <input v-model="form.resource" type="text" placeholder="e.g. user" />
          </div>
          <div class="form-group">
            <label>Action</label>
            <input v-model="form.action" type="text" placeholder="e.g. create" />
          </div>
          <div class="form-group">
            <label>HTTP Method</label>
            <select v-model="form.httpMethod">
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>
          <div class="form-group">
            <label>Path Template</label>
            <input v-model="form.pathTemplate" type="text" placeholder="e.g. /api/users" />
          </div>
          <div class="form-group">
            <label>Operation Type</label>
            <input v-model="form.operationType" type="text" placeholder="e.g. READ/WRITE" />
          </div>
          <div class="form-group">
            <label>Description</label>
            <input v-model="form.description" type="text" />
          </div>
          <div class="form-group full">
             <label>Query Schema (JSON)</label>
             <textarea v-model="form.querySchema" rows="3"></textarea>
          </div>
          <div class="form-group full">
             <label>Body Schema (JSON)</label>
             <textarea v-model="form.bodySchema" rows="3"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="ghost" @click="closeModal">取消</button>
          <button class="primary" @click="submitForm">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

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
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const showModal = ref(false)
const isEdit = ref(false)
const form = reactive<ApiRoute>({
  resource: '',
  action: '',
  httpMethod: 'GET',
  pathTemplate: '',
  operationType: '',
  description: '',
  enabled: 1,
  querySchema: '',
  bodySchema: ''
})

async function fetchList() {
  loading.value = true
  try {
    const qs = new URLSearchParams()
    if (keyword.value) qs.set('keyword', keyword.value)
    qs.set('page', String(page.value))
    qs.set('size', String(size.value))
    const resp = await fetch(`${API_BASE}/api/admin/api-management/list?${qs.toString()}`)
    const data = await resp.json()
    items.value = (data.code === 200 && Array.isArray(data.data)) ? data.data : []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openModal(item?: ApiRoute) {
  if (item) {
    isEdit.value = true
    Object.assign(form, item)
  } else {
    isEdit.value = false
    Object.assign(form, {
      id: undefined,
      resource: '',
      action: '',
      httpMethod: 'GET',
      pathTemplate: '',
      pathParams: '',
      operationType: '',
      description: '',
      enabled: 1,
      querySchema: '',
      bodySchema: ''
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitForm() {
  const url = isEdit.value ? `${API_BASE}/api/admin/api-management/update` : `${API_BASE}/api/admin/api-management/add`
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
    const res = await resp.json()
    if (res.code === 200) {
      alert('保存成功')
      closeModal()
      fetchList()
    } else {
      alert(res.message || '保存失败')
    }
  } catch (e) {
    alert('保存失败')
  }
}

async function toggleStatus(item: ApiRoute, status: number) {
  const action = status === 1 ? 'enable' : 'disable'
  if (!confirm(`确定要${status === 1 ? '启用' : '停用'}该接口吗？`)) return
  try {
    const resp = await fetch(`${API_BASE}/api/admin/api-management/${action}/${item.id}`, { method: 'POST' })
    const res = await resp.json()
    if (res.code === 200) {
      fetchList()
    } else {
      alert(res.message || '操作失败')
    }
  } catch (e) {
    alert('操作失败')
  }
}

async function deleteItem(item: ApiRoute) {
  if (!confirm('确定要删除该接口吗？此操作不可恢复。')) return
  try {
    const resp = await fetch(`${API_BASE}/api/admin/api-management/delete/${item.id}`, { method: 'POST' })
    const res = await resp.json()
    if (res.code === 200) {
      fetchList()
    } else {
      alert(res.message || '删除失败')
    }
  } catch (e) {
    alert('删除失败')
  }
}

function prevPage() {
  if (page.value > 1) {
    page.value--
    fetchList()
  }
}

function nextPage() {
  page.value++
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 8px; padding: 5px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 12px; align-items: center; }
.filters label { display: flex; gap: 8px; align-items: center; font-size: 14px; color: #4b5563; }
.filters input { border: 1px solid #e5e7eb; border-radius: 6px; padding: 6px 10px; }

.card { background: #fff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); overflow-x: auto; }
.table { width: 100%; min-width: 1000px; border-collapse: separate; border-spacing: 0; table-layout: fixed; }
.table th, .table td { text-align: left; padding: 10px 5px; border-bottom: 1px solid #f0f0f0; font-size: 14px; word-wrap: break-word; background: #fff; }
.table th { background: #f9fafb; font-weight: 600; color: #6b7280; white-space: nowrap; }

/* Sticky Column */
.table .sticky-col { position: sticky; right: 0; z-index: 10; border-left: 1px solid #f0f0f0; box-shadow: -2px 0 6px rgba(0,0,0,0.04); }
.table th.sticky-col { background: #f9fafb; }

.code-font { font-family: monospace; color: #d63384; word-break: break-all; }

.badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 500; }
.method-get { background: #eff6ff; color: #2563eb; }
.method-post { background: #f0fdf4; color: #16a34a; }
.method-put { background: #fff7ed; color: #ea580c; }
.method-delete { background: #fef2f2; color: #dc2626; }
.method-patch { background: #f5f3ff; color: #7c3aed; }

.s-enabled { background: #ecfdf5; color: #065f46; }
.s-disabled { background: #f3f4f6; color: #374151; }

.row-actions { display: flex; gap: 6px; justify-content: flex-start; }
.primary, .ghost, .danger, .warning, .success { border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 500; transition: opacity 0.2s; white-space: nowrap; }
.primary { background: #2563eb; color: #fff; }
.ghost { background: #f3f4f6; color: #1f2937; }
.danger { background: #ef4444; color: #fff; }
.warning { background: #f59e0b; color: #fff; }
.success { background: #10b981; color: #fff; }
button:hover { opacity: 0.9; }
button:disabled { opacity: 0.5; cursor: not-allowed; }

.pager { display: flex; gap: 10px; align-items: center; justify-content: flex-end; }
.empty { text-align: center; color: #9ca3af; padding: 20px; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 12px; width: 600px; max-width: 90%; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); }
.modal-header { padding: 16px 20px; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; }
.modal-header h3 { margin: 0; font-size: 18px; font-weight: 600; }
.close-btn { background: none; border: none; font-size: 24px; color: #9ca3af; cursor: pointer; }
.modal-body { padding: 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full { grid-column: span 2; }
.form-group label { font-size: 13px; font-weight: 500; color: #374151; }
.form-group input, .form-group select, .form-group textarea { border: 1px solid #d1d5db; border-radius: 6px; padding: 8px; font-size: 14px; }
.modal-footer { padding: 16px 20px; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; gap: 10px; }
</style>
