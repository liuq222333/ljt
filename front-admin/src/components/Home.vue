<template>
  <div class="settings-layout">
    <aside class="sidebar">
      <nav>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('local-activity')">
            本地活动管理
            <span class="chevron" :class="{ open: open['local-activity'] }"></span>
          </button>
          <ul v-show="open['local-activity']">
            <li>
              <router-link to="/admin/local-activity/review">活动审核</router-link>
            </li>
            <li>
              <router-link to="/admin/local-activity/schedule-review">固定日程审核</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('neighbor')">
            邻里互助管理
            <span class="chevron" :class="{ open: open['neighbor'] }"></span>
          </button>
          <ul v-show="open['neighbor']">
            <li>
              <router-link to="/admin/neighbor-tasks/review">互助审核</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('second-hand')">
            二手市场管理
            <span class="chevron" :class="{ open: open['second-hand'] }"></span>
          </button>
          <ul v-show="open['second-hand']">
            <li>
              <router-link to="/admin/second-hand">二手市场管理</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('community')">
            社区动态管理
            <span class="chevron" :class="{ open: open['community'] }"></span>
          </button>
          <ul v-show="open['community']">
            <li>
              <router-link to="/admin/community">社区动态管理</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('user')">
            用户管理
            <span class="chevron" :class="{ open: open['user'] }"></span>
          </button>
          <ul v-show="open['user']">
            <li>
              <router-link to="/admin/user-management">用户管理</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('notice')">
            通知公告管理
            <span class="chevron" :class="{ open: open['notice'] }"></span>
          </button>
          <ul v-show="open['notice']">
            <li>
              <router-link to="/notifications/publish">发布通知公告</router-link>
            </li>
          </ul>
        </div>
        <div class="nav-group">
          <button class="group-title toggle" @click="toggleGroup('system')">
            系统管理
            <span class="chevron" :class="{ open: open['system'] }"></span>
          </button>
          <ul v-show="open['system']">
            <li>
              <router-link to="/admin/api-management">接口管理</router-link>
            </li>
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
import { ref } from 'vue'

const open = ref<Record<string, boolean>>({
  'local-activity': false,
  'neighbor': false,
  'second-hand': false,
  'community': false,
  'user': false,
  'notice': false,
  'system': false
})

function toggleGroup(id: string) {
  open.value[id] = !open.value[id]
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
  font-size: 18px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 15px;
}

.group-title.toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
}

.chevron {
  display: inline-block;
  transition: transform 0.2s ease;
}

.chevron::before {
  content: '▸';
  font-size: 18px;
  color: #666;
}

.chevron.open {
  transform: rotate(90deg);
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

.content-area {
  flex-grow: 1;
  padding: 40px;
}

.placeholder h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

</style>
