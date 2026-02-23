import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../components/Home.vue'),
    children: [
      {
        path: 'notifications/publish',
        name: 'NotificationPublish',
        component: () => import('../components/NotificationPublish.vue')
      },
      {
        path: 'admin/local-activity/review',
        name: 'ActivityReview',
        component: () => import('../components/ActivityReview.vue')
      },
      {
        path: 'admin/local-activity/schedule-review',
        name: 'ScheduleReview',
        component: () => import('../components/ActivityReview.vue')
      },
      {
        path:'admin/user-management',
        name:'UserManagement',
        component: () => import('../components/UserManagement.vue')
      },
      {
        path: 'admin/neighbor-tasks/review',
        name: 'TaskReview',
        component: () => import('../components/TaskReview.vue')
      },
      {
        path: 'admin/api-management',
        name: 'ApiManagement',
        component: () => import('../components/ApiManagement.vue')
      },
      {
        path: 'admin/second-hand',
        name: 'MarketProductsManagement',
        component: () => import('../components/MarketProductsManagement.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
