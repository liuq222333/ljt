<template>
  <!-- 尝试通过样式穿透使导航栏透明 -->
  <div >
    <dhstyle />
  </div>

  <div class="page-container">
    
    <!-- Full Width Header (Hero Section) -->
    <div class="moments-header-banner">
      <div class="cover-image" :style="{ backgroundImage: `url(${coverImg})` }">
        <div class="cover-overlay"></div>
        <button class="change-cover-btn" @click="triggerCoverUpload">📷 更换封面</button>
        <input type="file" ref="coverInput" accept="image/*" style="display: none" @change="onCoverFileChange" />
      </div>
      <div class="user-profile-row">
        <div class="user-name">{{ currentUser.name }}</div>
        <div class="user-avatar" @click="triggerAvatarUpload">
          <img :src="currentUser.avatar" alt="User" />
          <div class="avatar-overlay">
            <span>更换</span>
          </div>
          <input type="file" ref="avatarInput" accept="image/*" style="display: none" @change="onAvatarFileChange" />
        </div>
      </div>
    </div>

    <div class="moments-layout">
      
      <!-- Main Content: Feed -->
      <main class="feed-container">
        <!-- Publish Post Component -->
        <PublishPost 
          v-if="showPublish"
          :current-avatar="currentUser.avatar"
          @close="showPublish = false; currentTab = 'all'"
          @publish="handleNewPost"
        />

        <!-- My Profile Card -->
        <div v-else-if="currentTab === 'profile'" class="card-panel profile-detail-card">
          <div class="profile-cover-small" :style="{ backgroundImage: `url(${coverImg})` }"></div>
          <div class="profile-content">
            <div class="profile-header-inner">
              <div class="profile-avatar-wrap">
                 <img :src="currentUser.avatar" class="profile-avatar-img"/>
              </div>
              <div class="profile-text-info">
                <h2 class="profile-name">{{ currentUser.name }}</h2>
                <p class="profile-bio">{{ currentUser.bio || '这个人很懒，什么都没写~' }}</p>
              </div>
              <button class="edit-btn">编辑资料</button>
            </div>
            
            <div class="profile-stats-row">
               <div class="stat-box">
                 <div class="stat-num">{{ myPostsCount }}</div>
                 <div class="stat-label">动态</div>
               </div>
               <div class="stat-box">
                 <div class="stat-num">{{ currentUser.likes }}</div>
                 <div class="stat-label">获赞</div>
               </div>
               <div class="stat-box">
                 <div class="stat-num">{{ currentUser.neighbors }}</div>
                 <div class="stat-label">邻居</div>
               </div>
            </div>

            <div class="profile-detail-list">
              <div class="detail-item">
                <span class="label">📍 所在位置</span>
                <span class="value">幸福小区 3号楼</span>
              </div>
              <div class="detail-item">
                <span class="label">📅 加入时间</span>
                <span class="value">2023-09-15</span>
              </div>
              <div class="detail-item">
                <span class="label">🏷️ 兴趣标签</span>
                <div class="tags">
                  <span class="tag">摄影</span>
                  <span class="tag">烹饪</span>
                  <span class="tag">运动</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Post List (Moments Style) -->
        <div v-else class="card-panel post-list">
          <div v-for="post in filteredPosts" :key="post.id" class="moment-item" @click="post.showActions = false">
            <!-- Left: Avatar -->
            <div class="moment-avatar-col">
              <el-popover
                placement="right-start"
                :width="320"
                trigger="hover"
                @show="handleAvatarHover(post.userId)"
              >
                <template #reference>
                  <img :src="post.avatar" class="moment-avatar" />
                </template>
                
                <!-- Popover Content -->
                <div class="mini-profile-card">
                  <div v-if="!userCache[post.userId] || userCache[post.userId].loading" class="loading-skeleton">
                    加载中...
                  </div>
                  <div v-else>
                    <div class="mini-cover" :style="{ backgroundImage: `url(${coverImg})` }"></div>
                    <div class="mini-content">
                       <div class="mini-header">
                         <div class="mini-avatar-wrap">
                           <img :src="userCache[post.userId].avatar || post.avatar" />
                         </div>
                         <div class="mini-info">
                           <div class="mini-name">{{ userCache[post.userId].userName || post.author }}</div>
                           <div class="mini-bio">{{ userCache[post.userId].bio }}</div>
                         </div>
                       </div>
                       
                       <div class="mini-stats">
                          <div class="m-stat">
                            <span class="num">{{ userCache[post.userId].likes || 0 }}</span>
                            <span class="lbl">获赞</span>
                          </div>
                          <div class="m-stat">
                            <span class="num">{{ userCache[post.userId].neighbors || 0 }}</span>
                            <span class="lbl">邻居</span>
                          </div>
                       </div>
                       
                       <div class="mini-actions">
                         <button class="mini-btn primary">关注</button>
                         <button class="mini-btn">私信</button>
                       </div>
                    </div>
                  </div>
                </div>
              </el-popover>
            </div>
            
            <!-- Right: Content -->
            <div class="moment-content-col">
              <div class="moment-header">
                <span class="moment-name">{{ post.author }}</span>
              </div>
              
              <p class="moment-text">{{ post.content }}</p>

              <div v-if="post.images && post.images.length" class="moment-gallery" :class="getGridClass(post.images.length)">
                 <div v-for="(img, idx) in post.images" :key="idx" class="img-wrap">
                   <el-image 
                     style="width: 100%; height: 100%"
                     :src="img" 
                     :preview-src-list="post.images" 
                     :initial-index="idx" 
                     fit="cover"
                     loading="lazy" 
                     hide-on-click-modal
                   />
                 </div>
              </div>

              <div class="moment-footer">
                <div class="footer-info">
                  <span class="time-tag">{{ post.time }}</span>
                  <span v-if="post.locationText" class="location-tag">· {{ post.locationText }}</span>
                  <span v-if="post.isMine" class="delete-link" @click.stop="deletePost(post.id)">删除</span>
                </div>
                
                <div class="footer-actions">
                  <!-- Action Menu Button (Two Dots) -->
                  <button class="action-toggle-btn" @click.stop="toggleActions(post)">
                    <span class="dots">··</span>
                  </button>
                  
                  <!-- Animated Action Menu -->
                  <div class="action-popover" :class="{ show: post.showActions }" @click.stop>
                    <div class="pop-btn" @click="toggleLike(post)">
                       <span class="pop-icon">{{ post.liked ? '❤️' : '♡' }}</span>
                       <span>{{ post.liked ? '取消' : '赞' }}</span>
                    </div>
                    <div class="pop-divider"></div>
                    <div class="pop-btn" @click="toggleComments(post)">
                       <span class="pop-icon">💬</span>
                       <span>评论</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Likes/Comments Area -->
              <div class="interaction-area" v-if="shouldShowInteraction(post)" @click.stop>
                <div class="likes-list" v-if="post.likes > 0">
                  <span class="like-icon">♡</span>
                  <span class="like-names">
                    {{ post.likeUsers && post.likeUsers.length > 0 ? post.likeUsers.join(', ') : '' }}
                    {{ post.likes > (post.likeUsers?.length || 0) ? ` 等${post.likes}人` : '' }}
                    觉得很赞
                  </span>
                </div>
                
                <!-- Comments Section -->
                <div class="comments-section" v-if="true">
                  <div class="comment-list" v-if="post.commentList && post.commentList.length > 0">
                    <div
                      v-for="comment in (post.commentsExpanded ? post.commentList : post.commentList.slice(0, 10))"
                      :key="comment.id"
                      class="comment-item"
                      @contextmenu.prevent="onCommentContextMenu(post, comment)"
                    >
                      <span class="comment-user">{{ comment.username || '用户' + comment.userId }}:</span>
                      <span class="comment-content">{{ comment.content }}</span>
                    </div>
                    <!-- Expand Button -->
                    <div v-if="post.commentList.length > 10 && !post.commentsExpanded" 
                         class="expand-comments-btn"
                         @click="post.commentsExpanded = true">
                      查看全部 {{ post.commentList.length }} 条评论
                    </div>
                  </div>
                  <!-- Show empty state or just input if no comments -->
                  <div v-if="post.commentInputVisible" class="comment-input-box" @click.stop>
                    <input 
                      v-model="post.newComment" 
                      placeholder="评论..." 
                      @keyup.enter="submitComment(post)"
                    />
                    <button @click="submitComment(post)">发送</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Empty State -->
          <div v-if="filteredPosts.length === 0 && !loading" class="empty-state">
            <p>暂无动态，快来发布第一条吧！</p>
          </div>

          <!-- Load More Button (New) -->
          <div v-if="filteredPosts.length > 0 && hasMore" class="load-more-container">
            <button class="load-more-btn" @click="fetchPosts(false)" :disabled="loading">
              {{ loading ? '加载中...' : '加载更多' }}
            </button>
          </div>
          <div v-if="filteredPosts.length > 0 && !hasMore" class="no-more-tip">
            - 到底啦 -
          </div>
        </div>
      </main>

      <!-- Back to Top (New) -->
      <div class="back-to-top" :class="{ show: showBackTop }" @click="scrollToTop">
        ⬆
      </div>

      <!-- Right Sidebar -->
      <aside class="sidebar-right">
        <!-- Search Widget (New) -->
        <div class="card-panel search-widget">
          <div class="search-box">
            <input 
              v-model="searchKeyword" 
              type="text" 
              placeholder="搜索动态..." 
              @keyup.enter="handleSearch"
            />
            <button class="search-icon-btn" @click="handleSearch">🔍</button>
          </div>
        </div>

        <!-- Nav Menu (Moved Here) -->
        <nav class="card-panel nav-menu">
          <div 
            class="menu-item" 
            :class="{ active: currentTab === 'all' }" 
            @click="currentTab = 'all'; showPublish = false"
          >
            <span class="icon">🏠</span> 社区动态
          </div>
          <div 
            class="menu-item" 
            :class="{ active: currentTab === 'profile' }" 
            @click="currentTab = 'profile'; showPublish = false"
          >
            <span class="icon">📝</span> 我的资料
          </div>
          <div 
            class="menu-item" 
            :class="{ active: currentTab === 'my' }" 
            @click="currentTab = 'my'; showPublish = false"
          >
            <span class="icon">👤</span> 我的发布
          </div>
          <div class="menu-item" @click="showPublish = true; currentTab = 'publish'">
            <span class="icon">✏️</span> 发布动态
          </div>
          <div class="menu-item" @click="router.push({ name: 'CommunityMarketplace' })">
            <span class="icon">🛍️</span> 跳蚤市场
          </div>
        </nav>

        <!-- Hot Topics -->
        <div class="card-panel hot-topics">
          <h3>🔥 社区热议</h3>
          <ul class="topic-list">
            <li v-for="topic in topics" :key="topic.id" @click="searchTopic(topic.title)">
              <span class="hash">#</span> {{ topic.title }}
              <span class="heat">{{ topic.heat }}</span>
            </li>
          </ul>
        </div>
        
        <!-- Notice Board -->
        <div class="card-panel notice-board">
          <h3>📢 社区公告</h3>
          <p class="notice-text">本周六下午3点，社区中心举办亲子手工活动，欢迎报名参加！</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import coverImgSrc from './Pictures/1.png';
import userAvatar from './Pictures/avator.jpeg';
import avatar2 from './Pictures/2.png';
import PublishPost from './publishPost.vue';

const router = useRouter();
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
const MINIO_BASE = (import.meta.env as any).VITE_MINIO_BASE || '';

interface Comment {
  id: number;
  feedId: number;
  userId: number;
  username?: string; // Backend might need to return this or we fetch user info. Assuming backend Pojo has it or we handle it.
  avatar?: string;
  content: string;
  createdAt: string;
}

interface Post {
  id: number;
  userId: number;
  content: string;
  imageUrls?: string[]; // Backend: List<String> images
  images?: string[]; // Frontend usage
  likes: number;
  comments: number;
  viewCount: number;
  visibility: string;
  locationText?: string;
  createdAt: string;
  updatedAt: string;
  
  // Frontend helper props
   author?: string;
   avatar?: string;
   time?: string;
   liked?: boolean;
   showActions?: boolean;
   showComments?: boolean;
   commentList?: Comment[];
   newComment?: string;
   isMine?: boolean; // Need to determine based on current userId
   likeUsers?: string[]; // Store names of users who liked
   commentsExpanded?: boolean; // Whether to show all comments
   commentInputVisible?: boolean;
 }

// --- State ---
const searchKeyword = ref('');
const showBackTop = ref(false);
const currentTab = ref('all'); // 'all' | 'my' | 'publish'
const showPublish = ref(false);
const coverImg = ref(coverImgSrc);
const coverInput = ref<HTMLInputElement | null>(null);
const avatarInput = ref<HTMLInputElement | null>(null);
const userName =ref(localStorage.getItem('userName') || 'Sup')
const currentUser = ref({
  id: 'u1',
  name: userName.value,
  avatar: userAvatar,
  bio: '热爱生活，分享美好',
  likes: 128,
  neighbors: 45
});

// --- Methods ---
function handleSearch() {
  fetchPosts(true);
}

function searchTopic(topicTitle: string) {
  searchKeyword.value = topicTitle;
  handleSearch();
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleScroll() {
  showBackTop.value = window.scrollY > 300;
}

const userCache = ref<Record<string, any>>({});

async function handleAvatarHover(userId: number | string) {
  const idStr = String(userId);
  if (userCache.value[idStr]) return; // Already fetched

  // Initialize with loading state or basic info if needed
  userCache.value[idStr] = { loading: true };

  try {
    const res = await axios.get(`${API_BASE}/api/user/getUserById`, { params: { userId: idStr } });
    if (res.status === 200 && res.data) {
       const user = res.data;
       // Process avatar if needed, similar to fetchUserInfo
       let avatarUrl = '';
       if (user.avatarKey) {
         if (user.avatarKey.startsWith('http')) avatarUrl = user.avatarKey;
         else {
            const urls = buildImgUrls([user.avatarKey]);
            if (urls.length) avatarUrl = urls[0];
         }
       } else if (user.avatarUrl) {
         avatarUrl = user.avatarUrl;
       } else if (user.avatar_key && user.avatar_key.startsWith('http')) {
         avatarUrl = user.avatar_key;
       }
       
       userCache.value[idStr] = {
         ...user,
         avatar: avatarUrl || userAvatar, // Fallback
         bio: user.bio || '这个人很懒，什么都没写~',
         likes: user.likes || Math.floor(Math.random() * 100), // Mock if missing
         neighbors: user.neighbors || Math.floor(Math.random() * 50),
         loading: false
       };
    }
  } catch (e) {
    console.error('Fetch preview user info error:', e);
    userCache.value[idStr] = { 
        error: true, 
        loading: false,
        bio: '获取信息失败',
        likes: 0,
        neighbors: 0
    };
  }
}

function triggerCoverUpload() {
  coverInput.value?.click();
}

function triggerAvatarUpload() {
  avatarInput.value?.click();
}

function onCoverFileChange(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (files && files[0]) {
    const url = URL.createObjectURL(files[0]);
    coverImg.value = url;
  }
}

async function onAvatarFileChange(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (files && files[0]) {
    const file = files[0];
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', currentUser.value.id);

    try {
      const res = await axios.post(`${API_BASE}/api/community-feed/avatar/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.status === 200) {
        // 假设后端返回的是图片的 URL 字符串或包含 url 的对象
        const data = res.data;
        const newAvatarUrl = typeof data === 'string' ? data : (data.url || data);
        currentUser.value.avatar = newAvatarUrl;
        ElMessage.success('头像上传成功');
      }
    } catch (error) {
      console.error('Avatar upload error:', error);
      ElMessage.error('头像上传失败');
    }
  }
}

function handleNewPost(post: any) {
  // post is the backend response object
  // 1. 解析图片字段
  let rawImages: any = post.images ?? post.imageUrls ?? [];
  if (typeof rawImages === 'string') {
    try { rawImages = JSON.parse(rawImages); } catch (_) { rawImages = []; }
  }
  // 2. 构建完整 URL
  const images: string[] = Array.isArray(rawImages) ? buildImgUrls(rawImages) : [];

  const newPost: Post = {
    ...post,
    images: images,
    author: currentUser.value.name, // Or from response if available
    avatar: currentUser.value.avatar,
    locationText: post.locationText,
    time: new Date().toLocaleString(),
    liked: false,
    showActions: true,
    showComments: false,
  commentsExpanded: false,
  commentList: [],
  newComment: '',
  isMine: true
  };
  posts.value.unshift(newPost);
  showPublish.value = false;
  currentTab.value = 'all';
}

const topics = ref([
  { id: 1, title: '周末市集回顾', heat: '2.3k' },
  { id: 2, title: '寻猫启事', heat: '1.1k' },
  { id: 3, title: '社区羽毛球约战', heat: '856' },
  { id: 4, title: '阳台种菜心得', heat: '542' },
]);

const posts = ref<Post[]>([]);
const loading = ref(false);
const page = ref(1);
const hasMore = ref(true);
function hideAllCommentInputs() {
  posts.value.forEach(p => p.commentInputVisible = false);
}

function shouldShowInteraction(post: Post) {
  const hasLikes = Number(post.likes || 0) > 0;
  const hasComments = Array.isArray(post.commentList) && post.commentList.length > 0;
  return hasLikes || hasComments || !!post.commentInputVisible;
}

async function fetchPosts(reset = false) {
  if (reset) {
    page.value = 1;
    posts.value = [];
    hasMore.value = true;
  }
  if (loading.value || !hasMore.value) return;
  
  loading.value = true;
  try {
    const response = await axios.get(`${API_BASE}/api/community-feed/posts`, {
      params: {
        page: page.value,
        size: 10,
        keyword: searchKeyword.value
      }
    });
    
    if (response.status === 200 && Array.isArray(response.data)) {
      const newPosts = response.data.map((item: any) => {
        // images may be JSON string or array
        let rawImages: any = item.images ?? item.imageUrls ?? [];
        if (typeof rawImages === 'string') {
          try { rawImages = JSON.parse(rawImages); } catch (_) { rawImages = []; }
        }
        const images: string[] = Array.isArray(rawImages) ? buildImgUrls(rawImages) : [];

        return {
          ...item,
          images,
          author: item.username || `User${item.userId}`,
          avatar: item.userAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + item.userId,
          locationText: item.locationText,
          time: new Date(item.createdAt).toLocaleString(),
          likes: Number(item.likesCount || 0),
          comments: Number(item.commentsCount || 0),
          liked: false,
          showActions: false,
          showComments: true,
          commentsExpanded: false,
          commentInputVisible: false,
          commentList: Array.isArray(item.comments) ? item.comments : [],
          newComment: '',
          isMine: String(item.userId) === String(currentUser.value.id),
          likeUsers: []
        };
      });
      
      if (newPosts.length < 10) {
        hasMore.value = false;
      }
      
      // Preload likes and comments so they are visible on first render
      await Promise.all(newPosts.map(async (post: Post) => {
        await fetchLikeUsers(post);
        await fetchComments(post);
      }));
      
      posts.value.push(...newPosts);
      page.value++;
    }
  } catch (error) {
    console.error('Fetch posts error:', error);
    ElMessage.error('加载动态失败');
  } finally {
    loading.value = false;
  }
}

async function fetchUserInfo(id: string) {
  try {
    const res = await axios.get(`${API_BASE}/api/user/getUserById`, { params: { userId: id } });
    if (res.status === 200 && res.data) {
      const user = res.data;
      currentUser.value.id = user.userId || id;
      currentUser.value.name = user.userName || currentUser.value.name;
      
      let avatarUrl = '';
      // 优先使用后端返回的 avatarKey (现在可能是预签名 URL)
      if (user.avatarKey) {
        if (user.avatarKey.startsWith('http')) {
           avatarUrl = user.avatarKey;
        } else {
           const urls = buildImgUrls([user.avatarKey]);
           if (urls.length) avatarUrl = urls[0];
        }
      } else if (user.avatarUrl) {
        avatarUrl = user.avatarUrl;
      } else if (user.avatar_key) { // 兼容可能返回下划线的情况
         if (user.avatar_key.startsWith('http')) {
             avatarUrl = user.avatar_key;
         }
      }

      if (avatarUrl) {
        currentUser.value.avatar = avatarUrl;
        // 同步更新列表中已加载的“我的”动态头像
        posts.value.forEach(p => {
            if (String(p.userId) === String(currentUser.value.id)) {
                p.avatar = avatarUrl;
            }
        });
      } else {
        currentUser.value.avatar = userAvatar;
      }
    }
  } catch (error) {
    console.error('Fetch user info error:', error);
    currentUser.value.avatar = userAvatar;
  }
}

onMounted(() => {
  // Try to get real user ID
  const storedId = localStorage.getItem('userId');
  if (storedId) {
    currentUser.value.id = storedId;
    fetchUserInfo(storedId);
  }
  fetchPosts(true);

  // 点击其他区域时收起所有评论输入框
  window.addEventListener('click', hideAllCommentInputs);
  window.addEventListener('scroll', handleScroll);
});

onBeforeUnmount(() => {
  window.removeEventListener('click', hideAllCommentInputs);
  window.removeEventListener('scroll', handleScroll);
});

// --- Computed ---
const filteredPosts = computed(() => {
  if (currentTab.value === 'my') {
    return posts.value.filter(p => p.isMine);
  }
  return posts.value;
});

const myPostsCount = computed(() => posts.value.filter(p => p.isMine).length);

// --- Methods ---
function getGridClass(len: number) {
  if (len === 1) return 'grid-1';
  if (len === 2) return 'grid-2';
  if (len >= 3) return 'grid-3';
  return '';
}

function buildImgUrls(arr: string[]): string[] {
  const base = MINIO_BASE ? MINIO_BASE.replace(/\/+$/, '') : '';
  return arr.map((v) => {
    if (!v) return '';
    if (/^https?:\/\//i.test(v)) return v;
    return base ? `${base}/${v}` : v;
  }).filter(Boolean);
}

async function deletePost(id: number) {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条动态吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await axios.delete(`${API_BASE}/api/community-feed/posts/${id}`, {
      params: { userId: currentUser.value.id }
    });
    
    // Remove from list locally
    posts.value = posts.value.filter(p => p.id !== id);
    ElMessage.success('删除成功');
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Delete post error:', error);
      if (error.response && error.response.status === 403) {
        ElMessage.error('无权删除或动态不存在');
      } else {
        ElMessage.error('删除失败');
      }
    }
  }
}

async function fetchLikeUsers(post: Post) {
  try {
    const response = await axios.get(`${API_BASE}/api/community-feed/posts/${post.id}/likes`);
    if (response.status === 200 && Array.isArray(response.data)) {
      // response.data is List<Long> (userIds)
      // We need to fetch usernames for these IDs.
      // However, fetching user info for each ID might be slow.
      // For now, we will display "用户{ID}" and check if current user is in the list.
      // Optimization: Backend could return List<{userId, username}> instead of List<Long>.
      // Given the current backend implementation returns List<Long>:
      
      const userIds: number[] = response.data;
      
      const names: string[] = [];
      let isLikedByMe = false;

      // Use Promise.all to fetch user names if possible, or just use IDs.
      // Since we don't have a bulk get user endpoint, we might just show "用户ID" or try to match current user.
      // Actually, let's try to fetch names one by one or just display IDs for now to be safe and fast,
      // BUT the requirement implies a "perfect" like function.
      // Let's assume we want to show names. We can fetch user info for each ID.
      
      // Check current user first
      if (userIds.map(String).includes(String(currentUser.value.id))) {
          isLikedByMe = true;
          post.liked = true;
      }

      // Limit the number of names to show to avoid too many requests
      const displayLimit = 5;
      const idsToShow = userIds.slice(0, displayLimit);
      
      const namePromises = idsToShow.map(async (uid) => {
          if (String(uid) === String(currentUser.value.id)) {
              return '我';
          }
          // Call getUserById to get name
          try {
              const res = await axios.get(`${API_BASE}/api/user/getUserById`, { params: { userId: uid } });
              if (res.data && res.data.userName) {
                  return res.data.userName;
              }
          } catch (e) { /* ignore */ }
          return `用户${uid}`;
      });

      const fetchedNames = await Promise.all(namePromises);
      
      // If there are more likers than we fetched
      if (userIds.length > displayLimit) {
          // We won't add "等X人" here because the template handles it based on post.likes count
      }

      // Sort: '我' should be first if present
      fetchedNames.sort((a, b) => {
          if (a === '我') return -1;
          if (b === '我') return 1;
          return 0;
      });

      post.likeUsers = fetchedNames;
      // Update like count from backend source of truth
      post.likes = userIds.length;
    }
  } catch (error) {
    console.error(`Fetch likes for post ${post.id} error:`, error);
  }
}

function toggleActions(post: Post) {
  const current = post.showActions;
  // Close all others
  posts.value.forEach(p => p.showActions = false);
  post.showActions = !current;
}

async function toggleLike(post: Post) {
  const userId = currentUser.value.id;
  const userName = currentUser.value.name;
  
  try {
    if (post.liked) {
      await axios.delete(`${API_BASE}/api/community-feed/posts/${post.id}/like`, { params: { userId } });
      post.liked = false;
      post.likes = Math.max(0, post.likes - 1);
      // Remove current user from list
      if (post.likeUsers) {
        post.likeUsers = post.likeUsers.filter(name => name !== userName && name !== '我');
      }
    } else {
      await axios.post(`${API_BASE}/api/community-feed/posts/${post.id}/like`, null, { params: { userId } });
      post.liked = true;
      post.likes++;
      // Add current user to list
      if (!post.likeUsers) post.likeUsers = [];
      post.likeUsers.push('我');
    }
    post.showActions = true;
  } catch (error) {
    ElMessage.error('操作失败');
  }
}

async function fetchComments(post: Post) {
  try {
    const res = await axios.get(`${API_BASE}/api/community-feed/posts/${post.id}/comments`);
    if (res.status === 200) {
      post.commentList = res.data.map((c: any) => ({
        ...c,
        avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + c.userId // fallback avatar
      }));
      post.comments = post.commentList.length;
    }
  } catch (e) {
    console.error(`Fetch comments for post ${post.id} error:`, e);
  }
}

async function toggleComments(post: Post) {
  post.showActions = false;
  await fetchComments(post);
  post.commentInputVisible = true;
}

async function submitComment(post: Post) {
  if (!post.newComment || !post.newComment.trim()) return;
  
  try {
    const res = await axios.post(`${API_BASE}/api/community-feed/posts/${post.id}/comments`, {
      userId: currentUser.value.id,
      content: post.newComment
    });
    
    if (res.status === 200) {
      if (!post.commentList) post.commentList = [];
      post.commentList.push({
        ...res.data,
        username: currentUser.value.name,
        avatar: currentUser.value.avatar
      });
      post.comments++;
      post.newComment = '';
      ElMessage.success('评论成功');
    }
  } catch (e) {
    ElMessage.error('评论失败');
  }
}

function canDeleteComment(post: Post, comment: Comment) {
  return String(comment.userId) === String(currentUser.value.id) || String(post.userId) === String(currentUser.value.id);
}

async function deleteComment(post: Post, comment: Comment) {
  try {
    await axios.delete(`${API_BASE}/api/community-feed/posts/${post.id}/comments/${comment.id}`, {
      params: { userId: currentUser.value.id }
    });
    post.commentList = (post.commentList || []).filter(c => c.id !== comment.id);
    post.comments = Math.max(0, (post.comments || 0) - 1);
    ElMessage.success('删除成功');
  } catch (error) {
    ElMessage.error('删除失败');
  }
}

function onCommentContextMenu(post: Post, comment: Comment) {
  if (!canDeleteComment(post, comment)) return;
  ElMessageBox.confirm('确认删除该评论？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => deleteComment(post, comment))
    .catch(() => {});
}
</script>

<style scoped>
:deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* Theme Variables */
:root {
  --primary: #ff7043;
  --link-blue: #576b95;
  --bg-grey: #f5f5f5;
  --text-main: #333;
  --text-sub: #999;
}

/* 
  Attempt to override the dhstyle nav bar.
  Since we can't modify dhstyle directly, we use deep selectors 
  targeting common tags/classes that might be inside it.
*/
.nav-style-override :deep(header), 
.nav-style-override :deep(nav), 
.nav-style-override :deep(.navbar) {
  background-color: rgba(0,0,0,0.2) !important;
  backdrop-filter: blur(4px);
  box-shadow: none !important;
  border-bottom: none !important;
  color: white !important;
  transition: background-color 0.3s;
}
.nav-style-override :deep(a), 
.nav-style-override :deep(span), 
.nav-style-override :deep(div) {
  color: white !important; /* Ensure text is white on dark image */
}

.page-container {
  padding-top: 0; /* Remove top padding so header can go to top */
  background-color: #eff2f5;
  min-height: 100vh;
  font-family: "PingFang SC", "Helvetica Neue", Arial, sans-serif;
  color: var(--text-main);
  display: flex;
  flex-direction: column;
}

/* --- Full Width Header --- */
.moments-header-banner {
  position: relative;
  width: 100%;
  height: 400px;
  margin-bottom: 40px; /* Space for avatar to hang */
}

.cover-image {
  width: 100%;
  height: 100%;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  position: relative;
}
.cover-image:hover .change-cover-btn {
  opacity: 1;
}
.change-cover-btn {
  position: absolute;
  top: 100px;
  right: 20px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 10;
}
.change-cover-btn:hover {
  background: rgba(0, 0, 0, 0.7);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.4), transparent 30%, rgba(0,0,0,0.1));
}

.user-profile-row {
  position: absolute;
  bottom: -30px;
  right: 10%; /* Position relative to width, roughly aligning with content */
  display: flex;
  align-items: flex-end; /* Align text bottom with avatar bottom */
  gap: 20px;
  z-index: 2;
  padding-bottom: 10px;
}

.user-name {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 5px rgba(0,0,0,0.7);
  margin-bottom: 15px;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  border: 3px solid #fff;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
  position: relative;
  cursor: pointer;
}
.user-avatar img { width: 100%; height: 100%; object-fit: cover; }

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.user-avatar:hover .avatar-overlay {
  opacity: 1;
}
.avatar-overlay span {
  color: white;
  font-size: 12px;
  font-weight: 500;
}

/* --- Layout --- */
.moments-layout {
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 0 24px 40px 24px;
  display: flex;
  gap: 30px;
  align-items: flex-start;
  justify-content: center;
}

/* --- Common --- */
.card-panel {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e6e8eb;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  overflow: hidden;
}

/* --- Main Content --- */
.feed-container {
  flex: 1;
  max-width: 800px; /* Limit width for better readability */
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

/* Publisher */
/* Removed publisher styles */

/* --- Moments Style Item --- */
.post-list {
  padding: 0;
}

.moment-item {
  padding: 24px;
  display: flex;
  gap: 14px;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}
.moment-item:last-child {
  border-bottom: none;
}

.moment-avatar-col {
  flex: 0 0 44px;
}
.moment-avatar {
  width: 44px; height: 44px;
  border-radius: 6px; 
  object-fit: cover;
  background: #eee;
}

.moment-content-col {
  flex: 1;
  min-width: 0;
}

.moment-header {
  display: flex;
  align-items: center;
  height: 22px;
}
.moment-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--link-blue);
  cursor: pointer;
}

.moment-text {
  font-size: 15px;
  line-height: 1.6;
  color: #000;
  margin: 4px 0 8px 0;
  white-space: pre-wrap;
}

/* Image Grid */
.moment-gallery {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}
.img-wrap {
  position: relative;
  cursor: pointer;
}
.img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* Single Image - Large */
.moment-gallery.grid-1 {
  display: block;
}
.moment-gallery.grid-1 .img-wrap {
  width: 240px;
  height: 180px;
  border-radius: 4px;
  overflow: hidden;
}
/* Removed direct img styling as el-image handles it with object-fit: cover via global scope */

/* Multiple Images */
.moment-gallery.grid-2 {
  grid-template-columns: repeat(2, 120px);
  grid-template-rows: 120px;
}
.moment-gallery.grid-3 {
  grid-template-columns: repeat(3, 100px);
  grid-template-rows: 100px;
}

/* Footer Row */
.moment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
  height: 24px;
  position: relative;
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #b0b0b0;
}
.delete-link {
  color: var(--link-blue);
  cursor: pointer;
}
.delete-link:hover { text-decoration: underline; }

.location-tag {
  color: #576b95;
  margin-right: 4px;
}

/* Action Menu */
.footer-actions {
  position: relative;
}

.action-toggle-btn {
  background: #f7f7f7;
  border: none;
  color: #576b95;
  width: 32px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}
.action-toggle-btn:hover { background: #e0e0e0; }
.dots {
  font-weight: 900;
  font-size: 12px;
  line-height: 1;
  letter-spacing: 1px;
  margin-top: -4px;
}

/* Popover */
.action-popover {
  position: absolute;
  right: 40px;
  top: -10px;
  background: #4c4c4c;
  color: #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  overflow: hidden;
  opacity: 0;
  transform: scaleX(0);
  transform-origin: right center;
  transition: all 0.2s ease;
  white-space: nowrap;
  z-index: 99;
}
.action-popover.show {
  opacity: 1;
  transform: scaleX(1);
}

.pop-btn {
  padding: 8px 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
  min-width: 80px;
  justify-content: center;
}
.pop-btn:hover { background: #5c5c5c; }
.pop-icon { font-size: 14px; }
.pop-divider { width: 1px; height: 20px; background: #333; }

/* Interactions Area */
.interaction-area {
  margin-top: 10px;
  background: #f7f7f7;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 13px;
  color: #576b95;
  line-height: 1.5;
}
.like-icon { font-size: 14px; margin-right: 4px; }
.like-names { color: #576b95; font-weight: 500; }

.comments-section {
  border-top: 1px solid #e6e8eb;
  margin-top: 6px;
  padding-top: 6px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 2px;
}

.comment-user {
  color: #576b95;
  font-weight: 500;
  margin-right: 4px;
}

.comment-content {
  color: #333;
}

.comment-input-box {
  display: flex;
  margin-top: 8px;
  gap: 8px;
}

.comment-input-box input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 13px;
  outline: none;
}

.comment-input-box button {
  border: none;
  background: #ff7043;
  color: white;
  border-radius: 4px;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.comment-input-box button:hover {
  opacity: 0.9;
}

.expand-comments-btn {
  font-size: 13px;
  color: var(--link-blue);
  cursor: pointer;
  margin-top: 6px;
}
.expand-comments-btn:hover {
  text-decoration: underline;
}


/* --- Right Sidebar --- */
.sidebar-right {
  flex: 0 0 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 90px;
}

/* Nav Menu in Sidebar */
.nav-menu { padding: 12px; display: flex; flex-direction: column; gap: 4px; }
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  color: #555;
}
.menu-item:hover { background: #f5f6fa; }
.menu-item.active { background: #fff5f2; color: #ff7043; }
.menu-item .icon { font-size: 18px; }

.hot-topics, .notice-board { padding: 20px; }
.sidebar-right h3 { margin: 0 0 16px 0; font-size: 15px; font-weight: 700; color: #333; border-left: 4px solid #ff7043; padding-left: 10px; }

.topic-list { list-style: none; padding: 0; margin: 0; }
.topic-list li {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; font-size: 14px; cursor: pointer;
  color: #555; transition: color 0.2s;
}
.topic-list li:hover { color: #ff7043; }
.hash { color: #ff7043; font-weight: bold; margin-right: 4px; }
.heat { font-size: 12px; color: #999; background: #f5f6fa; padding: 2px 6px; border-radius: 4px; }

.notice-text { font-size: 13px; color: #666; line-height: 1.5; margin: 0; }

/* Search Widget */
.search-widget {
  padding: 20px;
}
.search-box {
  display: flex;
  gap: 10px;
}
.search-box input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s;
}
.search-box input:focus {
  border-color: var(--primary);
}
.search-icon-btn {
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 6px;
  width: 36px;
  cursor: pointer;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.search-icon-btn:hover {
  opacity: 0.9;
}

/* Load More */
.load-more-container {
  text-align: center;
  padding: 20px 0;
}
.load-more-btn {
  background: white;
  border: 1px solid #ddd;
  padding: 8px 24px;
  border-radius: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.load-more-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}
.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.no-more-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 13px;
}

/* Back to Top */
.back-to-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 20px;
  color: var(--primary);
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s;
  z-index: 999;
  pointer-events: none;
}
.back-to-top.show {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}
.back-to-top:hover {
  background: var(--primary);
  color: white;
}

/* Profile Detail Card */
.profile-detail-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  min-height: 400px;
}
.profile-cover-small {
  height: 120px;
  background-size: cover;
  background-position: center;
  background-color: #eee;
}
.profile-content {
  padding: 0 30px 30px;
  position: relative;
}
.profile-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: -40px;
  margin-bottom: 20px;
}
.profile-avatar-wrap {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid #fff;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.profile-text-info {
  flex: 1;
  margin-left: 20px;
  padding-bottom: 5px;
}
.profile-name {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  margin: 0 0 4px 0;
}
.profile-bio {
  font-size: 14px;
  color: #666;
  margin: 0;
}
.edit-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}
.edit-btn:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.profile-stats-row {
  display: flex;
  gap: 40px;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}
.stat-box {
  text-align: center;
}
.stat-num {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}
.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.profile-detail-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detail-item {
  display: flex;
  align-items: flex-start;
  font-size: 14px;
}
.detail-item .label {
  color: #999;
  width: 100px;
}
.detail-item .value {
  color: #333;
  flex: 1;
}
.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag {
  background: #f0f2f5;
  color: #666;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* Mini Profile Card in Popover */
.mini-profile-card {
  margin: -12px; /* Counteract popover padding if needed, or just style normally */
}
/* Element Plus Popover default padding is usually 12px. 
   To make the card flush, we might need negative margins or global style overrides. 
   Using negative margins for now. */
   
.mini-cover {
  height: 80px;
  background-size: cover;
  background-position: center;
  background-color: #eee;
  border-radius: 4px 4px 0 0;
}
.mini-content {
  padding: 12px 16px 16px;
  position: relative;
}
.mini-header {
  display: flex;
  align-items: flex-end;
  margin-top: -30px;
  margin-bottom: 12px;
}
.mini-avatar-wrap {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 3px solid #fff;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  flex-shrink: 0;
}
.mini-avatar-wrap img {
  width: 100%; height: 100%; object-fit: cover;
}
.mini-info {
  margin-left: 12px;
  padding-bottom: 4px;
  flex: 1;
  min-width: 0;
}
.mini-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 2px;
}
.mini-bio {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}
.m-stat {
  display: flex;
  flex-direction: column;
}
.m-stat .num {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}
.m-stat .lbl {
  font-size: 12px;
  color: #999;
}

.mini-actions {
  display: flex;
  gap: 10px;
}
.mini-btn {
  flex: 1;
  padding: 6px 0;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.mini-btn.primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}
.mini-btn:hover {
  opacity: 0.9;
}

.loading-skeleton {
  padding: 20px;
  text-align: center;
  color: #999;
}

@media (max-width: 1000px) {
  .sidebar-right { display: none; }
  .moments-layout { justify-content: center; }
  .user-profile-row { right: 20px; }
}
</style>
