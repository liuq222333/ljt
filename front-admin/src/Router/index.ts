import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../components/Home.vue'),
    children: [
      {
        path: '',
        redirect: '/admin/governance/dashboard'
      },
      {
        path: 'admin/governance/dashboard',
        name: 'GovernanceDashboard',
        component: () => import('../components/governance/GovernanceDashboard.vue'),
        meta: {
          section: '治理',
          title: '治理总览',
          description: '查看治理状态、最近记录与关键待办。',
        },
      },
      {
        path: 'admin/governance/replay',
        name: 'GovernanceReplayCenter',
        component: () => import('../components/governance/GovernanceReplayCenter.vue'),
        meta: {
          section: '治理',
          title: '回放中心',
          description: '定位执行链路与工具交互，快速复盘异常会话。',
        },
      },
      {
        path: 'admin/governance/eval',
        name: 'GovernanceEvalCenter',
        component: () => import('../components/governance/GovernanceEvalCenter.vue'),
        meta: {
          section: '治理',
          title: '评测中心',
          description: '管理评测集与回归对比，跟踪质量变化。',
        },
      },
      {
        path: 'admin/governance/release',
        name: 'GovernanceReleaseCenter',
        component: () => import('../components/governance/GovernanceReleaseCenter.vue'),
        meta: {
          section: '治理',
          title: '发布管理',
          description: '安排发布检查、灰度节奏与上线审批。',
        },
      },
      {
        path: 'admin/governance/diagnostics',
        name: 'GovernanceDiagnosticsCenter',
        component: () => import('../components/governance/GovernanceDiagnosticsCenter.vue'),
        meta: {
          section: '治理',
          title: '诊断中心',
          description: '查看诊断信号、连通状态与问题排查建议。',
        },
      },
      {
        path: 'admin/launch/center',
        name: 'LaunchCenter',
        component: () => import('../components/launch/LaunchCenter.vue'),
        meta: {
          section: '上线准备',
          title: '上线工作台',
          description: '查看上线准备、演练记录与执行交接。',
        },
      },
      {
        path: 'notifications/publish',
        name: 'NotificationPublish',
        component: () => import('../components/NotificationPublish.vue'),
        meta: {
          section: '运营管理',
          title: '通知发布',
          description: '编写并发布运营通知，统一消息触达。',
        },
      },
      {
        path: 'admin/local-activity/review',
        name: 'ActivityReview',
        component: () => import('../components/ActivityReview.vue'),
        meta: {
          section: '运营管理',
          title: '活动审核',
          description: '审核活动申请并处理通过与驳回。',
        },
      },
      {
        path: 'admin/local-activity/schedule-review',
        name: 'ScheduleReview',
        component: () => import('../components/ActivityReview.vue'),
        meta: {
          section: '运营管理',
          title: '排期审核',
          description: '审核固定时段活动并安排执行窗口。',
        },
      },
      {
        path:'admin/user-management',
        name:'UserManagement',
        component: () => import('../components/UserManagement.vue'),
        meta: {
          section: '运营管理',
          title: '用户管理',
          description: '维护用户资料、状态与账号管理操作。',
        },
      },
      {
        path: 'admin/neighbor-tasks/review',
        name: 'TaskReview',
        component: () => import('../components/TaskReview.vue'),
        meta: {
          section: '运营管理',
          title: '任务审核',
          description: '审核社区任务内容并处理运营决策。',
        },
      },
      {
        path: 'admin/api-management',
        name: 'ApiManagement',
        component: () => import('../components/ApiManagement.vue'),
        meta: {
          section: '运营管理',
          title: '接口管理',
          description: '维护接口路由、启停状态与配置项。',
        },
      },
      {
        path: 'admin/second-hand',
        name: 'MarketProductsManagement',
        component: () => import('../components/MarketProductsManagement.vue'),
        meta: {
          section: '运营管理',
          title: '二手市场管理',
          description: '管理二手商品内容与审核处理流程。',
        },
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
