<template>
  <div class="task-detail" v-if="taskStore.currentTask">
    <div class="detail-header">
      <el-button @click="$router.push('/tasks')">← 返回</el-button>
      <h2>{{ taskStore.currentTask.name }}</h2>
      <el-tag :color="getTaskStatusColor(taskStore.currentTask.status)" effect="dark">
        {{ getTaskStatusLabel(taskStore.currentTask.status) }}
      </el-tag>
      <div class="header-actions">
        <el-dropdown v-if="taskStore.currentTask.status === 'FAILED' || taskStore.currentTask.status === 'PAUSED' || taskStore.currentTask.status === 'CANCELLED'" @command="handleRestartCommand">
          <el-button type="warning">
            重新执行
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="from-start">从头开始执行</el-dropdown-item>
              <el-dropdown-item command="from-breakpoint" :disabled="!taskStore.currentTask.restartFromNodeId">
                从断点处继续执行
                <span v-if="taskStore.currentTask.restartFromNodeId" style="color: #909399; font-size: 12px; margin-left: 4px;">
                  ({{ getFailedNodeName() }})
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-if="taskStore.currentTask.status === 'RUNNING'" @click="handlePause">暂停</el-button>
        <el-button type="danger" v-if="taskStore.currentTask.status === 'RUNNING'" @click="handleCancel">取消</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="animation-card">
          <template #header>
            <div class="card-title">
              <span>执行流程</span>
              <el-tag v-if="taskStore.activeNodeId" type="warning" size="small">
                当前: {{ taskStore.activeNodeId }}
              </el-tag>
            </div>
          </template>
          <div class="canvas-container">
            <CanvasToolbar
                v-model:edge-style="edgeStyle"
                v-model:show-animation="showAnimation"
                :zoom-level="canvasZoomLevel"
                @zoom-in="flowCanvasRef?.zoomIn()"
                @zoom-out="flowCanvasRef?.zoomOut()"
                @fit-to-view="flowCanvasRef?.fitToView()"
                @reset-zoom="flowCanvasRef?.resetZoom()"
            />
            <FlowCanvas
              v-if="flowDefinition"
              ref="flowCanvasRef"
              :flow="flowDefinition"
              :node-status-map="mergedNodeStatusMap"
              :active-node-id="taskStore.activeNodeId"
              :show-animation="showAnimation"
              :executed-edge-ids="executedEdgeIds"
              @node-click="handleNodeClick"
              @node-move="() => {}"
              @connect="() => {}"
              @delete-node="() => {}"
              @delete-edge="() => {}"
            />
          </div>
        </el-card>

        <el-card class="stats-card">
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="stat-item extract">
                <div class="stat-value">{{ taskStore.currentTask.extractedRecords ?? 0 }}</div>
                <div class="stat-label">提取记录数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item load">
                <div class="stat-value">{{ taskStore.currentTask.loadedRecords ?? 0 }}</div>
                <div class="stat-label">加载记录数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item success">
                <div class="stat-value">{{ taskStore.currentTask.loadedSuccessRecords ?? 0 }}</div>
                <div class="stat-label">加载成功</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item failed">
                <div class="stat-value">{{ taskStore.currentTask.loadedFailedRecords ?? 0 }}</div>
                <div class="stat-label">加载失败</div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 12px">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">{{ taskStore.currentTask.successRecords ?? 0 }}</div>
                <div class="stat-label">总成功</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">{{ taskStore.currentTask.failedRecords ?? 0 }}</div>
                <div class="stat-label">总失败</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value">{{ taskStore.currentTask.completedNodes }}/{{ taskStore.currentTask.totalNodes }}</div>
                <div class="stat-label">节点进度</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card class="executions-card">
          <template #header>
            <div class="card-title">
              <span>节点执行记录</span>
              <el-button size="small" text @click="loadExecutions">刷新</el-button>
            </div>
          </template>
          <div class="executions-list" v-if="nodeExecutions.length">
            <div
              v-for="exec in nodeExecutions"
              :key="exec.id"
              class="execution-item"
              :class="exec.status.toLowerCase()"
              @click="handleExecutionClick(exec)"
            >
              <div class="exec-left">
                <div class="exec-status-dot" :class="exec.status.toLowerCase()"></div>
                <div class="exec-info">
                  <div class="exec-name">{{ exec.nodeName || exec.nodeId }}</div>
                  <div class="exec-type">{{ exec.nodeType }}</div>
                </div>
              </div>
              <div class="exec-center">
                <div class="exec-summary" v-if="exec.outputSummary">{{ exec.outputSummary }}</div>
                <div class="exec-error" v-if="exec.errorMessage">{{ exec.errorMessage }}</div>
              </div>
              <div class="exec-right">
                <div class="exec-duration" v-if="exec.duration > 0">
                  {{ formatDuration(exec.duration) }}
                </div>
                <div class="exec-time" v-if="exec.startedAt">
                  {{ formatTime(exec.startedAt) }}
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无执行记录" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="log-card">
          <template #header>
            <div class="card-title">
              <span>执行日志</span>
              <el-button size="small" text @click="loadLogs">刷新</el-button>
            </div>
          </template>
          <LogPanel :logs="logs" />
        </el-card>

        <el-card class="error-card" v-if="taskStore.currentTask.status === 'FAILED'">
          <template #header>
            <div class="card-title">
              <span>错误分析</span>
              <el-button size="small" text @click="loadErrorAnalysis">分析</el-button>
            </div>
          </template>
          <ErrorAnalysis :analysis="errorAnalysis" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useTaskStore } from '../stores/task'
import { useFlowStore } from '../stores/flow'
import { logApi } from '../api/log'
import { getTaskStatusLabel, getTaskStatusColor } from '../types'
import type { TaskLog, NodeExecution, ErrorAnalysis as ErrorAnalysisType, TaskStatusCode } from '../types'
import type { FlowDefinition } from '../stores/flow'
import FlowCanvas from '../components/FlowCanvas.vue'
import CanvasToolbar from '../components/CanvasToolbar.vue'
import LogPanel from '../components/LogPanel.vue'
import ErrorAnalysis from '../components/ErrorAnalysis.vue'

const route = useRoute()
const taskStore = useTaskStore()
const flowStore = useFlowStore()

const flowDefinition = ref<FlowDefinition | null>(null)
const logs = ref<TaskLog[]>([])
const nodeExecutions = ref<NodeExecution[]>([])
const errorAnalysis = ref<ErrorAnalysisType | null>(null)
const edgeStyle = ref<'straight' | 'orthogonal' | 'curved'>('straight')
const showAnimation = ref(true)
const flowCanvasRef = ref<InstanceType<typeof FlowCanvas> | null>(null)
const localNodeStatusMap = reactive<Record<string, TaskStatusCode>>({})

const canvasZoomLevel = computed(() => {
  const z = (flowCanvasRef.value as any)?.zoomLevel
  return z != null ? (typeof z === 'object' && 'value' in z ? z.value : z) : 1
})

const mergedNodeStatusMap = computed(() => {
  const merged: Record<string, TaskStatusCode> = { ...localNodeStatusMap }
  for (const [k, v] of Object.entries(taskStore.nodeStatusMap)) {
    if (v === 'RUNNING') {
      merged[k] = v
    } else if (!merged[k] || merged[k] === 'PENDING') {
      merged[k] = v
    }
  }
  return merged
})

const executedEdgeIds = computed(() => {
  const ids = new Set<string>()
  for (const exec of nodeExecutions.value) {
    if (exec.incomingEdgeId) {
      ids.add(exec.incomingEdgeId)
    }
  }
  return ids
})

let logPolling: number | null = null

onMounted(async () => {
  const taskId = Number(route.params.id)
  await taskStore.loadTask(taskId)
  taskStore.initWebSocket()
  taskStore.resetProgress()

  if (taskStore.currentTask) {
    await flowStore.loadFlow(taskStore.currentTask.flowDefinitionId)
    flowDefinition.value = flowStore.currentFlow
  }

  await Promise.all([loadLogs(), loadExecutions()])

  logPolling = window.setInterval(() => {
    loadLogs()
    loadExecutions()
  }, 3000)
})

onUnmounted(() => {
  if (logPolling) clearInterval(logPolling)
})

async function loadLogs() {
  if (!taskStore.currentTask) return
  try {
    logs.value = await logApi.taskLogs(taskStore.currentTask.id)
  } catch (e) { /* ignore */ }
}

async function loadExecutions() {
  if (!taskStore.currentTask) return
  try {
    nodeExecutions.value = await logApi.nodeExecutions(taskStore.currentTask.id)
    rebuildNodeStatusMap()
  } catch (e) { /* ignore */ }
}

function rebuildNodeStatusMap() {
  Object.keys(localNodeStatusMap).forEach(k => delete localNodeStatusMap[k])
  if (!flowDefinition.value) return

  const executedNodeIds = new Set<string>()
  for (const exec of nodeExecutions.value) {
    localNodeStatusMap[exec.nodeId] = exec.status
    executedNodeIds.add(exec.nodeId)
  }

  const taskFinished = taskStore.currentTask?.status === 'SUCCESS' ||
                       taskStore.currentTask?.status === 'FAILED' ||
                       taskStore.currentTask?.status === 'CANCELLED'
  if (taskFinished) {
    for (const node of flowDefinition.value.nodes) {
      if (!executedNodeIds.has(node.nodeId)) {
        localNodeStatusMap[node.nodeId] = 'SKIPPED'
      }
    }
  }
}

async function loadErrorAnalysis() {
  if (!taskStore.currentTask) return
  try {
    errorAnalysis.value = await logApi.errorAnalysis(taskStore.currentTask.id)
  } catch (e) { /* ignore */ }
}

function handleNodeClick(nodeId: string) {
  const exec = nodeExecutions.value.find(e => e.nodeId === nodeId)
  if (exec) {
    handleExecutionClick(exec)
  }
}

function handleExecutionClick(exec: NodeExecution) {
  if (!taskStore.currentTask) return
  logApi.nodeLogs(taskStore.currentTask.id, exec.nodeId).then(nodeLogs => {
    logs.value = nodeLogs
  }).catch(() => {})
}

async function handleRestartCommand(command: string) {
  if (!taskStore.currentTask) return
  if (command === 'from-breakpoint' && taskStore.currentTask.restartFromNodeId) {
    await taskStore.restartTask(taskStore.currentTask.id, taskStore.currentTask.restartFromNodeId)
    ElMessage.success('从断点处继续执行')
  } else {
    await taskStore.restartTask(taskStore.currentTask.id)
    ElMessage.success('从头开始执行')
  }
  taskStore.resetProgress()
}

function getFailedNodeName(): string {
  if (!taskStore.currentTask?.restartFromNodeId) return ''
  const exec = nodeExecutions.value.find(e => e.nodeId === taskStore.currentTask!.restartFromNodeId)
  return exec ? exec.nodeName : taskStore.currentTask.restartFromNodeId
}

async function handlePause() {
  if (!taskStore.currentTask) return
  await taskStore.pauseTask(taskStore.currentTask.id)
}

async function handleCancel() {
  if (!taskStore.currentTask) return
  await taskStore.cancelTask(taskStore.currentTask.id)
}

function formatTime(time?: string): string {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
}

function formatDuration(ms: number): string {
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}
</script>

<style scoped>
.task-detail { max-width: 1400px; margin: 0 auto; }
.detail-header {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px; padding: 12px 16px; background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.detail-header h2 { font-size: 18px; flex: 1; }
.header-actions { display: flex; gap: 8px; }
.animation-card { margin-bottom: 16px; }
.animation-card :deep(.el-card__body) {
  height: 500px;
  padding: 0;
  position: relative;
  overflow: hidden;
}
.canvas-container {
  width: 100%;
  height: 100%;
  position: relative;
}
.card-title { display: flex; justify-content: space-between; align-items: center; }
.stats-card { margin-bottom: 16px; }
.stat-item { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }
.stat-item.extract .stat-value { color: #409EFF; }
.stat-item.load .stat-value { color: #E6A23C; }
.stat-item.success .stat-value { color: #67C23A; }
.stat-item.failed .stat-value { color: #F56C6C; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }

.executions-card { margin-bottom: 16px; }
.executions-card :deep(.el-card__body) { max-height: 400px; overflow-y: auto; }
.executions-list { display: flex; flex-direction: column; gap: 8px; }
.execution-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s ease;
}
.execution-item:hover {
  background: #f5f7fa;
  border-color: #409EFF;
}
.execution-item.success { border-left: 3px solid #67C23A; }
.execution-item.failed { border-left: 3px solid #F56C6C; background: #fef0f0; }
.execution-item.running { border-left: 3px solid #409EFF; }
.execution-item.pending { border-left: 3px solid #909399; }

.exec-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}
.exec-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.exec-status-dot.success { background: #67C23A; }
.exec-status-dot.failed { background: #F56C6C; }
.exec-status-dot.running { background: #409EFF; animation: pulse 1.5s infinite; }
.exec-status-dot.pending { background: #909399; }

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.6; }
}

.exec-info { display: flex; flex-direction: column; }
.exec-name { font-size: 13px; font-weight: 600; color: #303133; }
.exec-type { font-size: 11px; color: #909399; }

.exec-center { flex: 1; min-width: 0; }
.exec-summary {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.exec-error {
  font-size: 12px;
  color: #F56C6C;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exec-right {
  text-align: right;
  min-width: 80px;
}
.exec-duration { font-size: 12px; font-weight: 600; color: #409EFF; }
.exec-time { font-size: 11px; color: #909399; margin-top: 2px; }

.log-card { margin-bottom: 16px; }
.log-card :deep(.el-card__body) { max-height: 300px; overflow-y: auto; }
.error-card :deep(.el-card__body) { max-height: 300px; overflow-y: auto; }
</style>
