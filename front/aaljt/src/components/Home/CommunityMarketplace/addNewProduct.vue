<template>
  <div class="publish-entry-page">
    <dhstyle />

    <div class="publish-entry-shell">
      <CebianTool />

      <main class="publish-entry-main">
        <section class="surface-card entry-hero">
          <div>
            <p class="section-kicker">新增商品</p>
            <h1>准备进入正式发布流程</h1>
            <p class="section-desc">
              这个入口不再维护第二套发布界面，会统一跳转到正式的发布商品页，保证字段、上传和提交流程一致。
            </p>
          </div>

          <span class="countdown-badge">{{ countdown }} 秒后自动跳转</span>
        </section>

        <div class="entry-grid">
          <section class="surface-card entry-card">
            <div class="entry-card-copy">
              <h2>接下来你会完成这些内容</h2>
              <div class="entry-list">
                <article class="entry-item">
                  <span>01</span>
                  <div>
                    <strong>填写基础信息</strong>
                    <p>标题、描述和图片会决定买家是否继续查看。</p>
                  </div>
                </article>

                <article class="entry-item">
                  <span>02</span>
                  <div>
                    <strong>确认交易信息</strong>
                    <p>价格、库存、成色和分类会直接影响搜索展示。</p>
                  </div>
                </article>

                <article class="entry-item">
                  <span>03</span>
                  <div>
                    <strong>补充位置信息</strong>
                    <p>地点会用于附近推荐和同城交易展示。</p>
                  </div>
                </article>
              </div>
            </div>

            <div class="entry-actions">
              <button class="primary-btn" type="button" @click="goToPublish">去发布商品</button>
              <button class="ghost-btn" type="button" @click="goToMarket">返回市场</button>
            </div>
          </section>

          <aside class="surface-card tip-card">
            <p class="section-kicker">发布提醒</p>
            <h2>让买家更快做决定</h2>
            <div class="tip-list">
              <article class="tip-item">
                <strong>标题尽量具体</strong>
                <p>建议包含品牌、型号、成色和核心配件。</p>
              </article>
              <article class="tip-item">
                <strong>图片先放封面图</strong>
                <p>优先展示商品整体，再补充细节和瑕疵图。</p>
              </article>
              <article class="tip-item">
                <strong>描述保持真实</strong>
                <p>把使用情况、转让原因和交易方式一次说清楚。</p>
              </article>
            </div>
          </aside>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dhstyle from '../../dhstyle/dhstyle.vue'
import CebianTool from './cebianTool.vue'

const router = useRouter()
const countdown = ref(3)

let countdownTimer: number | null = null
let redirectTimer: number | null = null

function goToPublish() {
  router.replace({ name: 'AddProduct' })
}

function goToMarket() {
  router.replace({ name: 'CommunityMarketplace' })
}

onMounted(() => {
  countdownTimer = window.setInterval(() => {
    if (countdown.value > 1) {
      countdown.value -= 1
    }
  }, 1000)

  redirectTimer = window.setTimeout(() => {
    goToPublish()
  }, 3000)
})

onBeforeUnmount(() => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer)
  }
  if (redirectTimer !== null) {
    window.clearTimeout(redirectTimer)
  }
})
</script>

<style scoped>
.publish-entry-page {
  --green: #24b55d;
  --green-deep: #1f8f4b;
  --green-soft: #eef8f1;
  --line: #e4ece1;
  --line-strong: #d7e2d4;
  --text-main: #233224;
  --text-sub: #6d7d6d;
  min-height: 100vh;
  background: #f5f7f2;
}

.publish-entry-main {
  width: min(1280px, calc(100vw - 116px));
  margin: 0 auto;
  padding: 82px 28px 36px;
  display: grid;
  gap: 18px;
}

.surface-card {
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 12px 26px rgba(67, 98, 67, 0.06);
}

.entry-hero {
  padding: 24px 26px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.section-kicker {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--green-deep);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.entry-hero h1,
.entry-card h2,
.tip-card h2 {
  margin: 10px 0 0;
  color: var(--text-main);
}

.entry-hero h1 {
  font-size: 36px;
  line-height: 1.16;
}

.section-desc,
.entry-item p,
.tip-item p {
  margin: 12px 0 0;
  color: var(--text-sub);
  line-height: 1.75;
}

.countdown-badge {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.entry-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.entry-card,
.tip-card {
  padding: 24px;
}

.entry-list,
.tip-list {
  margin-top: 20px;
  display: grid;
  gap: 12px;
}

.entry-item,
.tip-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #edf2ea;
  background: #fbfcfa;
}

.entry-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 14px;
}

.entry-item span {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.entry-item strong,
.tip-item strong {
  color: var(--text-main);
}

.entry-actions {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.primary-btn,
.ghost-btn {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.primary-btn {
  border: 1px solid transparent;
  background: var(--green);
  color: #ffffff;
}

.ghost-btn {
  border: 1px solid var(--line-strong);
  background: #ffffff;
  color: #4c5c4d;
}

.ghost-btn:hover {
  border-color: rgba(36, 181, 93, 0.22);
  background: #f6fbf7;
  color: var(--green-deep);
}

@media (max-width: 1180px) {
  .entry-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .publish-entry-main {
    width: calc(100vw - 28px);
    padding: 80px 14px 28px;
  }

  .entry-hero,
  .entry-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
