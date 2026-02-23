import type { RouteRecordRaw } from 'vue-router';

const CommunityServiceRoutes: RouteRecordRaw[] = [
    {
        path: '/services',
        name: 'CommunityService',
        component: () => import('../../components/Home/CommunityService/CommunityService.vue')
    }
  ];
  
  export default CommunityServiceRoutes;
