<template>
  <dhstyle />
  <div class="lnc-page">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">活动通知中心</p>
        <h1>统一管理报名提醒、候补通知与签到推送</h1>
        <p class="subtitle">使用规则引擎自动触发短信、站内信或微信服务通知，确保邻居不错过活动的重要节点。</p>
        <div class="hero-actions">
          <button class="primary">
            <i class="fas fa-plus"></i>
            创建通知策略
          </button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat">
          <span class="stat-icon stat-orange"><i class="fas fa-bell"></i></span>
          <strong>{{ rules.length }}</strong>
          <span>启用中策略</span>
        </div>
        <div class="stat">
          <span class="stat-icon stat-blue"><i class="fas fa-paper-plane"></i></span>
          <strong>1.2k</strong>
          <span>本月发送</span>
        </div>
        <div class="stat">
          <span class="stat-icon stat-green"><i class="fas fa-check-double"></i></span>
          <strong>96.4%</strong>
          <span>到达率</span>
        </div>
      </div>
    </section>

    <section class="grid">
      <article v-for="rule in rules" :key="rule.id" class="card">
        <div class="card-head">
          <span class="card-icon"><i class="far fa-bell"></i></span>
          <span class="channel">{{ rule.channel }}</span>
        </div>
        <h3>{{ rule.title }}</h3>
        <p class="event">活动：{{ rule.event }}</p>
        <p class="desc">{{ rule.desc }}</p>
        <div class="trigger">
          <i class="fas fa-bolt"></i>
          {{ rule.trigger }}
        </div>
        <footer>
          <span class="status-on">
            <span class="dot"></span>
            已启用
          </span>
          <button class="ghost">编辑</button>
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
  background: #fafbfc;
}

.lnc-page {
  color: #0f172a;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: 32px;
  align-items: center;
}

.subtitle {
  margin: 0;
  max-width: 540px;
}

.hero-actions {
  margin-top: 22px;
  display: flex;
  gap: 10px;
}

.hero-actions .primary i {
  margin-right: 6px;
  font-size: 11px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  padding: 18px 12px;
  border-radius: 14px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.stat-icon.stat-orange {
  background: rgba(255, 107, 44, 0.12);
  color: #ff6b2c;
}

.stat-icon.stat-blue {
  background: rgba(78, 142, 247, 0.12);
  color: #4e8ef7;
}

.stat-icon.stat-green {
  background: rgba(56, 185, 130, 0.12);
  color: #38b982;
}

.stat strong {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat span:last-child {
  font-size: 11.5px;
  color: #94a3b8;
}

.grid {
  max-width: 1280px;
  margin: 0 auto 60px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 36px rgba(15, 23, 42, 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.card-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(78, 142, 247, 0.1);
  color: #4e8ef7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.channel {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(78, 142, 247, 0.08);
  color: #4e8ef7;
  font-size: 11.5px;
  font-weight: 500;
}

.card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: #0f172a;
}

.event {
  margin: 0;
  font-size: 12.5px;
  color: #94a3b8;
}

.desc {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
}

.trigger {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  font-size: 12.5px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger i {
  color: #ff6b2c;
  font-size: 11px;
}

.card footer {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12.5px;
}

.status-on {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #38b982;
  font-weight: 500;
}

.status-on .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38b982;
  box-shadow: 0 0 0 3px rgba(56, 185, 130, 0.16);
}

.ghost {
  border: none;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  padding: 0 16px;
  height: 30px;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s ease;
}

.ghost:hover {
  background: #eef2f6;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>
