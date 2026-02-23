<template>

  <div class="product-detail-page">
    <div>
        <dhstyle />
    </div>
    <CebianTool />
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载商品详情...</p>
    </div>
    <div v-else-if="error" class="error-state">
      <p>😕 {{ error }}</p>
    </div>
    <div v-else-if="product" class="product-container">
      <!-- Left Side: Image Gallery -->
      <div class="product-gallery">
        <div class="main-image-wrapper">
          <img :src="activeImage" :alt="product.title" @error="onImageError" class="main-image" />
        </div>
        <div class="thumbnail-wrapper">
          <img
            v-for="(image, index) in galleryImages"
            :key="index"
            :src="image"
            :alt="`${product.title} thumbnail ${index + 1}`"
            :class="{ active: image === activeImage }"
            @click="activeImage = image"
            @error="onImageError"
            class="thumbnail-image"
          />
        </div>
      </div>

      <!-- Right Side: Product Information -->
      <div class="product-info">
        <h1 class="title">{{ product.title }}</h1>
        <p class="price">¥ {{ Number(product.price).toFixed(2) }}</p>
        
        <div class="metadata-grid">
          <div class="meta-item">
            <span class="meta-icon">📍</span>
            <span class="meta-label">地点:</span>
            <span class="meta-value">
              {{ product.location }}
              <span v-if="distanceLabel != null"> · 距离约 {{ distanceLabel }}</span>
              <span v-else> · 距离未知</span>
            </span>
          </div>
        
          <div class="meta-item">
            <span class="meta-icon">✨</span>
            <span class="meta-label">成色:</span>
            <span class="meta-value">{{ product.condition }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-icon">📦</span>
            <span class="meta-label">库存:</span>
            <span class="meta-value">{{ product.stockQuantity }}</span>
          </div>
           <div class="meta-item">
            <span class="meta-icon">📅</span>
            <span class="meta-label">发布于:</span>
            <span class="meta-value">{{ formatDate(product.createdAt) }}</span>
          </div>
        </div>

        <p class="description">{{ product.description }}</p>

        <!-- Quantity Selector -->
        <div class="quantity-selector">
          <span class="qty-label">数量</span>
          <div class="qty-controls">
            <button class="qty-btn" @click="decreaseQty" aria-label="减少数量">−</button>
            <input
              class="qty-input"
              type="number"
              v-model.number="quantity"
              :min="1"
              :max="Number(product.stockQuantity) || 999"
            />
            <button class="qty-btn" @click="increaseQty" aria-label="增加数量">＋</button>
          </div>
        </div>

        <div class="seller-card">
          <img :src="cleanedAvatarUrl" alt="Seller Avatar" @error="onAvatarError" class="seller-avatar" />
          <div class="seller-details">
            <span class="seller-name">{{ product.username }}</span>
            <span class="seller-contact">联系方式: {{ product.phone || product.email }}</span>
          </div>
          <button class="contact-seller-btn">联系卖家</button>
        </div>

        <div class="action-buttons">
          <button class="buy-now-btn" @click="buyNow">立即购买</button>
          <button class="add-to-cart-btn" @click="addToCart">加入购物车</button>
        </div>

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
const API_BASE_URL = 'http://localhost:8080/api';
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
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

.product-detail-page {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding: 2rem;
  font-family: 'Noto Sans SC', sans-serif;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.loading-state, .error-state {
  text-align: center;
  padding: 5rem;
  color: #555;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-left-color: #007bff;
  animation: spin 1s ease infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.product-container {
  margin-top: 70px;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 3rem;
  max-width: 1200px;
  width: 100%;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  padding: 2rem;
}

/* Gallery */
.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.main-image-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.main-image {
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.main-image:hover {
    transform: scale(1.05);
}

.thumbnail-wrapper {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 0.75rem;
}

.thumbnail-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.thumbnail-image:hover {
  border-color: #007bff;
  opacity: 0.8;
}

.thumbnail-image.active {
  border-color: #007bff;
  box-shadow: 0 0 12px rgba(0, 123, 255, 0.5);
}

/* Info */
.product-info {
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 2.25rem;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 0.5rem;
  color: #212529;
}

.price {
  font-size: 2.5rem;
  font-weight: 700;
  color: #dc3545;
  margin-bottom: 1.5rem;
}

.description {
  font-size: 1rem;
  line-height: 1.7;
  color: #495057;
  margin-bottom: 2rem;
}

.metadata-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}
.meta-icon {
    font-size: 1.1rem;
}
.meta-label {
  font-weight: 500;
  color: #6c757d;
}
.meta-value {
    color: #343a40;
}

.seller-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 12px;
  margin-top: auto; /* Pushes to the bottom */
  margin-bottom: 1.5rem;
}

.seller-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
}

.seller-details {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.seller-name {
  font-weight: 700;
  color: #343a40;
}

.seller-contact {
  font-size: 0.85rem;
  color: #6c757d;
}

.contact-seller-btn {
  background: none;
  border: 1px solid #007bff;
  color: #007bff;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
}

.contact-seller-btn:hover {
  background: #007bff;
  color: #fff;
}

.action-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.buy-now-btn, .add-to-cart-btn {
  padding: 1rem;
  border: none;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.buy-now-btn {
  background: linear-gradient(45deg, #dc3545, #c82333);
  color: white;
}

.add-to-cart-btn {
  background: linear-gradient(45deg, #007bff, #0056b3);
  color: white;
}

.buy-now-btn:hover, .add-to-cart-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

/* Quantity */
.quantity-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: #f8f9fa;
  border-radius: 10px;
  margin-bottom: 1rem;
}
.qty-label {
  font-weight: 600;
  color: #343a40;
}
.qty-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.qty-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  background: #fff;
  cursor: pointer;
  font-size: 1.25rem;
  line-height: 1;
  transition: all 0.2s ease;
}
.qty-btn:hover { background: #f1f3f5; }
.qty-input {
  width: 64px;
  text-align: center;
  height: 36px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
}

/* Responsive */
@media (max-width: 1024px) {
  .product-detail-page {
    padding: 1rem;
  }
  .product-container {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 1.25rem;
  }
  .metadata-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .thumbnail-wrapper {
    display: flex;
    overflow-x: auto;
    gap: 0.5rem;
  }
  .action-buttons { grid-template-columns: 1fr; }
  .title { font-size: 1.5rem; }
  .price { font-size: 1.75rem; }
}
</style>
