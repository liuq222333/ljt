import type { RouteRecordRaw } from 'vue-router';
const CommunityFeedRoutes: RouteRecordRaw[] = [
    {
        path: '/community-feed',
        name: 'CommunityFeed',
        component: () => import('../../components/Home/CommunityFeed/CommunityFeed.vue')
    },
    {
        path: '/community-feed/publish',
        name: 'PublishPost',
        component: () => import('../../components/Home/CommunityFeed/publishPost.vue')
    }
  ];
  
  export default CommunityFeedRoutes;
  