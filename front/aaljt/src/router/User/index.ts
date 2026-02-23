import type { RouteRecordRaw } from 'vue-router';

const userRoutes: RouteRecordRaw[] = [
  {
    path: '/user',
    name: 'AccountSettingsLayout',
    component: () => import('../../components/User/AccountSettingsLayout.vue'),
    children: [
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../../components/User/Profile.vue')
      },
      {
        path: 'settings',
        name: 'UserSettings',
        component: () => import('../../components/User/settings.vue')
      },
      {
        path: 'system-notification',
        name: 'SystemNotification',
        component: () => import('../../components/User/SystemNotification.vue')
      },
      {
        path: 'usage',
        name: 'UserUsage',
        component: () => import('../../components/User/Usage.vue')
      }
    ]
  }
];

export default userRoutes;
