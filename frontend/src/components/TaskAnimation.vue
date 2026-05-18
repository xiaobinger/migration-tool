<template>
  <div class="task-animation">
    <!-- 节点执行时间线 -->
    <div class="timeline">
      <div
        v-for="node in nodeExecutions"
        :key="node.nodeId"
        class="timeline-item"
        :class="getNodeClass(node)"
      >
        <div class="timeline-dot" :style="{ background: getNodeColor(node) }">
          <span class="dot-icon" v-if="node.status === 'RUNNING'">⟳</span>
          <span class="dot-icon" v-else-if="node.status === 'SUCCESS'">✓</span>
          <span class="dot-icon" v-else-if="node.status === 'FAILED'">✗</span>
        </div>
        <div class="timeline-content">
          <div class="node-header">
            <span class="node-name">{{ node.nodeName || node.nodeId }}</span>
            <el-tag :type="getStatusType(node.status)" size="small">{{ getStatusLabel(node.status) }}</el-tag>
          </div>
          <div class="node-meta">
            <span v-if="node.duration">{{ node.duration }}ms</span>
            <span v-if="node.retryCount > 0">重试 {{ node.retryCount }} 次</span>
          </div>
          <div class="node-error" v-if="node.errorMessage">
            {{ node.errorMessage }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NodeExecution, TaskStatusCode } from '../types'

defineProps<{ nodeExecutions: NodeExecution[] }>()

function getNodeClass(node: NodeExecution) {
  return {
    running: node.status === 'RUNNING',
    success: node.status === 'SUCCESS',
    failed: node.status === 'FAILED',
  }
}

function getNodeColor(node: NodeExecution) {
  switch (node.status) {
    case 'RUNNING': return '#409EFF'
    case 'SUCCESS': return '#67C23A'
    case 'FAILED': return '#F56C6C'
    default: return '#909399'
  }
}

function getStatusType(status?: TaskStatusCode) {
  switch (status) {
    case 'RUNNING': return 'warning'
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

function getStatusLabel(status?: TaskStatusCode) {
  const map: Record<string, string> = {
    PENDING: '待执行', RUNNING: '执行中', SUCCESS: '成功',
    FAILED: '失败', SKIPPED: '跳过',
  }
  return map[status || ''] || status || ''
}
</script>

<style scoped>
.timeline { padding-left: 20px; }
.timeline-item {
  display: flex; gap: 12px; padding: 8px 0;
  border-left: 2px solid #e4e7ed; margin-left: 8px; padding-left: 16px;
  position: relative;
}
.timeline-item.running { border-left-color: #409EFF; }
.timeline-item.success { border-left-color: #67C23A; }
.timeline-item.failed { border-left-color: #F56C6C; }

.timeline-dot {
  position: absolute; left: -9px; top: 12px;
  width: 16px; height: 16px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 10px;
}
.timeline-item.running .timeline-dot { animation: pulse 1.5s infinite; }

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.timeline-content { flex: 1; }
.node-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.node-name { font-weight: 600; font-size: 13px; }
.node-meta { display: flex; gap: 12px; font-size: 11px; color: #909399; }
.node-error { color: #F56C6C; font-size: 12px; margin-top: 4px; padding: 4px 8px; background: #fef0f0; border-radius: 4px; }
</style>
