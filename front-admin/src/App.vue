<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand">Admin Console</div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f6fa;
  color: #1f2933;
}
.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: #1f2937;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.4px;
}
.content {
  padding: 24px;
}
</style>
