export default {
  path: "/audit",
  redirect: "/audit/index",
  meta: {
    icon: "ri:shield-check-line",
    title: "审核管理",
    rank: 4
  },
  children: [
    {
      path: "/audit/index",
      name: "AuditManagement",
      component: () => import("@/views/audit/index.vue"),
      meta: {
        title: "审核管理"
      }
    }
  ]
} satisfies RouteConfigsTable;

