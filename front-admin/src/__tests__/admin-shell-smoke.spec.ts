import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createMemoryHistory, createRouter } from 'vue-router'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Home from '../components/Home.vue'

const DummyScreen = { template: '<div class="dummy-screen">screen</div>' }
const readSource = (relativePath: string) =>
  readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

describe('admin shell', () => {
  it('renders sidebar, topbar, and workspace around child routes', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: Home,
          children: [
            {
              path: '',
              component: DummyScreen,
              meta: {
                section: '治理',
                title: '治理总览',
                description: '查看治理状态、最近记录与关键待办。',
              },
            },
          ],
        },
      ],
    })

    router.push('/')
    await router.isReady()

    const wrapper = mount(Home, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: true,
        },
      },
    })

    expect(wrapper.find('.admin-shell').exists()).toBe(true)
    expect(wrapper.find('.admin-shell__sidebar').exists()).toBe(true)
    expect(wrapper.find('.admin-shell__topbar').exists()).toBe(true)
    expect(wrapper.text()).toContain('治理总览')
    expect(wrapper.find('.dummy-screen').exists()).toBe(true)
  })

  it('locks page scrolling and lets the right workspace scroll independently', () => {
    const shellSource = readSource('components/Home.vue')
    const sidebarSource = readSource('components/admin/AdminSidebar.vue')
    const styleSource = readSource('style.css')

    expect(styleSource).toContain('html,')
    expect(styleSource).toContain('overflow: hidden;')
    expect(shellSource).toContain('height: 100vh;')
    expect(shellSource).toContain('.admin-shell__workspace')
    expect(shellSource).toContain('overflow-y: auto;')
    expect(shellSource).toContain('overscroll-behavior: contain;')
    expect(sidebarSource).toContain('.admin-sidebar__nav')
    expect(sidebarSource).toContain('overflow-y: auto;')
  })
})
