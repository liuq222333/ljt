<template>
  <div class="page">
    <div class="toolbar">
      <div class="filters">
        <label>
          关键字
          <input v-model="keyword" type="text" placeholder="标题/描述" />
        </label>
        <button class="primary" :disabled="loading" @click="fetchList">刷新</button>
      </div>
    </div>

    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 60px;">ID</th>
            <th style="width: 80px;">图片</th>
            <th style="min-width: 200px;">标题</th>
            <th style="width: 100px;">价格</th>
            <th style="width: 100px;">分类ID</th>
            <th style="width: 100px;">状态</th>
            <th style="width: 100px;">卖家ID</th>
            <th style="width: 160px;">发布时间</th>
            <th style="width: 100px;" class="sticky-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredItems" :key="item.id">
            <td>{{ item.id }}</td>
            <td>
              <img v-if="getFirstImage(item.imageUrls)" :src="getFirstImage(item.imageUrls)" class="thumb" />
              <span v-else class="no-img">无图</span>
            </td>
            <td>
              <div class="title" :title="item.title">{{ item.title }}</div>
              <div class="desc" :title="item.description">{{ item.description }}</div>
            </td>
            <td class="price">¥{{ item.price }}</td>
            <td>{{ item.categoryId }}</td>
            <td>
              <span class="badge" :class="getStatusClass(item.status)">{{ item.status }}</span>
            </td>
            <td>{{ item.sellerId }}</td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td class="sticky-col">
              <div class="row-actions">
                <button class="danger" @click="deleteItem(item)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && filteredItems.length===0">
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
import { ref, computed, onMounted } from 'vue'

interface Product {
  id: number
  sellerId: number
  categoryId: number
  title: string
  description: string
  price: number
  stockQuantity: number
  condition: string
  location: string
  imageUrls: string
  status: string
  createdAt: string
  updatedAt: string
}

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const items = ref<Product[]>([])
const loading = ref(false)
const keyword = ref('')

// Client-side filtering since getAllProducts returns everything
const filteredItems = computed(() => {
  if (!keyword.value) return items.value
  const k = keyword.value.toLowerCase()
  return items.value.filter(i => 
    (i.title && i.title.toLowerCase().includes(k)) || 
    (i.description && i.description.toLowerCase().includes(k))
  )
})

async function fetchList() {
  loading.value = true
  try {
    const resp = await fetch(`${API_BASE}/api/products/getAllProducts`)
    if (resp.ok) {
      const data = await resp.json()
      items.value = Array.isArray(data) ? data : []
    } else {
      console.error('Failed to fetch products')
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function getFirstImage(jsonStr: string): string | null {
  try {
    const arr = JSON.parse(jsonStr)
    if (Array.isArray(arr) && arr.length > 0) return arr[0]
  } catch (e) {}
  return null
}

function getStatusClass(status: string) {
  if (status === '在售') return 's-active'
  if (status === '已售出') return 's-sold'
  if (status === '已下架') return 's-inactive'
  return 's-default'
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

async function deleteItem(item: Product) {
  if (!confirm(`确定要删除商品 "${item.title}" (ID: ${item.id}) 吗？`)) return

  try {
    const resp = await fetch(`${API_BASE}/admin/market/products/${item.id}`, {
      method: 'DELETE'
    })
    const res = await resp.json()
    if (res.code === 200) {
      alert('删除成功')
      fetchList()
    } else {
      alert(res.message || '删除失败')
    }
  } catch (e) {
    console.error(e)
    alert('请求失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page {
  padding: 20px;
  max-width: 100%;
  box-sizing: border-box;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filters {
  display: flex;
  gap: 15px;
  align-items: center;
}
.filters label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}
.filters input {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 200px;
}
.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow: hidden; /* Contains the table */
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1000px;
  table-layout: fixed;
}
.table th, .table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}
.table .sticky-col {
  position: sticky;
  right: 0;
  background-color: #fff; /* Match row bg */
  z-index: 1;
  border-left: 1px solid #f0f0f0;
  box-shadow: -2px 0 6px rgba(0,0,0,0.04);
}
.table th.sticky-col {
  background-color: #f8f9fa; /* Match header bg */
  z-index: 2; /* Header sticky must be above row sticky */
}

.thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  background: #f0f0f0;
}
.no-img {
  display: inline-block;
  width: 40px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: #f0f0f0;
  color: #999;
  font-size: 12px;
  border-radius: 4px;
}

.title {
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}
.desc {
  font-size: 12px;
  color: #888;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
.price {
  color: #f5222d;
  font-weight: 600;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.5;
}
.s-active { background: #e6f7ff; color: #1890ff; }
.s-sold { background: #f5f5f5; color: #8c8c8c; }
.s-inactive { background: #fff1f0; color: #f5222d; }
.s-default { background: #f5f5f5; color: #666; }

/* Buttons */
.primary {
  background: #1890ff;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.primary:hover { opacity: 0.9; }
.primary:disabled { background: #ccc; cursor: not-allowed; }

.danger {
  background: #ff4d4f;
  color: #fff;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
.danger:hover { opacity: 0.9; }

.row-actions {
  display: flex;
  gap: 8px;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
