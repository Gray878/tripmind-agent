import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - TripMind 智能旅游规划平台',
      description: 'TripMind 智能旅游规划平台，基于多智能体架构的AI旅游规划助手'
    }
  }
  // TODO: 后续添加旅游规划页面路由
  // {
  //   path: '/trip-planner',
  //   name: 'TripPlanner',
  //   component: () => import('../views/TripPlanner.vue'),
  //   meta: {
  //     title: '旅游规划 - TripMind 智能旅游规划平台',
  //     description: 'TripMind 智能旅游规划，为您定制专属旅行方案'
  //   }
  // }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
