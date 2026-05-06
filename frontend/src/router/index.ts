import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "boot",
      component: () => import("../pages/BootPage.vue"),
    },
    {
      path: "/game/:sessionId",
      name: "game",
      component: () => import("../pages/GamePage.vue"),
    },
    {
      path: "/replay/:sessionId",
      name: "replay",
      component: () => import("../pages/ReplayPage.vue"),
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("../pages/AdminPage.vue"),
    },
    {
      path: "/world-factory",
      name: "world-factory",
      component: () => import("../pages/WorldFactoryPage.vue"),
    },
  ],
});

export default router;
