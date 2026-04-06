import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import GovernanceDashboard from '../components/governance/GovernanceDashboard.vue'

vi.mock('../api/adminGovernance', () => ({
  fetchGovernanceDashboard: vi.fn().mockResolvedValue({
    overview: { replay_total: 18, days: 7, w12_ready: true, open_failure_total: 2, stage: 'W13' },
    metrics_summary: { degraded_rate: 0.12, degraded_total: 2 },
    recent_releases: [],
    recent_replays: [],
    recent_eval_versions: [],
    recent_regression_sets: [],
    recent_gray_configs: [],
    recent_eval_runs: [],
    metrics_trend: [],
    eval_case_stats: { total: 5, enabled_total: 4, disabled_total: 1, by_risk_level: { high: 1 } },
    error_attribution_summary: { error_total: 1, degraded_total: 2, error_rate: 0.05, degraded_rate: 0.12 },
  }),
  fetchGovernanceMetricsDaily: vi.fn().mockResolvedValue([]),
  fetchErrorAttributionTrend: vi.fn().mockResolvedValue([]),
  getGovernanceApiBase: vi.fn().mockReturnValue(''),
}))

describe('GovernanceDashboard layout', () => {
  it('uses the overview template and shared admin panels', async () => {
    const wrapper = mount(GovernanceDashboard, {
      global: {
        stubs: { RouterLink: true },
      },
    })

    await flushPromises()

    expect(wrapper.find('.admin-page').exists()).toBe(true)
    expect(wrapper.find('.admin-page-header').exists()).toBe(true)
    expect(wrapper.findAll('.admin-metric-card')).toHaveLength(4)
    expect(wrapper.find('.admin-overview-grid').exists()).toBe(true)
    expect(wrapper.findAll('.admin-panel').length).toBeGreaterThanOrEqual(4)
  })
})
