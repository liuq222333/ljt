<template>
    <div class="title">
      <dhstyle />
    <div class="nd-content">
      <div class="nd-content-inner">
        <h1 class="title">Welcome to Nexthome</h1>
        <p>Find your perfect neighbour and connect with them easily.</p>
      </div>
    </div>
    </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import dhstyle from '../dhstyle/dhstyle.vue'

const count = ref(0)
</script>

<style scoped>
.home {
  height: 100%;
  background-color: #f5f5f5;
}
</style>

<style scoped>
:root {
  --nd-green: #1AA053;
  --nd-dark: rgba(0,0,0,.6);
  --nd-bg: #ffffff;
}


.nd-content {
  background-color: #fff;
  width: 100%;
  padding: 100px;
  height: 800px;

}
.nd-content-inner {
  background-color: #fff;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
}
.title {
  font-size: 32px;
  font-weight: 7000;
  margin-bottom: 24px;
  color: #222;
}

</style>