<template>
  <div class="settings-page">
    <div class="container">
      <div class="title-row">
        <h2>User Info</h2>
        <button @click="loadinfo = true" class="edit-btn">update</button>
      </div>
      <div class="info-card">
        <!-- Avatar Row -->
        <div class="info-row">
          <div class="info-label">
            <h3>Avatar</h3>
            <p>JPG, PNG, or GIF (max 2 MB)</p>
          </div>
          <div class="info-value">
            <img src="./userPictures/user1.jpg" alt="User Avatar" class="avatar-img" />
          </div>
        </div>

        <!-- Name Row -->
        <div class="info-row">
          <div class="info-label">
            <h3>Name</h3>
            <p>Your profile name</p>
          </div>
          <div class="info-value">
            <span>{{ username }}</span>
            <button @click="loadname = true" class="edit-btn">&#9998;</button>
          </div>
        </div>

        <!-- Email Row -->
        <div class="info-row">
          <div class="info-label">
            <h3>Email</h3>
            <p>Your login email</p>
          </div>
          <div class="info-value">
            <span>{{ userInfo?.email }}</span>
          </div>
        </div>
        <!-- 手机号 Row -->
        <div class="info-row">
          <div class="info-label">
            <h3>Phone</h3>
            <p>Your login phone</p>
          </div>
          <div class="info-value">
            <span>{{ userInfo?.phone }}</span>
          </div>
        </div>
        <!-- 地址 Row -->
        <div class="info-row">
          <div class="info-label">
            <h3>Address</h3>
            <p>Your login address</p>
          </div>
          <div class="info-value">
            <span>{{ userInfo?.address }}</span>
          </div>
        </div>
      </div>
    </div>
     <!-- 更新用户名窗口 -->
    <div v-if="loadname" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Edit Username</h2>
          <button @click="loadname = false" class="close-button">&times;</button>
        </div>
        <div class="modal-body">
          <input type="text" v-model="username" placeholder="Enter new username" maxlength="20" />
          <span class="char-counter">{{ username.length }}/20</span>
        </div>
        <div class="modal-footer">
          <button @click="loadname = false" class="cancel-btn">Cancel</button>
          <button @click="updateUserNameById" class="ok-btn">OK</button>
        </div>
      </div>
    </div>

     <!-- 更新用户信息窗口 -->
    <div v-if="loadinfo" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Edit User Info</h2>
          <button @click="loadinfo = false" class="close-button">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="message.text" :class="`message ${message.type}`">
            {{ message.text }}
          </div>
          <div v-if="userInfo">
            <div class="input-group">
              <label for="email">Email</label>
              <input type="email" id="email" v-model="userInfo.email">
            </div>
            <div class="input-group">
              <label for="phone">Phone</label>
              <input type="text" id="phone" v-model="userInfo.phone">
            </div>
            <div class="input-group">
              <label for="address">Address</label>
              <div class="address-input-row">
                <input type="text" id="address" v-model="userInfo.address">
                <button class="icon-btn" @click="locateAndFillAddress" :disabled="locating" :title="locating ? '定位中...' : '定位填充'" aria-label="定位填充">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 256 256">
                    <path fill="currentColor" d="M128 60a44 44 0 1 0 44 44a44.05 44.05 0 0 0-44-44m0 64a20 20 0 1 1 20-20a20 20 0 0 1-20 20m0-112a92.1 92.1 0 0 0-92 92c0 77.36 81.64 135.4 85.12 137.83a12 12 0 0 0 13.76 0a259 259 0 0 0 42.18-39C205.15 170.57 220 136.37 220 104a92.1 92.1 0 0 0-92-92m31.3 174.71a249.4 249.4 0 0 1-31.3 30.18a249.4 249.4 0 0 1-31.3-30.18C80 167.37 60 137.31 60 104a68 68 0 0 1 136 0c0 33.31-20 63.37-36.7 82.71"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="loadinfo = false" class="cancel-btn">Cancel</button>
          <button @click="updateUserInfoById" class="ok-btn">OK</button>
        </div>
      </div>
    </div>
    <!-- 用户安全板块 -->
    <div class="container">
      <div class="title-row">
        <h2 style="margin-top: 40px;">User Save</h2>
      </div>
      <div class="info-card">
        <!-- logout Row -->
        <div class="info-row">
          <div class="info-label">
            <div>登出</div>
          </div>
          <div class="info-value">
            <button @click="showLogoutConfirm = true" class="action-btn logout-btn">Logout</button>
          </div>
        </div>

        <!-- 修改密码 -->
        <div class="info-row">
          <div class="info-label">
            <div>修改密码</div>
          </div>
          <div class="info-value">
            <button @click="loadupdatePwd = true" class="action-btn change-pwd-btn">修改密码</button>
          </div>
        </div>

        <!-- 删除用户 -->
        <div class="info-row">
          <div class="info-label">
            <div>删除用户</div>
          </div>
          <div class="info-value">
            <button @click="showDeleteConfirm = true" class="action-btn change-pwd-btn">删除用户</button>
          </div>
        </div>

      </div>
    </div>
    

  </div>

  <!-- Logout Confirm Modal -->
   <!-- 登出确认窗口 -->
  <div v-if="showLogoutConfirm" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Confirm Logout</h2>
        <button @click="showLogoutConfirm = false" class="close-button">&times;</button>
      </div>
      <div class="modal-body">
        <p>确定要退出登录吗？</p>
      </div>
      <div class="modal-footer">
        <button @click="showLogoutConfirm = false" class="cancel-btn">Cancel</button>
        <button @click="handleConfirmLogout" class="ok-btn">OK</button>
      </div>
    </div>
  </div>

  <!-- Change Password Modal -->
  <!-- 修改密码窗口 -->
  <div v-if="loadupdatePwd" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Change Password</h2>
        <button @click="loadupdatePwd = false" class="close-button">&times;</button>
      </div>
      <div class="modal-body">
        <div v-if="pwdMessage.text" :class="`message ${pwdMessage.type}`">
          {{ pwdMessage.text }}
        </div>
        <div class="input-group">
          <label for="oldPwd">原密码</label>
          <input id="oldPwd" type="password" v-model="oldPassword" placeholder="输入原密码" />
        </div>
        <div class="input-group">
          <label for="newPwd">新密码</label>
          <input id="newPwd" type="password" v-model="newPassword" placeholder="输入新密码" />
        </div>
      </div>
      <div class="modal-footer">
        <button @click="loadupdatePwd = false" class="cancel-btn">Cancel</button>
        <button @click="handleChangePassword" class="ok-btn">OK</button>
      </div>
    </div>
  </div>
  <!-- 删除用户确认窗口 -->
  <div v-if="showDeleteConfirm" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Confirm Delete</h2>
        <button @click="showDeleteConfirm = false" class="close-button">&times;</button>
      </div>
      <div class="modal-body">
        <p>确定要删除用户吗？</p>
      </div>
      <div class="modal-footer">
        <button @click="showDeleteConfirm = false" class="cancel-btn">Cancel</button>
        <button @click="deleteConfirm=true" class="ok-btn">OK</button>
      </div>
    </div>
  </div>
  <!-- 删除用户确认窗口 -->
  <div v-if="deleteConfirm" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Confirm Delete</h2>
        <button @click="deleteConfirm=false" class="close-button">&times;</button>
      </div>
      <div class="modal-body">
        <p>请输入用户密码</p>
        <input type="password" v-model="deletePassword" placeholder="输入密码" />
      </div>
      <div class="modal-footer">
        <button @click="deleteConfirm=false" class="cancel-btn">Cancel</button>
        <button @click="handleDeleteUser" class="ok-btn">OK</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { env as ViteEnv } from '@/env';

// Reactive state
const username = ref(localStorage.getItem('username') || '');
const API_BASE = import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080';
const loadname = ref(false);
const loadinfo = ref(false);
const message = ref({ text: '', type: '' });
const showLogoutConfirm = ref(false);
const loadupdatePwd = ref(false);
const oldPassword = ref('');
const newPassword = ref('');
const pwdMessage = ref<{ text: string; type: 'success' | 'error' | '' }>({ text: '', type: '' });
const locating = ref(false);
const AMAP_KEY = (
  ViteEnv?.VITE_AMAP_KEY ??
  (import.meta as any)?.env?.VITE_AMAP_KEY ??
  (window as any)?.VITE_AMAP_KEY ??
  ''
);
const GEOCODER_BASE = 'https://restapi.amap.com/v3/geocode/regeo';
const COORD_CONVERT_BASE = 'https://restapi.amap.com/v3/assistant/coordinate/convert';
const GEOCODER_FORWARD = 'https://restapi.amap.com/v3/geocode/geo';
const router = useRouter();
const showDeleteConfirm = ref(false);
const deleteConfirm=ref(false);
const deletePassword=ref('');
const coordsLocked = ref(false);

// User data interface and state
interface UserInfo {
  userId: string;
  userName: string;
  email: string;
  phone: string;
  address: string;
  password: string;
  latitude?: number;
  longitude?: number;
}
const userInfo = ref<UserInfo | null>(null);

// Fetch user info on component mount
async function getUserInfofetch() {
  try {
    const res = await fetch(`${API_BASE}/api/getUserByName?userName=${username.value}`);
    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(`Bad request: ${res.status} ${res.statusText} - ${errorText}`);
    }
    const data = await res.json();
    userInfo.value = data as UserInfo;
  } catch (error) {
    console.error('Error fetching user info:', error);
  }
}

//改名
async function updateUserNameById() {
  try {
    if (!username.value) {
      console.error('Username is not available.');
      return;
    }
    const checkRes = await fetch(`${API_BASE}/api/UserIsExist?userName=${username.value}`);
    if (!checkRes.ok) {
      throw new Error('Failed to check username existence');
    }
    const checkData = await checkRes.json();
    if (checkData.exists) {
      alert('Username already exists!');
      loadname.value = true;
      return;
    }

    const res = await fetch(`${API_BASE}/api/user/updateUser?userId=${userInfo.value?.userId}&userName=${username.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(`Bad request: ${res.status} ${res.statusText} - ${errorText}`);
    }
    localStorage.setItem('username', username.value);
    alert('修改成功！');
    window.location.reload();
  } catch (error) {
    console.error('Error updating user info:', error);
  }
}

// Update user info (email, phone, address)
async function updateUserInfoById() {
  message.value = { text: '', type: '' }; // Reset message
  try {
    if (!coordsLocked.value && AMAP_KEY && (userInfo.value?.address || '').trim() && (userInfo.value?.latitude == null || userInfo.value?.longitude == null)) {
      try {
        const r = await fetch(`${GEOCODER_FORWARD}?key=${AMAP_KEY}&address=${encodeURIComponent((userInfo.value?.address || '').trim())}`);
        const j = await r.json();
        const g = Array.isArray(j?.geocodes) ? j.geocodes[0] : null;
        const loc = g?.location ? String(g.location).split(',') : null;
        const glng = loc && loc[0] ? parseFloat(loc[0]) : null;
        const glat = loc && loc[1] ? parseFloat(loc[1]) : null;
        if (glat != null && glng != null && !Number.isNaN(glat) && !Number.isNaN(glng)) {
          userInfo.value = { ...(userInfo.value as UserInfo), latitude: glat, longitude: glng };
        }
      } catch {}
    }
    const userInfoDTO = {
      userId: userInfo.value?.userId,
      userName: userInfo.value?.userName,
      email: userInfo.value?.email,
      phone: userInfo.value?.phone,
      address: userInfo.value?.address,
      latitude: userInfo.value?.latitude,
      longitude: userInfo.value?.longitude
    };
    const res = await fetch(`/api/user/updateUserInfo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(userInfoDTO)
    });
    const contentType = res.headers.get('content-type') || '';
    let data: { message?: string } = {};
    if (contentType.includes('application/json')) {
      data = await res.json();
    } else {
      const text = await res.text();
      data = { message: text };
    }
    if (!res.ok) {
      throw new Error(data.message || `Bad request: ${res.status} ${res.statusText}`);
    }
    message.value = { text: data.message || '修改成功！', type: 'success' };
    setTimeout(() => {
      loadinfo.value = false; // Close modal after 1.5s
    }, 1500);
  } catch (error) {
    const err = error as Error;
    message.value = { text: `更新失败: ${err.message}`, type: 'error' };
    console.error('Error updating user info:', error);
  }
}

// Lifecycle hook
onMounted(() => {
  getUserInfofetch();
});

// 登出
function logout() {
  try {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    username.value = '';
    userInfo.value = null;
    router.push('/');
  } catch (e) {
    console.error('Logout failed:', e);
  }
}

function handleConfirmLogout() {
  showLogoutConfirm.value = false;
  logout();
}

// 修改密码
async function handleChangePassword() {
  pwdMessage.value = { text: '', type: '' };
  try {
    if (!userInfo.value?.userId) {
      pwdMessage.value = { text: '用户信息未加载', type: 'error' };
      return;
    }
    if (!oldPassword.value || !newPassword.value) {
      pwdMessage.value = { text: '请输入原密码和新密码', type: 'error' };
      return;
    }
    const res = await fetch(`/api/changePassword`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userInfo.value.userId,
        oldPassword: oldPassword.value,
        newPassword: newPassword.value
      })
    });
    const contentType = res.headers.get('content-type') || '';
    let data: { message?: string } = {};
    if (contentType.includes('application/json')) {
      data = await res.json();
    } else {
      const text = await res.text();
      data = { message: text };
    }
    if (!res.ok) {
      pwdMessage.value = { text: data.message || '修改失败', type: 'error' };
      return;
    }
    pwdMessage.value = { text: data.message || '修改成功！', type: 'success' };
    setTimeout(() => {
      loadupdatePwd.value = false;
      oldPassword.value = '';
      newPassword.value = '';
    }, 1500);
  } catch (e) {
    const err = e as Error;
    pwdMessage.value = { text: `修改失败: ${err.message}`, type: 'error' };
    console.error('Change password error:', e);
  }
}

// 删除用户
async function handleDeleteUser() {
  if (!deletePassword.value) {
    alert('请输入密码');
    return;
  }else{
    try {
      const res = await fetch(`/api/user/deleteUser?userId=${userInfo.value?.userId}&password=${deletePassword.value}`, {
        method: 'DELETE'
      });
      const contentType = res.headers.get('content-type') || '';
      let data: { message?: string } = {};
      if (contentType.includes('application/json')) {
        data = await res.json();
      } else {
        const text = await res.text();
        data = { message: text };
      }
      if (!res.ok) {
        throw new Error(data.message || `Bad request: ${res.status} ${res.statusText}`);
      }
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      username.value = '';
      userInfo.value = null;
      router.push('/');
    } catch (error) {
      console.error('Error deleting user:', error);
    }
  }
}

//高德地图定位服务
async function locateAndFillAddress() {
  message.value = { text: '', type: '' };
  if (!AMAP_KEY) {
    message.value = { text: '未配置地图密钥：请设置 VITE_AMAP_KEY', type: 'error' };
    return;
  }
  if (!('geolocation' in navigator)) {
    message.value = { text: '当前浏览器不支持定位', type: 'error' };
    return;
  }
  locating.value = true;
  try {
    const coords = await new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        pos => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        err => reject(err),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    });
    const lat = coords.latitude;
    const lng = coords.longitude;
    alert(`当前定位坐标：纬度 ${lat.toFixed(6)}, 经度 ${lng.toFixed(6)}`);
    // 坐标转换：WGS84 -> GCJ-02（提升逆地理精度）
    let useLng = lng;
    let useLat = lat;
    try {
      const convertResp = await fetch(`${COORD_CONVERT_BASE}?key=${AMAP_KEY}&locations=${lng},${lat}&coordsys=gps`);
      const convertData = await convertResp.json();
      // 检查转换是否成功，且返回了有效坐标，才更新 useLng 和 useLat，否则回退使用原始坐标
      if (convertData.status === '1' && convertData.locations) {
        // 解析转换后的坐标字符串，检查是否包含有效经度和纬度
        const parts = String(convertData.locations).split(',');
        // 检查转换后的字符串是否包含经度和纬度，且都是有效数字，如果是，则更新 useLng 和 useLat
        if (parts.length === 2) {
          const clng = parseFloat(parts[0]);
          const clat = parseFloat(parts[1]);
          // 检查转换后的经度和纬度是否都是有效数字，如果是，则更新 useLng 和 useLat
          if (!Number.isNaN(clng) && !Number.isNaN(clat)) {
            useLng = clng;
            useLat = clat;
            alert(`转换后的坐标：纬度 ${useLat.toFixed(6)}, 经度 ${useLng.toFixed(6)}`);
          }
        }
      }
    } catch (_) {
      // 转换失败则回退使用原始坐标
    }

    // 逆地理编码：获取详细地址信息
    const resp = await fetch(`${GEOCODER_BASE}?key=${AMAP_KEY}&location=${useLng},${useLat}&extensions=base`);
    if (!resp.ok) {
      const t = await resp.text();
      throw new Error(`定位服务错误: ${resp.status} ${resp.statusText} - ${t}`);
    }
    const data = await resp.json();
    if (data.status !== '1' || !data.regeocode || !data.regeocode.formatted_address) {
      throw new Error('未能解析到地址');
    }
    const formatted = data.regeocode.formatted_address as string;
    userInfo.value = { ...(userInfo.value as UserInfo), address: formatted, latitude: useLat, longitude: useLng };
    coordsLocked.value = true;
    message.value = { text: '已根据定位填充地址', type: 'success' };
  } catch (e) {
    const err: any = e;
    let msg = '定位失败';
    if (err && typeof err === 'object' && 'code' in err) {
      const code = Number(err.code);
      if (code === 1) msg = '定位权限被拒绝';
      else if (code === 2) msg = '位置不可用';
      else if (code === 3) msg = '定位超时';
    } else if (err && typeof err.message === 'string') {
      msg = err.message || msg;
    }
    message.value = { text: msg, type: 'error' };
  } finally {
    locating.value = false;
  }
}
</script>

<style scoped>
  .settings-page {
    padding: 2rem;
    color: #333;
  }
  .title-row {
    display: flex;
    justify-content: space-between;
  }
  .container {
    max-width: 1000px;
    margin: 0 auto;
  }

  .container h2 {
    font-size: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .info-card {
    background-color: rgb(243, 232, 232);
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    overflow: hidden;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #eaeaea;
  }

  .info-row:last-child {
    border-bottom: none;
  }

  .info-label h3 {
    margin: 0 0 0.25rem 0;
    font-size: 1rem;
    font-weight: 500;
  }

  .info-label p {
    margin: 0;
    color: #666;
    font-size: 0.875rem;
  }

  .info-value {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .avatar-img {
    width: 48px;
    height: 48px;
    border-radius: 50%;
  }

  .edit-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1.2rem;
    color: #666;
  }

  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }

  .modal-content {
    background-color: #2d2d2d;
    color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
    width: 400px;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 10px;
    margin-bottom: 20px;
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.25rem;
  }

  .close-button {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: #aaa;
  }

  .modal-body {
    position: relative;
  }

  .modal-body input {
    width: 100%;
    padding: 10px;
    border: 1px solid #555;
    border-radius: 4px;
    box-sizing: border-box;
    background-color: #333;
    color: white;
  }

  .message {
    padding: 10px;
    margin-bottom: 15px;
    border-radius: 4px;
    color: #fff;
    text-align: center;
  }
  
  .message.success {
    background-color: #28a745;
  }
  
  .message.error {
    background-color: #dc3545;
  }
  
  .input-group {
    margin-bottom: 15px;
  }

  .input-group label {
    display: block;
    margin-bottom: 5px;
    color: #ccc;
  }

  .char-counter {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    color: #888;
    font-size: 0.9rem;
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }

  .modal-footer button {
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    margin-left: 10px;
  }

  .cancel-btn {
    background-color: #444;
    color: white;
  }

  .ok-btn {
    background-color: #f0f0f0;
    color: #111;
  }

  /* Unified action button style for Settings page */
  .action-btn {
    padding: 8px 14px;
    border: 1px solid #665a5a;
    background-color: #9c6969;
    color: #333;
    border-radius: 6px;
    font-size: 0.95rem;
    cursor: pointer;
    transition: background-color 0.2s, color 0.2s, border-color 0.2s, box-shadow 0.2s;
  }

  .action-btn:hover {
    background-color: #f0f0f0;
    border-color: #bbb;
  }

  .action-btn:active {
    box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.12);
  }

  /* Logout button variant (subtle danger, aligned with page’s light style) */
  .logout-btn {
    background-color: rgb(243, 218, 203);
    color: #0c0000;
    border-color: #e6b8bd;
  }

  .logout-btn:hover {
    background-color: #ffecec;
    color: #a8231e;
    border-color: #e79aa3;
  }

  /* Change password button variant (neutral primary) */
  .change-pwd-btn {
    background-color: rgb(243, 218, 203);
    color: #111;
    border-color: #bbb;
  }

  .change-pwd-btn:hover {
    background-color: #f2f2f2;
    border-color: #a9a9a9;
  }

  .address-input-row {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .mini-btn {
    padding: 6px 10px;
    font-size: 0.85rem;
  }

  .mini-btn[disabled] {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* 图标按钮：用于定位填充 */
  .icon-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: 6px;
    border: 1px solid #555;
    background: transparent;
    color: #ddd;
    cursor: pointer;
    transition: background-color 0.2s, border-color 0.2s, color 0.2s;
  }

  .icon-btn:hover {
    background-color: #3a3a3a;
    border-color: #666;
    color: #fff;
  }

  .icon-btn[disabled] {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .icon-btn svg {
    width: 18px;
    height: 18px;
    display: block;
  }
</style>
