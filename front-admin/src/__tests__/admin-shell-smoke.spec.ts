import { createMemoryHistory, createRouter } from 'vue-router'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Home from '../components/Home.vue'

const DummyScreen = { template: '<div class="dummy-screen">screen</div>' }

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
})
