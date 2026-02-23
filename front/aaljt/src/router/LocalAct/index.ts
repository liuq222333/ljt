import type { RouteRecordRaw } from 'vue-router';

const localActRoutes: RouteRecordRaw[] = [
  {
    path: '/local-act',
    name: 'LocalAct',
    component: () => import('../../components/Home/LocalAct/LocalAct.vue')
  },
  {
    path: '/local-act/publish',
    name: 'LocalActPublish',
    component: () => import('../../components/Home/LocalAct/PublishActivity.vue')
  },
  {
    path: '/local-act/recurring',
    name: 'LocalActRecurring',
    component: () => import('../../components/Home/LocalAct/ScheduleTemplates.vue')
  },
  {
    path: '/local-act/notifications',
    name: 'LocalActNotifications',
    component: () => import('../../components/Home/LocalAct/NotificationsCenter.vue')
  },
  {
    path: '/local-act/neighbor-support',
    name: 'LocalActNeighborSupport',
    component: () => import('../../components/Home/LocalAct/NeighborSupport.vue')
  },
  {
    path: '/local-act/map-view',
    name: 'LocalActMapView',
    component: () => import('../../components/Home/LocalAct/MapView.vue')
  },
  {
    path: '/local-act/:id',
    name: 'LocalActDetail',
    component: () => import('../../components/Home/LocalAct/ActivityDetail.vue')
  },
  {
    path: '/local-act/my-enrollments',
    name: 'LocalActEnrollments',
    component: () => import('../../components/Home/LocalAct/MyEnrollments.vue')
  },
  {
    path: '/local-act/stories',
    name: 'LocalActStories',
    component: () => import('../../components/Home/LocalAct/StoriesArchive.vue')
  },
  {
    path: '/local-act/stories/:id',
    name: 'LocalActStoryDetail',
    component: () => import('../../components/Home/LocalAct/StoriesArchiveDetail.vue')
  }
];

export default localActRoutes;
