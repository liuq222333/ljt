<template>
  <div class="card">
    <h2>发布通知公告</h2>
    <form @submit.prevent="submit">
      <div class="form-row">
        <label>标题</label>
        <input v-model="form.title" type="text" placeholder="请输入标题" required />
      </div>
      <div class="form-row">
        <label>内容</label>
        <textarea v-model="form.content" rows="4" placeholder="请输入内容" required></textarea>
      </div>
      <div class="form-row">
        <label>通知类型</label>
        <input v-model="form.kind" type="text" placeholder="如 SYSTEM / PRODUCT_PUBLISHED" required />
      </div>
      <div class="form-grid">
        <div class="form-row">
          <label>目标类型</label>
          <select v-model="form.targetType">
            <option value="USER">单用户</option>
            <option value="BROADCAST">全员广播</option>
            <option value="GROUP">分组</option>
          </select>
        </div>
        <div class="form-row">
          <label>目标用户ID</label>
          <input v-model.number="form.targetUserId" type="number" :disabled="form.targetType === 'BROADCAST'" placeholder="用户ID" />
        </div>
        <div class="form-row">
          <label>分组Key</label>
          <input v-model="form.groupKey" type="text" :disabled="form.targetType !== 'GROUP'" placeholder="GROUP 时填写" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-row">
          <label>优先级</label>
          <input v-model.number="form.priority" type="number" min="0" placeholder="默认 0" />
        </div>
        <div class="form-row">
          <label>TTL(ms)</label>
          <input v-model.number="form.ttlMs" type="number" min="0" placeholder="可选" />
        </div>
      </div>
      <div class="form-row">
        <label>跳转链接</label>
        <input v-model="form.actionUrl" type="url" placeholder="https://example.com/detail" />
      </div>
      <div class="actions">
        <button type="button" class="ghost" @click="reset">重置</button>
        <button type="submit" class="primary" :disabled="loading">
          {{ loading ? '提交中...' : '提交发布' }}
        </button>
      </div>
      <p v-if="message" :class="['msg', messageType]">{{ message }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

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
  actionUrl: ''
})

const loading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

function buildPayload() {
  const payload: Record<string, any> = {
    title: form.title,
    content: form.content,
    kind: form.kind,
    targetType: form.targetType,
    priority: form.priority,
    ttlMs: form.ttlMs,
    actionUrl: form.actionUrl
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
  form.kind = ''
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
    message.value = '标题/内容/类型不能为空'
    messageType.value = 'error'
    return
  }
  if (form.targetType === 'USER' && !form.targetUserId) {
    message.value = '请填写目标用户ID'
    messageType.value = 'error'
    return
  }
  loading.value = true
  try {
    const resp = await fetch(`${API_BASE}/api/admin/notifications/add`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(buildPayload())
    })
    const data = await resp.json()
    if (!resp.ok || data.code !== 200) {
      throw new Error(data?.message || '发布失败')
    }
    message.value = '发布成功'
    alert("发布成功")
    messageType.value = 'success'
    reset()
  } catch (e: any) {
    message.value = e?.message || '发布失败'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card {
  max-width: 780px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}
h2 {
  margin: 0 0 16px;
  font-size: 20px;
  color: #111827;
}
form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
label {
  font-size: 14px;
  color: #4b5563;
}
input,
select,
textarea {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
input:focus,
select:focus,
textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}
textarea {
  resize: vertical;
}
.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}
.primary,
.ghost {
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.primary {
  background: linear-gradient(120deg, #2563eb, #1d4ed8);
  color: #fff;
}
.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ghost {
  background: #f3f4f6;
  color: #111827;
}
.msg {
  margin-top: 4px;
  font-size: 14px;
}
.msg.success {
  color: #15803d;
}
.msg.error {
  color: #dc2626;
}
</style>
