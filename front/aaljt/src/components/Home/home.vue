<template>
  <div class="home-wrapper">
    <dhstyle />
    
    <!-- Carousel Container -->
    <div class="carousel-container" 
      @mouseenter="pauseTimer" 
      @mouseleave="startTimer"
      @wheel="handleWheel"
    >
      
      <!-- Slides -->
      <transition-group name="fade" tag="div" class="slides-wrapper">
        <div 
          v-for="(slide, index) in slides" 
          :key="slide.id" 
          v-show="currentSlide === index"
          class="slide-item"
          :style="{ backgroundImage: `url(${slide.image})` }"
        >
          <!-- Gradient Overlay for Left-Aligned Text Readability -->
          <div class="slide-overlay"></div>
          
          <!-- Content Box -->
          <div class="slide-content">
            <div class="text-wrapper">
              <span class="slide-subtitle">{{ slide.subtitle }}</span>
              <h2 class="slide-title" v-html="slide.title"></h2>
              <p class="slide-desc">{{ slide.description }}</p>
              
              <div class="btn-group">
                <button class="cta-btn primary" @click="navigate(slide.route)">
                  {{ slide.buttonText }}
                </button>
                <button v-if="slide.secondaryText" class="cta-btn secondary">
                  {{ slide.secondaryText }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition-group>

      <!-- Controls (Moved to bottom right or kept minimal) -->
      <div class="nav-controls">
        <button class="nav-btn prev" @click="prevSlide">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
        </button>
        <button class="nav-btn next" @click="nextSlide">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </button>
      </div>

      <!-- Indicators (Left aligned) -->
      <div class="indicators">
        <span 
          v-for="(slide, index) in slides" 
          :key="index" 
          class="dot" 
          :class="{ active: currentSlide === index }"
          @click="goToSlide(index)"
        ></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import dhstyle from '../dhstyle/dhstyle.vue';

const router = useRouter();
const currentSlide = ref(0);
const isThrottled = ref(false); // Throttle flag for scroll
let timer: any = null;

// Slide Data
const slides = [
  {
    id: 1,
    subtitle: "COMMUNITY MARKETPLACE",
    title: "闲置好物，<br>适用于邻里的放心交易",
    description: "让闲置物品在家门口流动起来。真实邻居，当面交易，告别繁琐物流，重拾邻里信任。",
    image: "https://images.unsplash.com/photo-1556740758-90de374c12ad?ixlib=rb-1.2.1&auto=format&fit=crop&w=1950&q=80",
    route: "/community-marketplace",
    buttonText: "立即浏览",
    secondaryText: "发布闲置"
  },
  {
    id: 2,
    subtitle: "NEIGHBOURHOOD CIRCLE",
    title: "智能无限，<br>协作无间",
    description: "Nexthome 致力于成为真正的邻里中心。分享生活瞬间，组织线下聚会，无缝融入你的社区生活。",
    image: "https://images.unsplash.com/photo-1511632765486-a01980e01a18?ixlib=rb-1.2.1&auto=format&fit=crop&w=1950&q=80",
    route: "/community-feed",
    buttonText: "加入圈子",
    secondaryText: "了解更多"
  },
  {
    id: 3,
    subtitle: "LOCAL ACTIVITIES",
    title: "发现乐趣，<br>就在你的身边",
    description: "周末去哪儿？无需远行。发现周边的市集、运动局与亲子活动，丰富你的业余生活。",
    image: "https://images.unsplash.com/photo-1527529482837-4698179dc6ce?ixlib=rb-1.2.1&auto=format&fit=crop&w=1950&q=80",
    route: "/local-act",
    buttonText: "寻找活动",
    secondaryText: null
  },
  {
    id: 4,
    subtitle: "PUBLIC SERVICES",
    title: "便民服务，<br>安全守护每一天",
    description: "获取实时社区安全警报，查找值得信赖的周边维修、家政服务，为您打造安心的居住环境。",
    image: "https://images.unsplash.com/photo-1581578731117-104f2a41272e?ixlib=rb-1.2.1&auto=format&fit=crop&w=1950&q=80",
    route: "/services",
    buttonText: "查看服务",
    secondaryText: null
  }
];

// Navigation Logic
const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % slides.length;
};

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + slides.length) % slides.length;
};

const goToSlide = (index: number) => {
  currentSlide.value = index;
};

const handleWheel = (e: WheelEvent) => {
  if (isThrottled.value) return;

  // Add a small threshold to ignore tiny trackpad jitters
  if (Math.abs(e.deltaY) < 30) return;

  isThrottled.value = true;
  
  // Determine direction
  if (e.deltaY > 0) {
    nextSlide();
  } else {
    prevSlide();
  }

  // Reset manual timer to avoid immediate auto-switch after manual switch
  pauseTimer();
  startTimer();

  // Unlock throttle after animation roughly completes (1s)
  setTimeout(() => {
    isThrottled.value = false;
  }, 1000);
};

const navigate = (path: string) => {
  if (path === '/community-feed') {
     router.push({ name: 'CommunityFeed' });
  } else if (path === '/community-marketplace') {
     router.push({ name: 'CommunityMarketplace' });
  } else {
     router.push(path);
  }
};

// Autoplay Logic
const startTimer = () => {
  // Clear existing timer just in case
  if (timer) clearInterval(timer);
  timer = setInterval(nextSlide, 6000); // Slightly slower for reading
};

const pauseTimer = () => {
  if (timer) clearInterval(timer);
  timer = null;
};

onMounted(() => {
  startTimer();
});

onUnmounted(() => {
  pauseTimer();
});
</script>

<style scoped>
.home-wrapper {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  position: relative;
  background: #000;
}

.carousel-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.slides-wrapper {
  width: 100%;
  height: 100%;
}

.slide-item {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  /* Changed from center to flex-start for left alignment */
  justify-content: flex-start; 
}

/* 
  Gradient Overlay 
  Mimics the "Fade to Black" style in the reference images.
  Stronger on the left to support text.
*/
.slide-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.8) 35%, rgba(0,0,0,0.2) 100%);
  z-index: 1;
}

/* Content Layout */
.slide-content {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding-left: 10%; /* Left padding */
  padding-right: 20px;
}

.text-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* Align text to left */
  max-width: 650px;
  animation: slideInLeft 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

/* Typography */
.slide-subtitle {
  font-size: 16px;
  font-weight: 700;
  color: #ff7043; /* Brand color */
  letter-spacing: 1px;
  margin-bottom: 16px;
  text-transform: uppercase;
}

.slide-title {
  font-size: 64px; /* Larger font like the reference */
  font-weight: 700;
  color: #fff;
  margin: 0 0 24px 0;
  line-height: 1.1;
  letter-spacing: -1px;
}

.slide-desc {
  font-size: 20px;
  font-weight: 400;
  color: rgba(255,255,255,0.85);
  margin-bottom: 48px;
  line-height: 1.6;
  max-width: 580px;
}

/* Button Group */
.btn-group {
  display: flex;
  gap: 16px;
}

.cta-btn {
  padding: 14px 36px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 50px; /* Pill shape */
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Primary Button (White style like reference) */
.cta-btn.primary {
  background: #fff;
  color: #000;
  border: 2px solid #fff;
}
.cta-btn.primary:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
  border-color: #f0f0f0;
}

/* Secondary Button (Transparent/Outline) */
.cta-btn.secondary {
  background: transparent;
  color: #fff;
  border: 2px solid rgba(255,255,255,0.3);
}
.cta-btn.secondary:hover {
  border-color: #fff;
  background: rgba(255,255,255,0.1);
}

/* Controls */
.nav-controls {
  position: absolute;
  bottom: 40px;
  right: 40px;
  display: flex;
  gap: 12px;
  z-index: 10;
}

.nav-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}
.nav-btn:hover { 
  background: #fff; 
  color: #000;
  border-color: #fff;
}

/* Indicators */
.indicators {
  position: absolute;
  bottom: 56px; /* Align with nav buttons horizontally */
  left: 10%; /* Align with text */
  display: flex;
  gap: 12px;
  z-index: 10;
}

.dot {
  width: 40px; /* Long bars instead of dots */
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.3s;
}
.dot.active {
  background: #ff7043;
  width: 60px;
}

/* Animations */
@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-40px); }
  to { opacity: 1; transform: translateX(0); }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.8s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .slide-overlay {
    background: rgba(0,0,0,0.6); /* Simpler overlay on mobile */
  }
  .slide-content {
    padding-left: 24px;
    padding-right: 24px;
    justify-content: center;
  }
  .text-wrapper {
    align-items: center;
    text-align: center;
  }
  .slide-title { font-size: 40px; }
  .slide-desc { font-size: 16px; text-align: center; }
  .indicators { left: 50%; transform: translateX(-50%); bottom: 20px; }
  .nav-controls { display: none; }
}
</style>