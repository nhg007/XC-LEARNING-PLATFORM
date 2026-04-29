<template>
  <section class="pure-layout" :class="{ collapsed }">
    <aside class="pure-sidebar">
      <div class="pure-brand">
        <div class="pure-logo">XC</div>
        <span v-show="!collapsed">{{ t('app.shortTitle') }}</span>
      </div>
      <el-scrollbar class="pure-menu-scroll">
        <el-menu
          :collapse="collapsed"
          :default-active="activeMenu"
          router
          class="pure-menu"
        >
          <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
            <el-icon>
              <component :is="resolveIcon(item.meta?.icon)" />
            </el-icon>
            <template #title>{{ t(item.meta?.title || '') }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </aside>

    <section class="pure-main">
      <header class="pure-navbar">
        <div class="navbar-left">
          <el-button
            text
            class="collapse-button"
            :title="collapsed ? t('common.openMenu') : t('common.closeMenu')"
            @click="collapsed = !collapsed"
          >
            <el-icon>
              <component :is="collapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ t('menus.dashboard') }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ t(String(route.meta.title || '')) }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="navbar-actions">
          <el-dropdown trigger="click" @command="changeLocale">
            <button class="language-button" :title="t('common.language')">
              <el-icon><Switch /></el-icon>
              <span>{{ currentLocaleLabel }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-CN">简体中文</el-dropdown-item>
                <el-dropdown-item command="en">English</el-dropdown-item>
                <el-dropdown-item command="ru">Русский</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="profile-button">
              <span class="avatar">{{ profileName.slice(0, 1).toUpperCase() }}</span>
              <span>{{ profileName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">{{ t('common.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <nav class="pure-page-tabs">
        <div class="page-tabs-scroll">
          <button
            v-for="tab in openedTabs"
            :key="tab.path"
            class="page-tab"
            :class="{ active: tab.path === activeMenu }"
            @click="openMenuTab(tab.path)"
          >
            <span>{{ t(tab.title) }}</span>
            <el-icon v-if="tab.closable" class="tab-close" @click.stop="closeTab(tab.path)">
              <Close />
            </el-icon>
          </button>
        </div>
        <el-dropdown trigger="click" @command="handleTabCommand">
          <button class="tab-actions-button" :title="t('common.tabActions')">
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="refresh">{{ t('common.refreshCurrent') }}</el-dropdown-item>
              <el-dropdown-item command="close-current" :disabled="activeMenu === homePath">{{ t('common.closeCurrent') }}</el-dropdown-item>
              <el-dropdown-item command="close-left" :disabled="!hasCloseLeft">{{ t('common.closeLeft') }}</el-dropdown-item>
              <el-dropdown-item command="close-right" :disabled="!hasCloseRight">{{ t('common.closeRight') }}</el-dropdown-item>
              <el-dropdown-item command="close-others" :disabled="!hasCloseOthers">{{ t('common.closeOthers') }}</el-dropdown-item>
              <el-dropdown-item command="close-all" :disabled="openedTabs.length <= 1">{{ t('common.closeAll') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <main class="pure-content">
        <RouterView v-slot="{ Component }">
          <component :is="Component" :key="viewKey" />
        </RouterView>
      </main>
    </section>
  </section>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { RouteRecordRaw } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Close,
  Collection,
  DataAnalysis,
  Expand,
  Fold,
  House,
  Menu,
  School,
  Setting,
  Switch,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { setLocale, type AdminLocale } from '@/plugins/i18n'
import { constantMenus } from '@/router'
import { hasRouteAccess } from '@/router/permission'
import { useSessionStore } from '@/stores/session'

const icons: Record<string, Component> = {
  Collection,
  DataAnalysis,
  House,
  Menu,
  School,
  Setting,
  User,
  Wallet
}

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const { locale, t } = useI18n()
const collapsed = ref(false)
const homePath = '/dashboard'
const openedTabs = ref<PageTab[]>([])
const refreshKey = ref(0)

const activeMenu = computed(() => route.path)
const activeTabIndex = computed(() => openedTabs.value.findIndex(tab => tab.path === route.path))
const hasCloseLeft = computed(() => activeTabIndex.value >= 0 && openedTabs.value.some((tab, index) => tab.closable && index < activeTabIndex.value))
const hasCloseRight = computed(() => activeTabIndex.value >= 0 && openedTabs.value.some((tab, index) => tab.closable && index > activeTabIndex.value))
const hasCloseOthers = computed(() => openedTabs.value.some(tab => tab.closable && tab.path !== route.path))
const profileName = computed(() => session.profile?.displayName || session.profile?.username || t('common.profile'))
const currentLocaleLabel = computed(() => {
  if (locale.value === 'en') {
    return 'English'
  }
  if (locale.value === 'ru') {
    return 'Русский'
  }
  return '简体中文'
})
const visibleMenus = computed(() => constantMenus.filter(item => hasRouteAccess(item.meta || {}, session.profile)))
const viewKey = computed(() => `${route.fullPath}:${refreshKey.value}`)

interface PageTab {
  path: string
  title: string
  closable: boolean
}

function resolveIcon(name?: string) {
  return name ? icons[name] || Menu : Menu
}

function routeToTab(item: RouteRecordRaw): PageTab {
  return {
    path: item.path,
    title: String(item.meta?.title || ''),
    closable: item.path !== homePath
  }
}

function findMenu(path: string) {
  return visibleMenus.value.find(item => item.path === path)
}

function ensureTab(path: string) {
  const item = findMenu(path)
  if (!item || openedTabs.value.some(tab => tab.path === path)) {
    return
  }
  openedTabs.value.push(routeToTab(item))
}

function ensureHomeTab() {
  const home = findMenu(homePath) || visibleMenus.value[0]
  if (!home) {
    return
  }
  if (!openedTabs.value.some(tab => tab.path === home.path)) {
    openedTabs.value.unshift(routeToTab(home))
  }
}

function changeLocale(command: string) {
  if (command === 'zh-CN' || command === 'en' || command === 'ru') {
    setLocale(command as AdminLocale)
  }
}

async function openMenuTab(name: string) {
  const path = String(name)
  if (path !== route.path) {
    await router.push(path)
  }
}

async function closeTab(path: string) {
  if (path === homePath) {
    return
  }
  const index = openedTabs.value.findIndex(tab => tab.path === path)
  openedTabs.value = openedTabs.value.filter(tab => tab.path !== path)
  if (path === route.path) {
    const nextTab = openedTabs.value[index - 1] || openedTabs.value[index] || openedTabs.value[0]
    await router.push(nextTab?.path || homePath)
  }
}

async function handleTabCommand(command: string) {
  if (command === 'refresh') {
    refreshKey.value += 1
    return
  }
  if (command === 'close-current') {
    await closeTab(route.path)
    return
  }
  if (command === 'close-left') {
    openedTabs.value = openedTabs.value.filter((tab, index) => tab.path === homePath || index >= activeTabIndex.value)
    ensureHomeTab()
    return
  }
  if (command === 'close-right') {
    openedTabs.value = openedTabs.value.filter((tab, index) => tab.path === homePath || index <= activeTabIndex.value)
    ensureHomeTab()
    return
  }
  if (command === 'close-others') {
    openedTabs.value = openedTabs.value.filter(tab => tab.path === homePath || tab.path === route.path)
    ensureHomeTab()
    return
  }
  if (command === 'close-all') {
    openedTabs.value = []
    ensureHomeTab()
    await router.push(homePath)
  }
}

async function handleUserCommand(command: string) {
  if (command !== 'logout') {
    return
  }
  session.logout()
  await router.push('/login')
}

watch(
  () => [route.path, visibleMenus.value.length],
  () => {
    ensureHomeTab()
    ensureTab(route.path)
  },
  { immediate: true }
)
</script>
