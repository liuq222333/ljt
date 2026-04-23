import { computed, ref } from 'vue';

export interface MarketplaceAiCard {
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
}

export interface MarketplaceAiFinalAnswer {
  answerType?: string;
  answerText?: string;
  summary?: string;
  cards?: MarketplaceAiCard[];
}

export interface MarketplaceAiMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  time: string;
  preview?: string;
  cards?: MarketplaceAiCard[];
  answerType?: string;
}

interface UseCommunityMarketplaceAiOptions {
  apiBase: string;
  initialAgentMessage?: string;
  quickPrompts?: string[];
  sessionStorageKey?: string;
  buildUserProfile?: () => Record<string, unknown>;
}

function formatChatTime(date = new Date()) {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function summarizeText(text: string, maxLength = 60) {
  const normalized = text.replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return '';
  }
  return normalized.length > maxLength ? `${normalized.slice(0, maxLength)}...` : normalized;
}

export function useCommunityMarketplaceAi(options: UseCommunityMarketplaceAiOptions) {
  const chatApi = `${options.apiBase}/api/agent/chat`;
  const storageKey = options.sessionStorageKey ?? '';

  const drawerOpen = ref(false);
  const input = ref('');
  const loading = ref(false);
  const error = ref('');
  const sessionId = ref(storageKey ? sessionStorage.getItem(storageKey) || '' : '');
  const messages = ref<MarketplaceAiMessage[]>(
    options.initialAgentMessage
      ? [
          {
            id: 'welcome',
            sender: 'agent',
            text: options.initialAgentMessage,
            time: formatChatTime()
          }
        ]
      : []
  );

  const latestUserMessage = computed(() => {
    return messages.value.slice().reverse().find((message) => message.sender === 'user') ?? null;
  });

  const latestAgentMessage = computed(() => {
    return messages.value.slice().reverse().find((message) => message.sender === 'agent') ?? null;
  });

  const latestUserPreview = computed(() => {
    return latestUserMessage.value ? summarizeText(latestUserMessage.value.preview ?? latestUserMessage.value.text, 42) : '';
  });

  const latestAgentReplyPreview = computed(() => {
    return latestAgentMessage.value ? summarizeText(latestAgentMessage.value.preview ?? latestAgentMessage.value.text, 72) : '';
  });

  function openDrawer() {
    drawerOpen.value = true;
  }

  function closeDrawer() {
    drawerOpen.value = false;
  }

  function toggleDrawer() {
    drawerOpen.value = !drawerOpen.value;
  }

  function prefillInput(text: string) {
    input.value = text;
  }

  async function submitMessage() {
    const text = input.value.trim();
    if (!text || loading.value) {
      return;
    }

    error.value = '';
    const now = new Date();
    messages.value.push({
      id: `${now.getTime()}-user`,
      sender: 'user',
      text,
      preview: text,
      time: formatChatTime(now)
    });
    input.value = '';
    loading.value = true;

    try {
      const token = localStorage.getItem('token') || '';
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (token) {
        headers.Authorization = token;
      }

      const response = await fetch(chatApi, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          messages: messages.value
            .filter((message) => message.id !== 'welcome')
            .map((message) => ({
              role: message.sender === 'user' ? 'user' : 'assistant',
              content: message.text
            })),
          sessionId: sessionId.value || undefined,
          userProfile: options.buildUserProfile?.()
        })
      });

      let result: any = null;
      try {
        result = await response.json();
      } catch {
        result = null;
      }

      if (!response.ok || !result || result.code !== 200) {
        throw new Error(result?.message || '服务响应异常，请稍后重试');
      }

      const finalAnswer = (result?.data?.finalAnswer ?? null) as MarketplaceAiFinalAnswer | null;
      const reply =
        result?.data?.reply?.trim() ||
        finalAnswer?.answerText?.trim() ||
        '已收到你的需求，请继续补充更多细节。';
      const returnedSessionId = result?.data?.sessionId?.trim?.() || '';
      if (returnedSessionId && storageKey) {
        sessionId.value = returnedSessionId;
        sessionStorage.setItem(storageKey, returnedSessionId);
      }

      messages.value.push({
        id: `${Date.now()}-agent`,
        sender: 'agent',
        text: reply,
        preview: finalAnswer?.summary?.trim?.() || reply,
        time: formatChatTime(),
        cards: Array.isArray(finalAnswer?.cards) ? finalAnswer.cards : [],
        answerType: finalAnswer?.answerType
      });
    } catch (err: any) {
      error.value = err?.message || '网络错误，请稍后重试';
      setTimeout(() => {
        if (error.value === (err?.message || '网络错误，请稍后重试')) {
          error.value = '';
        }
      }, 3000);
    } finally {
      loading.value = false;
    }
  }

  return {
    drawerOpen,
    input,
    loading,
    error,
    messages,
    quickPrompts: options.quickPrompts ?? [],
    latestUserMessage,
    latestAgentMessage,
    latestUserPreview,
    latestAgentReplyPreview,
    openDrawer,
    closeDrawer,
    toggleDrawer,
    prefillInput,
    submitMessage
  };
}
