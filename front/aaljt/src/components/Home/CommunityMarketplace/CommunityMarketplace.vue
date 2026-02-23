<template>
  <div class="nd-page">
    <dhstyle />

    <!-- Option B: Removed Full Width Hero -->
    <!-- <div class="market-hero">...</div> -->

    <div class="page-container">
      <div class="market-layout">
        
        <!-- 右侧固定侧边栏 -->
        <CebianTool />

        <!-- 左侧悬浮区域 (Agent Chat) -->
        <aside class="sidebar-wrapper">
          <div class="sidebar-sticky">
            <section class="card-panel agent-chat-card">
              <div class="chat-header">
                <div class="header-left">
                  <h3>社区助手</h3>
                  <span class="status-tag">在线</span>
                </div>
                <p class="subtitle">发布闲置 / 寻找好物 / 活动咨询</p>
              </div>

              <div class="chat-window" ref="chatWindowRef">
                <div v-if="!agentMessages.length" class="chat-empty">
                  👋 嗨！我是社区助手，想发布什么宝贝？
                </div>
                <div v-else class="chat-messages">
                  <div
                    v-for="message in agentMessages"
                    :key="message.id"
                    :class="['chat-row', message.sender]"
                  >
                    <div class="chat-bubble">{{ message.text }}</div>
                    <span class="chat-time">{{ message.time }}</span>
                  </div>
                  <div v-if="agentLoading" class="chat-row agent">
                    <div class="chat-bubble loading">
                      <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="chat-error" v-if="agentError">{{ agentError }}</div>

              <div class="chat-input-area">
                <input
                  v-model="agentInput"
                  type="text"
                  placeholder="输入需求..."
                  @keyup.enter="sendAgentMessage"
                  :disabled="agentLoading"
                />
                <button
                  class="send-btn"
                  :disabled="agentLoading || !agentInput.trim()"
                  @click="sendAgentMessage"
                  type="button"
                  aria-label="发送消息"
                >
                  <svg class="send-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M2.01 21 23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor" />
                  </svg>
                </button>
              </div>
            </section>
          </div>
        </aside>

        <!-- 右侧内容区域 -->
        <main class="main-content">
          
          <!-- Option B: Content-Area Header (Integrated Search) -->
          <section class="content-header">
            <div class="header-bg">
              <img :src="heroImage" alt="Header Background" />
              <div class="header-overlay"></div>
            </div>
            <div class="header-content">
              <div class="text-group">
                <h2>社区寻宝</h2>
                <p>让闲置流动起来</p>
              </div>
              
              <!-- Integrated Large Search Bar -->
              <div class="hero-search-box">
                <div class="search-wrapper">
                  <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  <input 
                    v-model="keyword" 
                    type="text" 
                    placeholder="搜索好物 (例如: 自行车, 沙发...)" 
                    @keyup.enter="doSearch"
                  />
                  <button class="search-btn" @click="doSearch">搜索</button>
                </div>
                <div class="hot-tags">
                  <span>热搜:</span>
                  <a @click="searchTag('二手家具')">二手家具</a>
                  <a @click="searchTag('儿童玩具')">儿童玩具</a>
                  <a @click="searchTag('健身器材')">健身器材</a>
                </div>
              </div>
            </div>
          </section>

          <!-- 商品推荐区 -->
          <section class="card-panel recommendations">
            <!-- 筛选栏 (Simplified, removed search input) -->
            <div class="filter-bar">
              <div class="tabs">
                <span :class="{ active: activeTab === 'recommend' }" @click="activeTab = 'recommend'">为你推荐</span>
              </div>
              <div class="location-filters">
                <div class="radius-group">
                  <button :class="['radius-btn', nearbyRadius===1?'active':'']" @click="toggleNearby(1)">1km</button>
                  <button :class="['radius-btn', nearbyRadius===3?'active':'']" @click="toggleNearby(3)">3km</button>
                  <button :class="['radius-btn', nearbyRadius===5?'active':'']" @click="toggleNearby(5)">5km</button>
                  <button class="radius-btn clear" :class="{active: nearbyRadius === null}" @click="clearNearby">全城</button>
                </div>
                <div class="location-input">
                  <input v-model="addr" type="text" placeholder="定位不准？输入地址..." @keyup.enter="locateByAddress"/>
                  <button class="locate-icon-btn" @click="locateByAddress"><i class="fas fa-map-marker-alt"></i></button>
                </div>
              </div>
            </div>  

            <!-- 商品列表 -->
            <div class="item-grid">
              <div v-if="loading" class="state-box">
                <div class="spinner"></div>
                <p>正在寻找附近的宝贝...</p>
              </div>
              <div v-else-if="errorMsg" class="state-box error">{{ errorMsg }}</div>
              <div v-else-if="products.length === 0" class="state-box empty">暂无相关商品</div>
              
              <div v-else v-for="product in products" :key="product.id" class="item-card" @click="navigateToDetail(product)">
                <div class="image-container">
                  <img :src="getFirstImage(product)" :alt="product.title" @error="(e)=>{e.target.src=FALLBACK_ITEM}" />
                  <div v-if="isDown(product)" class="status-badge down">已下架</div>
                </div>
                
                <div class="card-content">
                  <h3 class="item-title" :title="product.title">{{ product.title }}</h3>
                  
                  <div class="price-row">
                    <div class="price-value">
                      <span class="currency">¥</span>
                      <span class="amount">{{ Number(product.price).toFixed(2) }}</span>
                    </div>
                    <div class="seller-info">
                      <img :src="getSellerAvatar(product)" @error="(e)=>{e.target.src=FALLBACK_AVATAR}"/>
                      <span class="seller-name">{{ formatSellerId(product.seller_id ?? product.sellerId) }}</span>
                    </div>
                  </div>

                  <div class="meta-row">
                    <span class="location-tag">
                      {{ formatLocation(product.location ?? product.loaction) }}
                      <span v-if="product.distanceKm != null" class="dist"> {{ formatDistance(product.distanceKm) }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';
import CebianTool from './cebianTool.vue';
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';

// --- Hero Image ---
const heroImage = new URL('./marketPictures/image3.png', import.meta.url).href; 

// --- Icons ---
const ICONS = {
  add: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>`,
  box: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`,
  receipt: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`,
  cart: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>`,
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
  home: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
  user: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
  headset: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 18v-6a9 9 0 0 1 18 0v6"/><path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/></svg>`,
  phone: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12" y2="18"/></svg>`,
  edit: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>`,
  arrowUp: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="16 12 12 8 8 12"/><line x1="12" y1="16" x2="12" y2="8"/></svg>`,
  chevronRight: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>`,
  chevronLeft: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>`
};

// --- State ---
const activeTab = ref('recommend');
const router = useRouter();
const route = useRoute();
const API_BASE = 'http://localhost:8080'; 
const products = ref([]);
const loading = ref(false);
const errorMsg = ref('');
const keyword = ref(String(route.query.keyword ?? ''));
const showToolbar = ref(false);
const userLat = ref(null);
const userLng = ref(null);
const nearbyRadius = ref(null);
const addr = ref('');
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '');
const chatWindowRef = ref(null);

// --- Chat Logic ---
const agentInput = ref('');
const agentLoading = ref(false);
const agentError = ref('');
const agentMessages = ref([
  {
    id: 'welcome',
    sender: 'agent',
    text: '你好，我是社区助手。我可以帮你快速发布商品，或者帮你寻找附近的宝贝，请直接告诉我你的需求~',
    time: formatChatTime()
  }
]);
const AGENT_CHAT_API = `${API_BASE}/api/agent/chat`;

function scrollToBottom() {
  nextTick(() => {
    if (chatWindowRef.value) {
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
    }
  });
}

const sendAgentMessage = async () => {
  const text = agentInput.value.trim();
  if (!text || agentLoading.value) return;

  const now = new Date();
  agentMessages.value.push({
    id: `${now.getTime()}`,
    sender: 'user',
    text,
    time: formatChatTime(now)
  });
  agentInput.value = '';
  scrollToBottom();

  const agentRequest = {
    messages: agentMessages.value
      .filter(msg => msg.id !== 'welcome')
      .map(msg => ({
        role: msg.sender === 'user' ? 'user' : 'assistant',
        content: msg.text
      }))
  };

  agentLoading.value = true;
  
  try {
    const token = localStorage.getItem('token') || '';
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = token;

    const response = await fetch(AGENT_CHAT_API, {
      method: 'POST',
      headers,
      body: JSON.stringify(agentRequest)
    });
    
    let result = null;
    try { result = await response.json(); } catch(_) { result = null; }

    if (!response.ok || !result || result.code !== 200) {
      throw new Error(result?.message || '服务响应异常');
    }

    const reply = result?.data?.reply?.trim() || '收到';
    agentMessages.value.push({
      id: `${Date.now()}`,
      sender: 'agent',
      text: reply,
      time: formatChatTime()
    });

  } catch (error: any) {
    agentError.value = error.message || '网络错误';
    setTimeout(() => { agentError.value = '' }, 3000);
  } finally {
    agentLoading.value = false;
    scrollToBottom();
  }
};

// --- Data Logic ---
const FALLBACK_ITEM = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="100%" height="100%" fill="%23f4f5f7"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999">暂无图片</text></svg>';
const FALLBACK_AVATAR = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"><circle cx="12" cy="12" r="12" fill="%23e0e0e0"/><text x="12" y="16" text-anchor="middle" fill="%23666" font-size="12" font-family="Arial">U</text></svg>';

function getFirstImage(product) {
  const imgs = product?.image_urls ?? product?.imageUrls;
  if (!imgs) return FALLBACK_ITEM;
  let path = '';
  if (Array.isArray(imgs)) path = imgs[0] || '';
  else if (typeof imgs === 'string') {
    const sanitized = imgs.trim().replace(/`/g, '');
    if (sanitized.startsWith('[')) {
      try {
        const parsed = JSON.parse(sanitized);
        if (Array.isArray(parsed) && parsed.length > 0) path = parsed[0];
      } catch (_) {
        const m = sanitized.match(/(https?:\/\/[^"'\]\s]+|\/[\w\-\/\.]+)/);
        if (m) path = m[1];
      }
    } else path = sanitized;
  }
  if (!path) return FALLBACK_ITEM;
  if (path.startsWith('/')) return `${API_BASE}${path}`;
  return path;
}

function getSellerAvatar(product) {
  const avatar = product?.seller_avatar ?? product?.sellerAvatar;
  if (!avatar) return FALLBACK_AVATAR;
  if (avatar.startsWith('/')) return `${API_BASE}${avatar}`;
  return avatar;
}

function unwrapList(data) {
  if (Array.isArray(data)) return data;
  if (data && typeof data === 'object') {
    for (const key of ['items', 'list', 'data', 'records', 'rows']) {
      if (Array.isArray(data[key])) return data[key];
    }
  }
  return [];
}

function isDown(product) {
  const rawStatus = (product.status ?? product['status']);
  const stock = Number(product.stock_quantity ?? product.stockQuantity ?? 0);
  let downByStatus = false;
  if (rawStatus != null) {
    const s = String(rawStatus).trim();
    if (['下架', '已下架', 'inactive', '2'].includes(s)) downByStatus = true;
  }
  return downByStatus || stock <= 0;
}

async function fetchProducts() {
  loading.value = true;
  errorMsg.value = '';
  try {
    const params = new URLSearchParams();
    const keys = ['keyword','categoryId','subCategoryId','brand','priceMin','priceMax','minRating','sort','order'];
    keys.forEach(k => { if(route.query[k]) params.set(k, String(route.query[k])); });
    if (!params.has('page')) params.set('page', '1');
    if (!params.has('size')) params.set('size', '20'); 

    let res;
    if (nearbyRadius.value && userLat.value != null && userLng.value != null) {
      params.set('lat', String(userLat.value));
      params.set('lng', String(userLng.value));
      params.set('radiusKm', String(nearbyRadius.value));
      params.set('limit', '20');
      res = await fetch(`${API_BASE}/api/products/nearby?${params.toString()}`);
    } else {
      res = await fetch(`${API_BASE}/api/products/getProducts?${params.toString()}`);
    }

    let list = [];
    if (res.ok) {
      const data = await res.json();
      list = Array.isArray(data) ? data : unwrapList(data);
    } else {
      const resAll = await fetch(`${API_BASE}/api/products/getAllProducts`);
      if (resAll.ok) {
        const allData = await resAll.json();
        list = unwrapList(allData).slice(0, 20);
      }
    }
    products.value = list;
  } catch (e) {
    errorMsg.value = '无法加载商品数据';
  } finally {
    loading.value = false;
  }
}

function doSearch() {
  const q = { ...route.query, keyword: keyword.value || undefined, page: '1' };
  router.push({ name: 'CommunityMarketplaceFind', query: q });
}
function searchTag(tag: string) {
  keyword.value = tag;
  doSearch();
}

function navigateToDetail(product) {
  const q = {} as any;
  if (userLat.value) { q.lat = userLat.value; q.lng = userLng.value; }
  const url = router.resolve({ name: 'ProductDetail', params: { id: product.id }, query: q }).href;
  window.open(url, '_blank');
}

function formatDistance(d) {
  const n = Number(d);
  return Number.isFinite(n) ? (n < 1 ? `${Math.round(n*1000)}m` : `${n.toFixed(1)}km`) : '';
}
function formatChatTime(d = new Date()) { return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }); }
function formatSellerId(id) { return id ? (id.length > 6 ? id.substring(0,6)+'...' : id) : '卖家'; }
function formatLocation(loc) { return loc || '同城'; }

async function toggleNearby(r) {
  nearbyRadius.value = r;
  if (!userLat.value) await locateByAddress();
  fetchProducts();
}
function clearNearby() { nearbyRadius.value = null; fetchProducts(); }

async function locateByAddress() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(pos => {
      userLat.value = pos.coords.latitude;
      userLng.value = pos.coords.longitude;
      if (!nearbyRadius.value) nearbyRadius.value = 3;
      fetchProducts();
    }, () => {
      if (addr.value && AMAP_KEY) {
         fetch(`https://restapi.amap.com/v3/geocode/geo?key=${AMAP_KEY}&address=${encodeURIComponent(addr.value)}`)
           .then(r=>r.json()).then(j=>{
             const loc = j.geocodes?.[0]?.location?.split(',');
             if(loc){ userLng.value=parseFloat(loc[0]); userLat.value=parseFloat(loc[1]); fetchProducts(); }
           });
      }
    });
  }
}

onMounted(() => {
  nearbyRadius.value = 3;
  locateByAddress();
  fetchProducts();
});
watch(() => route.query, fetchProducts, { deep: true });
</script>

<style scoped>
:root {
  --primary: #ff7043;
  --primary-dark: #e64a19;
  --bg-page: #eff2f5;
  --bg-card: #ffffff;
  --text-main: #2c3e50;
  --text-light: #95a5a6;
  --border-color: #e6e8eb;
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.04);
}

.nd-page {
  background-color: var(--bg-page);
  min-height: 100vh;
  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  color: var(--text-main);
}

/* Removed full width hero .market-hero styles */

.page-container {
  margin: 20px auto 40px;
  max-width: 1300px;
  padding: 0 24px;
  padding-top: 60px; /* Ensure space below fixed nav */
}

.market-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* --- Sidebar --- */
.sidebar-wrapper {
  flex: 0 0 340px;
  position: sticky;
  top: 90px;
  height: fit-content;
  z-index: 10;
}

.sidebar-wrapper::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 24px;
  background: linear-gradient(180deg, #fff, #fff7f2);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.08);
  pointer-events: none;
  z-index: 0;
}

.sidebar-sticky {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  z-index: 1;
}

/* Agent Chat Card */
.agent-chat-card {
  height: 520px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.chat-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; background: #fff; }
.header-left { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.header-left h3 { margin: 0; font-size: 16px; font-weight: 700; color: #333; }
.status-tag { font-size: 12px; color: #00c853; background: #e8f5e9; padding: 2px 8px; border-radius: 10px; }
.subtitle { margin: 0; font-size: 12px; color: #999; }
.chat-window { flex: 1; background: #f9fafb; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; }
.chat-messages { display: flex; flex-direction: column; gap: 16px; }
.chat-row { display: flex; flex-direction: column; max-width: 85%; }
.chat-row.agent { align-self: flex-start; align-items: flex-start; }
.chat-row.user { align-self: flex-end; align-items: flex-end; }
.chat-bubble { padding: 12px 16px; border-radius: 14px; font-size: 14px; line-height: 1.5; position: relative; word-wrap: break-word; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.agent .chat-bubble { background: #ffffff; color: #333; border: 1px solid #e6e8eb; border-top-left-radius: 2px; }
.user .chat-bubble { background: linear-gradient(135deg, #ff7043, #ff5722); color: #ffffff; border-top-right-radius: 2px; box-shadow: 0 4px 10px rgba(255, 87, 34, 0.2); }
.chat-time { font-size: 11px; color: #bdc3c7; margin-top: 4px; padding: 0 4px; }
.chat-empty { text-align: center; color: #999; margin-top: 40px; font-size: 13px; }
.chat-input-area { padding: 12px 16px; background: #fff; border-top: 1px solid #f0f0f0; display: flex; gap: 10px; }
.chat-input-area input { flex: 1; background: #f5f6fa; border: 1px solid transparent; border-radius: 20px; padding: 10px 16px; font-size: 14px; outline: none; transition: all 0.2s; }
.chat-input-area input:focus { background: #f1f1f1; border-color: var(--primary); }
.send-btn { width: 40px; height: 40px; border-radius: 50%; background: var(--primary); color: #fff; border: none; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: transform 0.2s; box-shadow: 0 10px 20px rgba(255, 112, 67, 0.35); }
.send-btn .send-icon { width: 18px; height: 18px; display: block; }
.send-btn:hover:not(:disabled) { transform: scale(1.05); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.loading .dot { display: inline-block; width: 6px; height: 6px; background: #ccc; border-radius: 50%; margin: 0 2px; animation: bounce 1.4s infinite ease-in-out; }
.loading .dot:nth-child(1) { animation-delay: -0.32s; }
.loading .dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
.chat-error { color: #e74c3c; text-align: center; font-size: 12px; padding: 4px; }

/* --- Main Content --- */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 0;
}

/* --- Option B: Content Header --- */
.content-header {
  position: relative;
  height: 220px;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  padding: 0 40px;
}

.header-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}
.header-bg img { width: 100%; height: 100%; object-fit: cover; }
.header-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.8) 50%, rgba(255,255,255,0.4) 100%);
}

.header-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 600px;
}

.text-group h2 {
  font-size: 28px;
  color: #333;
  margin: 0 0 4px 0;
  font-weight: 800;
}
.text-group p {
  color: #666;
  margin: 0 0 20px 0;
  font-size: 16px;
}

.hero-search-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  background: #fff;
  border: 2px solid var(--primary);
  border-radius: 30px;
  padding: 4px 4px 4px 20px;
  box-shadow: 0 6px 16px rgba(255, 112, 67, 0.15);
  transition: transform 0.2s;
}
.search-wrapper:focus-within { transform: scale(1.02); }

.search-icon { width: 20px; height: 20px; color: #999; margin-right: 10px; }

.search-wrapper input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
}

.search-btn {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 24px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.search-btn:hover { background: var(--primary-dark); }

.hot-tags {
  font-size: 13px;
  color: #888;
  display: flex;
  gap: 10px;
  padding-left: 10px;
}
.hot-tags a { color: #555; cursor: pointer; text-decoration: underline; }
.hot-tags a:hover { color: var(--primary); }


/* Recommendations */
.card-panel {
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.recommendations { padding: 24px; min-height: 400px; }

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.tabs span { font-size: 18px; font-weight: 700; color: #333; position: relative; padding-bottom: 8px; cursor: pointer; }
.tabs span.active::after { content: ''; position: absolute; bottom: -17px; left: 0; width: 100%; height: 3px; background: var(--primary); border-radius: 2px; }

.location-filters { display: flex; gap: 16px; align-items: center; }
.radius-btn { background: #fff; border: 1px solid #e0e0e0; padding: 5px 12px; border-radius: 16px; font-size: 12px; color: #666; cursor: pointer; transition: all 0.2s; }
.radius-btn.active { background: #fff5f2; border-color: var(--primary); color: var(--primary); font-weight: 600; }
.location-input { position: relative; display: flex; align-items: center; }
.location-input input { width: 180px; padding: 6px 32px 6px 12px; border: 1px solid #e0e0e0; border-radius: 16px; font-size: 12px; outline: none; transition: border-color 0.2s; }
.location-input input:focus { border-color: var(--primary); }
.locate-icon-btn { position: absolute; right: 4px; background: none; border: none; color: var(--primary); cursor: pointer; padding: 4px; }

/* Item Grid */
.item-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }
.item-card { background: #fff; border: 1px solid #f0f0f0; border-radius: 10px; overflow: hidden; cursor: pointer; transition: all 0.3s ease; position: relative; }
.item-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.08); border-color: #ffe0b2; }
.image-container { position: relative; padding-top: 85%; background: #f9f9f9; }
.image-container img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; }
.status-badge.down { position: absolute; top: 6px; right: 6px; background: rgba(0,0,0,0.6); color: #fff; font-size: 10px; padding: 2px 4px; border-radius: 4px; backdrop-filter: blur(2px); }
.card-content { padding: 10px; }
.item-title { font-size: 13px; font-weight: 500; color: #2c3e50; margin: 0 0 6px 0; line-height: 1.4; height: 36px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.price-row { color: #e64a19; font-weight: 700; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.price-value { display: flex; align-items: baseline; gap: 1px; }
.currency { font-size: 11px; margin-right: 1px; }
.amount { font-size: 16px; }
.meta-row { display: flex; justify-content: flex-start; align-items: center; font-size: 10px; color: #95a5a6; }
.seller-info { display: flex; align-items: center; gap: 4px; }
.seller-info img { width: 16px; height: 16px; border-radius: 50%; object-fit: cover; border: 1px solid #eee; }
.location-tag { display: flex; align-items: center; gap: 4px; }
.location-tag .dist { color: var(--primary); background: #fff3e0; padding: 0 2px 0; border-radius: 4px; }
.state-box { grid-column: 1 / -1; padding: 60px 0; text-align: center; color: #b0b0b0; }
.spinner { width: 30px; height: 30px; border: 3px solid #f3f3f3; border-top: 3px solid var(--primary); border-radius: 50%; animation: spin 1s infinite linear; margin: 0 auto 15px; }
@keyframes spin { 100% { transform: rotate(360deg); } }
.chat-error { color: #e74c3c; text-align: center; font-size: 12px; padding: 4px; }

@media (max-width: 1200px) {
  .floating-toolbar-container { display: none; }
}
</style>
