<template>
  <div class="page-wrapper">
    <div class="dashboard-container">
      <main class="main-content">
        <header class="content-header">
          <h2 class="page-title">System Notification</h2>
          <div class="header-actions">
            <div class="filter-links">
              <span
                v-for="tab in tabs"
                :key="tab.id"
                class="filter-link"
                :class="{ active: currentTab === tab.id }"
                @click="switchTab(tab.id)"
              >
                {{ tab.label }}
              </span>
            </div>
            <span class="divider">|</span>
            <button class="text-btn" @click="markAllAsRead">Mark all as read</button>
          </div>
        </header>

        <div class="data-container">
          <transition name="fade" mode="out-in">
            <div v-if="!selectedNotification" key="list" class="view-wrapper list-view">
              <div v-if="loading" class="loading-state">Loading notifications...</div>
              <div v-else-if="errorMsg" class="error-state">{{ errorMsg }}</div>

              <div v-else-if="paginatedNotifications.length > 0" class="notification-list">
                <div
                  v-for="item in paginatedNotifications"
                  :key="item.id"
                  class="notif-row"
                  :class="{ unread: !item.isRead }"
                  @click="openDetail(item)"
                >
                  <div class="notif-icon">
                    <img v-if="item.avatar" :src="item.avatar" class="avatar" />
                    <div v-else class="icon-placeholder" :class="item.type">
                      {{ getIconChar(item.type) }}
                    </div>
                  </div>

                  <div class="notif-details">
                    <div class="notif-top-line">
                      <span class="notif-title">{{ item.title }}</span>
                      <span class="notif-time">{{ item.time }}</span>
                    </div>
                    <div class="notif-message">{{ item.content }}</div>
                  </div>

                  <div class="notif-action">
                    <button class="icon-btn delete" @click.stop="deleteItem(item)">🗑️</button>
                    <div class="status-dot" v-if="!item.isRead"></div>
                  </div>
                </div>
              </div>

              <div v-else class="empty-state">
                <span>No notifications here</span>
              </div>

              <div v-if="!loading && !errorMsg && totalPages > 1" class="pagination-controls">
                <button class="page-btn" :disabled="currentPage === 1" @click="prevPage">&lt;</button>
                <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
                <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage">&gt;</button>
              </div>
            </div>

            <div v-else key="detail" class="view-wrapper detail-view">
              <div class="detail-nav">
                <button class="back-link" @click="closeDetail">← Back to list</button>
                <div class="detail-actions">
                  <button class="icon-btn delete-lg" @click="deleteItem(selectedNotification); closeDetail()" title="Delete">
                    🗑️
                  </button>
                </div>
              </div>

              <div class="detail-header">
                <div class="detail-icon-box">
                  <img v-if="selectedNotification?.avatar" :src="selectedNotification.avatar" class="avatar-lg" />
                  <div v-else class="icon-placeholder lg">
                    {{ selectedNotification ? getIconChar(selectedNotification.type) : '' }}
                  </div>
                </div>
                <div class="detail-title-group">
                  <h3 class="detail-heading">{{ selectedNotification?.title }}</h3>
                  <span class="detail-timestamp">{{ selectedNotification?.time }}</span>
                </div>
              </div>

              <div class="detail-body">
                <p class="detail-text">{{ selectedNotification?.content }}</p>

                <div v-if="selectedNotification?.type === 'order'" class="extra-info-card">
                  <h4>Order Details</h4>
                  <p>Tracking Number: <strong>SF1234567890</strong></p>
                  <p>Carrier: SF Express</p>
                  <button class="secondary-btn">Track Package</button>
                </div>

                <div v-if="selectedNotification?.type === 'interaction'" class="extra-info-card reply-section">
                  <input type="text" class="reply-input" placeholder="Reply to this comment..." />
                  <button class="primary-btn sm">Send</button>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import dhstyle from '../dhstyle/dhstyle.vue';
import SockJS from 'sockjs-client/dist/sockjs.js';
import Stomp from 'stompjs';
import { useNotifyStore } from '@/store/notify';

type NotifType = 'system' | 'interaction' | 'order' | string;

interface Notification {
  id: number;
  type: NotifType;
  title: string;
  content: string;
  time: string;
  isRead: boolean;
  linkUrl?: string;
  meta?: string;
  avatar?: string;
}

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const route = useRoute();

const userId = ref<string | null>(localStorage.getItem('userId'));
const loading = ref(false);
const errorMsg = ref('');
const currentTab = ref('all');
const currentPage = ref(1);
const itemsPerPage = 5;
const selectedNotification = ref<Notification | null>(null);
const notifyStore = useNotifyStore();
let stomp: Stomp.Client | null = null;

const tabs = [
  { id: 'all', label: 'All' },
  { id: 'system', label: 'System' },
  { id: 'interaction', label: 'Interaction' },
  { id: 'order', label: 'Order' }
];

const notifications = ref<Notification[]>([]);

const filteredNotifications = computed(() => {
  if (currentTab.value === 'all') return notifications.value;
  return notifications.value.filter((n) => n.type === currentTab.value);
});

const totalPages = computed(() => Math.ceil(filteredNotifications.value.length / itemsPerPage) || 1);

const paginatedNotifications = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  return filteredNotifications.value.slice(start, start + itemsPerPage);
});

function getIconChar(type: NotifType) {
  if (type === 'system') return '📢';
  if (type === 'order') return '📦';
  return '🔔';
}

function switchTab(tabId: string) {
  currentTab.value = tabId;
  currentPage.value = 1;
  selectedNotification.value = null;
}

async function openDetail(item: Notification) {
  selectedNotification.value = item;
  await markAsRead(item);
  if (item.linkUrl) {
    window.open(item.linkUrl, '_blank');
  }
}

function closeDetail() {
  selectedNotification.value = null;
}

async function markAsRead(item: Notification) {
  if (item.isRead) return;
  const wasUnread = !item.isRead;
  item.isRead = true;
  if (!userId.value) return;
  try {
    const res = await fetch(`${API_BASE}/api/notifications/${item.id}/read?userId=${userId.value}`, {
      method: 'PATCH',
      headers: buildAuthHeader()
    });
    const data = await res.json();
    if (!res.ok || data.code !== 200) throw new Error(data?.message || '标记已读失败');
    if (wasUnread) {
      notifyStore.incr(-1);
      const idx = notifications.value.findIndex((n) => n.id === item.id);
      if (idx >= 0) notifications.value[idx].isRead = true;
    }
  } catch (_) {
    item.isRead = false;
  } finally {
    fetchNotifications();
  }
}

async function markAllAsRead() {
  if (!userId.value) return;
  notifications.value.forEach((n) => (n.isRead = true));
  notifyStore.clear();
  try {
    const res = await fetch(`${API_BASE}/api/notifications/markAllRead?userId=${userId.value}`, {
      method: 'POST',
      headers: buildAuthHeader()
    });
    const data = await res.json();
    if (!res.ok || data.code !== 200) throw new Error(data?.message || '标记全部已读失败');
  } catch (_) {
  } finally {
    fetchNotifications();
  }
}

async function deleteItem(item: Notification | null) {
  if (!item || !userId.value) return;
  if (!item.isRead) {
    const ok = window.confirm('删除未读通知将减少未读数量，是否继续？');
    if (!ok) return;
  }
  try {
    const res = await fetch(`${API_BASE}/api/notifications/${item.id}?userId=${userId.value}`, {
      method: 'DELETE',
      headers: buildAuthHeader()
    });
    const data = await res.json();
    if (!res.ok || data.code !== 200) throw new Error(data?.message || '删除失败');
    if (!item.isRead) {
      notifyStore.incr(-1);
    }
    notifications.value = notifications.value.filter((n) => n.id !== item.id);
    selectedNotification.value = null;
    if (paginatedNotifications.value.length === 0 && currentPage.value > 1) {
      currentPage.value--;
    }
    alert("删除成功")
  } catch (e: any) {
    errorMsg.value = e?.message || '删除失败';
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++;
}

function prevPage() {
  if (currentPage.value > 1) currentPage.value--;
}

function formatTime(ts?: string) {
  if (!ts) return '';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  return d.toLocaleString();
}

async function fetchNotifications() {
  loading.value = true;
  errorMsg.value = '';

  if (!userId.value) {
    loading.value = false;
    errorMsg.value = 'Please log in to view notifications.';
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/notifications?userId=${userId.value}`, {
      headers: buildAuthHeader()
    });
    const data = await res.json();
    if (data.code !== 200) throw new Error(data.message || '加载失败');

    const list = Array.isArray(data.data) ? data.data : [];
    const mapped = list.map((n: any) => ({
      id: n.id,
      type: n.type || 'system',
      title: n.title || 'Notification',
      content: n.content || '',
      time: formatTime(n.createdAt),
      isRead: n.readStatus === 1,
      linkUrl: n.linkUrl,
      meta: n.meta
    }));
    notifications.value = mapped;
    const unreadCount = mapped.filter((n) => !n.isRead).length;
    notifyStore.set(unreadCount);
  } catch (e: any) {
    errorMsg.value = e?.message || 'Failed to load notifications.';
  } finally {
    loading.value = false;
  }
}

async function fetchUnreadCount() {
  if (!userId.value) return;
  try {
    const res = await fetch(`${API_BASE}/api/notice/unread?userId=${userId.value}`, {
      headers: buildAuthHeader()
    });
    const data = await res.json();
    if (data.code === 200) {
      notifyStore.set(Number(data.data || 0));
    }
  } catch (_) {
  }
}

function connectSocket() {
  if (!userId.value || stomp) return;
  const sock = new SockJS(`${API_BASE}/ws-notice`);
  const client = Stomp.over(sock);
  client.connect({}, () => {
    client.subscribe('/user/queue/notify', (msg) => {
      try {
        const payload = JSON.parse(msg.body || '{}');
        if (payload.unread !== undefined) {
          notifyStore.set(Number(payload.unread));
        } else if (payload.delta) {
          notifyStore.incr(Number(payload.delta));
        }
      } catch (e) {
        console.warn('Parse notify payload failed', e);
      }
    });
  });
  stomp = client;
}

function disconnectSocket() {
  if (stomp) {
    stomp.disconnect(() => {});
    stomp = null;
  }
}

onMounted(async () => {
  fetchUnreadCount();
  fetchNotifications();
  connectSocket();
});

onUnmounted(() => {
  disconnectSocket();
});

function buildAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('token') || '';
  return token ? { Authorization: token } : {};
}

// sockjs 兼容 global
if (typeof window !== 'undefined' && !(window as any).global) {
  (window as any).global = window;
}
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background-color: #ffffff;
  padding-top: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #333;
}

.dashboard-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
}

.main-content {
  width: 100%;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 400;
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.filter-links {
  display: flex;
  gap: 16px;
}

.filter-link {
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}
.filter-link:hover {
  color: #666;
}
.filter-link.active {
  color: #000;
  font-weight: 600;
}

.divider {
  color: #eee;
}

.text-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}
.text-btn:hover {
  text-decoration: underline;
}

.data-container {
  background-color: #fbf2f2;
  border-radius: 8px;
  padding: 30px;
  min-height: 500px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  position: relative;
}

.view-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.loading-state,
.error-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}
.error-state {
  color: #e74c3c;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
}

.notif-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: opacity 0.2s;
}
.notif-row:last-child {
  border-bottom: none;
}
.notif-row:hover {
  opacity: 0.8;
}

.notif-icon {
  width: 40px;
  height: 40px;
  margin-right: 20px;
  flex-shrink: 0;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.icon-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.notif-details {
  flex: 1;
  margin-right: 20px;
}

.notif-top-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
}

.notif-title {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.notif-time {
  font-size: 12px;
  color: #999;
  margin-left: 12px;
}

.notif-message {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  background: none;
  border: none;
  color: #aaa;
  cursor: pointer;
  font-size: 14px;
}
.icon-btn:hover {
  color: #333;
}

.status-dot {
  width: 8px;
  height: 8px;
  background-color: #ff7043;
  border-radius: 50%;
}

.notif-row.unread .notif-title {
  color: #000;
}
.notif-row.unread .notif-message {
  color: #333;
  font-weight: 500;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: auto;
  padding-top: 20px;
  gap: 16px;
}
.page-btn {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  width: 32px;
  height: 32px;
  cursor: pointer;
  color: #666;
}
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.page-info {
  font-size: 14px;
  color: #666;
}

.detail-view {
  animation: fadeIn 0.3s ease;
}

.detail-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 15px;
}

.back-link {
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
}
.back-link:hover {
  color: #000;
}

.detail-actions {
  display: flex;
  gap: 10px;
}
.delete-lg {
  font-size: 16px;
}

.detail-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 25px;
}

.avatar-lg {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.icon-placeholder.lg {
  width: 64px;
  height: 64px;
  font-size: 28px;
}

.detail-title-group {
  flex: 1;
}
.detail-heading {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}
.detail-timestamp {
  font-size: 13px;
  color: #999;
}

.detail-body {
  font-size: 16px;
  line-height: 1.7;
  color: #444;
}
.detail-text {
  margin-bottom: 20px;
  white-space: pre-wrap;
}

.extra-info-card {
  background: #fff;
  border-radius: 6px;
  padding: 16px;
  margin-top: 20px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}
.extra-info-card h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.extra-info-card p {
  margin: 5px 0;
  font-size: 14px;
}

.secondary-btn {
  margin-top: 10px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.primary-btn.sm {
  background: #333;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.reply-section {
  display: flex;
  gap: 10px;
}
.reply-input {
  flex: 1;
  border: 1px solid #eee;
  background: #fafafa;
  padding: 8px 12px;
  border-radius: 4px;
  outline: none;
}
.reply-input:focus {
  background: #fff;
  border-color: #ddd;
}

.section-footer {
  margin-top: 40px;
}
.section-footer h3 {
  font-size: 16px;
  font-weight: 400;
  margin-bottom: 8px;
  color: #333;
}
.section-footer p {
  font-size: 13px;
  color: #999;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
