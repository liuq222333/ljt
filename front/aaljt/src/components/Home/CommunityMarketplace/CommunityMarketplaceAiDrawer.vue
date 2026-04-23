<template>
  <transition name="drawer-fade">
    <div v-if="modelValue" class="market-ai-drawer-mask" @click="emit('update:modelValue', false)"></div>
  </transition>

  <aside :class="['market-ai-drawer', { open: modelValue }]">
    <header class="drawer-header">
      <div>
        <p class="drawer-kicker">{{ kicker }}</p>
        <h3>{{ title }}</h3>
      </div>
      <button class="ghost-btn compact" type="button" @click="emit('update:modelValue', false)">关闭</button>
    </header>

    <div class="drawer-body">
      <section class="drawer-chat-card">
        <header class="chat-header">
          <div class="chat-header-main">
            <strong>{{ headline }}</strong>
            <span class="chat-status">{{ loading ? '处理中' : '在线' }}</span>
          </div>
          <p>{{ helperText }}</p>
        </header>

        <div ref="chatWindowRef" class="chat-window">
          <div v-if="!messages.length" class="chat-empty">
            {{ emptyText }}
          </div>

          <div v-else class="chat-messages">
            <div v-for="message in messages" :key="message.id" :class="['chat-row', message.sender]">
              <div class="chat-bubble">{{ message.text }}</div>

              <div v-if="message.sender === 'agent' && message.cards?.length" class="chat-card-list">
                <article
                  v-for="card in message.cards"
                  :key="`${message.id}-${card.entityId || card.title}`"
                  class="chat-result-card"
                  :class="{ clickable: canOpenCard(card) }"
                  @click="handleCardClick(card)"
                >
                  <div class="chat-result-card-media">
                    <img :src="card.imageUrl || fallbackImage" :alt="card.title || 'result card'" />
                  </div>
                  <div class="chat-result-card-body">
                    <div class="chat-result-card-head">
                      <h5>{{ card.title || '未命名商品' }}</h5>
                      <span v-if="card.priceText" class="chat-result-price">{{ card.priceText }}</span>
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

        <div v-if="quickPrompts.length" class="ai-quick-list">
          <button v-for="prompt in quickPrompts" :key="prompt" class="ai-quick-btn" type="button" @click="emit('prompt', prompt)">
            {{ prompt }}
          </button>
        </div>

        <p v-if="error" class="chat-error">{{ error }}</p>

        <div class="chat-input-area">
          <input
            ref="chatInputRef"
            :value="inputValue"
            type="text"
            :disabled="loading"
            :placeholder="placeholder"
            @input="emit('update:inputValue', ($event.target as HTMLInputElement).value)"
            @keyup.enter="emit('send')"
          />
          <button class="primary-btn" type="button" :disabled="loading || !inputValue.trim()" @click="emit('send')">
            {{ loading ? '发送中' : '发送' }}
          </button>
        </div>
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import type { MarketplaceAiCard, MarketplaceAiMessage } from './useCommunityMarketplaceAi';

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="220"><rect width="100%" height="100%" fill="%23eef2ec"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23839280" font-size="16">暂无图片</text></svg>';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    kicker?: string;
    title?: string;
    headline?: string;
    helperText?: string;
    emptyText?: string;
    placeholder?: string;
    quickPrompts?: string[];
    messages: MarketplaceAiMessage[];
    inputValue: string;
    loading: boolean;
    error?: string;
  }>(),
  {
    kicker: 'AI 对话框',
    title: '市场助手',
    headline: '随时提问',
    helperText: '支持继续追问，消息会发送到后端接口，并保留当前会话上下文。',
    emptyText: '你好，我是你的社区二手市场助手。你可以直接问我找商品、写文案、判断性价比或整理卖点。',
    placeholder: '继续输入你的问题',
    quickPrompts: () => [],
    error: ''
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'update:inputValue', value: string): void;
  (event: 'prompt', value: string): void;
  (event: 'send'): void;
  (event: 'card-click', value: MarketplaceAiCard): void;
}>();

const chatWindowRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<HTMLInputElement | null>(null);

function canOpenCard(card: MarketplaceAiCard) {
  return card.entityType === 'product' && !!card.entityId;
}

function handleCardClick(card: MarketplaceAiCard) {
  if (!canOpenCard(card)) {
    return;
  }
  emit('card-click', card);
}

function syncChatWindow() {
  nextTick(() => {
    if (chatWindowRef.value) {
      chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
    }
  });
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return;
    }
    nextTick(() => {
      chatInputRef.value?.focus();
      syncChatWindow();
    });
  }
);

watch(
  () => props.messages.length,
  () => {
    syncChatWindow();
  }
);

watch(
  () => props.loading,
  () => {
    syncChatWindow();
  }
);
</script>

<style scoped>
.market-ai-drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(19, 30, 20, 0.22);
  z-index: 1300;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.market-ai-drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: min(560px, calc(100vw - 92px));
  height: 100vh;
  background: #ffffff;
  border-left: 1px solid #e5ece1;
  box-shadow: -18px 0 42px rgba(57, 84, 58, 0.12);
  transform: translateX(102%);
  transition: transform 0.24s ease;
  z-index: 1301;
  display: flex;
  flex-direction: column;
}

.market-ai-drawer.open {
  transform: translateX(0);
}

.drawer-header {
  padding: 22px 24px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #ebf1e6;
}

.drawer-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2d8b56;
  font-weight: 700;
}

.drawer-header h3 {
  margin: 8px 0 0;
  font-size: 26px;
  color: #1d2d1f;
}

.drawer-body {
  flex: 1;
  padding: 18px;
  overflow: hidden;
}

.drawer-chat-card {
  height: 100%;
  border: 1px solid #ebf1e6;
  border-radius: 18px;
  background: #fcfefb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 18px 18px 14px;
  border-bottom: 1px solid #ebf1e6;
}

.chat-header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.chat-header-main strong {
  font-size: 18px;
  color: #223123;
}

.chat-header p {
  margin: 6px 0 0;
  color: #6b7a6d;
  line-height: 1.65;
}

.chat-status {
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: #edf8f1;
  color: #2d8b56;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
}

.chat-empty {
  padding: 18px;
  border-radius: 14px;
  border: 1px dashed #d8e4d4;
  background: #ffffff;
  color: #6b7a6d;
  font-size: 14px;
  line-height: 1.8;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 90%;
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
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid #e1eadc;
  background: #ffffff;
  color: #314032;
  font-size: 14px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-row.user .chat-bubble {
  border-color: rgba(40, 183, 93, 0.18);
  background: #edf8f1;
}

.chat-time {
  font-size: 11px;
  color: #99a498;
}

.chat-card-list {
  display: grid;
  gap: 10px;
  width: 100%;
}

.chat-result-card {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #e5ece1;
  background: #ffffff;
}

.chat-result-card.clickable {
  cursor: pointer;
}

.chat-result-card.clickable:hover {
  border-color: rgba(40, 183, 93, 0.35);
  box-shadow: 0 8px 18px rgba(40, 183, 93, 0.08);
}

.chat-result-card-media {
  width: 92px;
  height: 92px;
  border-radius: 12px;
  overflow: hidden;
  background: #f2f5f0;
}

.chat-result-card-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.chat-result-card-body {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.chat-result-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.chat-result-card-head h5 {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
  color: #233224;
}

.chat-result-subtitle,
.chat-result-reason,
.chat-result-meta {
  margin: 0;
  color: #6b7a6d;
  line-height: 1.65;
}

.chat-result-price {
  color: #ef5824;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
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
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #edf7f0;
  color: #2d8b56;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
}

.chat-result-highlights {
  margin: 0;
  padding-left: 18px;
  color: #6b7a6d;
  font-size: 12px;
  line-height: 1.62;
}

.chat-bubble.loading {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #90a08f;
  animation: typingPulse 1s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes typingPulse {
  0%,
  80%,
  100% {
    opacity: 0.42;
    transform: scale(0.72);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.ai-quick-list {
  display: grid;
  gap: 8px;
  padding: 0 18px 14px;
}

.ai-quick-btn {
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid #e7eee3;
  background: #ffffff;
  color: #465547;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.ai-quick-btn:hover {
  border-color: rgba(40, 183, 93, 0.24);
  background: #f6fbf6;
  color: #2d8b56;
}

.chat-error {
  margin: 0 18px 12px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid #f3d1d1;
  background: #fff7f7;
  color: #c85656;
  font-size: 13px;
}

.chat-input-area {
  padding: 14px 18px 18px;
  border-top: 1px solid #ebf1e6;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.chat-input-area input {
  min-height: 44px;
  border-radius: 14px;
  border: 1px solid #dce7d7;
  background: #ffffff;
  padding: 0 14px;
}

.chat-input-area input:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(40, 183, 93, 0.12);
}

@media (max-width: 1100px) {
  .market-ai-drawer {
    width: min(100vw, 560px);
  }
}

@media (max-width: 780px) {
  .chat-input-area {
    grid-template-columns: 1fr;
  }

  .market-ai-drawer {
    width: 100vw;
  }
}
</style>
