<template>
  <div class="app-container">
    <div class="gradient-bg"></div>
    <div class="gradient-overlay"></div>

    <el-container>
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M7 8L3 12L7 16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M17 8L21 12L17 16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14 4L10 20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="logo-text">
              <span class="logo-title">数据迁移工具</span>
              <span class="logo-subtitle">Migration Tool</span>
            </div>
          </div>

          <el-menu
            mode="horizontal"
            :default-active="currentRoute"
            router
            class="header-nav"
            :ellipsis="false"
          >
            <el-menu-item index="/flows" class="nav-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="nav-icon">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                <path d="M9 9h6M9 13h6M9 17h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span>流程设计</span>
            </el-menu-item>
            <el-menu-item index="/tasks" class="nav-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="nav-icon">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
                <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>任务监控</span>
            </el-menu-item>
            <el-menu-item index="/datasources" class="nav-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="nav-icon">
                <ellipse cx="12" cy="6" rx="8" ry="3" stroke="currentColor" stroke-width="2"/>
                <path d="M4 6v6c0 1.657 3.582 3 8 3s8-1.343 8-3V6" stroke="currentColor" stroke-width="2"/>
                <path d="M4 12v6c0 1.657 3.582 3 8 3s8-1.343 8-3v-6" stroke="currentColor" stroke-width="2"/>
              </svg>
              <span>数据源</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentRoute = computed(() => {
  if (route.path.startsWith('/flows')) return '/flows'
  if (route.path.startsWith('/tasks')) return '/tasks'
  if (route.path.startsWith('/datasources')) return '/datasources'
  return '/'
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

:root {
  --primary-color: #667eea;
  --primary-dark: #5568d3;
  --secondary-color: #764ba2;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --bg-dark: #0f172a;
  --bg-light: #f8fafc;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-light: #94a3b8;
  --glass-bg: rgba(255, 255, 255, 0.85);
  --glass-border: rgba(255, 255, 255, 0.3);
  --shadow-soft: 0 4px 20px rgba(0, 0, 0, 0.08);
  --shadow-medium: 0 8px 32px rgba(0, 0, 0, 0.12);
  --shadow-glow: 0 0 40px rgba(102, 126, 234, 0.3);
  --border-radius: 16px;
  --transition-speed: 0.3s;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-dark);
  color: var(--text-primary);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.gradient-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #312e81 100%);
  z-index: -2;
}

.gradient-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 20% 50%, rgba(102, 126, 234, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(118, 75, 162, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 50% 100%, rgba(102, 126, 234, 0.1) 0%, transparent 50%);
  z-index: -1;
  animation: gradientShift 20s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% {
    background-position: 0% 50%;
    opacity: 1;
  }
  50% {
    background-position: 100% 50%;
    opacity: 0.8;
  }
}

.app-header {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--glass-border);
  padding: 0;
  box-shadow: var(--shadow-soft);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all var(--transition-speed) ease;
  padding: 8px 12px;
  border-radius: 12px;
}

.logo:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(4px);
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  animation: logoGlow 3s ease-in-out infinite;
}

@keyframes logoGlow {
  0%, 100% {
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  }
  50% {
    box-shadow: 0 6px 30px rgba(102, 126, 234, 0.6);
  }
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.logo-subtitle {
  font-size: 11px;
  color: var(--text-light);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  font-weight: 500;
}

.header-nav {
  border-bottom: none;
  background: transparent;
}

.header-nav .el-menu-item {
  height: 52px;
  line-height: 52px;
  border-radius: 12px;
  margin: 0 6px;
  padding: 0 20px;
  transition: all var(--transition-speed) ease;
  font-weight: 500;
  font-size: 15px;
  color: var(--text-secondary);
  border: none;
  background: transparent;
}

.header-nav .el-menu-item:hover {
  background: rgba(102, 126, 234, 0.08);
  color: var(--primary-color);
}

.header-nav .el-menu-item.is-active {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-icon {
  transition: transform var(--transition-speed) ease;
}

.header-nav .el-menu-item:hover .nav-icon {
  transform: scale(1.1);
}

.app-main {
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  min-height: calc(100vh - 72px);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: all var(--transition-speed) ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .logo-text {
    display: none;
  }

  .app-main {
    padding: 16px;
  }

  .header-nav .el-menu-item span:not(.nav-icon) {
    display: none;
  }

  .header-nav .el-menu-item {
    padding: 0 12px;
  }
}
</style>
