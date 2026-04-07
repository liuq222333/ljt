<template>
  <section class="admin-page notification-publish-page">
    <AdminPageHeader eyebrow="运营管理" title="通知发布" description="编写运营通知并统一配置触达目标、优先级和跳转链接。">
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="reset">
          清空表单
        </button>
      </template>
    </AdminPageHeader>

    <AdminToolbar>
      <template #filters>
        <span class="meta-chip">通知类型 {{ form.kind || '未填写' }}</span>
        <span class="meta-chip">触达范围 {{ targetSummary }}</span>
        <span class="meta-chip">优先级 {{ form.priority ?? 0 }}</span>
      </template>
      <template #actions>
        <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="reset">
          重置
        </button>
        <button class="admin-button admin-button--primary" type="button" :disabled="loading" @click="submit">
          {{ loading ? '发布中...' : '提交发布' }}
        </button>
      </template>
    </AdminToolbar>

    <AdminStateBlock v-if="message" :tone="messageTone" :message="message" />

    <form class="publish-layout" @submit.prevent="submit">
      <AdminPanel title="通知内容" description="填写标题、正文和消息类型，保持公告语言清晰一致。">
        <div class="form-grid">
          <label class="form-field form-field--full">
            <span>标题</span>
            <input v-model="form.title" type="text" placeholder="请输入通知标题" />
          </label>
          <label class="form-field form-field--full">
            <span>内容</span>
            <textarea v-model="form.content" rows="8" placeholder="请输入通知正文"></textarea>
          </label>
          <label class="form-field">
            <span>通知类型</span>
            <input v-model="form.kind" type="text" placeholder="例如 SYSTEM / PRODUCT_PUBLISHED" />
          </label>
          <label class="form-field">
            <span>跳转链接</span>
            <input v-model="form.actionUrl" type="url" placeholder="https://example.com/detail" />
          </label>
        </div>
      </AdminPanel>

      <div class="side-stack">
        <AdminPanel title="投放设置" description="控制消息发送范围和有效期。">
          <div class="form-grid form-grid--stack">
            <label class="form-field">
              <span>目标类型</span>
              <select v-model="form.targetType">
                <option value="USER">单个用户</option>
                <option value="BROADCAST">全员广播</option>
                <option value="GROUP">分组推送</option>
              </select>
            </label>
            <label class="form-field">
              <span>目标用户 ID</span>
              <input
                v-model.number="form.targetUserId"
                type="number"
                :disabled="form.targetType !== 'USER'"
                placeholder="仅单个用户时填写"
              />
            </label>
            <label class="form-field">
              <span>分组 Key</span>
              <input
                v-model="form.groupKey"
                type="text"
                :disabled="form.targetType !== 'GROUP'"
                placeholder="仅分组推送时填写"
              />
            </label>
            <label class="form-field">
              <span>优先级</span>
              <input v-model.number="form.priority" type="number" min="0" placeholder="默认 0" />
            </label>
            <label class="form-field">
              <span>TTL（毫秒）</span>
              <input v-model.number="form.ttlMs" type="number" min="0" placeholder="不填则按服务默认值" />
            </label>
          </div>
        </AdminPanel>

        <AdminPanel title="发送预览" description="提交前快速确认消息落点与校验要点。">
          <dl class="summary-list">
            <div>
              <dt>触达范围</dt>
              <dd>{{ targetSummary }}</dd>
            </div>
            <div>
              <dt>有效期</dt>
              <dd>{{ ttlSummary }}</dd>
            </div>
            <div>
              <dt>优先级</dt>
              <dd>{{ form.priority ?? 0 }}</dd>
            </div>
            <div>
              <dt>跳转链接</dt>
              <dd>{{ form.actionUrl || '无' }}</dd>
            </div>
          </dl>

          <div class="tips-block">
            <h3>发布前检查</h3>
            <ul>
              <li>标题、内容、通知类型不能为空。</li>
              <li>单个用户推送时，需要填写目标用户 ID。</li>
              <li>分组推送时，建议填写明确的业务分组 Key。</li>
            </ul>
          </div>

          <div class="panel-actions">
            <button class="admin-button admin-button--secondary" type="button" :disabled="loading" @click="reset">
              重置
            </button>
            <button class="admin-button admin-button--primary" type="submit" :disabled="loading">
              {{ loading ? '发布中...' : '提交发布' }}
            </button>
          </div>
        </AdminPanel>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import AdminPageHeader from './admin/AdminPageHeader.vue'
import AdminPanel from './admin/AdminPanel.vue'
import AdminStateBlock from './admin/AdminStateBlock.vue'
import AdminToolbar from './admin/AdminToolbar.vue'

type TargetType = 'USER' | 'BROADCAST' | 'GROUP'

const API_BASE = (import.meta as any)?.env?.VITE_API_BASE ?? 'http://localhost:8080'

const form = reactive({
  title: '',
  content: '',
  kind: 'SYSTEM',
  targetType: 'USER' as TargetType,
  targetUserId: undefined as number | undefined,
  groupKey: '',
  priority: 0 as number | undefined,
  ttlMs: undefined as number | undefined,
  actionUrl: '',
})

const loading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const messageTone = computed(() => (messageType.value === 'success' ? 'success' : 'danger'))
const targetSummary = computed(() => {
  if (form.targetType === 'BROADCAST') return '全员广播'
  if (form.targetType === 'GROUP') return form.groupKey ? `分组 ${form.groupKey}` : '分组推送（未填写 Key）'
  return form.targetUserId ? `用户 ${form.targetUserId}` : '单个用户（未填写 ID）'
})
const ttlSummary = computed(() => (form.ttlMs ? `${form.ttlMs} ms` : '按服务默认值'))

function buildPayload() {
  const payload: Record<string, any> = {
    title: form.title,
    content: form.content,
    kind: form.kind,
    targetType: form.targetType,
    priority: form.priority,
    ttlMs: form.ttlMs,
    actionUrl: form.actionUrl,
  }

  if (form.targetType === 'USER') {
    payload.targetUserId = form.targetUserId
  }
  if (form.targetType === 'GROUP') {
    payload.groupKey = form.groupKey
  }
  return payload
}

function reset() {
  form.title = ''
  form.content = ''
  form.kind = 'SYSTEM'
  form.targetType = 'USER'
  form.targetUserId = undefined
  form.groupKey = ''
  form.priority = 0
  form.ttlMs = undefined
  form.actionUrl = ''
  message.value = ''
}

async function submit() {
  message.value = ''

  if (!form.title || !form.content || !form.kind) {
    message.value = '标题、内容和通知类型不能为空'
    messageType.value = 'error'
    return
  }

  if (form.targetType === 'USER' && !form.targetUserId) {
    message.value = '请选择目标用户并填写用户 ID'
    messageType.value = 'error'
    return
  }

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/notifications/add`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(buildPayload()),
    })
    const data = await response.json()
    if (!response.ok || data?.code !== 200) {
      throw new Error(data?.message || '发布失败')
    }
    messageType.value = 'success'
    message.value = '通知发布成功'
    window.alert('通知发布成功')
    reset()
  } catch (error: any) {
    messageType.value = 'error'
    message.value = error?.message || '发布失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.notification-publish-page {
  gap: 12px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  color: var(--admin-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.publish-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 360px;
  gap: 12px;
  align-items: start;
}

.side-stack {
  display: grid;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-grid--stack {
  grid-template-columns: 1fr;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
}

.form-field textarea {
  resize: vertical;
  min-height: 168px;
  line-height: 1.55;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  border-color: var(--admin-border-strong);
  box-shadow: 0 0 0 3px rgba(37, 50, 68, 0.08);
}

.summary-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.summary-list div {
  display: grid;
  gap: 4px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eceff3;
}

.summary-list dt {
  color: var(--admin-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.summary-list dd {
  margin: 0;
  color: var(--admin-text-primary);
  font-size: 13px;
  line-height: 1.45;
}

.tips-block {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
}

.tips-block h3 {
  margin: 0 0 8px;
  color: var(--admin-text-primary);
  font-size: 13px;
}

.tips-block ul {
  margin: 0;
  padding-left: 16px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.admin-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.admin-button:hover {
  border-color: var(--admin-border-strong);
}

.admin-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.admin-button--primary {
  border-color: var(--admin-accent);
  background: var(--admin-accent);
  color: #fff;
}

.admin-button--secondary {
  background: var(--admin-bg-subtle);
}
</style>
