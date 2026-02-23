import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import communityMarketplaceRoutes from './CommunityMarketplace'
import localActRoutes from './LocalAct'
import CommunityFeedRoutes from './CommunityFeed'
import userRoutes from './User'
import CommunityServiceRoutes from './CommunityService'
const routes : RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../components/LoginPages/login.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../components/Home/home.vue')
  },
  {
    path: '/publicServices',
    name: 'PublicServices',
    component: () => import('../components/PublicServices/publicServices.vue')
  },
  {
    path: '/business',
    name: 'Business',
    component: () => import('../components/Business/business.vue')
  },
  {
    path:'/notification',
    name: 'Notification',
    component:() =>import('../components/Notification/Notification.vue')
  },
  ...communityMarketplaceRoutes,
  ...localActRoutes,
  ...CommunityFeedRoutes,
  ...userRoutes,
  ...CommunityServiceRoutes,
] 

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
