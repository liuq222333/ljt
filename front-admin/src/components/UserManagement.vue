<template>
  <section class="admin-page user-management-page">
    <AdminPageHeader eyebrow="运营管理" title="用户管理" description="维护用户资料、账号状态与常用后台操作。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="fetchUsers">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <label class="filter-field filter-field--search">
          <span>搜索</span>
          <input v-model="keyword" type="text" placeholder="用户名 / 邮箱 / 手机号" />
        </label>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="search">
          查询
        </button>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="reset">
          重置
        </button>
      </template>
      <template #actions>
        <div class="toolbar-meta">
          <span class="meta-chip">当前页 {{ users.length }} 条</span>
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

    <AdminPanel title="用户账号列表" description="支持编辑资料、重置密码和删除账号。">
      <div class="table-shell">
        <table class="admin-data-table user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>手机号</th>
              <th>地址</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.userId">
              <td>{{ user.userId }}</td>
              <td class="name-cell">{{ user.userName }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td class="address-cell">{{ user.address || '-' }}</td>
              <td>
                <div class="row-actions">
                  <button
                    class="admin-button admin-button--secondary admin-button--small"
                    type="button"
                    :disabled="loading"
                    @click="editUser(user)"
                  >
                    编辑
                  </button>
                  <button
                    class="admin-button admin-button--secondary admin-button--small"
                    type="button"
                    :disabled="loading"
                    @click="resetPassword(user)"
                  >
                    重置密码
                  </button>
                  <button
                    class="admin-button admin-button--danger admin-button--small"
                    type="button"
                    :disabled="loading"
                    @click="deleteUser(user)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && users.length === 0">
              <td colspan="6" class="empty-state">暂无用户数据</td>
            </tr>
            <tr v-if="loading">
              <td colspan="6" class="empty-state">正在加载用户列表...</td>
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

type AdminUser = {
  userId: string
  userName: string
  email?: string
  phone?: string
  address?: string
}

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const users = ref<AdminUser[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(8)
const hasMore = ref(false)
const loading = ref(false)

async function fetchUsers() {
  loading.value = true
  try {
    const searchParams = new URLSearchParams({
      page: String(page.value),
      size: String(size.value),
    })
    if (keyword.value.trim()) {
      searchParams.set('keyword', keyword.value.trim())
    }

    const response = await fetch(`${API_BASE}/api/admin/users?${searchParams.toString()}`)
    const data = await response.json()
    const list: AdminUser[] = response.ok && data?.code === 200 && Array.isArray(data?.data) ? data.data : []
    users.value = list
    hasMore.value = list.length >= size.value
  } catch (error) {
    console.error(error)
    window.alert('获取用户失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchUsers()
}

function reset() {
  keyword.value = ''
  page.value = 1
  fetchUsers()
}

async function editUser(user: AdminUser) {
  const userName = window.prompt('用户名', user.userName) ?? ''
  if (!userName.trim()) return
  const email = window.prompt('邮箱', user.email || '') ?? ''
  const phone = window.prompt('手机号', user.phone || '') ?? ''
  const address = window.prompt('地址', user.address || '') ?? ''

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/users/${user.userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userName, email, phone, address }),
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '更新失败')
    }
    window.alert('用户资料已更新')
    fetchUsers()
  } catch (error: any) {
    window.alert(error?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

async function resetPassword(user: AdminUser) {
  const password = window.prompt('请输入新密码（至少 6 位）')
  if (!password || password.length < 6) {
    window.alert('密码长度至少 6 位')
    return
  }

  loading.value = true
  try {
    const response = await fetch(
      `${API_BASE}/api/admin/users/${user.userId}/reset-password?password=${encodeURIComponent(password)}`,
      { method: 'POST' },
    )
    const data = await response.json().catch(() => ({}))
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '重置失败')
    }
    window.alert('密码已重置')
  } catch (error: any) {
    window.alert(error?.message || '重置失败')
  } finally {
    loading.value = false
  }
}

async function deleteUser(user: AdminUser) {
  if (!window.confirm(`确认删除用户 ${user.userName} 吗？`)) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/users/${user.userId}`, { method: 'DELETE' })
    const data = await response.json().catch(() => ({}))
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '删除失败')
    }
    users.value = users.value.filter((item) => item.userId !== user.userId)
  } catch (error: any) {
    window.alert(error?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1
    fetchUsers()
  }
}

function nextPage() {
  page.value += 1
  fetchUsers()
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-management-page {
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

.user-table {
  min-width: 920px;
}

.name-cell {
  color: var(--admin-text-primary);
  font-weight: 700;
}

.address-cell {
  min-width: 220px;
  max-width: 320px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
</style>
