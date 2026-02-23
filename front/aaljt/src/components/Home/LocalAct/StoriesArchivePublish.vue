<template>
  <dhstyle />
  <div class="lsp-page">
    <section class="hero">
      <div>
        <p class="eyebrow">发布故事</p>
        <h1>把你的活动瞬间分享给邻里</h1>
        <p class="subtitle">上传封面、摘要和正文，可选择公开或社区可见。</p>
      </div>
    </section>

    <form class="card" @submit.prevent="submit">
      <label class="field">
        <span>标题 *</span>
        <input v-model="form.title" type="text" placeholder="如：夜跑团的首次5km" />
      </label>
      <label class="field">
        <span>封面图 URL</span>
        <input v-model="form.coverUrl" type="url" placeholder="https://example.com/cover.jpg" />
      </label>
      <label class="field">
        <span>摘要</span>
        <textarea v-model="form.summary" rows="3" placeholder="一句话概括故事亮点"></textarea>
      </label>
      <label class="field">
        <span>关联活动ID（可选）</span>
        <input v-model.number="form.activityId" type="number" min="1" placeholder="活动ID" />
      </label>
      <label class="field">
        <span>正文 *</span>
        <textarea v-model="form.content" rows="8" placeholder="详细描述发生了什么、收获和感受"></textarea>
      </label>
      <label class="field">
        <span>可见性</span>
        <select v-model="form.visibility">
          <option value="PUBLIC">公开</option>
          <option value="COMMUNITY">社区可见</option>
          <option value="PRIVATE">仅自己</option>
        </select>
      </label>

      <div class="actions">
        <button class="ghost" type="button" @click="reset" :disabled="loading">重置</button>
        <button class="primary" type="submit" :disabled="loading">{{ loading ? '提交中...' : '发布' }}</button>
      </div>
      <p v-if="message" :class="['msg', messageType]">{{ message }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import dhstyle from '../../dhstyle/dhstyle.vue';

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080';
const username = ref(localStorage.getItem('username') || '');

const form = reactive({
  title: '',
  coverUrl: '',
  summary: '',
  content: '',
  visibility: 'PUBLIC',
  activityId: undefined as number | undefined
});

const loading = ref(false);
const message = ref('');
const messageType = ref<'success' | 'error'>('success');

const reset = () => {
  form.title = '';
  form.coverUrl = '';
  form.summary = '';
  form.content = '';
  form.visibility = 'PUBLIC';
  form.activityId = undefined;
  message.value = '';
};

const submit = async () => {
  message.value = '';
  if (!username.value) {
    message.value = '请先登录再发布故事';
    messageType.value = 'error';
    return;
  }
  if (!form.title.trim() || !form.content.trim()) {
    message.value = '标题和正文不能为空';
    messageType.value = 'error';
    return;
  }
  loading.value = true;
  try {
    const resp = await fetch(`${API_BASE}/api/local-act/stories`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        title: form.title,
        coverUrl: form.coverUrl,
        summary: form.summary,
        content: form.content,
        visibility: form.visibility,
        activityId: form.activityId
      })
    });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || data?.code !== 200) {
      throw new Error(data?.message || '发布失败');
    }
    message.value = '发布成功';
    messageType.value = 'success';
    reset();
  } catch (e: any) {
    message.value = e?.message || '发布失败';
    messageType.value = 'error';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
:global(body) {
  background: #f5f6f8;
}
.lsp-page {
  padding-top: 80px;
  color: #111827;
  max-width: 900px;
  margin: 0 auto 60px;
}
.hero {
  margin: 32px 0 16px;
}
.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 13px;
  color: #7c3aed;
}
.subtitle {
  color: #6b7280;
}
.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field input,
.field textarea,
.field select {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
}
.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 6px;
}
.primary,
.ghost {
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.primary { background: linear-gradient(120deg, #2563eb, #1d4ed8); color: #fff; }
.primary:disabled { opacity: 0.6; cursor: not-allowed; }
.ghost { background: #f3f4f6; color: #111827; }
.msg {
  font-size: 14px;
}
.msg.success { color: #15803d; }
.msg.error { color: #dc2626; }
</style>
