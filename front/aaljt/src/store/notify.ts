import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotifyStore = defineStore('notify', () => {
  const unread = ref(0)
  function incr(delta: number) {
    unread.value += delta
    if (unread.value < 0) unread.value = 0
  }
  function set(count: number) {
    unread.value = Math.max(0, count || 0)
  }
  function clear() {
    unread.value = 0
  }
  return { unread, incr, set, clear }
})
