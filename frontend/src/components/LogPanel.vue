<template>
  <div class="log-panel">
    <div v-for="(log, index) in logs" :key="index" class="log-item" :class="log.level?.toLowerCase()">
      <span class="log-time">{{ formatTime(log.logTime) }}</span>
      <el-tag :type="getLevelType(log.level)" size="small" class="log-level">
        {{ log.level }}
      </el-tag>
      <span class="log-node" v-if="log.nodeName">[{{ log.nodeName }}]</span>
      <span class="log-message">{{ log.message }}</span>
      <el-popover v-if="log.stackTrace" trigger="click" width="500">
        <template #reference>
          <el-button size="small" type="danger" text>堆栈</el-button>
        </template>
        <pre class="stack-trace">{{ log.stackTrace }}</pre>
      </el-popover>
    </div>
    <el-empty v-if="!logs.length" description="暂无日志" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import type { TaskLog } from '../types'

defineProps<{ logs: TaskLog[] }>()

function formatTime(time?: string): string {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
}

function getLevelType(level?: string): 'info' | 'success' | 'warning' | 'danger' {
  switch (level) {
    case 'ERROR': return 'danger'
    case 'WARN': return 'warning'
    case 'DEBUG': return 'info'
    default: return 'info'
  }
}
</script>

<style scoped>
.log-panel { font-size: 12px; font-family: 'Menlo', 'Monaco', 'Courier New', monospace; }
.log-item {
  display: flex; align-items: flex-start; gap: 6px; padding: 4px 8px;
  border-bottom: 1px solid #f5f5f5; line-height: 1.6;
}
.log-item.error { background: #fef0f0; }
.log-item.warn { background: #fdf6ec; }
.log-time { color: #909399; white-space: nowrap; flex-shrink: 0; }
.log-level { flex-shrink: 0; }
.log-node { color: #409EFF; white-space: nowrap; flex-shrink: 0; }
.log-message { flex: 1; word-break: break-all; }
.stack-trace { font-size: 11px; max-height: 300px; overflow: auto; white-space: pre-wrap; }
</style>
