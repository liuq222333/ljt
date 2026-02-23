<template>
  <div class="verification-card">
    <h2>手机验证</h2>
    <p>已向您的手机发送验证码，请在下方输入：</p>
    <input type="text" v-model="smsCode" placeholder="请输入6位验证码" />
    <button @click="verifySmsCode">验证</button>
    <button @click="resendSmsCode" :disabled="resendDisabled">{{ resendText }}</button>
  </div>
</template>

<script setup>
import { ref, computed, defineProps, defineEmits } from 'vue';

// 定义组件的 props，接收父组件传递的手机号
const props = defineProps({
  phoneNumber: String,
});

// 定义组件的 emits，用于向父组件发送事件
const emit = defineEmits(['update:phoneNumber', 'verified']);

// 短信验证码
const smsCode = ref('');
// 是否正在倒计时
const isCountingDown = ref(false);
// 倒计时秒数
const countdown = ref(60);
// 定时器
let timer = null;

// 计算手机号是否有效
const isPhoneNumberValid = computed(() => /^1[3-9]\d{9}$/.test(props.phoneNumber));

/**
 * @description 获取短信验证码
 */
const getSmsCode = async () => {
  if (!isPhoneNumberValid.value) {
    alert('请输入有效的手机号');
    return;
  }
  // 开始倒计时
  isCountingDown.value = true;
  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value === 0) {
      clearInterval(timer);
      isCountingDown.value = false;
    }
  }, 1000);

  try {
    // 调用后端接口发送验证码
    const response = await fetch(`http://localhost:8080/api/sms/send-code`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ phoneNumber: props.phoneNumber }),
    });
    if (response.ok) {
      alert('验证码已发送');
    } else {
      alert('获取短信验证码失败');
    }
  } catch (error) {
    console.error('获取短信验证码失败:', error);
    alert('获取短信验证码失败');
  }
};

/**
 * @description 验证短信验证码
 */
const onVerify = async () => {
  try {
    // 调用后端接口验证验证码
    const response = await fetch('http://localhost:8080/api/sms/verify-code', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ phoneNumber: props.phoneNumber, code: smsCode.value }),
    });
    if (response.ok) {
      const result = await response.json();
      if (result) {
        alert('验证成功');
        // 验证成功后，向父组件发送 'verified' 事件
        emit('verified');
      } else {
        alert('验证码错误');
      }
    } else {
      alert('验证失败');
    }
  } catch (error) {
    console.error('验证失败:', error);
    alert('验证失败');
  }
};
</script>

<style scoped>
.verification-card {
  /* 在这里添加您的样式 */
}
</style>