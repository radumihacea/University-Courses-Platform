import { createRouter, createWebHistory } from 'vue-router'

import { useAuth } from '@/composables/useAuth'
import AppLayout from '@/layouts/AppLayout.vue'

/**
 * Paginile panoului de admin. "meta.title" apare in bara de sus,
 * iar "meta.icon" / "meta.group" sunt folosite pentru meniul lateral.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: 'Autentificare', public: true },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: '/acasa' },
        {
          path: 'acasa',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: 'Acasă', icon: 'mdi-home-outline', group: 'General' },
        },
        {
          path: 'orar',
          name: 'timetable',
          component: () => import('@/views/TimetableView.vue'),
          meta: { title: 'Orar', icon: 'mdi-calendar-month-outline', group: 'Orar' },
        },
        {
          path: 'cereri',
          name: 'change-requests',
          component: () => import('@/views/ChangeRequestsView.vue'),
          meta: { title: 'Cereri de modificare', icon: 'mdi-swap-horizontal', group: 'Orar' },
        },
        {
          path: 'statistici',
          name: 'statistics',
          component: () => import('@/views/StatisticsView.vue'),
          meta: { title: 'Statistici', icon: 'mdi-chart-box-outline', group: 'Orar' },
        },
        {
          path: 'asignari',
          name: 'assignments',
          component: () => import('@/views/AssignmentsView.vue'),
          meta: { title: 'Asignări didactice', icon: 'mdi-clipboard-list-outline', group: 'Planificare' },
        },
        {
          path: 'restrictii',
          name: 'constraints',
          component: () => import('@/views/ConstraintsView.vue'),
          meta: { title: 'Restricții profesori', icon: 'mdi-clock-alert-outline', group: 'Planificare' },
        },
        {
          path: 'discipline',
          name: 'disciplines',
          component: () => import('@/views/DisciplinesView.vue'),
          meta: { title: 'Discipline', icon: 'mdi-book-open-variant-outline', group: 'Planificare' },
        },
        {
          path: 'profesori',
          name: 'professors',
          component: () => import('@/views/ProfessorsView.vue'),
          meta: { title: 'Profesori', icon: 'mdi-account-tie-outline', group: 'Oameni' },
        },
        {
          path: 'studenti',
          name: 'students',
          component: () => import('@/views/StudentsView.vue'),
          meta: { title: 'Studenți', icon: 'mdi-school-outline', group: 'Oameni' },
        },
        {
          path: 'serii',
          name: 'series',
          component: () => import('@/views/SeriesView.vue'),
          meta: { title: 'Serii', icon: 'mdi-layers-outline', group: 'Structură' },
        },
        {
          path: 'grupe',
          name: 'groups',
          component: () => import('@/views/GroupsView.vue'),
          meta: { title: 'Grupe', icon: 'mdi-account-group-outline', group: 'Structură' },
        },
        {
          path: 'semigrupe',
          name: 'subgroups',
          component: () => import('@/views/SubgroupsView.vue'),
          meta: { title: 'Semigrupe', icon: 'mdi-account-multiple-outline', group: 'Structură' },
        },
        {
          path: 'cladiri',
          name: 'buildings',
          component: () => import('@/views/BuildingsView.vue'),
          meta: { title: 'Clădiri', icon: 'mdi-office-building-outline', group: 'Spații' },
        },
        {
          path: 'sali',
          name: 'rooms',
          component: () => import('@/views/RoomsView.vue'),
          meta: { title: 'Săli', icon: 'mdi-door', group: 'Spații' },
        },
        {
          path: 'notificari',
          name: 'notifications',
          component: () => import('@/views/NotificationsView.vue'),
          meta: { title: 'Notificări', icon: 'mdi-bell-outline', group: 'Comunicare' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/acasa' },
  ],
})

/** Garda de autentificare: fara sesiune de admin, orice pagina duce la login. */
router.beforeEach((to) => {
  const auth = useAuth()
  auth.refresh()
  if (to.meta.public) {
    // deja autentificat -> nu are rost sa vada din nou pagina de login
    return auth.isAuthenticated.value ? { path: '/acasa' } : true
  }
  if (!auth.isAuthenticated.value) {
    return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'Admin'
  document.title = `${title} · UCP Admin`
})

export default router
