<template>
  <div class="node-panel">
    <div class="panel-header">
      <h4>节点组件</h4>
    </div>
    <div class="panel-body">
      <div v-for="category in categories" :key="category" class="category">
        <div class="category-title">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
            <path d="M9 12h6M12 9v6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          {{ category }}
        </div>
        <div class="node-list">
          <div
            v-for="node in getNodesByCategory(category)"
            :key="node.code"
            class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node.code)"
            @click="$emit('add-node', node.code)"
          >
            <div class="node-icon" :style="{ background: node.color }">
              {{ getNodeIconText(node.code) }}
            </div>
            <span class="node-label">{{ node.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NODE_TYPE_LIST } from '../types'
import type { NodeTypeCode } from '../types'

const emit = defineEmits<{
  'add-node': [type: NodeTypeCode]
}>()

const categories = computed(() => {
  const cats = new Set(NODE_TYPE_LIST.map(n => n.category))
  return Array.from(cats)
})

function getNodesByCategory(category: string) {
  return NODE_TYPE_LIST.filter(n => n.category === category)
}

function handleDragStart(event: DragEvent, code: NodeTypeCode) {
  event.dataTransfer?.setData('nodeType', code)
}

function getNodeIconText(code: string) {
  const iconMap: Record<string, string> = {
    'START': '▶',
    'END': '■',
    'DATA_EXTRACT': '↑',
    'DATA_TRANSFORM': '⟳',
    'DATA_LOAD': '↓',
    'CONDITION': '◇',
    'PARALLEL': '⇅',
    'MERGE': '⊕',
    'DELAY': '⏱',
    'SCRIPT': '</>',
    'VALIDATION': '✓',
    'NOTIFICATION': '🔔'
  }
  return iconMap[code] || '●'
}
</script>

<style scoped>
.node-panel {
  width: 220px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f1f5f9;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.panel-header h4 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-body {
  padding: 16px;
  overflow-y: auto;
  flex: 1;
}

.category { margin-bottom: 20px; }

.category:last-child { margin-bottom: 0; }

.category-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 10px;
  padding-left: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.category-title svg { color: #667eea; }

.node-list { display: flex; flex-direction: column; gap: 6px; }

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 13px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid transparent;
}

.node-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-color: rgba(102, 126, 234, 0.2);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.node-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.node-item:hover .node-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
}

.node-label { flex: 1; font-weight: 500; }

@media (max-width: 992px) {
  .node-panel { width: 200px; }
}

@media (max-width: 768px) {
  .node-panel { width: 180px; }
}
</style>
