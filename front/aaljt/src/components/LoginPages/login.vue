<template>
    <div class="nd-page">
     <dhstyle />

      <!-- 登录卡片 -->
      <section class="nd-hero">
        <img class="hero-bg" :src="currentImage" :key="currentImage" alt="社区背景" />
        <div class="hero-shade"></div>
        
        <div class="nd-card" v-if="!isLoggedIn">
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
          <!-- 注册 -->
          <template v-if="mode === 'register'">
            <div v-if="verificationStep === 'register'">
              <label class="field-label" for="username">用户名</label>
              <input id="username" class="nd-input" type="text" placeholder="用户名" v-model="registerUsername" />
  
              <label class="field-label" for="pwd">创建密码</label>
              <div class="pwd-group">
                <input
                  id="pwd"
                  class="nd-input"
                  :type="showPwd ? 'text' : 'password'"
                  placeholder="创建密码"
                  v-model="registerPwd"
                />
                <button class="eye" type="button" @click="showPwd = !showPwd" aria-label="切换显示密码">
                  <svg v-if="showPwd" viewBox="0 0 24 24"><path fill="#555" d="M12 5c-4.6 0-8.5 2.9-10 7 1.5 4.1 5.4 7 10 7s8.5-2.9 10-7c-1.5-4.1-5.4-7-10-7zm0 11.3A4.3 4.3 0 1 1 12 7a4.3 4.3 0 0 1 0 8.6z"/></svg>
                  <svg v-else viewBox="0 0 24 24"><path fill="#555" d="M2 12c1.3-3.6 4.6-6.3 8.6-7L8 2l2-2 12 12-2 2-3.4-3.4c-1.1 3.9-4.8 6.7-9.1 6.7C5.5 17.3 2.6 15.2 2 12z"/></svg>
                </button>
              </div>
  
              <GraphicalCaptcha ref="captcha" />
  
              <p class="consent">继续注册，即表示你同意我们的隐私政策、Cookie 政策和条款。</p>
  
              <button class="primary" type="button" @click="checkUsername" :disabled="loading">继续</button>
              <p v-if="registerError" class="error" style="margin-top:8px;">{{ registerError }}</p>
              <p v-if="registerOk" class="success" style="margin-top:8px;">注册成功，请使用该用户名登录</p>
  
              <div class="subline">
                <span>有主意吗？</span>
                <a href="#">开始使用</a>
              </div>
  
              <button class="secondary" type="button">有邀请码吗？</button>
              <div class="login-text"><a href="#" @click.prevent="toLogin">已有账号？登录</a></div>
            </div>
            <div v-if="verificationStep === 'phone'">
              <label class="field-label" for="phone">手机号</label>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="registerForm.phone" @blur="validatePhoneNumber" placeholder="请输入手机号"></el-input>
                <span v-if="phoneError" class="error-message">{{ phoneError }}</span>
              </el-form-item>
              <div class="sms-code-container">
                <input type="text" class="nd-input" v-model="smsCode" placeholder="请输入6位验证码" />
                <button @click="getSmsCode" :disabled="isCountingDown" class="sms-button">{{ resendText }}</button>
              </div>
              <button class="primary" type="button" @click="validateCaptchaAndProceed" :disabled="loading" style="margin-top: 14px;">验证手机号并注册</button>
              <a href="#" @click.prevent="verificationStep = 'register'">返回上一步</a>
            </div>
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
  
      </section>
  
      <!-- 登录成功轻提示 -->
      <div v-if="toastVisible" class="toast success">登录成功</div>
  
      <section class="nd-features">
        <div class="features-inner">
          <h3 class="features-title">通过 Nextdoor 充分利用您的社区</h3>
          <div class="features-grid">
            <div class="feature">
              <div class="ficon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <rect x="6" y="2" width="12" height="20" rx="2" fill="#1AA053" opacity="0.15"/>
                  <path d="M9 14l2 2 4-4" stroke="#1AA053" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="ftitle">基本</div>
              <div class="fdesc">来自邻居、企业和公共服务的实时相关新闻和信息。</div>
            </div>
            <div class="feature">
              <div class="ficon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="10" r="4" fill="#1AA053" opacity="0.15"/>
                  <path d="M12 2c4.4 0 8 3.6 8 8 0 5.4-8 12-8 12S4 15.4 4 10c0-4.4 3.6-8 8-8z" stroke="#1AA053" stroke-width="2" fill="none"/>
                </svg>
              </div>
              <div class="ftitle">当地</div>
              <div class="fdesc">立即与您附近的个人、企业和事件建立联系的第一方法。</div>
            </div>
            <div class="feature">
              <div class="ficon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3 11l9-7 9 7v9H3z" fill="#1AA053" opacity="0.15"/>
                  <path d="M3 11l9-7 9 7" stroke="#1AA053" stroke-width="2" fill="none"/>
                </svg>
              </div>
              <div class="ftitle">信任</div>
              <div class="fdesc">确认所有邻居的安全环境。</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
  import { useRouter } from 'vue-router';
  import GraphicalCaptcha from './GraphicalCaptcha.vue';
  import { ElMessage } from 'element-plus';
  import dhstyle from '../dhstyle/dhstyle.vue';
  
  const router = useRouter();
  const dropdownVisible = ref(false);
  
  // 背景图片轮播
  const images = [
    new URL('@/pictures/homePicture1.jpg', import.meta.url).href,
    new URL('@/pictures/homePicture2.jpg', import.meta.url).href,
    new URL('@/pictures/homePicture3.jpg', import.meta.url).href,
  ];
  const currentIndex = ref(0);
  const currentImage = computed(() => images[currentIndex.value]);
  let timer;
  onMounted(() => {
    timer = setInterval(() => {
      currentIndex.value = (currentIndex.value + 1) % images.length;
    }, 5000);
  });
  onBeforeUnmount(() => {
    if (timer) clearInterval(timer);
  });
  
  // 登录状态管理
  const token = ref(localStorage.getItem('token') || '');
  const username = ref(localStorage.getItem('username') || '');
  const isLoggedIn = computed(() => !!token.value);
  
  const onLoggedIn = (payload) => {
    username.value = payload?.username || localStorage.getItem('username') || '';
    token.value = localStorage.getItem('token') || '';
    showToast();
    router.push('/home');
  };
  
  // 轻提示
  const toastVisible = ref(false);
  const showToast = () => {
    toastVisible.value = true;
    setTimeout(() => {
      toastVisible.value = false;
    }, 1000);
  };
  
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
  // 登出
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    token.value = '';
    username.value = '';
    dropdownVisible.value = false;
    router.push('/');
  };
  
  // from loginCard
  const showPwd = ref(false);
  const mode = ref('login');
  const verificationStep = ref('register'); // 'register', 'phone'
  const captcha = ref(null);
  
  const toLogin = () => {
    mode.value = 'login';
  };
  const toRegister = () => {
    mode.value = 'register';
    verificationStep.value = 'register';
  };
  
  
  const onForgot = () => { /* 可在此触发找回密码流程或路由 */ };
  
  const loginUsername = ref('');
  const loginPwd = ref('');
  const registerUsername = ref('');
  const registerPwd = ref('');
  const registerForm = ref({ username: '', password: '', phone: '' });
  const loginError = ref(false);
  const loading = ref(false);
  const registerError = ref('');
  // --- Phone Verification Logic ---
  const smsCode = ref('');
  const phoneError = ref('');
  
  const validatePhoneNumber = () => {
    const phoneRegex = /^1[3-9]\d{9}$/;
    if (!phoneRegex.test(registerForm.value.phone)) {
      phoneError.value = '请输入有效的11位手机号码';
    } else {
      phoneError.value = '';
    }
  };
  
  const isCountingDown = ref(false);
  const countdown = ref(60);
  let smsTimer = null;
  
  const resendText = computed(() => {
    return isCountingDown.value ? `${countdown.value}s 后重发` : '获取验证码';
  });
  
  const isPhoneNumberValid = computed(() => /^1[3-9]\d{9}$/.test(registerForm.value.phone));
  
  // API base URL
  const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080';

  // 开始倒计时 Helper
  const startCountdown = (seconds) => {
    registerError.value = '';
    if (smsTimer) clearInterval(smsTimer);
    
    isCountingDown.value = true;
    countdown.value = seconds;
    
    smsTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(smsTimer);
        isCountingDown.value = false;
        // 倒计时结束后重置为默认值，方便下次显示
        countdown.value = 60;
      }
    }, 1000);
  };

  // 获取验证码
  const getSmsCode = async () => {
    if (!isPhoneNumberValid.value) {
      registerError.value = '请输入有效的手机号';
      return;
    }
    
    registerError.value = '';
    // 避免重复点击，可以加个简单的锁或利用 loading 状态
    // 此处 isCountingDown 已被 template 中的 :disabled 使用，所以无需额外判断
    
    try {
      const response = await fetch(`${API_BASE}/api/sms/send-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: registerForm.value.phone }),
      });
      
      if (response.ok) {
         // 200 OK: 验证码发送成功，开始 60s 倒计时
         ElMessage.success('验证码已发送');
         startCountdown(60);
      } else if (response.status === 429) {
         // 429 Too Many Requests: 获取剩余时间
         const data = await response.json();
         const remaining = parseInt(data.remaining, 10);
         ElMessage.warning(data.message || '操作过于频繁，请稍后再试');
         
         // 如果后端返回了剩余时间，则同步倒计时
         if (!isNaN(remaining) && remaining > 0) {
            startCountdown(remaining);
         } else {
            // 兜底
            startCountdown(60);
         }
      } else {
         // 其他错误
         const text = await response.text();
         let msg = '获取验证码失败';
         try {
            const json = JSON.parse(text);
            if (json.message) msg = json.message;
         } catch(e) {}
         throw new Error(msg);
      }
    } catch (error) {
      console.error('获取短信验证码失败:', error);
      registerError.value = error.message || '获取短信验证码失败';
      // 发生错误时，不启动倒计时（或停止倒计时），允许用户重试
      isCountingDown.value = false;
      if (smsTimer) clearInterval(smsTimer);
    }
  };
  
    // 检测用户名
    const checkUsername = async () => {
      // 1. 校验图形验证码
      if (!captcha.value.validate()) {
        registerError.value = '图形验证码不正确';
        return;
      }
    
      // 2. 校验用户名和密码是否为空
      const u = registerUsername.value.trim();
      const p = registerPwd.value;
      if (!u) {
        registerError.value = '用户名不能为空';
        return;
      }
      if (!p) {
        registerError.value = '密码不能为空';
        return;
      }
    
      loading.value = true;
      registerError.value = '';
    
      try {
        const res = await fetch(`${API_BASE}/api/UserIsExist?userName=${u}`); // 修正URL大小写
        if (!res.ok) {
          throw new Error('验证用户名服务失败');
        }
        
        const data = await res.json(); // 只读取一次响应体
        
        if (data.exists) { // 使用正确的属性 'exists'
          registerError.value = '用户名已存在';
        } else {
          // 用户名可用，且图形验证码正确，进入手机验证步骤
          registerForm.value.username = u;
          registerForm.value.password = p;
          verificationStep.value = 'phone'; 
        }
      } catch (e) {
        registerError.value = '验证用户名时发生网络错误';
        console.error(e);
      } finally {
        loading.value = false;
      }
    };
    // 登录
    const onLogin = async () => {
      //trim 去掉首尾空格
      //定义userName和password
      const u = loginUsername.value.trim();
      const p = loginPwd.value;
      if (!u || !p) {
        loginError.value = true;
        return;
      }
      loginError.value = false;
      loading.value = true;
      try {
        const res = await fetch(`${API_BASE}/api/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: u, password: p }),
        });
        if (!res.ok) throw new Error('bad');
        const data = await res.json();
        localStorage.setItem('token', data.token);
        const name = data?.username || u;
        localStorage.setItem('username', name);
        localStorage.setItem('userId', data.userId);
        console.log('userId', localStorage.getItem('userId'));
        onLoggedIn({ username: name });
      } catch (e) {
        loginError.value = true;
      } finally {
        loading.value = false;
      }
    };
    // 注册
    const onRegister = async () => {
      const { username, password, phone } = registerForm.value;
      loading.value = true;
      registerError.value = '';
      try {
        const res = await fetch(`${API_BASE}/api/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password, phone }),
        });
        if (!res.ok) {
          const text = await res.text().catch(() => '');
          ElMessage.error(text || '注册失败，请稍后重试');
          return;
        }
        ElMessage.success('注册成功！');
        setTimeout(() => {
            mode.value = 'login';
            loginUsername.value = registerForm.value.username; // 使用 registerForm 中的用户名
            loginPwd.value = '';
            verificationStep.value = 'register';
        }, 1500);
      } catch (e) {
        ElMessage.error('网络异常或服务器错误，请稍后再试');
        console.error(e);
      } finally {
        loading.value = false;
      }
    };
    // 验证码是否正确
    const validateCaptchaAndProceed = async () => {
      try {
        // 使用 API_BASE 替换硬编码的 localhost
        const response = await fetch(`${API_BASE}/api/sms/verify-code`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone: registerForm.value.phone, code: smsCode.value })
        });
    
        if (response.ok) {
          const isVerified = await response.json();
          if (isVerified) {
            await onRegister(); // 验证成功，执行注册
          } else {
            ElMessage.error('手机验证码错误');
          }
        } else {
          ElMessage.error('手机验证码验证失败');
        }
      } catch (error) {
        console.error('验证码验证请求失败:', error);
        ElMessage.error('网络错误，请稍后重试');
      }
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
    justify-content: space-between;
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
  
  .nd-hero {
    position: relative;
    height: 88vh;
    min-height: 720px;
    margin-top: 0;
    display: grid;
    place-items: center;
  }
  .hero-bg {
    position: absolute; inset: 0;
    width: 100%; height: 100%;
    object-fit: cover;
  }
  .hero-shade {
    position: absolute; inset: 0;
    background: linear-gradient(to bottom, rgba(0,0,0,.35) 0%, rgba(0,0,0,0) 40%);
  }
  
  .nd-card {
    position: relative;
    z-index: 1;
    width: 360px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(0,0,0,.2);
    margin: 0;
    padding: 18px 20px 16px;
    overflow: hidden;
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
  
  .sms-code-container {
    display: flex;
    gap: 10px;
    margin-top: 10px;
    align-items: center;
  }
  
  .sms-code-container .nd-input {
    flex-grow: 1;
  }
  
  .sms-button {
    height: 40px;
    border-radius: 8px;
    border: 1px solid #dcdcdc;
    background: #f5f5f5;
    padding: 0 12px;
    cursor: pointer;
    white-space: nowrap;
  }
  
  .sms-button:disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }
  
  
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
  
  .nd-features { background: #fff; padding: 28px 0 48px; border-top: 1px solid rgba(0,0,0,.1); }
  .features-inner { max-width: 1100px; margin: 0 auto; padding: 0 24px; }
  .features-title { font-size: 24px; margin-bottom: 24px; color: #222; }
  .features-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 32px; }
  .feature { text-align: center; }
  .ficon { width: 84px; height: 84px; margin: 0 auto 8px; }
  .ficon svg { width: 100%; height: 100%; }
  .ftitle { font-weight: 700; color: #333; margin: 6px 0; }
  .fdesc { color: #666; font-size: 13px; line-height: 1.5; }
  
  /* 轻提示样式 */
  .toast {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 2000;
    padding: 12px 20px;
    border-radius: 10px;
    color: #fff;
    font-size: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,.3);
    opacity: 0.95;
  }
  .toast.success { background: rgba(0,0,0,0.6); }
  
  @media (max-width: 960px) {
    .nd-card { width: 92%; }
    .nd-hero { height: 80vh; }
    .features-grid { grid-template-columns: 1fr; }
  }
  .login-button-container {
    position: relative;
    margin-left: auto;
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
