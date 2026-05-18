import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/flows',
    },
    {
      path: '/flows',
      name: 'FlowList',
      component: () => import('../views/FlowList.vue'),
    },
    {
      path: '/flows/designer/:id?',
      name: 'FlowDesigner',
      component: () => import('../views/FlowDesigner.vue'),
    },
    {
      path: '/tasks',
      name: 'TaskMonitor',
      component: () => import('../views/TaskMonitor.vue'),
    },
    {
      path: '/tasks/:id',
      name: 'TaskDetail',
      component: () => import('../views/TaskDetail.vue'),
    },
    {
      path: '/datasources',
      name: 'DataSourceList',
      component: () => import('../views/DataSourceList.vue'),
    },
  ],
})

export default router
