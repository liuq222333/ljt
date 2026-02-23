<template>
  <dhstyle />
  <div class="lnc-page">
    <section class="hero">
      <div>
        <p class="eyebrow">活动通知中心</p>
        <h1>统一管理报名提醒、候补通知与签到推送</h1>
        <p class="subtitle">使用规则引擎自动触发短信、站内信或微信服务通知，确保邻居不错过活动的重要节点。</p>
      </div>
      <button class="primary">创建通知策略</button>
    </section>

    <section class="grid">
      <article v-for="rule in rules" :key="rule.id" class="card">
        <header>
          <div>
            <h3>{{ rule.title }}</h3>
            <p>{{ rule.event }}</p>
          </div>
          <span class="channel">{{ rule.channel }}</span>
        </header>
        <p class="desc">{{ rule.desc }}</p>
        <footer>
          <span>触发条件：{{ rule.trigger }}</span>
          <button class="ghost">编辑规则</button>
        </footer>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import dhstyle from '../../dhstyle/dhstyle.vue';

const rules = [
  {
    id: 'r1',
    title: '报名成功提醒',
    event: '邻里共享厨房',
    channel: '短信 + 站内信',
    desc: '告知集合时间、需携带物品，并附带取消链接。',
    trigger: '报名审核通过后立刻发送'
  },
  {
    id: 'r2',
    title: '候补上榜通知',
    event: '社区夜跑',
    channel: '微信服务通知',
    desc: '提醒开跑前 2 小时补位，并提供签到二维码。',
    trigger: '有人取消报名，自动轮候'
  },
  {
    id: 'r3',
    title: '签到反馈',
    event: '公园绿色行动',
    channel: '站内信',
    desc: '感谢参与并引导填写活动反馈问卷。',
    trigger: '活动结束 1 小时后'
  }
];
</script>

<style scoped>
:global(body) {
  background: #f4f6f8;
}

.lnc-page {
  padding-top: 80px;
  color: #111827;
}

.hero {
  margin: 48px;
  padding: 32px;
  border-radius: 28px;
  background: linear-gradient(120deg, #0c2132, #2060a4);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.primary {
  border: none;
  border-radius: 999px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.grid {
  margin: 0 48px 60px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.card {
  background: #fff;
  border-radius: 22px;
  padding: 20px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.channel {
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(32, 96, 164, 0.12);
  color: #1f4c86;
  font-size: 12px;
}

.desc {
  color: #4b5563;
}

.card footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #6b7280;
}

.ghost {
  border: 1px solid #d1d9ea;
  background: transparent;
  color: #1f2a37;
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
}

@media (max-width: 800px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
