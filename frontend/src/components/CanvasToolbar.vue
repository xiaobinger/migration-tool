<template>
  <div class="canvas-toolbar">
    <div class="style-group">
      <button
        class="style-btn"
        :class="{ active: edgeStyle === 'straight' }"
        @click="$emit('update:edgeStyle', 'straight')"
        title="直线连接"
      >
        <svg width="16" height="16" viewBox="0 0 16 16">
          <line x1="2" y1="8" x2="14" y2="8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
      <button
        class="style-btn"
        :class="{ active: edgeStyle === 'orthogonal' }"
        @click="$emit('update:edgeStyle', 'orthogonal')"
        title="折线连接"
      >
        <svg width="16" height="16" viewBox="0 0 16 16">
          <path d="M 2 8 L 8 8 L 8 14 L 14 14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>
        </svg>
      </button>
      <button
        class="style-btn"
        :class="{ active: edgeStyle === 'curved' }"
        @click="$emit('update:edgeStyle', 'curved')"
        title="曲线连接"
      >
        <svg width="16" height="16" viewBox="0 0 16 16">
          <path d="M 2 8 C 2 4, 14 12, 14 8" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <div class="toolbar-divider"></div>

    <button
      class="tool-btn"
      :class="{ active: showAnimation }"
      @click="$emit('update:showAnimation', !showAnimation)"
      :title="showAnimation ? '关闭动画' : '开启动画'"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" v-if="showAnimation">
        <polygon points="5,3 19,12 5,21" fill="currentColor"/>
      </svg>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" v-else>
        <rect x="6" y="4" width="4" height="16" fill="currentColor"/>
        <rect x="14" y="4" width="4" height="16" fill="currentColor"/>
      </svg>
    </button>

    <div class="toolbar-divider"></div>

    <div class="zoom-group">
      <button class="zoom-btn" @click="$emit('zoom-out')" title="缩小">
        <svg width="14" height="14" viewBox="0 0 16 16">
          <line x1="4" y1="8" x2="12" y2="8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
      <span class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</span>
      <button class="zoom-btn" @click="$emit('zoom-in')" title="放大">
        <svg width="14" height="14" viewBox="0 0 16 16">
          <line x1="4" y1="8" x2="12" y2="8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <line x1="8" y1="4" x2="8" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
      <button class="zoom-btn fit-btn" @click="$emit('fit-to-view')" title="适应大小">
        <svg width="14" height="14" viewBox="0 0 16 16">
          <path d="M2 5V2h3M11 2h3v3M14 11v3h-3M5 14H2v-3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <button class="zoom-btn reset-btn" @click="$emit('reset-zoom')" title="重置(100%)">
        <svg width="14" height="14" viewBox="0 0 16 16">
          <path d="M3 8a5 5 0 1 1 1 3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
          <path d="M3 11V8h3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  edgeStyle: 'straight' | 'orthogonal' | 'curved'
  showAnimation: boolean
  zoomLevel: number
}>()

defineEmits<{
  'update:edgeStyle': [style: 'straight' | 'orthogonal' | 'curved']
  'update:showAnimation': [show: boolean]
  'zoom-in': []
  'zoom-out': []
  'fit-to-view': []
  'reset-zoom': []
}>()
</script>

<style scoped>
.canvas-toolbar {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.style-group {
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 4px;
  gap: 2px;
}

.style-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.style-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.style-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
}

.tool-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.tool-btn:hover {
  background: #667eea;
  color: white;
}

.tool-btn.active {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.zoom-group {
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 4px;
  gap: 2px;
}

.zoom-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.zoom-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.zoom-level {
  min-width: 44px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  user-select: none;
}

.fit-btn:hover {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.reset-btn:hover {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

@media (max-width: 768px) {
  .canvas-toolbar {
    top: 8px;
    right: 8px;
    padding: 8px 12px;
    gap: 8px;
  }
}
</style>
