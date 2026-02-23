<template>
  <div class="nd-card">
    <h2 class="card-title">{{ mode === 'register' ? '探索您的社区' : '登录您的账户' }}</h2>

    <button class="oauth oauth-google" type="button">
      <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
        <g fill="none" stroke="none">
          <path fill="#EA4335" d="M12 10.2h10.3c.1.6.2 1.2.2 1.8 0 6-4 10-10.5 10-6.2 0-11.2-5-11.2-11.2S5.8.6 12 .6c3.3 0 6.1 1.3 8.1 3.3l-3.3 3.3C15.6 5.9 13.9 5.3 12 5.3c-4 0-7.3 3.3-7.3 7.3S8 19.9 12 19.9c3.7 0 6.4-2.1 6.9-5.1H12v-4.6z"/>
        </g>
      </svg>
      <span>继续使用 Google</span>
    </button>

    <button class="oauth oauth-apple" type="button">
      <svg class="oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M16.3 24c-1.3 0-1.9-.8-3.3-.8-1.4 0-2 .8-3.3.8C8.1 24 6 21.6 6 18.7c0-3.2 2.9-4.7 3-4.7-.8-1.2-.8-3 .1-4.3 1.1-1.4 2.9-1.5 3.6-1.5.9 0 2 .3 2.6 1 .7-.3 1.9-1 3-1-.1.9-.5 2-1.5 2.7.7.1 2 .6 2.7 1.9-1.6.6-2.2 2.2-2.1 3.6 0 2.7 2.2 3.6 2.2 3.7-.1.2-1.1 3.9-3.9 3.9z" fill="#000" />
      </svg>
      <span>继续使用 Apple</span>
    </button>

    <div class="divider"><span>或</span></div>

    <template v-if="mode === 'register'">
      <div v-if="verificationStep === 'register'">
        <label class="field-label" for="username">用户名</label>
        <input id="username" class="nd-input" type="text" placeholder="用户名" v-model="registerUsername" />
        <button class="primary" @click="checkUsername">下一步</button>
      </div>

      <div v-if="verificationStep === 'captcha'">
        <GraphicalCaptcha ref="captcha" />
        <button class="primary" @click="validateCaptchaAndProceed">下一步</button>
      </div>

      <div v-if="verificationStep === 'phone'">
        <label class="field-label" for="phone">手机号</label>
        <input id="phone" class="nd-input" type="text" placeholder="手机号" v-model="phone" />
        <div class="sms-code-container">
          <input id="sms-code" class="nd-input" type="text" placeholder="短信验证码" v-model="smsCode" />
          <button class="secondary" @click="getSmsCode" :disabled="isCountingDown">{{ isCountingDown ? `${countdown}s` : '获取验证码' }}</button>
        </div>
        <button class="primary" @click="onRegister">注册</button>
      </div>

      <p class="consent">继续注册，即表示你同意我们的隐私政策、Cookie 政策和条款。</p>

      <p v-if="registerError" class="error" style="margin-top:8px;">{{ registerError }}</p>
      <p v-if="registerOk" class="success" style="margin-top:8px;">注册成功，请使用该用户名登录</p>

      <div class="subline">
        <span>有主意吗？</span>
        <a href="#">开始使用</a>
      </div>

      <button class="secondary" type="button">有邀请码吗？</button>
      <div class="login-text"><a href="#" @click.prevent="toLogin">已有账号？登录</a></div>
    </template>

    <template v-if="mode === 'login'">
      <label class="field-label" for="user">用户名或邮箱</label>
      <input id="user" class="nd-input" type="text" placeholder="用户名或邮箱" v-model="loginUsername" @keyup.enter="onLogin" />

      <label class="field-label" for="lpwd">密码</label>
      <div class="pwd-group">
        <input
          id="lpwd"
          class="nd-input"
          :type="showPwd ? 'text' : 'password'"
          placeholder="密码"
          v-model="loginPwd"
          @keyup.enter="onLogin"
        />
        <button class="eye" type="button" @click="showPwd = !showPwd" aria-label="切换显示密码">
          <svg v-if="showPwd" viewBox="0 0 24 24"><path fill="#555" d="M12 5c-4.6 0-8.5 2.9-10 7 1.5 4.1 5.4 7 10 7s8.5-2.9 10-7c-1.5-4.1-5.4-7-10-7zm0 11.3A4.3 4.3 0 1 1 12 7a4.3 4.3 0 0 1 0 8.6z"/></svg>
          <svg v-else viewBox="0 0 24 24"><path fill="#555" d="M2 12c1.3-3.6 4.6-6.3 8.6-7L8 2l2-2 12 12-2 2-3.4-3.4c-1.1 3.9-4.8 6.7-9.1 6.7C5.5 17.3 2.6 15.2 2 12z"/></svg>
        </button>
      </div>
      <button class="primary" type="button" @click="onLogin" :disabled="loading" style="margin-top: 14px;">登录</button>
      <p v-if="loginError" class="error">用户名或者密码错误</p>
      <div class="auth-links">
        <a href="#" @click.prevent="onForgot">找回密码</a>
        <span class="sep">·</span>
        <a href="#" @click.prevent="toRegister">立即注册</a>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import GraphicalCaptcha from './GraphicalCaptcha.vue'

const emit = defineEmits(['logged-in'])

const showPwd = ref(false)
const mode = ref('login')
const verificationStep = ref('register'); // 'register', 'captcha', 'phone'
const captcha = ref(null);
const phone = ref('');
const smsCode = ref('');
const isCountingDown = ref(false);
const countdown = ref(60);
let timer = null;

const isPhoneNumberValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value));

const toLogin = () => { mode.value = 'login' }
const toRegister = () => { 
  mode.value = 'register';
  verificationStep.value = 'register';
};

const checkUsername = async () => {
  const u = registerUsername.value.trim();
  if (!u) {
    registerError.value = '请输入用户名';
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/api/getUserByName?userName=${u}`);
    if (res.status === 404) {
      verificationStep.value = 'captcha';
      registerError.value = '';
    } else {
      registerError.value = '用户名已存在';
    }
  } catch (e) {
    registerError.value = '网络异常或服务器错误';
  }
};

const validateCaptchaAndProceed = () => {
  if (captcha.value.validate()) {
    verificationStep.value = 'phone';
  } else {
    alert('图形验证码不正确');
  }
};

const getSmsCode = async () => {
  if (!isPhoneNumberValid.value) {
    alert('请输入有效的手机号');
    return;
  }
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
    const response = await fetch(`${API_BASE}/api/sms/send-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: phone.value }),
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

const onForgot = () => { /* 可在此触发找回密码流程或路由 */ }

const router = useRouter()
const loginUsername = ref('')
const loginPwd = ref('')
const registerUsername = ref('')
const registerPwd = ref('')
const loginError = ref(false)
const loading = ref(false)
const registerError = ref('')
const registerOk = ref(false)
// 后端接口地址：允许通过环境变量覆盖
const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080'

const onLogin = async () => {
//trim 去掉首尾空格
//定义userName和password
  const u = loginUsername.value.trim()
  const p = loginPwd.value
  if (!u || !p) {
    loginError.value = true
    return
  }
  loginError.value = false
  loading.value = true
  try {
    const res = await fetch(`${API_BASE}/api/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p })
    })
    if (!res.ok) throw new Error('bad')
    const data = await res.json()
    localStorage.setItem('token', data.token)
    const name = data?.username || u
    localStorage.setItem('username', name)
    emit('logged-in', { username: name })
  } catch (e) {
    loginError.value = true
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  const u = registerUsername.value.trim();
  const p = registerPwd.value;
  const ph = phone.value;
  const c = smsCode.value;

  if (!u || !p || !ph || !c) {
    registerError.value = '请填写所有字段';
    return;
  }

  loading.value = true;
  registerError.value = '';
  registerOk.value = false;

  try {
    const verifyRes = await fetch(`${API_BASE}/api/sms/verify-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: ph, code: c }),
    });

    if (!verifyRes.ok) {
      registerError.value = '短信验证码错误';
      return;
    }

    const registerRes = await fetch(`${API_BASE}/api/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p, phone: ph }),
    });

    if (!registerRes.ok) {
      if (registerRes.status === 409) {
        registerError.value = '用户名已存在';
      } else {
        const text = await registerRes.text().catch(() => '');
        registerError.value = text || '注册失败，请稍后重试';
      }
      return;
    }

    registerOk.value = true;
    mode.value = 'login';
    loginUsername.value = u;
  } catch (e) {
    registerError.value = '网络异常或服务器错误，请稍后再试';
    console.error(e);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.nd-card {
  position: relative;
  z-index: 1;
  width: 360px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0,0,0,.2);
  margin: 0;
  padding: 18px 20px 16px;
}
.card-title { text-align: center; font-size: 20px; font-weight: 700; }

.oauth { width: 100%; display: flex; align-items: center; gap: 10px; justify-content: center; height: 40px; border-radius: 24px; border: 1px solid #e5e5e5; background: #fff; margin-top: 14px; }
.oauth-google .oauth-icon { width: 20px; height: 20px; }
.oauth-apple .oauth-icon { width: 18px; height: 18px; }
.divider { display: flex; align-items: center; justify-content: center; color: #888; margin: 12px 0; }

.field-label { display: block; font-size: 13px; color: #666; margin: 10px 0 6px; }
.nd-input { width: 100%; height: 40px; border: 1px solid #dcdcdc; border-radius: 8px; padding: 0 12px; font-size: 14px; background: #fff; }
.pwd-group { position: relative; }
.pwd-group .eye { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); border: none; background: transparent; width: 32px; height: 32px; display: grid; place-items: center; }
.pwd-group .eye svg { width: 20px; height: 20px; }

.consent { color: #666; font-size: 12px; line-height: 1.4; margin: 10px 0; }
.primary { width: 100%; height: 42px; border-radius: 24px; background: var(--nd-green, #1AA053); color: #fff; border: none; font-weight: 600; display: inline-block; }
.primary[disabled] { opacity: .7; cursor: not-allowed; }
.subline { display: flex; gap: 6px; justify-content: center; margin: 10px 0; font-size: 13px; color: #666; }
.secondary { width: 100%; height: 36px; border-radius: 20px; background: #fff; border: 1px solid #e5e5e5; }
.login-text { text-align: center; padding-top: 8px; font-size: 13px; }
.login-text a { color: #333; }
.auth-links { text-align: center; padding-top: 8px; font-size: 13px; }
.auth-links a { color: #333; }
.auth-links .sep { margin: 0 6px; color: #999; }
.error { color: #d93025; text-align: center; font-size: 13px; margin-top: 8px; }
.success { color: #1AA053; text-align: center; font-size: 13px; margin-top: 8px; }
.sms-code-container { display: flex; gap: 10px; margin-top: 10px; }
</style>