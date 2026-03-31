<template>
  <article class="json-card">
    <div class="head">
      <strong>{{ title }}</strong>
      <div class="actions">
        <button class="ghost" type="button" @click="toggleExpanded">
          {{ expanded ? '收起' : '展开' }}
        </button>
        <button class="ghost" type="button" @click="copyText">
          {{ copied ? '已复制' : '复制' }}
        </button>
      </div>
    </div>
    <pre :class="{ collapsed: !expanded }">{{ formatted }}</pre>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  value?: unknown
  defaultExpanded?: boolean
}>(), {
  defaultExpanded: false,
})

const expanded = ref(props.defaultExpanded)
const copied = ref(false)

watch(
  () => props.defaultExpanded,
  (value) => {
    expanded.value = value
  },
)

const formatted = computed(() => JSON.stringify(props.value ?? {}, null, 2))

function toggleExpanded() {
  expanded.value = !expanded.value
}

async function copyText() {
  try {
    await navigator.clipboard.writeText(formatted.value)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 1200)
  } catch {
    copied.value = false
  }
}
</script>

<style scoped>
.json-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.actions {
  display: flex;
  gap: 8px;
}

.ghost {
  border: 0;
  border-radius: 999px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  background: #dbeafe;
  color: #1d4ed8;
}

pre {
  margin: 0;
  padding: 12px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
}

pre.collapsed {
  max-height: 180px;
}
</style>
