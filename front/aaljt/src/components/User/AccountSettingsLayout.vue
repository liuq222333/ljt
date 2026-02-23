<template>

  <div class="settings-layout">
    <dhstyle />
    <aside class="sidebar">
      <nav>
        <div class="nav-group">
          <h3 class="group-title">ACCOUNT</h3>
          <ul>
            <li><router-link to="/user/profile">Profile</router-link></li>
            <li><router-link to="/user/settings">Settings</router-link></li>
          </ul>
        </div>
        <div class="nav-group">
          <h3 class="group-title">Notification</h3>
          <ul>
            <li>
              <router-link to="/user/system-notification" class="notif-link">
                <span>System Notification</span>
                <span v-if="unreadBadge > 0" class="badge">{{ unreadBadge }}</span>
              </router-link>
            </li>
            <li><router-link to="/user/usage">Usage</router-link></li>
          </ul>
        </div>
      </nav>
    </aside>
    <main class="content-area">
      <router-view></router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import dhstyle from '../dhstyle/dhstyle.vue'
import { useNotifyStore } from '@/store/notify'

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'
const notifyStore = useNotifyStore()
const unreadBadge = computed(() => notifyStore.unread)

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  try {
    const res = await fetch(`${API_BASE}/api/notifications/unread?userId=${userId}`, {
      headers: buildAuthHeader()
    })
    const data = await res.json()
    if (data?.code === 200) {
      notifyStore.set(Number(data.data || 0))
    }
  } catch (_) {
    // ignore
  }
})

function buildAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('token') || ''
  return token ? { Authorization: token } : {}
}
</script>

<style scoped>
.settings-layout {
  display: flex;
  min-height: 100vh;
  background-color: #fff;
  color: #000;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background-color: #f0f0f0;
  padding: 80px 20px;
  border-right: 1px solid #ddd;
}

.nav-group {
  margin-bottom: 30px;
}

.group-title {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 15px;
}

.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar li a {
  display: block;
  padding: 10px 15px;
  border-radius: 6px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.2s, color 0.2s;
}

.sidebar li a:hover {
  background-color: #e0e0e0;
  color: #000;
}

.sidebar li a.router-link-exact-active {
  background-color: #d0d0d0;
  color: #000;
  font-weight: 500;
}

.notif-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  margin-left: 8px;
  min-width: 18px;
  padding: 1px 6px;
  border-radius: 10px;
  background: #f35653;
  color: #fff;
  font-size: 12px;
  line-height: 1.2;
  text-align: center;
}

.content-area {
  flex-grow: 1;
  padding: 40px;
}
</style>
