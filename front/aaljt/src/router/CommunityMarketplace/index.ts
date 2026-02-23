import type { RouteRecordRaw } from 'vue-router';

const communityMarketplaceRoutes: RouteRecordRaw[] = [
  {
    path: '/community-marketplace',
    name: 'CommunityMarketplace',
    component: () => import('../../components/Home/CommunityMarketplace/CommunityMarketplace.vue')
  },
  {
    path: '/community-marketplace-find',
    name: 'CommunityMarketplaceFind',
    component: () => import('../../components/Home/CommunityMarketplace/CommunityMarketplaceFind.vue')
  },
  {
    path: '/addProduct',
    name: 'AddProduct',
    component: () => import('../../components/Home/CommunityMarketplace/addProduct.vue')
  },
  {
    path: '/add-new-product',
    name: 'AddNewProduct',
    component: () => import('../../components/Home/CommunityMarketplace/addNewProduct.vue')
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../../components/Home/CommunityMarketplace/ProductDetail.vue'),
    props: true
  },
  {
    path: '/shop-car',
    name: 'ShopCar',
    component: () => import('../../components/Home/CommunityMarketplace/ShopCar.vue')
  },
  {
    path: '/my-products',
    name: 'MyProducts',
    component: () => import('../../components/Home/CommunityMarketplace/MyProducts.vue')
  },
  {
    path: '/my-orders',
    name: 'MyOrder',
    component: () => import('../../components/Home/CommunityMarketplace/MyOrder.vue')
  }
];

export default communityMarketplaceRoutes;
