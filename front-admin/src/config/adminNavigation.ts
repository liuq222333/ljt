export type AdminNavItem = {
  label: string
  to: string
}

export type AdminNavGroup = {
  id: string
  title: string
  items: AdminNavItem[]
}

export const adminNavGroups: AdminNavGroup[] = [
  {
    id: 'governance',
    title: '治理',
    items: [
      { label: '治理总览', to: '/admin/governance/dashboard' },
      { label: '回放中心', to: '/admin/governance/replay' },
      { label: '评测中心', to: '/admin/governance/eval' },
      { label: '发布管理', to: '/admin/governance/release' },
      { label: '诊断中心', to: '/admin/governance/diagnostics' },
    ],
  },
  {
    id: 'launch',
    title: '上线准备',
    items: [{ label: '上线工作台', to: '/admin/launch/center' }],
  },
  {
    id: 'operations',
    title: '运营管理',
    items: [
      { label: '活动审核', to: '/admin/local-activity/review' },
      { label: '排期审核', to: '/admin/local-activity/schedule-review' },
      { label: '任务审核', to: '/admin/neighbor-tasks/review' },
      { label: '通知发布', to: '/notifications/publish' },
      { label: '用户管理', to: '/admin/user-management' },
      { label: '接口管理', to: '/admin/api-management' },
      { label: '二手市场管理', to: '/admin/second-hand' },
    ],
  },
]
