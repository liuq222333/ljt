<template>
  <transition name="drawer-fade">
    <div v-if="modelValue" class="local-ai-mask" @click="emit('update:modelValue', false)"></div>
  </transition>

  <aside :class="['local-ai-drawer', { open: modelValue }]">
    <div class="drawer-body">
      <section class="drawer-chat-card">
        <div class="chat-header">
          <div>
            <strong>{{ headline }}</strong>
            <p>{{ emptyText }}</p>
          </div>
          <div class="chat-header-actions">
            <span class="chat-status">{{ loading ? '处理中' : '在线' }}</span>
            <button class="drawer-close-btn" type="button" @click="emit('update:modelValue', false)">关闭</button>
          </div>
        </div>

        <div ref="chatWindowRef" class="chat-window">
          <div v-if="!messages.length" class="chat-empty">
            暂无会话内容，可以直接输入活动通知、推荐文案或复盘建议。
          </div>

          <div v-else class="chat-list">
            <div v-for="message in messages" :key="message.id" :class="['chat-row', message.sender]">
              <div class="chat-bubble">{{ message.text }}</div>

              <div v-if="message.sender === 'agent' && message.cards?.length" class="chat-card-list">
                <article
                  v-for="card in message.cards"
                  :key="`${message.id}-${card.entityType || 'card'}-${card.entityId || card.title}`"
                  class="chat-result-card"
                  :class="{ clickable: canOpenCard(card) }"
                  @click="handleCardClick(card)"
                >
                  <div class="chat-result-media">
                    <img
                      :src="card.imageUrl || fallbackImage"
                      :alt="card.title || '推荐卡片'"
                      @error="handleImageError"
                    />
                  </div>
                  <div class="chat-result-body">
                    <div class="chat-result-head">
                      <div>
                        <span class="chat-result-type">{{ resolveCardType(card.entityType) }}</span>
                        <h5>{{ card.title || '未命名内容' }}</h5>
                      </div>
                      <strong v-if="card.priceText" class="chat-result-price">{{ card.priceText }}</strong>
                    </div>
                    <p v-if="card.subtitle" class="chat-result-subtitle">{{ card.subtitle }}</p>
                    <div v-if="card.locationText || card.realtimeStatusText" class="chat-result-meta">
                      <span v-if="card.locationText">{{ card.locationText }}</span>
                      <span v-if="card.realtimeStatusText">{{ card.realtimeStatusText }}</span>
                    </div>
                    <p v-if="card.recommendReason" class="chat-result-reason">{{ card.recommendReason }}</p>
                    <div v-if="card.tags?.length" class="chat-result-tags">
                      <span v-for="tag in card.tags" :key="tag" class="chat-result-tag">{{ tag }}</span>
                    </div>
                    <ul v-if="card.highlights?.length" class="chat-result-highlights">
                      <li v-for="highlight in card.highlights" :key="highlight">{{ highlight }}</li>
                    </ul>
                  </div>
                </article>
              </div>

              <span class="chat-time">{{ message.time }}</span>
            </div>

            <div v-if="loading" class="chat-row agent">
              <div class="chat-bubble loading">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <p v-if="error" class="chat-error">{{ error }}</p>

        <div class="chat-input-panel">
          <div v-if="coverUploadVisible || uploadedCoverPreview" class="cover-upload-assist">
            <input
              ref="coverFileInputRef"
              class="cover-file-input"
              type="file"
              accept="image/*"
              @change="handleCoverUploadChange"
            />
            <div class="cover-upload-copy">
              <strong>活动封面图</strong>
              <span>{{ uploadedCoverPreview ? '封面已上传，可继续确认发布。' : '选择本地图片上传到 MinIO。' }}</span>
            </div>
            <img v-if="uploadedCoverPreview" class="cover-upload-preview" :src="uploadedCoverPreview" alt="活动封面预览" />
            <button
              class="cover-upload-btn"
              type="button"
              :disabled="loading || coverUploading"
              @click="triggerCoverUpload"
            >
              {{ coverUploading ? '上传中' : uploadedCoverPreview ? '更换封面' : '上传封面' }}
            </button>
          </div>

          <textarea
            ref="chatInputRef"
            :value="inputValue"
            :disabled="loading"
            :placeholder="placeholder"
            @input="emit('update:inputValue', ($event.target as HTMLTextAreaElement).value)"
            @keydown="handleTextareaKeydown"
          ></textarea>

          <div class="chat-input-footer">
            <span>Enter 发送，Shift + Enter 换行</span>
            <button class="send-btn" type="button" :disabled="loading || !inputValue.trim()" @click="emit('send')">
              {{ loading ? '发送中' : '发送' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

export type LocalAiCard = {
  entityId?: string | number;
  entityType?: string;
  title?: string;
  subtitle?: string;
  imageUrl?: string;
  priceText?: string;
  tags?: string[];
  highlights?: string[];
  locationText?: string;
  realtimeStatusText?: string;
  recommendReason?: string;
};

type AgentMessage = {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
  cards?: LocalAiCard[];
};

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="220"><rect width="100%" height="100%" fill="%23f8fafc"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%2394a3b8" font-size="16">暂无图片</text></svg>';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    messages: AgentMessage[];
    inputValue: string;
    loading: boolean;
    error?: string;
    headline?: string;
    emptyText?: string;
    placeholder?: string;
    coverUploadVisible?: boolean;
    coverUploading?: boolean;
    uploadedCoverPreview?: string;
  }>(),
  {
    error: '',
    headline: '围绕当前活动继续提问',
    emptyText: '消息会发送到后端活动助手接口，并保留当前会话上下文。',
    placeholder: '例如：帮我写一条今晚活动的群提醒，语气亲切一些',
    coverUploadVisible: false,
    coverUploading: false,
    uploadedCoverPreview: ''
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'update:inputValue', value: string): void;
  (event: 'send'): void;
  (event: 'card-click', value: LocalAiCard): void;
  (event: 'cover-upload', value: File): void;
}>();

const chatWindowRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<HTMLTextAreaElement | null>(null);
const coverFileInputRef = ref<HTMLInputElement | null>(null);

const syncScroll = () => {
  nextTick(() => {
    if (!chatWindowRef.value) {
      return;
    }
    chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
  });
};

const focusInput = () => {
  nextTick(() => {
    chatInputRef.value?.focus();
  });
};

const handleTextareaKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    emit('send');
  }
};

const triggerCoverUpload = () => {
  if (props.loading || props.coverUploading) {
    return;
  }
  coverFileInputRef.value?.click();
};

const handleCoverUploadChange = (event: Event) => {
  const input = event.target as HTMLInputElement | null;
  const file = input?.files?.[0];
  if (file) {
    emit('cover-upload', file);
  }
  if (input) {
    input.value = '';
  }
};

const resolveCardType = (entityType?: string) => {
  const type = String(entityType || '').toLowerCase();
  if (type === 'product') return '二手好物';
  if (['event', 'activity', 'local_act', 'local-activity'].includes(type)) return '社区活动';
  if (type === 'story') return '社区故事';
  return '推荐';
};

const canOpenCard = (card: LocalAiCard) => {
  const type = String(card.entityType || '').toLowerCase();
  return !!card.entityId && ['product', 'event', 'activity', 'local_act', 'local-activity', 'story'].includes(type);
};

const handleCardClick = (card: LocalAiCard) => {
  if (!canOpenCard(card)) {
    return;
  }
  emit('card-click', card);
};

const handleImageError = (event: Event) => {
  const image = event.target as HTMLImageElement | null;
  if (!image || image.src === fallbackImage) {
    return;
  }
  image.src = fallbackImage;
};

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return;
    }
    focusInput();
    syncScroll();
  }
);

watch(
  () => props.messages.length,
  () => {
    syncScroll();
  }
);

watch(
  () => props.loading,
  () => {
    syncScroll();
  }
);
</script>

<style scoped>
.local-ai-mask {
  position: fixed;
  top: 72px;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(6px);
  z-index: 1250;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.local-ai-drawer {
  position: fixed;
  top: 72px;
  right: 0;
  width: min(520px, calc(100vw - 48px));
  height: calc(100vh - 72px);
  background: #ffffff;
  border-radius: 22px 0 0 0;
  box-shadow: -16px 0 48px rgba(15, 23, 42, 0.08);
  transform: translateX(102%);
  transition: transform 0.24s ease;
  z-index: 1251;
  display: flex;
  flex-direction: column;
}

.local-ai-drawer.open {
  transform: translateX(0);
}

.drawer-close-btn,
.send-btn {
  border: none;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.18s ease;
}

.drawer-close-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12.5px;
  font-weight: 500;
}

.drawer-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.drawer-body {
  flex: 1;
  min-height: 0;
  padding: 26px;
  display: flex;
  flex-direction: column;
}

.drawer-chat-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
  border-radius: 16px;
  background: transparent;
  box-shadow: none;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f1f5f9;
}

.chat-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.chat-header strong {
  display: block;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: #0f172a;
  letter-spacing: -0.015em;
}

.chat-header p {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: #64748b;
}

.chat-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(56, 185, 130, 0.1);
  color: #1aa053;
  font-size: 11.5px;
  font-weight: 500;
  white-space: nowrap;
}

.chat-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1aa053;
  box-shadow: 0 0 0 3px rgba(56, 185, 130, 0.18);
}

.chat-window {
  flex: 1;
  min-height: 260px;
  margin-top: 18px;
  padding: 4px 2px 4px 0;
  overflow-y: auto;
}

.chat-empty {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.7;
  padding: 22px;
  text-align: center;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 88%;
}

.chat-row.user {
  margin-left: auto;
  align-items: flex-end;
}

.chat-row.agent {
  margin-right: auto;
  align-items: flex-start;
}

.chat-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  background: #f1f5f9;
  color: #0f172a;
  font-size: 13.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-row.user .chat-bubble {
  background: #ff6b2c;
  color: #ffffff;
}

.chat-time {
  font-size: 10.5px;
  color: #94a3b8;
}

.chat-card-list {
  display: grid;
  gap: 12px;
  width: min(100%, 430px);
}

.chat-result-card {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 14px;
  width: 100%;
  padding: 12px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 10px 26px rgba(15, 23, 42, 0.05);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.chat-result-card.clickable {
  cursor: pointer;
}

.chat-result-card.clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 16px 32px rgba(255, 107, 44, 0.12);
}

.chat-result-media {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
}

.chat-result-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.chat-result-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.chat-result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.chat-result-head h5 {
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.45;
}

.chat-result-type {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 7px;
  background: rgba(255, 107, 44, 0.09);
  color: #f25a1b;
  font-size: 11px;
  font-weight: 600;
}

.chat-result-price {
  color: #f25a1b;
  font-size: 13px;
  line-height: 1.5;
  white-space: nowrap;
}

.chat-result-subtitle,
.chat-result-reason,
.chat-result-meta {
  margin: 0;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.6;
}

.chat-result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chat-result-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 11.5px;
}

.chat-result-highlights {
  margin: 0;
  padding-left: 17px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.chat-bubble.loading {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 107, 44, 0.5);
  animation: blink 1s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.chat-error {
  margin: 14px 0 0;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
  font-size: 12px;
  line-height: 1.55;
}

.chat-input-panel {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  transition: box-shadow 0.18s ease, background 0.18s ease;
}

.chat-input-panel:focus-within {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 107, 44, 0.12);
}

.cover-upload-assist {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #fed7aa;
}

.cover-file-input {
  display: none;
}

.cover-upload-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cover-upload-copy strong {
  color: #0f172a;
  font-size: 13px;
  line-height: 1.4;
}

.cover-upload-copy span {
  color: #64748b;
  font-size: 11.5px;
  line-height: 1.5;
}

.cover-upload-preview {
  width: 44px;
  height: 44px;
  border-radius: 9px;
  object-fit: cover;
  grid-row: span 2;
}

.cover-upload-btn {
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 999px;
  background: #fff7ed;
  color: #f25a1b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: background 0.18s ease, transform 0.18s ease;
}

.cover-upload-btn:hover:not(:disabled) {
  background: #ffedd5;
  transform: translateY(-1px);
}

.cover-upload-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.chat-input-panel textarea {
  width: 100%;
  min-height: 86px;
  resize: none;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.65;
  outline: none;
  font-family: inherit;
}

.chat-input-panel textarea::placeholder {
  color: #94a3b8;
}

.chat-input-footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-input-footer span {
  font-size: 11.5px;
  color: #94a3b8;
}

.send-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  background: #ff6b2c;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
}

.send-btn:hover:not(:disabled) {
  background: #f25a1b;
  transform: translateY(-1px);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
}

@keyframes blink {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }

  40% {
    transform: translateY(-2px);
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .local-ai-mask {
    top: 68px;
  }

  .local-ai-drawer {
    top: 68px;
    width: 100vw;
    height: calc(100vh - 68px);
    border-radius: 0;
  }

  .drawer-body {
    padding-left: 18px;
    padding-right: 18px;
  }

  .drawer-body {
    padding-bottom: 18px;
  }
}

@media (max-width: 640px) {
  .chat-header,
  .chat-input-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .chat-header-actions {
    justify-content: space-between;
  }

  .send-btn {
    width: 100%;
  }

  .cover-upload-assist {
    grid-template-columns: 1fr;
  }

  .cover-upload-preview,
  .cover-upload-btn {
    width: 100%;
  }
}
</style>
