import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import LaunchCenter from '../components/launch/LaunchCenter.vue'

vi.mock('../api/adminLaunch', () => ({
  fetchLaunchReadiness: vi.fn().mockResolvedValue({ overallReady: true, gates: { checklist: true } }),
  fetchLaunchChecklist: vi.fn().mockResolvedValue([]),
  fetchLaunchFinalSummary: vi.fn().mockResolvedValue({ finalReady: true, gates: { readinessGate: true } }),
  fetchLaunchHandoffSummary: vi.fn().mockResolvedValue({ finalReady: true, ownerActions: [] }),
  fetchLaunchRunbookBundle: vi.fn().mockResolvedValue({ documents: {} }),
  fetchLaunchTimeline: vi.fn().mockResolvedValue([]),
  fetchLaunchPackage: vi.fn().mockResolvedValue({}),
  listLaunchLoadTests: vi.fn().mockResolvedValue([]),
  listLaunchDrills: vi.fn().mockResolvedValue([]),
  listLaunchWindows: vi.fn().mockResolvedValue([]),
  runLaunchSmoke: vi.fn(),
  forceLaunchRealtimeFallback: vi.fn(),
  recoverLaunchRealtimeFallback: vi.fn(),
  recordLaunchLoadTest: vi.fn(),
  recordLaunchDrill: vi.fn(),
  recordLaunchSignoff: vi.fn(),
  recordLaunchDependencyCheck: vi.fn(),
  createLaunchWindow: vi.fn(),
  closeLaunchWindow: vi.fn(),
}))

vi.mock('../utils/governanceFile', () => ({
  downloadJsonFile: vi.fn(),
}))

describe('LaunchCenter layout', () => {
  it('renders the workbench template with toolbar, content column, and side column', async () => {
    const wrapper = mount(LaunchCenter)
    await flushPromises()

    expect(wrapper.find('.admin-page').exists()).toBe(true)
    expect(wrapper.find('.admin-toolbar').exists()).toBe(true)
    expect(wrapper.find('.admin-workbench').exists()).toBe(true)
    expect(wrapper.findAll('.admin-panel').length).toBeGreaterThanOrEqual(4)
  })
})
