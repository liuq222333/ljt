<template>
  <div class="nd-page">
    <dhstyle />

    <!-- Hero Section -->
    <div class="service-hero">
      <img class="hero-bg" :src="heroImage" alt="Community Service" />
      <div class="hero-shade"></div>
      <div class="hero-text">
        <h1>社区服务中心</h1>
        <p>获取实时安全警报，查找值得信赖的周边维修、家政服务</p>
        <p>为您打造安心的居住环境</p>
      </div>
    </div>

    <div class="main-container">
      <div class="content-grid">
        <!-- Left Column: Services -->
        <div class="left-column">
          <section class="content-section">
            <div class="section-header">
              <h2>🛠️ 便民服务推荐</h2>
              <span class="more-link">更多服务 ></span>
            </div>
            
            <div class="service-tabs">
              <button :class="{active: currentCategory === 'repair'}" @click="currentCategory = 'repair'">家庭维修</button>
              <button :class="{active: currentCategory === 'cleaning'}" @click="currentCategory = 'cleaning'">家政保洁</button>
              <button :class="{active: currentCategory === 'security'}" @click="currentCategory = 'security'">安防服务</button>
            </div>

            <div class="service-list">
              <div class="service-item" v-for="service in currentServices" :key="service.id">
                <div class="service-thumb">
                   <img :src="service.imageUrl" :alt="service.name" />
                </div>
                <div class="service-details">
                  <div class="service-header-row">
                    <h3>{{ service.name }}</h3>
                    <div class="service-meta-top">
                       <span>接单量: {{ service.orders }}</span>
                    </div>
                  </div>
                  
                  <div class="service-row">
                    <span class="label">评分:</span>
                    <span class="value highlight">{{ service.rating }}</span>
                    <span class="label" style="margin-left: 12px">评价:</span>
                    <span class="value">{{ service.reviews }}条</span>
                    <span class="label" style="margin-left: 12px">状态:</span>
                    <span class="value status-badge">营业中</span>
                  </div>

                  <div class="service-row">
                    <span class="label">标签:</span>
                    <div class="tags-inline">
                      <span v-for="tag in service.tags" :key="tag" class="tag-pill">{{ tag }}</span>
                    </div>
                  </div>

                   <div class="service-row description-row">
                    <span class="label">描述:</span>
                    <span class="value description-text">{{ service.desc }}</span>
                  </div>

                  <button class="contact-btn-small">联系商家</button>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- Right Column: Safety Alerts -->
        <div class="right-column">
          <section class="content-section alerts-section">
            <div class="section-header">
              <h2>社区公告</h2>
              <span class="more-link">全部 ></span>
            </div>
            <div class="alert-list">
              <div class="alert-card" v-for="alert in alerts" :key="alert.id">
                <div class="alert-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#d93025" d="M12 2L1 21h22L12 2zm1 17h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
                </div>
                <div class="alert-content">
                  <h3>{{ alert.title }}</h3>
                  <p class="alert-meta">{{ alert.time }} · {{ alert.location }}</p>
                  <div class="alert-desc">{{ alert.desc }}</div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';

// Hero Image
const heroImage = new URL('@/pictures/homePicture1.jpg', import.meta.url).href;

// Mock Data
const alerts = ref([
  { id: 1, title: '小区北门路面施工提醒', time: '10分钟前', location: '北门入口', desc: '因管道维修，北门入口处暂时封闭，请车辆绕行西门。预计工期2天。' },
  { id: 2, title: '近期防盗安全提示', time: '2小时前', location: '社区警务室', desc: '近期周边发生零星盗窃案件，请各位住户出门关好门窗，注意陌生人尾随。' },
  { id: 3, title: '寻狗启事', time: '5小时前', location: '3号楼附近', desc: '丢失一只金毛犬，名叫“大黄”，希望能有好心人提供线索。' },
]);

const currentCategory = ref('repair');

// Mock Images
const mockImg1 = 'https://images.unsplash.com/photo-1581578731117-104f2a41272e?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80';
const mockImg2 = 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80';
const mockImg3 = 'https://images.unsplash.com/photo-1558611848-73f7eb4001a1?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80';
const mockImg4 = 'https://images.unsplash.com/photo-1505798577917-a651a4d63259?ixlib=rb-1.2.1&auto=format&fit=crop&w=300&q=80';

const servicesData: Record<string, any[]> = {
  repair: [
    { id: 1, name: '强力水电维修', imageUrl: mockImg1, rating: 4.9, reviews: 128, orders: 1250, tags: ['持证上岗', '24小时', '快速上门'], desc: '专业水电维修，20年经验老师傅，承接各类家庭电路改造、水管漏水维修。' },
    { id: 2, name: '极速开锁换锁', imageUrl: mockImg2, rating: 4.8, reviews: 85, orders: 890, tags: ['公安备案', '随叫随到'], desc: '正规备案开锁公司，安全可靠，无损开锁，更换C级锁芯。' },
    { id: 3, name: '家电清洗专家', imageUrl: mockImg3, rating: 4.7, reviews: 210, orders: 3400, tags: ['专业设备', '无隐形消费'], desc: '使用专业高温蒸汽设备，深度清洗空调、油烟机、洗衣机，杀菌消毒。' },
    { id: 4, name: '墙面粉刷翻新', imageUrl: mockImg4, rating: 4.6, reviews: 56, orders: 320, tags: ['环保漆', '工期短'], desc: '立邦/多乐士授权施工，刷新服务，家具保护，完工保洁。' },
  ],
  cleaning: [
    { id: 11, name: '安心家政', imageUrl: mockImg2, rating: 4.9, reviews: 340, orders: 5600, tags: ['实名认证', '保险保障', '全员体检'], desc: '提供日常保洁、擦玻璃、开荒保洁服务。所有阿姨均实名认证，购买保险。' },
    { id: 12, name: '深度保洁', imageUrl: mockImg3, rating: 4.8, reviews: 112, orders: 1200, tags: ['全屋清洁', '除螨', '除甲醛'], desc: '专注高端家庭深度清洁，全屋无死角打扫，进口清洁剂，呵护家居。' },
  ],
  security: [
    { id: 21, name: '智能门锁安装', imageUrl: mockImg4, rating: 4.9, reviews: 78, orders: 450, tags: ['指纹锁', '人脸识别', '免费安装'], desc: '品牌智能锁专卖，提供上门测量、安装、教学一站式服务，售后无忧。' },
    { id: 22, name: '家庭监控布置', imageUrl: mockImg1, rating: 4.7, reviews: 45, orders: 230, tags: ['远程查看', '高清夜视', '双向语音'], desc: '专业安防工程师上门设计，高清摄像头安装，手机远程实时查看。' },
  ]
};

const currentServices = computed(() => servicesData[currentCategory.value] || []);

</script>

<style scoped>
:root {
  --nd-green: #1AA053;
  --nd-text: #222;
  --nd-sub: #666;
  --nd-bg: #f4f5f7;
}

.nd-page {
  background-color: #f4f5f7;
  min-height: 100vh;
  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  color: #222;
}

/* Hero Section */
.service-hero {
  position: relative;
  height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: white;
  margin-top: 0;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5); /* Slightly darker for text readability */
  z-index: 1;
}

.hero-text {
  position: relative;
  z-index: 2;
  padding: 0 20px;
}

.hero-text h1 {
  font-size: 36px;
  margin-bottom: 16px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.hero-text p {
  font-size: 18px;
  opacity: 0.95;
  margin: 8px 0;
  text-shadow: 0 1px 4px rgba(0,0,0,0.3);
}

/* Container */
.main-container {
  max-width: 1200px;
  margin: 20px auto 40px;
  padding: 0 20px;
  position: relative;
  z-index: 10;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px; /* Main content + Sidebar */
  gap: 24px;
  align-items: start;
}

/* Sections */
.content-section {
  background: #fff;
  border-radius: 16px; /* Match login card radius */
  box-shadow: 0 8px 24px rgba(0,0,0,0.08); /* Match login card shadow */
  padding: 28px;
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.more-link {
  font-size: 14px;
  color: #1AA053; /* --nd-green */
  cursor: pointer;
  font-weight: 500;
}
.more-link:hover { text-decoration: underline; }

/* Alerts (Right Column) */
.alerts-section {
  position: sticky;
  top: 90px; /* Sticky sidebar */
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.alert-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fffbfb;
  border: 1px solid #fee2e2;
  border-radius: 12px;
  align-items: flex-start;
  transition: background 0.2s;
}
.alert-card:hover {
  background: #fff5f5;
}

.alert-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.alert-content h3 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #d93025; /* Error red */
  font-weight: 600;
  line-height: 1.4;
}

.alert-meta {
  margin: 0 0 6px;
  font-size: 12px;
  color: #888;
}

.alert-desc {
  font-size: 13px;
  color: #444;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Service Tabs */
.service-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.service-tabs button {
  padding: 8px 24px;
  border-radius: 24px; /* Rounder pill shape */
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  font-weight: 600;
}

.service-tabs button.active {
  background: #1AA053;
  color: white;
  border-color: #1AA053;
  box-shadow: 0 4px 12px rgba(26, 160, 83, 0.3);
}

/* --- New Service List (Enterprise Style) --- */
.service-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.service-item {
  display: flex;
  gap: 24px;
  padding: 24px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background: #fff;
  transition: all 0.2s ease;
}

.service-item:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  border-color: #d0d0d0;
}

.service-thumb {
  width: 160px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.service-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.service-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}

.service-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.service-header-row h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.service-meta-top {
  font-size: 13px;
  color: #888;
}

.service-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
  flex-wrap: wrap;
}

.service-row .label {
  color: #888;
  margin-right: 8px;
}

.service-row .value {
  color: #333;
  font-weight: 500;
}

.service-row .value.highlight {
  color: #ff7043;
  font-weight: 700;
}

.status-badge {
  color: #1AA053 !important;
  background: #e8f5e9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.tags-inline {
  display: flex;
  gap: 6px;
}

.tag-pill {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.description-row {
  margin-top: 4px;
  align-items: flex-start;
}

.description-text {
  flex: 1;
  color: #666;
  line-height: 1.5;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.contact-btn-small {
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #1AA053;
  color: #1AA053;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.contact-btn-small:hover {
  background: #1AA053;
  color: white;
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr; /* Stack on smaller screens */
  }
  .alerts-section {
    position: static; /* Remove sticky when stacked */
    order: -1; /* Put alerts on top if needed, or remove this to keep order */
  }
}

@media (max-width: 768px) {
  .service-hero { height: 240px; }
  .main-container { margin-top: -40px; }
  
  .service-item {
    flex-direction: column;
    gap: 16px;
  }
  .service-thumb {
    width: 100%;
    height: 180px;
  }
  .contact-btn-small {
    position: static;
    width: 100%;
    margin-top: 12px;
  }
}

@media (max-width: 480px) {
  .hero-text h1 { font-size: 24px; }
}
</style>
