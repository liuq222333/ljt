<template>
  <div>
    <header class="nd-header">
      <div class="nd-header-inner">
        <div class="brand">
          <svg class="leaf" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 12c6.5-7 13-9 18-9-2 6-6 12-12 18-3-3-5-6-6-9z" fill="#1AA053" />
          </svg>
          <span class="brand-text">Nexthome</span>
        </div>
        <nav class="nd-nav">
          <a @click.prevent="goToHome" href="/home" class="nav-link" style="cursor:pointer">Neighbour</a>
          <a @click.prevent="goToPublicServices" href="/publicServices" class="nav-link" style="cursor:pointer">Public Services</a>
          <a @click.prevent="goToBusiness" href="/business" class="nav-link" style="cursor:pointer">Business</a>
        </nav>
        <div class="login-button-container">
          <button class="login-pill" @click="isLoggedIn ? toggleDropdown() : goToLogin()">
            {{ isLoggedIn ? username : '登录' }}
          </button>
          <div v-if="isLoggedIn && dropdownVisible" class="dropdown-menu">
            <div class="dropdown-item" @click="goToUserCenter">个人中心</div>
            <div class="dropdown-item" @click="goToShopCar">购物车</div>
            <div class="dropdown-item" @click="logout">退出登录</div>
          </div>
        </div>
      </div>
    </header>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const dropdownVisible = ref(false);

// 登录状态管理
const token = ref(localStorage.getItem('token') || '');
const username = ref(localStorage.getItem('username') || '');
const isLoggedIn = computed(() => !!token.value);


// 导航
const goToHome = () => {
  router.push('/home');
};

const goToLogin = () => {
  router.push('/');
};

const goToPublicServices = () => {
  router.push('/publicServices');
};

const goToBusiness = () => {
  router.push('/business');
};

const toggleDropdown = () => {
  dropdownVisible.value = !dropdownVisible.value;
};

const goToUserCenter = () => {
  router.push('/user/settings');
  dropdownVisible.value = false;
};

const goToShopCar = () => {
  router.push('/shop-car');
  dropdownVisible.value = false;
};

const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  token.value = '';
  username.value = '';
  dropdownVisible.value = false;
  router.push('/');
};
</script>


<style scoped>
:root {
  --nd-green: #1AA053;
  --nd-dark: rgba(0,0,0,.6);
  --nd-bg: #ffffff;
}

.nd-page {
  color: #222;
}

.nd-header {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 1000;
  background: rgba(0,0,0,0.35);
  height: 70px;
}
.nd-header-inner {
  max-width: none;
  width: 100%;
  margin: 0 auto;
  height: 70px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 15px 0 24px;
  color: #fff;
}
.brand { display: flex; align-items: center; gap: 8px; }
.leaf { width: 28px; height: 28px; }
.brand-text { font-weight: 700; letter-spacing: .2px; font-size: 20px; }
.nd-nav { display: flex; gap: 20px; flex: 1; }
.nav-link { 
  color: #fff; 
  opacity: .9; 
  font-size: 16px; 
  padding: 8px 16px;
  border-radius: 24px;
  transition: all 0.3s ease;
  position: relative;
  text-decoration: none;
}

.nav-link:hover {
  opacity: 1;
  background-color: rgba(255, 255, 255, 0.15);
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}
.login-pill {
  border: none;
  background: rgba(0,0,0,.4);
  color: #fff;
  padding: 10px 18px;
  border-radius: 22px;
  font-size: 15px;
  cursor: pointer;
}

.login-button-container {
  position: relative;
  margin-left: auto;
  display: flex;
  align-items: center;
}
.dropdown-menu {
  position: absolute;
  top: calc(100% + 5px);
  right: 0;
  background: rgba(0,0,0,.6);
  border-radius: 10px;
  padding: 5px 0;
  z-index: 100;
  width: 120px;
  list-style: none;
  margin: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,.3);
}
.dropdown-item {
  padding: 8px 16px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  text-align: center;
}
.dropdown-item:hover {
  background-color: rgba(255,255,255,.1);
}
</style>