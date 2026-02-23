<template>
  <div class="page">
    <div class="toolbar">
      <div class="filters">
        <input v-model="keyword" type="text" placeholder="用户名 / 邮箱 / 手机" />
        <button class="primary" :disabled="loading" @click="fetchUsers">查询</button>
        <button class="ghost" :disabled="loading" @click="reset">重置</button>
      </div>
      <div class="pager">
        <button class="ghost sm" :disabled="loading || page<=1" @click="prevPage">上一页</button>
        <span>第 {{ page }} 页</span>
        <button class="ghost sm" :disabled="loading || !hasMore" @click="nextPage">下一页</button>
      </div>
    </div>

    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>手机</th>
            <th>地址</th>
            <th style="width:260px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.userId">
            <td>{{ u.userId }}</td>
            <td>{{ u.userName }}</td>
            <td>{{ u.email || '-' }}</td>
            <td>{{ u.phone || '-' }}</td>
            <td class="address">{{ u.address || '-' }}</td>
            <td>
              <div class="row-actions tight">
                <button class="ghost sm" :disabled="loading" @click="editUser(u)">编辑</button>
                <button class="ghost sm" :disabled="loading" @click="resetPwd(u)">重置密码</button>
                <button class="danger sm" :disabled="loading" @click="deleteUser(u)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && users.length === 0">
            <td colspan="6" class="empty">暂无数据</td>
          </tr>
          <tr v-if="loading">
            <td colspan="6" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

type AdminUser = {
  userId: string;
  userName: string;
  email?: string;
  phone?: string;
  address?: string;
};

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';

const users = ref<AdminUser[]>([]);
const keyword = ref('');
const page = ref(1);
const size = ref(8);
const hasMore = ref(false);
const loading = ref(false);

const fetchUsers = async () => {
  loading.value = true;
  try {
    const qs = new URLSearchParams({
      page: String(page.value),
      size: String(size.value)
    });
    if (keyword.value.trim()) qs.set('keyword', keyword.value.trim());
    const resp = await fetch(`${API_BASE}/api/admin/users?${qs.toString()}`);
    const data = await resp.json();
    const list: AdminUser[] = resp.ok && data?.code === 200 && Array.isArray(data?.data) ? data.data : [];
    users.value = list;
    hasMore.value = list.length >= size.value;
  } catch (e) {
    console.error(e);
    alert('获取用户失败');
  } finally {
    loading.value = false;
  }
};

const editUser = async (u: AdminUser) => {
  const userName = window.prompt('用户名', u.userName) ?? '';
  if (!userName.trim()) return;
  const email = window.prompt('邮箱', u.email || '') ?? '';
  const phone = window.prompt('手机', u.phone || '') ?? '';
  const address = window.prompt('地址', u.address || '') ?? '';
  loading.value = true;
  try {
    const resp = await fetch(`${API_BASE}/api/admin/users/${u.userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userName, email, phone, address })
    });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || data?.code !== 200) throw new Error(data?.message || '更新失败');
    alert('已更新');
    fetchUsers();
  } catch (e: any) {
    alert(e?.message || '更新失败');
  } finally {
    loading.value = false;
  }
};

const resetPwd = async (u: AdminUser) => {
  const pwd = window.prompt('请输入新密码（至少6位）');
  if (!pwd || pwd.length < 6) {
    alert('密码长度至少6位');
    return;
  }
  loading.value = true;
  try {
    const resp = await fetch(`${API_BASE}/api/admin/users/${u.userId}/reset-password?password=${encodeURIComponent(pwd)}`, {
      method: 'POST'
    });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || data?.code !== 200) throw new Error(data?.message || '重置失败');
    alert('密码已重置');
  } catch (e: any) {
    alert(e?.message || '重置失败');
  } finally {
    loading.value = false;
  }
};

const deleteUser = async (u: AdminUser) => {
  if (!window.confirm(`确认删除用户 ${u.userName}?`)) return;
  loading.value = true;
  try {
    const resp = await fetch(`${API_BASE}/api/admin/users/${u.userId}`, { method: 'DELETE' });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || data?.code !== 200) throw new Error(data?.message || '删除失败');
    users.value = users.value.filter(item => item.userId !== u.userId);
  } catch (e: any) {
    alert(e?.message || '删除失败');
  } finally {
    loading.value = false;
  }
};

const prevPage = () => {
  if (page.value > 1) {
    page.value -= 1;
    fetchUsers();
  }
};
const nextPage = () => {
  page.value += 1;
  fetchUsers();
};
const reset = () => {
  keyword.value = '';
  page.value = 1;
  fetchUsers();
};

onMounted(fetchUsers);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filters {
  display: flex;
  gap: 10px;
}
input[type="text"] {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 220px;
}
.pager {
  display: flex;
  gap: 10px;
  align-items: center;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th, .table td {
  text-align: left;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.table thead th {
  background: #fafafa;
  font-weight: 600;
  font-size: 13px;
  color: #6b7280;
}
.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.primary, .ghost, .danger {
  border: none;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.primary { background: linear-gradient(120deg, #10b981, #059669); color: #fff; }
.ghost { background: #f3f4f6; color: #111827; }
.danger { background: #ef4444; color: #fff; }
.sm { padding: 6px 10px; font-size: 12px; }
.empty { text-align: center; color: #6b7280; }
.address { max-width: 240px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.row-actions.tight { gap: 6px; flex-wrap: nowrap; }
</style>
