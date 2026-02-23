<template>
  <dhstyle />
  <CebianTool />
  <div class="find-page">
    <div class="page-content">
      
      <!-- 顶部控制区：搜索 + 筛选 -->
      <aside class="control-panel">
        <!-- 1. 搜索栏 -->
        <div class="search-section">
          <div class="search-input-box">
            <i class="fas fa-search search-icon"></i>
            <input 
              v-model="keyword" 
              type="text" 
              placeholder="搜索商品、分类或想要的好物..." 
              @keyup.enter="doSearch"
            />
            <button class="search-btn" @click="doSearch">搜索</button>
          </div>
        </div>

        <div class="panel-divider"></div>

        <!-- 2. 筛选组 -->
        <div class="filters-wrapper">
          <!-- 分类 -->
          <div class="filter-row">
            <span class="filter-label">分类</span>
            <div class="chip-list">
              <button 
                :class="['chip', selectedCategoryId === '全部' ? 'active' : '']" 
                @click="selectedCategoryId = '全部'"
              >
                全部
              </button>
              <button 
                v-for="c in allCategories" 
                :key="c.id" 
                :class="['chip', selectedCategoryId === String(c.id) ? 'active' : '']" 
                @click="selectedCategoryId = String(c.id)"
              >
                {{ c.name }}
              </button>
            </div>
          </div>

          <!-- 价格 -->
          <div class="filter-row">
            <span class="filter-label">价格</span>
            <div class="chip-list">
              <button 
                :class="['chip', !selectedPrice ? 'active' : '']" 
                @click="selectedPrice = null"
              >
                不限
              </button>
              <button 
                v-for="r in priceRanges" 
                :key="r.label" 
                :class="['chip', isPriceActive(r) ? 'active' : '']" 
                @click="selectedPrice = r"
              >
                {{ r.label }}
              </button>
            </div>
          </div>

          <!-- 快捷筛选 -->
          <div class="filter-row">
            <span class="filter-label">筛选</span>
            <div class="chip-list">
              <button :class="['chip outline', quick.freeShipping ? 'active' : '']" @click="quick.freeShipping = !quick.freeShipping">包邮</button>
              <button :class="['chip outline', quick.invoice ? 'active' : '']" @click="quick.invoice = !quick.invoice">有发票</button>
              <button :class="['chip outline', quick.withImage ? 'active' : '']" @click="quick.withImage = !quick.withImage">仅看有图</button>
              <button :class="['chip outline', quick.highRating ? 'active' : '']" @click="quick.highRating = !quick.highRating">高评分</button>
            </div>
          </div>

          <!-- 范围与位置 (单独一行) -->
          <div class="filter-row">
            <span class="filter-label">范围</span>
            <div class="chip-list">
              <button :class="['chip outline', nearbyRadius === null ? 'active' : '']" @click="clearNearby">不限</button>
              <button :class="['chip outline', nearbyRadius === 1 ? 'active' : '']" @click="setNearby(1)">1km</button>
              <button :class="['chip outline', nearbyRadius === 3 ? 'active' : '']" @click="setNearby(3)">3km</button>
              <button :class="['chip outline', nearbyRadius === 5 ? 'active' : '']" @click="setNearby(5)">5km</button>
              <button :class="['chip outline', nearbyRadius === 10 ? 'active' : '']" @click="setNearby(10)">10km</button>
              
              <!-- 地址输入微调 -->
              <div class="mini-locator">
                <i class="fas fa-map-marker-alt locator-icon"></i>
                <input v-model="addr" type="text" placeholder="定位不准？输入地址" @keyup.enter="locateByAddress" />
                <button class="locator-btn" @click="locateByAddress">定位</button>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 商品结果区域 -->
      <main class="results-section">
        <div class="results-header-card">
          <!-- 排序 Tab -->
          <div class="sort-tabs">
            <span :class="['tab-item', sortBy === 'comprehensive' ? 'active' : '']" @click="setSort('comprehensive')">综合排序</span>
            <span :class="['tab-item', sortBy === 'latest' ? 'active' : '']" @click="setSort('latest')">最新发布</span>
            <div :class="['tab-item', sortBy === 'price' ? 'active' : '']" @click="setSort('price')">
              价格 
              <span class="sort-arrow" v-if="sortBy === 'price'">{{ sortDir === 'asc' ? '↑' : '↓' }}</span>
            </div>
          </div>
          <div class="result-count" v-if="!loading">
            共找到 <span class="count-num">{{ visibleProducts.length }}</span> 件宝贝
          </div>
        </div>

        <!-- 商品列表 Grid -->
        <div class="item-grid">
          <!-- Loading State -->
          <div v-if="loading" class="state-box">
            <div class="spinner"></div>
            <p>正在搜索好物...</p>
          </div>
          
          <!-- Error State -->
          <div v-else-if="errorMsg" class="state-box error">
            <i class="fas fa-exclamation-circle"></i>
            <p>{{ errorMsg }}</p>
          </div>
          
          <!-- Empty State -->
          <div v-else-if="visibleProducts.length === 0" class="state-box empty">
             <img src="https://cdn-icons-png.flaticon.com/512/4076/4076432.png" alt="Empty" width="80">
             <p>暂无相关商品，换个词试试？</p>
          </div>

          <!-- Product Cards -->
          <router-link 
            v-else 
            v-for="product in visibleProducts" 
            :key="product.id" 
            :to="{ name: 'ProductDetail', params: { id: product.id }, query: (userLat!=null&&userLng!=null? { lat: String(userLat), lng: String(userLng) } : {}) }" 
            class="product-card-link"
            target="_blank"
          >
            <div class="item-card">
              <div class="image-container">
                <img :src="getFirstImage(product)" :alt="product.title" @error="(e) => {(e.target as HTMLImageElement).src=FALLBACK_ITEM}" />
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
                    <img :src="getSellerAvatar(product)" @error="(e) => {(e.target as HTMLImageElement).src=FALLBACK_AVATAR}"/>
                    <span class="seller-name">{{ formatSellerId(product.seller_id ?? product.sellerId) }}</span>
                  </div>
                </div>

                <div class="meta-row">
                  <span class="location-tag">
                    {{ formatLocation(product.location ?? product.loaction) }}
                    <span v-if="(product as any).distanceKm != null" class="dist"> {{ formatDistance((product as any).distanceKm) }}</span>
                  </span>
                </div>
              </div>
            </div>
          </router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';
import CebianTool from './cebianTool.vue';
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';

// Type Definitions
interface Product {
  id: number | string;
  title: string;
  price: number | string;
  location?: string;
  loaction?: string;
  image_urls?: string | string[];
  imageUrls?: string | string[];
  seller_id?: number | string;
  sellerId?: number | string;
  seller_avatar?: string;
  sellerAvatar?: string;
  free_shipping?: boolean;
  freeShipping?: boolean;
  invoice?: boolean;
  hasInvoice?: boolean;
  rating?: number | string;
  created_at?: string | number | Date;
  createdAt?: string | number | Date;
  category_id?: number | string;
  categoryId?: number | string;
  sub_category_id?: number | string;
  subCategoryId?: number | string;
}

interface Category {
  id: number | string;
  name: string;
  parentId?: number | string | null;
}

interface PriceRange {
  min: number;
  max: number;
  label: string;
}

const API_BASE = 'http://localhost:8080';

const products = ref<Product[]>([]);
const loading = ref(false);
const errorMsg = ref('');

const keyword = ref('');
const selectedCategoryId = ref<string>('全部');
const selectedPrice = ref<PriceRange | null>(null);
const sortBy = ref('comprehensive');
const sortDir = ref('asc');
const quick = ref({ freeShipping: false, invoice: false, withImage: false, highRating: false });
const userLat = ref<number|null>(null);
const userLng = ref<number|null>(null);
const nearbyRadius = ref<number|null>(null);
const addr = ref<string>('');
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '');
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';

function formatDistance(d: any) {
  const n = Number(d);
  if (!Number.isFinite(n)) return '';
  if (n < 1) return `${Math.round(n * 1000)}m`;
  return `${n.toFixed(1)}km`;
}

function formatSellerId(id: any) {
  const s = String(id ?? '');
  return s.length > 6 ? s.substring(0, 6) + '...' : (s || '卖家');
}

function formatLocation(loc: any) {
  return loc || '同城';
}

function outOfChina(lng: number, lat: number) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}
function transformLat(x: number, y: number) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
  return ret;
}
function transformLon(x: number, y: number) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
  return ret;
}
function wgs84ToGcj02(lng: number, lat: number): [number, number] {
  if (outOfChina(lng, lat)) return [lng, lat];
  const a = 6378245.0;
  const ee = 0.00669342162296594323;
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLon = transformLon(lng - 105.0, lat - 35.0);
  const radLat = lat / 180.0 * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1.0 - ee * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((a * (1.0 - ee)) / (magic * sqrtMagic) * Math.PI);
  dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
  const mgLat = lat + dLat;
  const mgLon = lng + dLon;
  return [mgLon, mgLat];
}

const categories = ref<Category[]>([]);
const allCategories = computed(() => {
  const list = Array.isArray(categories.value) ? categories.value : [];
  return list.filter(c => !!c?.name).map(c => ({ id: c.id, name: c.name, parentId: c.parentId }));
});

const priceRanges = [
  { min: 0, max: 200, label: '0-200' },
  { min: 200, max: 1000, label: '200-1000' },
  { min: 1000, max: 2700, label: '1000-2700' },
  { min: 2700, max: 5000, label: '2700-5000' },
  { min: 5000, max: 8300, label: '5000-8300' },
  { min: 8300, max: 40000, label: '8300-40000+' },
];

const FALLBACK_ITEM = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200"><rect width="100%" height="100%" fill="%23f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-family="sans-serif" font-size="14">暂无图片</text></svg>';
const FALLBACK_AVATAR = 'https://ui-avatars.com/api/?name=User&background=random&color=fff';

function getFirstImage(product: Product) {
  const imgs = product?.image_urls ?? product?.imageUrls;
  if (!imgs) return FALLBACK_ITEM;
  let path = '';
  if (Array.isArray(imgs)) { path = imgs[0] || ''; } 
  else if (typeof imgs === 'string') {
    const sanitized = imgs.trim().replace(/`/g, '');
    if (sanitized.startsWith('[')) {
      try { const parsed = JSON.parse(sanitized); if (Array.isArray(parsed) && parsed.length > 0) path = parsed[0]; } 
      catch (_) { const m = sanitized.match(/(https?:\/\/[^"'\]\s]+|\/[\w\-\/\.]+)/); if (m) path = m[1]; }
    } else { path = sanitized; }
  }
  if (!path) return FALLBACK_ITEM;
  if (path.startsWith('/')) return `${API_BASE}${path}`;
  return path;
}

function getSellerAvatar(product: Product) {
  const avatar = product?.seller_avatar ?? product?.sellerAvatar;
  if (!avatar) return FALLBACK_AVATAR;
  if (avatar.startsWith('/')) return `${API_BASE}${avatar}`;
  return avatar;
}

function isDown(product: Product) {
  const rawStatus = (product as any).status ?? null;
  const stock = Number((product as any).stock_quantity ?? (product as any).stockQuantity ?? 0);
  let downByStatus = false;
  if (rawStatus != null) {
    const s = String(rawStatus).trim();
    if (["下架", "已下架", "inactive", "2"].includes(s)) downByStatus = true;
  }
  return downByStatus || stock <= 0;
}

function doSearch() { keyword.value = (keyword.value || '').trim(); }
function setSort(key: string) { sortBy.value = key; }
function toggleSortDir() { sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'; }
function isPriceActive(r: PriceRange) { const sp = selectedPrice.value; return !!sp && sp.label === r.label; }
function productCategoryIds(p: Product) { const cid = p?.category_id ?? p?.categoryId ?? null; const sid = p?.sub_category_id ?? p?.subCategoryId ?? null; return [cid, sid].filter(Boolean).map(v => String(v)); }

const visibleProducts = computed(() => {
  let list = Array.isArray(products.value) ? products.value.slice() : [];
  if (keyword.value) { const kw = keyword.value.toLowerCase(); list = list.filter(p => (p?.title ?? '').toLowerCase().includes(kw)); }
  if (selectedCategoryId.value && selectedCategoryId.value !== '全部') {
    const target = String(selectedCategoryId.value);
    list = list.filter(p => { const ids = productCategoryIds(p); return ids.includes(target); });
  }
  if (selectedPrice.value) { const { min, max } = selectedPrice.value; list = list.filter(p => { const price = Number(p?.price ?? 0); return price >= min && price < max; }); }
  if (quick.value.withImage) list = list.filter(p => getFirstImage(p) !== FALLBACK_ITEM);
  if (quick.value.freeShipping) list = list.filter(p => (p?.free_shipping ?? p?.freeShipping ?? false) === true);
  if (quick.value.invoice) list = list.filter(p => (p?.invoice ?? p?.hasInvoice ?? false) === true);
  if (quick.value.highRating) list = list.filter(p => Number(p?.rating ?? 0) >= 4);

  if (sortBy.value === 'latest') {
    list.sort((a, b) => { const ta = new Date(a?.created_at ?? a?.createdAt ?? 0).getTime(); const tb = new Date(b?.created_at ?? b?.createdAt ?? 0).getTime(); return tb - ta; });
  } else if (sortBy.value === 'price') {
    list.sort((a, b) => Number(a?.price ?? 0) - Number(b?.price ?? 0));
    if (sortDir.value === 'desc') list.reverse();
  }
  return list;
});

const route = useRoute();

async function fetchCategories() {
  try {
    const res = await fetch(`${API_BASE}/api/categories/getAllCategories`);
    if (!res.ok) throw new Error('Cat err');
    const data: any = await res.json();
    const arr = Array.isArray(data) ? data : (data.items || data.list || data.data || []);
    categories.value = arr.map((c: any) => ({ id: c.id ?? c.categoryId ?? c.Id, name: c.name ?? c.categoryName ?? `Cat${c.id}`, parentId: c.parentId ?? null }));
  } catch (e) { console.warn('加载分类失败', e); }
}

async function fetchProductsFind() {
  loading.value = true; errorMsg.value = '';
  try {
    keyword.value = String(route.query.keyword ?? '');
    const initialCat = String(route.query.subCategoryId ?? route.query.categoryId ?? '');
    selectedCategoryId.value = initialCat || '全部';
    await fetchCategories();
    let res;
    if (nearbyRadius.value && userLat.value != null && userLng.value != null) {
      const params = new URLSearchParams();
      params.set('lat', String(userLat.value)); params.set('lng', String(userLng.value));
      params.set('radiusKm', String(nearbyRadius.value)); params.set('limit', '200');
      res = await fetch(`${API_BASE}/api/products/nearby?${params.toString()}`);
    } else {
      res = await fetch(`${API_BASE}/api/products/getAllProducts`);
    }
    if (!res.ok) throw new Error('Err');
    const data: Product[] = await res.json();
    products.value = Array.isArray(data) ? data : [];
  } catch (e) { errorMsg.value = (e as any)?.message || '加载失败'; }
  finally { loading.value = false; }
}

onMounted(fetchProductsFind);

function requestLocation() {
  return new Promise<void>((resolve, reject) => {
    if (!('geolocation' in navigator)) return reject(new Error('no geo'));
    navigator.geolocation.getCurrentPosition(async (pos) => {
      let lat = pos.coords.latitude, lng = pos.coords.longitude, converted = false;
      if (AMAP_KEY) {
        try {
          const resp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${lng},${lat}&coordsys=gps`);
          const data = await resp.json();
          if (data.status === '1' && data.locations) {
             const p = data.locations.split(',');
             if(p.length===2){ lng=parseFloat(p[0]); lat=parseFloat(p[1]); converted=true; }
          }
        } catch {}
      }
      if (!converted) { const [glng, glat] = wgs84ToGcj02(lng, lat); lng = glng; lat = glat; }
      userLat.value = lat; userLng.value = lng;
      resolve();
    }, reject, { enableHighAccuracy: true, timeout: 8000 });
  });
}

async function setNearby(r: number) {
  nearbyRadius.value = r;
  if (!userLat.value) { try { await requestLocation(); } catch {} }
  fetchProductsFind();
}
function clearNearby() { nearbyRadius.value = null; fetchProductsFind(); }

const GEOCODER_FORWARD = 'https://restapi.amap.com/v3/geocode/geo';
async function locateByAddress() {
  errorMsg.value = '';
  try { await requestLocation(); if (nearbyRadius.value == null) nearbyRadius.value = 1; fetchProductsFind(); return; } catch {}
  const v = (addr.value || '').trim();
  if (v && AMAP_KEY) {
    try {
      const r = await fetch(`${GEOCODER_FORWARD}?key=${AMAP_KEY}&address=${encodeURIComponent(v)}`);
      const j = await r.json();
      const loc = j?.geocodes?.[0]?.location?.split(',');
      if (loc) { userLng.value = parseFloat(loc[0]); userLat.value = parseFloat(loc[1]); if (!nearbyRadius.value) nearbyRadius.value=1; fetchProductsFind(); return; }
    } catch {}
  }
  errorMsg.value = '定位失败，请检查权限或地址';
}
</script>

<style scoped>
:root {
  --primary: #1AA053;
  --primary-light: #e8f5e9;
  --bg-page: #f9fafb;
  --bg-card: #ffffff;
  --text-main: #222;
  --text-sec: #666;
  --border: #e0e0e0;
  --radius-lg: 16px;
  --radius-md: 8px;
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.04);
  --shadow-hover: 0 8px 24px rgba(0,0,0,0.08);
}

.find-page {
  padding-top: 80px;
  background-color: var(--bg-page);
  min-height: 100vh;
  font-family: 'PingFang SC', sans-serif;
  color: var(--text-main);
}

.page-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* --- Control Panel (Search + Filters) --- */
.control-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  border: 1px solid rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-section {
  display: flex;
  justify-content: center;
}

.search-input-box {
  width: 100%;
  max-width: 700px;
  display: flex;
  align-items: center;
  background: #fff;
  border: 2px solid #e0e0e0; /* Thicker border */
  border-radius: 24px;
  padding: 4px 4px 4px 20px;
  transition: all 0.3s;
}
.search-input-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(26, 160, 83, 0.15);
}

.search-icon { color: #b2bec3; font-size: 16px; margin-right: 10px; }
.search-input-box input { flex: 1; border: none; background: transparent; font-size: 15px; outline: none; color: var(--text-main); }
.search-btn {
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 10px 28px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s;
}
.search-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(26, 160, 83, 0.3); }

.panel-divider {
  height: 1px;
  background: #eee;
  margin: 0 10px;
}

/* Filters */
.filters-wrapper {
  display: flex;
  flex-direction: column;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px dashed #e0e0e0;
}
.filter-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.filter-label {
  min-width: 60px;
  font-weight: 700;
  color: #333;
  font-size: 14px;
  padding-top: 6px;
  text-align: left; /* Ensure left alignment */
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  flex: 1;
  padding-left: 0;
  margin-left: 0; /* Ensure no margin left */
}

.chip {
  border: 1px solid transparent;
  background: #f3f4f6;
  color: #555;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.chip:hover { background: #e5e7eb; color: #000; }
.chip.active {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
  font-weight: 700;
}

/* Enhanced Outline Chips */
.chip.outline {
  background: #fff;
  border: 1px solid #ccc; /* Darker border */
  color: #444;
}
.chip.outline:hover { border-color: #999; }
.chip.outline.active {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
  font-weight: 700;
}

/* Mini Locator */
.mini-locator {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #ccc; /* Stronger border */
  border-radius: 20px;
  padding: 4px 4px 4px 14px;
  margin-left: 8px;
  transition: border-color 0.2s;
}
.mini-locator:focus-within { border-color: var(--primary); }
.locator-icon { color: var(--primary); margin-right: 6px; font-size: 14px; }
.mini-locator input {
  border: none;
  background: transparent;
  width: 140px;
  font-size: 13px;
  outline: none;
  color: #333;
}
.locator-btn {
  background: #333;
  color: #fff;
  border: none;
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  margin-left: 6px;
}
.locator-btn:hover { background: #000; }

/* --- Results Section --- */
.results-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.results-header-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sort-tabs { display: flex; gap: 28px; }

.tab-item {
  font-size: 15px;
  color: var(--text-sec);
  cursor: pointer;
  position: relative;
  padding-bottom: 6px;
  transition: color 0.2s;
}
.tab-item:hover { color: var(--primary); }
.tab-item.active {
  color: var(--text-main);
  font-weight: 700;
}
.tab-item.active::after {
  content: ''; position: absolute; bottom: -6px; left: 0; width: 100%; height: 3px; background: var(--primary); border-radius: 2px;
}
.sort-arrow { font-size: 12px; margin-left: 2px; }

.result-count { font-size: 13px; color: #666; }
.count-num { color: var(--primary); font-weight: 700; margin: 0 2px; }

/* --- Grid --- */
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.product-card-link { text-decoration: none; display: block; }

.item-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
  border-color: #ffe0b2;
}

.image-container {
  position: relative;
  padding-top: 85%;
  background: #f9f9f9;
  border-bottom: 1px solid #f0f0f0;
}

.image-container img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
}

.status-badge.down {
  position: absolute;
  top: 6px; right: 6px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 4px;
  backdrop-filter: blur(2px);
}

.card-content {
  padding: 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 6px 0;
  line-height: 1.4;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-row {
  color: #e64a19;
  font-weight: 700;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.price-value { display: flex; align-items: baseline; gap: 1px; }
.currency { font-size: 11px; margin-right: 1px; }
.amount { font-size: 16px; }

.meta-row {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  font-size: 10px;
  color: #95a5a6;
}

.seller-info { display: flex; align-items: center; gap: 4px; }
.seller-info img { width: 16px; height: 16px; border-radius: 50%; object-fit: cover; border: 1px solid #eee; }
.seller-name { font-size: 11px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 80px; }

.location-tag { display: flex; align-items: center; gap: 4px; }
.location-tag .dist { color: var(--primary); background: #fff3e0; padding: 0 2px 0; border-radius: 4px; }

/* States */
.state-box {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: #b2bec3;
}
.spinner {
  width: 32px; height: 32px; border: 3px solid #eee; border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 16px;
}
@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
