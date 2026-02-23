<template>
  <div class="publish-post-container">
    <div class="publish-header">
      <h2>发布动态</h2>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>

    <div class="publish-body">
      <!-- 显示当前用户头像 -->
      <div class="publisher-info" style="display: flex; align-items: center; margin-bottom: 15px;">
        <img 
          :src="currentAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'" 
          alt="Avatar" 
          style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover; margin-right: 10px;"
        />
        <span style="font-weight: 600; font-size: 15px; color: #333;">分享新鲜事...</span>
      </div>

      <div class="input-group">
        <textarea
          v-model="content"
          placeholder="写点什么..."
          rows="6"
        ></textarea>
      </div>

      <div class="media-upload">
        <div class="media-preview" v-for="(img, index) in uploads" :key="index">
          <img :src="img.url" alt="preview" />
          <button class="remove-btn" @click="removeImage(index)">×</button>
        </div>

        <div class="upload-btn" @click="triggerUpload" v-if="uploads.length < 9">
          <i class="fas fa-plus"></i>
          <span>添加图片</span>
          <input
            type="file"
            ref="fileInput"
            multiple
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />
        </div>
      </div>

      <div class="options-bar">
        <div class="option-item" @click="getLocation" :class="{ active: locating || locationText !== '' }">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 256 256" style="margin-right: 4px;">
            <path fill="currentColor" d="M128 60a44 44 0 1 0 44 44a44.05 44.05 0 0 0-44-44m0 64a20 20 0 1 1 20-20a20 20 0 0 1-20 20m0-112a92.1 92.1 0 0 0-92 92c0 77.36 81.64 135.4 85.12 137.83a12 12 0 0 0 13.76 0a259 259 0 0 0 42.18-39C205.15 170.57 220 136.37 220 104a92.1 92.1 0 0 0-92-92m31.3 174.71a249.4 249.4 0 0 1-31.3 30.18a249.4 249.4 0 0 1-31.3-30.18C80 167.37 60 137.31 60 104a68 68 0 0 1 136 0c0 33.31-20 63.37-36.7 82.71"></path>
          </svg>
          <span>{{ locating ? '定位中...' : (locationText === '' ? '所在位置' : locationText) }}</span>
        </div>
        <div class="option-item">
          <i class="fas fa-hashtag"></i>
          <span>#  添加话题</span>
        </div>
        <div class="option-item">
          <i class="fas fa-globe"></i>
          <span>公开可见</span>
        </div>
      </div>
    </div>

    <div class="publish-footer">
      <button class="cancel-btn" @click="$emit('close')">取消</button>
      <button
        class="submit-btn"
        :disabled="(!content.trim() && uploads.length === 0) || isSubmitting"
        @click="submitPost"
      >
        {{ isSubmitting ? '发布中...' : '发布' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { env as ViteEnv } from '@/env';

const props = defineProps({
  currentAvatar: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['close', 'publish']);

const content = ref('');
const uploads = ref<{ key: string; url: string }[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const isSubmitting = ref(false);

// Location state
const locationText = ref('');
const locating = ref(false);
const AMAP_KEY = (
  ViteEnv?.VITE_AMAP_KEY ??
  (import.meta as any)?.env?.VITE_AMAP_KEY ??
  (window as any)?.VITE_AMAP_KEY ??
  ''
);
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo';
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';

// API Base URL
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

async function getLocation() {
  if (!AMAP_KEY) {
    ElMessage.warning('未配置地图密钥');
    return;
  }
  if (!('geolocation' in navigator)) {
    ElMessage.error('当前浏览器不支持定位');
    return;
  }

  locating.value = true;
  locationText.value = '定位中...';

  try {
    const coords = await new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        pos => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        err => reject(err),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    });

    const lat = coords.latitude;
    const lng = coords.longitude;

    // 坐标转换：WGS84 -> GCJ-02
    let useLng = lng;
    let useLat = lat;
    try {
      const convertResp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${lng},${lat}&coordsys=gps`);
      const convertData = await convertResp.json();
      if (convertData.status === '1' && convertData.locations) {
        const parts = String(convertData.locations).split(',');
        if (parts.length === 2) {
          const clng = parseFloat(parts[0]);
          const clat = parseFloat(parts[1]);
          if (!Number.isNaN(clng) && !Number.isNaN(clat)) {
            useLng = clng;
            useLat = clat;
          }
        }
      }
    } catch (_) {
      // 转换失败忽略
    }

    // 逆地理编码
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`);
    if (!resp.ok) throw new Error('定位服务错误');
    
    const data = await resp.json();
    if (data.status !== '1' || !data.regeocode || !data.regeocode.formatted_address) {
      throw new Error('未能解析到地址');
    }

    // 提取更简洁的地址（例如：区/县 + 街道/POI）
    const addressComponent = data.regeocode.addressComponent;
    const district = addressComponent.district || '';
    const township = addressComponent.township || '';
    const streetNumber = addressComponent.streetNumber?.street || '';
    // 优先构建简短地址
    let simpleAddr = district;
    if (township) simpleAddr += '·' + township;
    else if (streetNumber) simpleAddr += '·' + streetNumber;
    
    locationText.value = simpleAddr || data.regeocode.formatted_address;
    ElMessage.success('定位成功');

  } catch (error) {
    console.error('Location error:', error);
    locationText.value = '定位失败';
    ElMessage.error('获取位置失败');
  } finally {
    locating.value = false;
  }
}

function triggerUpload() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;
  const files = Array.from(input.files);
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post(`${API_BASE}/api/community-feed/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (response.data && response.data.key) {
        const previewUrl = response.data.url || URL.createObjectURL(file);
        uploads.value.push({ key: response.data.key, url: previewUrl });
        console.log('Uploaded image:', response.data);
      } else {
        ElMessage.error('图片上传失败');
      }
    } catch (error) {
      console.error('Upload error:', error);
      ElMessage.error('图片上传出错');
    }
  }
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

function removeImage(index: number) {
  uploads.value.splice(index, 1);
}

async function submitPost() {
  if (isSubmitting.value) return;

  isSubmitting.value = true;
  try {
    const payload = {
      content: content.value,
      images: uploads.value.map((i) => i.key),
      userId: localStorage.getItem('userId') || '1',
      locationText: locationText.value,
      visibility: 'PUBLIC'
    };

    const response = await axios.post(`${API_BASE}/api/community-feed/posts`, payload);

    if (response.status === 200) {
      ElMessage.success('发布成功');
      emit('publish', { ...response.data, images: uploads.value.map(i => i.key) });
      emit('close');
    } else {
      ElMessage.error('发布失败');
    }
  } catch (error) {
    console.error('Publish error:', error);
    ElMessage.error('发布出错，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
.publish-post-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.publish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.publish-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.publish-body {
  flex: 1;
  overflow-y: auto;
}

.input-group textarea {
  width: 100%;
  border: none;
  resize: none;
  font-size: 16px;
  line-height: 1.6;
  color: #333;
  outline: none;
  margin-bottom: 20px;
  font-family: inherit;
}

.media-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.media-preview {
  width: 100px;
  height: 100px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}

.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.upload-btn {
  width: 100px;
  height: 100px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: #ff7043;
  color: #ff7043;
  background: #fff5f2;
}

.upload-btn i {
  font-size: 24px;
  margin-bottom: 4px;
}

.upload-btn span {
  font-size: 12px;
}

.options-bar {
  display: flex;
  gap: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 20px;
  transition: background 0.2s;
}

.option-item:hover {
  background: #f5f5f5;
}

.option-item.active {
  background: #fff5f2;
}

.publish-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  padding: 8px 24px;
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.cancel-btn:hover {
  border-color: #999;
  color: #333;
}

.submit-btn {
  padding: 8px 24px;
  background: #ff7043;
  color: #fff;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.submit-btn:hover {
  background: #f4511e;
}

.submit-btn:disabled {
  background: #ffccbc;
  cursor: not-allowed;
}
</style>
