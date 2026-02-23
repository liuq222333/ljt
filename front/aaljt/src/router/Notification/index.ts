import type { RouteRecordRaw } from 'vue-router';
const NotificationRoutes: RouteRecordRaw[] = [
    {
        path: '/notification',
        name: 'Notification',
        component: () => import('../Notifition/Notification.vue')
    }
  ];
  
  export default NotificationRoutes;
  