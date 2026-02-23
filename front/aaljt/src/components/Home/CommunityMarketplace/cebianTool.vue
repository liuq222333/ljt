<template>
  <div class="right-sidebar">
    <!-- 首页 -->
    <div class="sidebar-item" @click="router.push({ name: 'CommunityMarketplaceFind' })">
      <div class="icon-box" v-html="ICONS.home"></div>
      <span class="label">首页</span>
    </div>

    <!-- 发布 -->
    <div class="sidebar-item" @click="router.push({ name: 'AddProduct' })">
      <div class="icon-box" v-html="ICONS.add"></div>
      <span class="label">发布</span>
    </div>
    
    <!-- 购物车 -->
    <div class="sidebar-item" @click="router.push({ name: 'ShopCar' })">
      <div class="icon-box" v-html="ICONS.cart"></div>
      <span class="label">购物车</span>
      <span class="badge">4</span>
    </div>
    
    <!-- 我的订单 -->
    <div class="sidebar-item" @click="router.push({ name: 'MyOrder' })">
      <div class="icon-box" v-html="ICONS.receipt"></div>
      <span class="label">我的订单</span>
    </div>

    <!-- 我的商品 -->
    <div class="sidebar-item" @click="router.push({ name: 'MyProducts' })">
      <div class="icon-box" v-html="ICONS.box"></div>
      <span class="label">我的商品</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const ICONS = {
  add: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>`,
  box: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`,
  receipt: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`,
  cart: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>`,
  home: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
};
</script>

<style scoped>
.right-sidebar {
  position: fixed;
  right: 0;
  width: 50px;
  background: #fff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: auto;
  top: 50%;
  bottom: auto;
  transform: translateY(-50%);
  transition: width 0.3s, transform 0.3s;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
  box-shadow: -2px 0 8px rgba(0,0,0,0.1);
}

.sidebar-item {
  width: 100%;
  height: 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #333;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  margin-bottom: 1px;
}
.sidebar-item:hover {
  background: #f5f5f5;
  color: #d7000f;
}
.sidebar-item.active {
  background: #f5f5f5;
  color: #d7000f;
}
.sidebar-item .icon-box {
  width: 20px;
  height: 20px;
}
.sidebar-item .icon-box :deep(svg) {
  width: 100%;
  height: 100%;
}
.sidebar-item .label {
  font-size: 10px;
  margin-top: 2px;
  transform: scale(0.9);
}

.sidebar-item .badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #ff7043;
  color: #fff;
  font-size: 10px;
  border-radius: 10px;
  padding: 0 4px;
  border: 1px solid #fff;
}
</style>
