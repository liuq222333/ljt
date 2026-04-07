<template>
  <article class="governance-json-block">
    <header class="governance-json-block__header">
      <strong class="governance-json-block__title">{{ title }}</strong>
      <div class="governance-json-block__actions">
        <button class="governance-json-block__button" type="button" @click="toggleExpanded">
          {{ expanded ? '收起' : '展开' }}
        </button>
        <button class="governance-json-block__button" type="button" @click="copyText">
          {{ copied ? '已复制' : '复制' }}
        </button>
      </div>
    </header>
    <pre class="governance-json-block__content" :class="{ 'is-collapsed': !expanded }">{{ formatted }}</pre>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    title: string
    value?: unknown
    defaultExpanded?: boolean
  }>(),
  {
    defaultExpanded: false,
  },
)

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
    if (!navigator.clipboard?.writeText) {
      copied.value = false
      return
    }
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
.governance-json-block {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-subtle);
  padding: 10px;
}

.governance-json-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.governance-json-block__title {
  color: var(--admin-text-primary);
  font-size: 12px;
  font-weight: 700;
}

.governance-json-block__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.governance-json-block__button {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: var(--admin-bg-surface);
  color: var(--admin-text-secondary);
  font-size: 12px;
  padding: 4px 8px;
  cursor: pointer;
}

.governance-json-block__content {
  margin: 8px 0 0;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-control);
  background: #f3f5f8;
  color: var(--admin-text-primary);
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  padding: 10px;
}

.governance-json-block__content.is-collapsed {
  max-height: 180px;
}
</style>
