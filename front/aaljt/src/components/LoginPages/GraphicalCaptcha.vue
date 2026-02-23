<template>
  <div class="captcha-container">
    <canvas ref="canvas" width="120" height="40" @click="generateCaptcha"></canvas>
    <input class="nd-input" type="text" v-model="userInput" placeholder="图形验证码" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const canvas = ref(null);
const userInput = ref('');
let captchaText = '';

const generateCaptcha = () => {
  const ctx = canvas.value.getContext('2d');
  captchaText = Math.random().toString().slice(2, 8);

  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height);
  ctx.fillStyle = '#f0f0f0';
  ctx.fillRect(0, 0, canvas.value.width, canvas.value.height);

  ctx.font = '24px Arial';
  ctx.fillStyle = '#333';
  ctx.fillText(captchaText, 10, 28);
};

const validate = () => {
  return userInput.value === captchaText;
};

onMounted(() => {
  generateCaptcha();
});

defineExpose({ validate });
</script>

<style scoped>
.captcha-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
canvas {
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  cursor: pointer;
}
.nd-input {
  flex: 1;
  height: 40px;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  background: #fff;
}
</style>