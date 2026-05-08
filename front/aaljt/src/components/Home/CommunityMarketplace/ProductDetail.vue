<template>
  <div class="product-detail-page">
    <dhstyle />
    <CebianTool />

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载商品详情...</p>
    </div>
    <div v-else-if="error" class="error-state">
      <p>😕 {{ error }}</p>
    </div>

    <div v-else-if="product" class="product-shell">
      <div class="product-layout">
        <!-- 左侧主内容：图片 + 商品信息 -->
        <div class="product-main">
          <div class="product-gallery">
            <div class="main-image-wrap">
              <img :src="activeImage" :alt="product.title" @error="onImageError" class="main-image" />
              <span v-if="isDown" class="gallery-badge down">已下架</span>
            </div>
            <div class="thumbnail-strip">
              <img
                v-for="(image, index) in galleryImages"
                :key="index"
                :src="image"
                :alt="`${product.title} ${index + 1}`"
                :class="['thumbnail', { active: image === activeImage }]"
                @click="activeImage = image"
                @error="onImageError"
              />
            </div>
          </div>

          <div class="product-info">
            <div class="title-row">
              <h1 class="title">{{ product.title }}</h1>
              <button class="fav-btn" :class="{ favorited: isFavorited }" @click="toggleFavorite">
                {{ isFavorited ? '♥' : '♡' }}
              </button>
            </div>

            <div class="price-row">
              <p class="price">¥ {{ Number(product.price).toFixed(2) }}</p>
              <span class="price-ref" v-if="refPrice">参考原价 ¥{{ refPrice }}</span>
            </div>

            <div class="chip-row">
              <span class="info-chip delivery">{{ deliveryLabel }}</span>
              <span class="info-chip category">{{ categoryLabel }}</span>
              <span class="info-chip views">👁 {{ viewCount }} 次浏览</span>
            </div>

            <div class="metadata-grid">
              <div class="meta-item">
                <span class="meta-icon">📍</span>
                <span class="meta-label">地点</span>
                <span class="meta-value">{{ product.location }}<span v-if="distanceLabel != null"> · 约 {{ distanceLabel }}</span></span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">✨</span>
                <span class="meta-label">成色</span>
                <span class="meta-value">{{ product.condition || '未标注' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">📦</span>
                <span class="meta-label">库存</span>
                <span class="meta-value">{{ product.stockQuantity || 1 }} 件</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">📅</span>
                <span class="meta-label">发布</span>
                <span class="meta-value">{{ formatDate(product.createdAt) }}</span>
              </div>
            </div>

            <div class="desc-block">
              <h3>商品描述</h3>
              <p>{{ product.description || '卖家暂未填写描述' }}</p>
            </div>

            <div class="quantity-row">
              <span class="qty-label">数量</span>
              <div class="qty-controls">
                <button class="qty-btn" @click="decreaseQty">−</button>
                <input class="qty-input" type="number" v-model.number="quantity" :min="1" :max="Number(product.stockQuantity) || 999" />
                <button class="qty-btn" @click="increaseQty">＋</button>
              </div>
            </div>

            <div class="seller-card">
              <img :src="cleanedAvatarUrl" alt="" @error="onAvatarError" class="seller-avatar" />
              <div class="seller-info">
                <span class="seller-name">{{ product.username }}</span>
                <span class="seller-contact">{{ product.phone || product.email || '暂未留联系方式' }}</span>
              </div>
              <button class="contact-btn">联系卖家</button>
            </div>

            <div class="action-buttons">
              <button class="buy-btn" @click="buyNow" :disabled="isDown">立即购买</button>
              <button class="cart-btn" @click="addToCart" :disabled="isDown">加入购物车</button>
            </div>
          </div>
        </div>

        <!-- 右侧边栏：AI评估 + 卖家其他 + 相似推荐 -->
        <aside class="product-sidebar">
          <section class="sidebar-card ai-panel">
            <div class="sidebar-card-head">
              <h3>AI 智能评估</h3>
              <span class="ai-badge">AI</span>
            </div>
            <p class="ai-desc">分析价格、成色是否靠谱，给出购买参考</p>

            <button class="ai-btn" :disabled="aiLoading" @click="evaluateProduct">
              {{ aiLoading ? '分析中...' : aiResult ? '重新评估' : '开始评估' }}
            </button>

            <div v-if="aiLoading" class="ai-loading">
              <span class="ai-dot"></span><span class="ai-dot"></span><span class="ai-dot"></span>
              <span>AI 分析中...</span>
            </div>

            <div v-if="aiResult" class="ai-result">
              <div class="ai-result-head">
                <span>{{ aiVerdictIcon }}</span>
                <strong>{{ aiVerdictText }}</strong>
              </div>
              <p>{{ aiResult }}</p>
              <span class="ai-meta">AI 生成 · 仅供参考</span>
            </div>

            <div v-if="aiError" class="ai-error">{{ aiError }}</div>
          </section>

          <section v-if="sellerProducts.length" class="sidebar-card">
            <div class="sidebar-card-head">
              <h3>卖家其他在售</h3>
              <span class="card-count">{{ sellerProducts.length }} 件</span>
            </div>
            <div class="side-product-list">
              <div
                v-for="sp in sellerProducts"
                :key="sp.id"
                class="side-product-item"
                @click="$router.push(`/product/${sp.id}`)"
              >
                <img :src="getSideImage(sp)" :alt="sp.title" @error="onImageError" />
                <div class="side-product-info">
                  <strong>{{ sp.title }}</strong>
                  <span>¥{{ Number(sp.price).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </section>

          <section v-if="similarProducts.length" class="sidebar-card">
            <div class="sidebar-card-head">
              <h3>相似推荐</h3>
            </div>
            <div class="side-product-list">
              <div
                v-for="sp in similarProducts"
                :key="sp.id"
                class="side-product-item"
                @click="$router.push(`/product/${sp.id}`)"
              >
                <img :src="getSideImage(sp)" :alt="sp.title" @error="onImageError" />
                <div class="side-product-info">
                  <strong>{{ sp.title }}</strong>
                  <span>¥{{ Number(sp.price).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import dhstyle from '../../dhstyle/dhstyle.vue';
import CebianTool from './cebianTool.vue';

// --- Interfaces and Constants ---
const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? (window as any)?.VITE_API_BASE ?? 'http://localhost:8080';
const API_BASE_URL = `${API_BASE}/api`;
const FALLBACK_AVATAR = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png';
const FALLBACK_IMAGE = 'https://via.placeholder.com/600x600.png?text=Image+Not+Available';
const AMAP_KEY = ((import.meta as any)?.env?.VITE_AMAP_KEY ?? (window as any)?.VITE_AMAP_KEY ?? '');
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';

interface Product {
  id: string;
  sellerId: string;
  categoryId: string;
  title: string;
  description: string;
  price: string;
  stockQuantity: string;
  condition: string;
  location: string;
  imageUrls: string; // JSON string
  status: string;
  createdAt: string;
  updatedAt: string;
  userId: string;
  username: string;
  avatarUrl: string;
  email: string;
  phone: string;
  address: string;
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
}

interface ProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  sortOrder: number;
  createdAt: string;
}

// --- Component State ---
const route = useRoute();
const product = ref<Product | null>(null);
const productImages = ref<ProductImage[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const activeImage = ref('');
const productId = route.params.id;
const quantity = ref(1);
const userLat = ref<number|null>(null);
const userLng = ref<number|null>(null);
const accuracyMeters = ref<number|null>(null);

// --- 新增状态：收藏、浏览量、侧边栏 ---
const isFavorited = ref(false);
const viewCount = ref(Math.floor(Math.random() * 200) + 30);
const sellerProducts = ref<Product[]>([]);
const similarProducts = ref<Product[]>([]);

// --- 收藏切换 ---
function toggleFavorite() {
  isFavorited.value = !isFavorited.value;
  ElMessage.success(isFavorited.value ? '已收藏' : '已取消收藏');
}

// --- 计算属性 ---
const refPrice = computed(() => {
  const p = Number(product.value?.price);
  return Number.isFinite(p) && p > 0 ? Math.round(p * 1.35).toString() : '';
});

const deliveryLabel = computed(() => {
  const d = distanceKmPreferred.value;
  return d != null && d <= 3 ? '同城自提' : '支持邮寄';
});

const categoryLabels: Record<string, string> = {
  phone: '手机数码', computer: '电脑办公', furniture: '家电家具',
  books: '图书文创', fashion: '服饰鞋包', sports: '运动户外'
};
const categoryLabel = computed(() => {
  const id = product.value?.categoryId;
  return id ? (categoryLabels[id] || '其他分类') : '其他分类';
});

const distanceKmPreferred = computed(() => {
  if (distanceKm.value != null) return distanceKm.value;
  const d = (product.value as any)?.distanceKm;
  return d != null ? Number(d) : null;
});


const distanceLabel = computed(() => {
  const d = distanceKmPreferred.value;
  if (d == null) return null;
  if (Number(d) < 1) return `${Math.round(Number(d) * 1000)}m`;
  return `${Number(d).toFixed(1)}km`;
});
const distanceKm = computed(() => {
  if (product.value?.latitude == null || product.value?.longitude == null) return null;
  if (userLat.value == null || userLng.value == null) return null;
  const toRad = (d: number) => d * Math.PI / 180;
  const R = 6371;
  const dLat = toRad(Number(product.value.latitude) - Number(userLat.value));
  const dLng = toRad(Number(product.value.longitude) - Number(userLng.value));
  const lat1 = toRad(Number(userLat.value));
  const lat2 = toRad(Number(product.value.latitude));
  const a = Math.sin(dLat/2)**2 + Math.sin(dLng/2)**2 * Math.cos(lat1) * Math.cos(lat2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
});
const isDown = computed(() => {
  const raw = product.value?.status ?? null;
  const stock = Number(product.value?.stockQuantity ?? 0);
  let downByStatus = false;
  if (raw != null) {
    const s = String(raw).trim();
    if (['下架', '已下架', 'inactive', '2'].includes(s)) downByStatus = true;
    else if (['在售', '出售中', 'active', '1'].includes(s)) downByStatus = false;
    else downByStatus = false;
  }
  return downByStatus || stock <= 0;
});

// --- Computed Properties for data cleaning ---
const parsedImageUrls = computed(() => {
  if (!product.value?.imageUrls) return [FALLBACK_IMAGE];
  try {
    // The imageUrls is a stringified array, e.g., "[\" `url` \"]"
    const urls = JSON.parse(product.value.imageUrls);
    if (Array.isArray(urls) && urls.length > 0) {
      return urls.map(url => url.trim().replace(/`/g, ''));
    }
  } catch (e) {
    console.error('Failed to parse image URLs:', e);
  }
  return [FALLBACK_IMAGE];
});

const galleryImages = computed(() => {
  if (productImages.value.length > 0) {
    return productImages.value.map(img => img.imageUrl);
  }
  if (!product.value?.imageUrls) return [FALLBACK_IMAGE];
  try {
    const urls = JSON.parse(product.value.imageUrls);
    if (Array.isArray(urls) && urls.length > 0) {
      return urls.map(url => url.trim().replace(/`/g, ''));
    }
  } catch (e) {
    console.error('Failed to parse image URLs:', e);
  }
  return [FALLBACK_IMAGE];
});

const cleanedAvatarUrl = computed(() => {
  if (!product.value?.avatarUrl) return FALLBACK_AVATAR;
  // Clean up potential backticks and spaces
  return product.value.avatarUrl.trim().replace(/`/g, '');
});

// --- Lifecycle Hooks ---
onMounted(async () => {
  if (!productId) {
    error.value = '无效的商品ID';
    loading.value = false;
    return;
  }

  try {
    // 1. Fetch main product details
    const productRes = await fetch(`${API_BASE_URL}/products/getProductAndSeller/${productId}`);
    if (!productRes.ok) {
      throw new Error(`无法获取商品信息 (状态: ${productRes.status})`);
    }
    const productData: any = await productRes.json();
    const latAny = productData?.latitude ?? productData?.lat ?? productData?.Latitude ?? productData?.Lat ?? null;
    const lngAny = productData?.longitude ?? productData?.lng ?? productData?.Longitude ?? productData?.Lng ?? null;
    product.value = {
      ...productData,
      latitude: latAny != null ? Number(latAny) : productData?.latitude,
      longitude: lngAny != null ? Number(lngAny) : productData?.longitude,
    } as Product;

    // 2. Fetch product images
    const imagesRes = await fetch(`${API_BASE_URL}/products/getProductImage/${productId}`);
    if (imagesRes.ok) {
      const imagesData: ProductImage[] = await imagesRes.json();
      if (imagesData.length > 0) {
        // Sort images by sortOrder if needed
        imagesData.sort((a, b) => a.sortOrder - b.sortOrder);
        productImages.value = imagesData;
      }
    } else {
      console.warn(`无法获取商品图片 (状态: ${imagesRes.status})`);
    }
    
    // Set initial active image from the new gallery images
    activeImage.value = galleryImages.value[0] || FALLBACK_IMAGE;

  } catch (e) {
    if (e instanceof Error) {
      error.value = e.message;
    } else {
      error.value = '获取商品信息时发生未知错误';
    }
    console.error(e);
  } finally {
    loading.value = false;
  }
  const qLatRaw: any = route.query?.lat ?? null;
  const qLngRaw: any = route.query?.lng ?? null;
  const qLat = qLatRaw != null ? Number(qLatRaw) : null;
  const qLng = qLngRaw != null ? Number(qLngRaw) : null;
  if (qLat != null && qLng != null && Number.isFinite(qLat) && Number.isFinite(qLng)) {
    userLat.value = qLat;
    userLng.value = qLng;
  }
  if (userLat.value == null || userLng.value == null) if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        let lat = pos.coords.latitude;
        let lng = pos.coords.longitude;
        accuracyMeters.value = typeof pos.coords.accuracy === 'number' ? pos.coords.accuracy : null;
        let converted = false;
        if (AMAP_KEY) {
          try {
            const resp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${lng},${lat}&coordsys=gps`);
            const data = await resp.json();
            if (data.status === '1' && data.locations) {
              const parts = String(data.locations).split(',');
              if (parts.length === 2) {
                const clng = parseFloat(parts[0]);
                const clat = parseFloat(parts[1]);
                if (!Number.isNaN(clng) && !Number.isNaN(clat)) {
                  lng = clng;
                  lat = clat;
                  converted = true;
                }
              }
            }
          } catch {}
        }
        if (!converted) {
          const outOfChina = (lng:number, lat:number) => lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
          const transformLat = (x:number,y:number) => {
            let ret=-100+2*x+3*y+0.2*y*y+0.1*x*y+0.2*Math.sqrt(Math.abs(x));
            ret+=(20*Math.sin(6*x*Math.PI)+20*Math.sin(2*x*Math.PI))*2/3;
            ret+=(20*Math.sin(y*Math.PI)+40*Math.sin(y/3*Math.PI))*2/3;
            ret+=(160*Math.sin(y/12*Math.PI)+320*Math.sin(y*Math.PI/30))*2/3;
            return ret;
          };
          const transformLon = (x:number,y:number) => {
            let ret=300+x+2*y+0.1*x*x+0.1*x*y+0.1*Math.sqrt(Math.abs(x));
            ret+=(20*Math.sin(6*x*Math.PI)+20*Math.sin(2*x*Math.PI))*2/3;
            ret+=(20*Math.sin(x*Math.PI)+40*Math.sin(x/3*Math.PI))*2/3;
            ret+=(150*Math.sin(x/12*Math.PI)+300*Math.sin(x/30*Math.PI))*2/3;
            return ret;
          };
          if (!outOfChina(lng, lat)) {
            const a=6378245.0, ee=0.00669342162296594323;
            let dLat=transformLat(lng-105, lat-35);
            let dLon=transformLon(lng-105, lat-35);
            const radLat=lat/180*Math.PI;
            let magic=Math.sin(radLat); magic=1-ee*magic*magic;
            const sqrtMagic=Math.sqrt(magic);
            dLat=(dLat*180)/((a*(1-ee))/(magic*sqrtMagic)*Math.PI);
            dLon=(dLon*180)/(a/sqrtMagic*Math.cos(radLat)*Math.PI);
            lat=lat+dLat; lng=lng+dLon;
          }
        }
        userLat.value = lat;
        userLng.value = lng;
      },
      () => {},
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }

  // 非阻塞加载侧边栏数据
  fetchSellerProducts();
  fetchSimilarProducts();
});
//加入购物车
async function addToCart() {
  // 检查登录态（username）
  const username = localStorage.getItem('username') || '';
  if (!username) {
    ElMessage.warning('请先登录后再加入购物车');
    return;
  }

  // 将当前商品加入购物车（使用后端设置时间，前端保留价格）
  const res = await fetch(`${API_BASE_URL}/carts/add`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      productId: Number(product.value?.id ?? 0),
      quantity: Number(quantity.value ?? 1),
      userName: username,
      price: Number(product.value?.price ?? 0)
    }),
  });
  if (res.ok) {
    ElMessage.success('加入购物车成功');
  } else {
    const msg = await res.text().catch(() => '');
    ElMessage.error(`加入购物车失败${msg ? `：${msg}` : ''}`);
  }
}
//购买商品
async function buyNow() {
  // 检查登录态（username）
  const username = localStorage.getItem('username') || '';
  if (!username) {
    ElMessage.warning('请先登录后再购买');
    return;
  }
  if (isDown.value) { ElMessage.warning('商品已下架或库存不足，无法购买'); return; }
  // 购买商品
  const pid = Number(product.value?.id ?? 0)
  const qty = Number(quantity.value ?? 1)
  const url = `${API_BASE_URL}/carts/buyCartItems?userName=${encodeURIComponent(username)}&productId=${pid}&quantity=${qty}`
  const res = await fetch(url, { method: 'POST' })
  if (res.ok) {
    ElMessage.success('购买成功');
  } else {
    const msg = await res.text().catch(() => '');
    ElMessage.error(`购买失败${msg ? `：${msg}` : ''}`);
  }

}


// --- 获取卖家其他商品 ---
async function fetchSellerProducts() {
  if (!product.value?.userId) return;
  try {
    const res = await fetch(`${API_BASE_URL}/products/seller/${product.value.userId}`);
    if (res.ok) {
      const data = await res.json();
      sellerProducts.value = (Array.isArray(data) ? data : (data.records || data.data || []))
        .filter((p: any) => String(p.id) !== String(productId))
        .slice(0, 4);
    }
  } catch {}
}

// --- 获取相似商品推荐 ---
async function fetchSimilarProducts() {
  try {
    const cat = product.value?.categoryId ? `&categoryId=${product.value.categoryId}` : '';
    const res = await fetch(`${API_BASE_URL}/products/list?page=1&size=4${cat}`);
    if (res.ok) {
      const data = await res.json();
      similarProducts.value = (Array.isArray(data) ? data : (data.records || data.data || []))
        .filter((p: any) => String(p.id) !== String(productId))
        .slice(0, 4);
    }
  } catch {}
}

// --- 获取侧边栏商品图片 ---
function getSideImage(p: any): string {
  if (!p) return FALLBACK_IMAGE;
  try {
    const urls = typeof p.imageUrls === 'string' ? JSON.parse(p.imageUrls) : (Array.isArray(p.imageUrls) ? p.imageUrls : []);
    return Array.isArray(urls) && urls.length > 0 ? urls[0].trim().replace(/`/g, '') : FALLBACK_IMAGE;
  } catch {
    return FALLBACK_IMAGE;
  }
}

// --- Helper Functions ---
function onImageError(e: Event) {
  (e.target as HTMLImageElement).src = FALLBACK_IMAGE;
}

function onAvatarError(e: Event) {
  (e.target as HTMLImageElement).src = FALLBACK_AVATAR;
}

function formatDate(dateString: string) {
  if (!dateString) return '未知日期';
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function decreaseQty() {
  if (quantity.value > 1) quantity.value -= 1;
}

function increaseQty() {
  const max = Number(product.value?.stockQuantity) || 999;
  if (quantity.value < max) quantity.value += 1;
}

// --- AI 智能评估 ---
const AGENT_CHAT_API = `${API_BASE}/api/agent/chat`;
const aiLoading = ref(false);
const aiResult = ref('');
const aiError = ref('');
const aiVerdictText = ref('');
const aiVerdictIcon = ref('');

function buildAiPrompt(): string {
  const p = product.value;
  if (!p) return '';
  return [
    `请帮我评估以下二手商品：`,
    `商品名称：${p.title}`,
    `售价：¥${Number(p.price).toFixed(2)}`,
    `成色：${p.condition || '未标注'}`,
    `描述：${p.description || '无描述'}`,
    `位置：${p.location || '未知'}`,
    `库存：${p.stockQuantity || '未知'}`,
    ``,
    `请从以下几个维度给出简要评估（控制在 150 字以内）：`,
    `1. 价格是否合理`,
    `2. 成色描述是否可信`,
    `3. 是否值得购买`,
    `4. 购买建议`,
  ].join('\n');
}

async function evaluateProduct() {
  const prompt = buildAiPrompt();
  if (!prompt) {
    aiError.value = '商品信息不完整，无法评估';
    return;
  }

  aiLoading.value = true;
  aiError.value = '';
  aiResult.value = '';

  try {
    const token = localStorage.getItem('token') || '';
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = token;

    const response = await fetch(AGENT_CHAT_API, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        messages: [{ role: 'user', content: prompt }],
        userProfile: { scene: 'product-detail-evaluation' }
      })
    });

    const data = await response.json().catch(() => null);
    if (!response.ok || !data || data.code !== 200) {
      throw new Error(data?.message || 'AI 服务响应异常');
    }

    const reply = data?.data?.reply?.trim() || data?.data?.finalAnswer?.answerText?.trim() || '';
    if (!reply) {
      throw new Error('AI 未返回有效内容');
    }

    aiResult.value = reply;

    // 根据回复内容判断倾向
    const lower = reply.toLowerCase();
    if (lower.includes('推荐') || lower.includes('值得') || lower.includes('划算') || lower.includes('建议入手')) {
      aiVerdictText.value = '推荐入手';
      aiVerdictIcon.value = '👍';
    } else if (lower.includes('谨慎') || lower.includes('注意') || lower.includes('偏高') || lower.includes('不值')) {
      aiVerdictText.value = '建议谨慎';
      aiVerdictIcon.value = '🤔';
    } else {
      aiVerdictText.value = '仅供参考';
      aiVerdictIcon.value = '📋';
    }
  } catch (e: any) {
    aiError.value = e?.message || '网络异常，请稍后重试';
  } finally {
    aiLoading.value = false;
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

/* ========== Page Shell ========== */
.product-detail-page {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding: 1.5rem 2rem 3rem;
  font-family: 'Noto Sans SC', sans-serif;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.loading-state, .error-state {
  text-align: center;
  padding: 5rem;
  color: #777;
  font-size: 1rem;
}

.spinner {
  border: 4px solid rgba(0,0,0,0.06);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border-left-color: #3b82f6;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1.2rem;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ========== Shell ========== */
.product-shell {
  max-width: 1280px;
  width: 100%;
  margin-top: 60px;
}

/* ========== Three-Column Layout ========== */
.product-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  align-items: start;
}

.product-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.75rem;
  background: #fff;
  border-radius: 16px;
  padding: 1.75rem;
  box-shadow: 0 2px 16px rgba(0,0,0,0.05);
}

/* ========== Gallery ========== */
.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.main-image-wrap {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #f3f4f6;
  aspect-ratio: 1 / 1;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s ease;
}

.main-image:hover { transform: scale(1.04); }

.gallery-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 14px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #fff;
  backdrop-filter: blur(4px);
}

.gallery-badge.down {
  background: rgba(239, 68, 68, 0.85);
}

.thumbnail-strip {
  display: flex;
  gap: 0.6rem;
  overflow-x: auto;
  padding-bottom: 2px;
}

.thumbnail {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid transparent;
  opacity: 0.6;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.thumbnail:hover {
  opacity: 0.85;
  border-color: #d1d5db;
}

.thumbnail.active {
  opacity: 1;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.25);
}

/* ========== Product Info ========== */
.product-info {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

/* Title + Favorite */
.title-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.title {
  flex: 1;
  font-size: 1.35rem;
  font-weight: 700;
  line-height: 1.45;
  color: #111827;
  margin: 0;
}

.fav-btn {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  font-size: 1.3rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #d1d5db;
  padding: 0;
  line-height: 1;
}

.fav-btn:hover {
  border-color: #fca5a5;
  color: #f87171;
}

.fav-btn.favorited {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #ef4444;
}

/* Price Row */
.price-row {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.price {
  font-size: 1.8rem;
  font-weight: 700;
  color: #e53e3e;
  margin: 0;
  line-height: 1.2;
}

.price-ref {
  font-size: 0.82rem;
  color: #9ca3af;
  text-decoration: line-through;
}

/* Chip Row */
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.info-chip {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.info-chip.delivery {
  background: #eff6ff;
  color: #2563eb;
}

.info-chip.category {
  background: #f5f3ff;
  color: #7c3aed;
}

.info-chip.views {
  background: #f9fafb;
  color: #6b7280;
}

/* Metadata Grid */
.metadata-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
  background: #f9fafb;
  padding: 0.85rem 1rem;
  border-radius: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
}

.meta-icon {
  font-size: 0.95rem;
}

.meta-label {
  font-weight: 500;
  color: #9ca3af;
  white-space: nowrap;
}

.meta-value {
  color: #374151;
}

/* Description */
.desc-block {
  padding: 0.75rem 1rem;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.desc-block h3 {
  margin: 0 0 0.35rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: #374151;
}

.desc-block p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.7;
  color: #6b7280;
  white-space: pre-wrap;
}

/* Quantity */
.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.6rem 1rem;
  background: #f9fafb;
  border-radius: 10px;
}

.qty-label {
  font-weight: 600;
  font-size: 0.88rem;
  color: #374151;
}

.qty-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
  color: #374151;
  transition: all 0.15s ease;
}

.qty-btn:hover { background: #f3f4f6; }

.qty-input {
  width: 56px;
  text-align: center;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.88rem;
  color: #374151;
}

/* Seller Card */
.seller-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: linear-gradient(135deg, #fafbfc, #f0f2f5);
  border-radius: 12px;
  border: 1px solid #eef0f2;
}

.seller-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  flex-shrink: 0;
}

.seller-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.seller-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: #1f2937;
}

.seller-contact {
  font-size: 0.78rem;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-btn {
  flex-shrink: 0;
  background: none;
  border: 1.5px solid #3b82f6;
  color: #3b82f6;
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
  font-size: 0.8rem;
  transition: all 0.2s ease;
}

.contact-btn:hover {
  background: #3b82f6;
  color: #fff;
}

/* Action Buttons */
.action-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-top: auto;
}

.buy-btn, .cart-btn {
  padding: 0.75rem 0;
  border: none;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.buy-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
}

.cart-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.buy-btn:hover, .cart-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0,0,0,0.18);
}

.buy-btn:disabled, .cart-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* ========== Sidebar ========== */
.product-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: sticky;
  top: 1.5rem;
}

.sidebar-card {
  background: #fff;
  border-radius: 14px;
  padding: 1.15rem 1.25rem;
  box-shadow: 0 2px 14px rgba(0,0,0,0.04);
}

.sidebar-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.sidebar-card-head h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
}

.card-count {
  font-size: 0.75rem;
  color: #9ca3af;
  font-weight: 500;
}

/* AI Panel */
.ai-panel {
  border: 1px solid #e2eee5;
  background: linear-gradient(135deg, #f9fdf9, #fdfdfd);
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
  font-size: 0.7rem;
  font-weight: 700;
}

.ai-desc {
  margin: 0 0 0.75rem;
  font-size: 0.8rem;
  color: #6b8b70;
  line-height: 1.55;
}

.ai-btn {
  width: 100%;
  height: 40px;
  border: 1.5px solid #22c55e;
  border-radius: 10px;
  background: #f0fdf4;
  color: #15803d;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ai-btn:hover:not(:disabled) {
  background: #22c55e;
  color: #fff;
}

.ai-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.65rem;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  background: #f8fafc;
  color: #6b8b70;
  font-size: 0.78rem;
}

.ai-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: aiDotPulse 1.4s infinite ease-in-out both;
}

.ai-dot:nth-child(1) { animation-delay: -0.32s; }
.ai-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes aiDotPulse {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.ai-result {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e2eee5;
}

.ai-result-head {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.5rem;
}

.ai-result-head strong {
  font-size: 0.88rem;
  color: #1a3b1f;
}

.ai-result p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.65;
  color: #3c5140;
  white-space: pre-wrap;
}

.ai-meta {
  display: block;
  margin-top: 0.5rem;
  padding-top: 0.45rem;
  border-top: 1px solid #eef4ef;
  font-size: 0.68rem;
  color: #9cb09d;
}

.ai-error {
  margin-top: 0.5rem;
  padding: 0.5rem 0.65rem;
  border-radius: 8px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 0.78rem;
}

/* Side Product List */
.side-product-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.side-product-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.55rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.side-product-item:hover {
  background: #f8fafc;
}

.side-product-item img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f3f4f6;
}

.side-product-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.side-product-info strong {
  font-size: 0.8rem;
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-product-info span {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e53e3e;
}

/* ========== Responsive ========== */
@media (max-width: 1100px) {
  .product-layout {
    grid-template-columns: 1fr;
  }

  .product-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 0.75rem;
  }
}

@media (max-width: 860px) {
  .product-detail-page {
    padding: 0.75rem;
  }

  .product-main {
    grid-template-columns: 1fr;
    gap: 1.25rem;
    padding: 1.25rem;
  }

  .metadata-grid {
    grid-template-columns: 1fr;
  }

  .product-sidebar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 540px) {
  .action-buttons {
    grid-template-columns: 1fr;
  }

  .title {
    font-size: 1.15rem;
  }

  .price {
    font-size: 1.45rem;
  }

  .main-image-wrap {
    aspect-ratio: 4/3;
  }
}
</style>
