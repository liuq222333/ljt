<template>
  <dhstyle />
  <div class="lsp-page">
    <section class="hero">
      <p class="eyebrow">发布故事</p>
      <h1>把你的活动瞬间分享给邻里</h1>
      <p class="subtitle">上传封面、摘要和正文，可选择公开或社区可见。一段真实的记录，会激发下一次的参与。</p>
    </section>

    <form class="card" @submit.prevent="submit">
      <div class="form-section">
        <h3>基础信息</h3>
        <div class="form-grid">
          <label class="field full">
            <span>标题 <em>*</em></span>
            <input v-model="form.title" type="text" placeholder="如：夜跑团的首次5km" />
          </label>
          <div class="field full">
            <span>故事封面 <em>*</em></span>
            <input ref="coverInput" class="file-input" type="file" accept="image/*" @change="handleCoverChange" />
            <div :class="['cover-uploader', { ready: coverPreviewUrl }]">
              <div v-if="coverPreviewUrl" class="cover-preview">
                <img :src="coverPreviewUrl" alt="故事封面预览" />
              </div>
              <div v-else class="cover-empty">
                <i class="far fa-image"></i>
                <strong>上传故事封面</strong>
                <p>这张图片会展示在故事列表和详情页顶部。</p>
              </div>
              <div class="cover-actions">
                <button class="ghost mini" type="button" :disabled="uploading" @click="selectCover">
                  {{ uploading ? '上传中...' : coverPreviewUrl ? '更换图片' : '选择图片' }}
                </button>
                <button v-if="coverPreviewUrl" class="ghost mini" type="button" :disabled="uploading" @click="removeCover">
                  移除
                </button>
              </div>
            </div>
          </div>
          <label class="field">
            <span>关联活动ID</span>
            <input v-model.number="form.activityId" type="number" min="1" placeholder="可选" />
          </label>
          <label class="field">
            <span>可见性</span>
            <select v-model="form.visibility">
              <option value="PUBLIC">公开</option>
              <option value="COMMUNITY">社区可见</option>
              <option value="PRIVATE">仅自己</option>
            </select>
          </label>
        </div>
      </div>

      <div class="form-section">
        <h3>故事内容</h3>
        <label class="field">
          <span>摘要</span>
          <textarea v-model="form.summary" rows="3" placeholder="一句话概括故事亮点"></textarea>
        </label>
        <label class="field">
          <span>正文 <em>*</em></span>
          <textarea v-model="form.content" rows="10" placeholder="详细描述发生了什么、收获和感受"></textarea>
        </label>
      </div>

      <div class="actions">
        <button class="ghost" type="button" @click="reset" :disabled="loading">重置</button>
        <button class="primary" type="submit" :disabled="loading">
          <i class="fas fa-paper-plane"></i>
          {{ loading ? '提交中...' : '发布故事' }}
        </button>
      </div>
      <p v-if="message" :class="['msg', messageType]">{{ message }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createLocalActStory, uploadLocalActMedia } from '@/api/localAct';
import dhstyle from '../../dhstyle/dhstyle.vue';

const router = useRouter();
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
const uploading = ref(false);
const message = ref('');
const messageType = ref<'success' | 'error'>('success');
const coverInput = ref<HTMLInputElement | null>(null);
const coverPreviewUrl = ref('');

const reset = () => {
  form.title = '';
  form.coverUrl = '';
  coverPreviewUrl.value = '';
  form.summary = '';
  form.content = '';
  form.visibility = 'PUBLIC';
  form.activityId = undefined;
  message.value = '';
};

const selectCover = () => {
  coverInput.value?.click();
};

const removeCover = () => {
  form.coverUrl = '';
  coverPreviewUrl.value = '';
};

const handleCoverChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    message.value = '请选择图片文件';
    messageType.value = 'error';
    input.value = '';
    return;
  }
  uploading.value = true;
  message.value = '';
  try {
    const result = await uploadLocalActMedia(file, 'story');
    form.coverUrl = result.objectKey;
    coverPreviewUrl.value = result.url || result.publicUrl || URL.createObjectURL(file);
    message.value = '封面上传成功';
    messageType.value = 'success';
  } catch (e: any) {
    form.coverUrl = '';
    coverPreviewUrl.value = '';
    message.value = e?.message || '封面上传失败';
    messageType.value = 'error';
  } finally {
    uploading.value = false;
    input.value = '';
  }
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
  if (!form.coverUrl) {
    message.value = '请先上传故事封面';
    messageType.value = 'error';
    return;
  }
  loading.value = true;
  try {
    const storyId = await createLocalActStory({
      username: username.value,
      title: form.title,
      coverUrl: form.coverUrl,
      summary: form.summary,
      content: form.content,
      visibility: form.visibility,
      activityId: form.activityId
    });
    message.value = '发布成功，即将进入故事详情';
    messageType.value = 'success';
    window.setTimeout(() => {
      router.push(storyId ? `/local-act/stories/${storyId}` : '/local-act/stories');
    }, 500);
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
  background: #fafbfc;
}

.lsp-page {
  max-width: 900px;
  margin: 0 auto;
  color: #0f172a;
}

.hero {
  margin: 0 0 28px;
  padding: 0 8px;
}

.subtitle {
  margin: 14px 0 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.65;
  color: #64748b;
}

.card {
  background: #ffffff;
  border-radius: 18px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.form-section h3 {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.005em;
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-section h3::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #ff6b2c;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field.full {
  grid-column: 1 / -1;
}

.field span {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.field span em {
  margin-left: 2px;
  color: #ff6b2c;
  font-style: normal;
}

.file-input {
  display: none;
}

.cover-uploader {
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #f8fafc;
  overflow: hidden;
}

.cover-uploader.ready {
  border-style: solid;
  background: #ffffff;
}

.cover-preview,
.cover-empty {
  aspect-ratio: 16 / 8;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  text-align: center;
  color: #94a3b8;
}

.cover-empty i {
  color: #cbd5e1;
  font-size: 28px;
}

.cover-empty strong {
  color: #475569;
  font-size: 14px;
}

.cover-empty p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
}

.cover-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid #edf2f7;
  background: #ffffff;
}

.field input,
.field select {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: none;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 14px;
  color: #0f172a;
  outline: none;
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.field textarea {
  width: 100%;
  padding: 12px 14px;
  border: none;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 14px;
  color: #0f172a;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.65;
  transition: background 0.18s ease, box-shadow 0.18s ease;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
}

.field input::placeholder,
.field textarea::placeholder {
  color: #94a3b8;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.primary,
.ghost {
  border: none;
  height: 42px;
  padding: 0 22px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: background 0.18s ease, transform 0.2s ease;
}

.primary i {
  font-size: 12px;
}

.primary {
  background: #ff6b2c;
  color: #ffffff;
}

.primary:hover:not(:disabled) {
  background: #f25a1b;
  transform: translateY(-1px);
}

.primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ghost {
  background: #f8fafc;
  color: #475569;
}

.ghost:hover:not(:disabled) {
  background: #eef2f6;
}

.ghost.mini {
  height: 34px;
  padding: 0 14px;
  font-size: 12.5px;
}

.msg {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 13px;
}

.msg.success {
  background: rgba(56, 185, 130, 0.08);
  color: #1aa053;
}

.msg.error {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
}

@media (max-width: 720px) {
  .card {
    padding: 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
