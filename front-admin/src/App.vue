<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand-block">
        <span class="brand-kicker">AAALJT</span>
        <span class="brand">管理端控制台</span>
      </div>
      <div class="topbar-tag">Governance Ready</div>
    </header>
    <GovernanceStatusBanner />
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import GovernanceStatusBanner from './components/governance/GovernanceStatusBanner.vue'
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4fb 0%, #f8fafc 100%);
  color: #1f2933;
}

.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(90deg, #0f172a 0%, #12314b 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.18);
}

.brand-block {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.brand-kicker {
  font-size: 11px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: #38bdf8;
}

.brand {
  font-weight: 700;
  letter-spacing: 0.03em;
}

.topbar-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.14);
  color: #7dd3fc;
  font-size: 12px;
  font-weight: 600;
}

.content {
  padding: 0;
}
</style>
