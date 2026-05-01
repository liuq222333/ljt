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

type AgentMessage = {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
};

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
  }>(),
  {
    error: '',
    headline: '围绕当前活动继续提问',
    emptyText: '消息会发送到后端活动助手接口，并保留当前会话上下文。',
    placeholder: '例如：帮我写一条今晚活动的群提醒，语气亲切一些'
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'update:inputValue', value: string): void;
  (event: 'send'): void;
}>();

const chatWindowRef = ref<HTMLDivElement | null>(null);
const chatInputRef = ref<HTMLTextAreaElement | null>(null);

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
}
</style>
