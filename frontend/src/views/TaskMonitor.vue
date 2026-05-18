<template>
  <div class="task-monitor">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <path d="M12 6v6l4 2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
          任务监控
        </h1>
        <p class="page-desc">实时监控数据迁移任务的执行状态</p>
      </div>
      <el-button type="primary" size="large" @click="showStartDialog = true" class="create-btn">
        <span class="btn-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
        新建任务
      </el-button>
    </div>

    <div class="stats-grid">
      <div class="stat-card" v-for="(stat, index) in stats" :key="stat.label" :style="{ animationDelay: index * 0.1 + 's' }">
        <div class="stat-icon" :style="{ background: stat.gradient }">
          <div v-html="stat.icon"></div>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-glow" :style="{ background: stat.gradient }"></div>
      </div>
    </div>

    <div class="running-section" v-if="runningTasks.length">
      <div class="section-header">
        <div class="section-title-wrapper">
          <span class="pulse-dot"></span>
          <h2 class="section-title">运行中任务</h2>
          <span class="task-count">{{ runningTasks.length }} 个任务</span>
        </div>
      </div>

      <div class="running-cards">
        <el-card
          v-for="(task, index) in runningTasks"
          :key="task.id"
          class="task-card running"
          :style="{ animationDelay: (index + stats.length) * 0.1 + 's' }"
          @click="viewTask(task)"
        >
          <div class="task-header">
            <div class="task-info">
              <h4 class="task-name">{{ task.name }}</h4>
              <div class="task-badge running">
                <div class="spinner"></div>
                执行中
              </div>
            </div>
            <div class="task-percentage">
              <span class="percentage-value">{{ Math.round(task.completedNodes / Math.max(task.totalNodes, 1) * 100) }}%</span>
            </div>
          </div>

          <el-progress
            :percentage="Math.round(task.completedNodes / Math.max(task.totalNodes, 1) * 100)"
            :stroke-width="10"
            :color="progressGradient"
            striped
            striped-flow
          />

          <div class="task-stats">
            <div class="stat-item">
              <div class="stat-icon-small">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="2"/>
                </svg>
              </div>
              <span>节点</span>
              <strong>{{ task.completedNodes }}/{{ task.totalNodes }}</strong>
            </div>
            <div class="stat-item success">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="2"/>
                <path d="M22 4L12 14.01l-3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span>成功</span>
              <strong>{{ task.successRecords }}</strong>
            </div>
            <div class="stat-item failed">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span>失败</span>
              <strong>{{ task.failedRecords }}</strong>
            </div>
          </div>

          <div class="card-glow running-glow"></div>
        </el-card>
      </div>
    </div>

    <div class="tasks-container">
      <div class="section-header">
        <h2 class="section-title">任务列表</h2>
        <span class="flow-count">{{ taskStore.tasks.length }} 个任务</span>
        <div class="batch-actions">
          <button class="action-btn clear-completed" @click="handleClearCompleted" v-if="hasCompletedTasks">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <polyline points="3 6 5 6 21 6" stroke="currentColor" stroke-width="2"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
            </svg>
            清除已完成
          </button>
          <button class="action-btn clear-all" @click="handleClearAll" v-if="taskStore.tasks.length > 0">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            清除全部
          </button>
        </div>
      </div>

      <el-table
        :data="taskStore.tasks"
        stripe
        class="tasks-table"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="name" label="任务名称" min-width="200">
          <template #default="{ row }">
            <div class="task-name-cell">
              <span class="task-name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="140" align="center">
          <template #default="{ row }">
            <div class="status-badge" :class="row.status.toLowerCase()">
              <span class="status-dot"></span>
              {{ getTaskStatusLabel(row.status) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="进度" width="220" align="center">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="Math.round(row.completedNodes / Math.max(row.totalNodes, 1) * 100)"
                :status="getProgressStatus(row.status)"
                :stroke-width="8"
                :show-text="false"
                :color="progressGradient"
              />
              <span class="progress-text">{{ row.completedNodes }}/{{ row.totalNodes }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="数据记录" width="160" align="center">
          <template #default="{ row }">
            <div class="records-cell">
              <span class="record success">{{ row.successRecords }}</span>
              <span class="record-divider">/</span>
              <span class="record failed">{{ row.failedRecords }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <button class="action-btn view" @click="viewTask(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                </svg>
                详情
              </button>
              <button class="action-btn restart" v-if="row.status === 'FAILED' || row.status === 'PAUSED' || row.status === 'CANCELLED'" @click="handleRestart(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M23 4v6h-6M1 20v-6h6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                重新执行
              </button>
              <button class="action-btn rerun" v-if="row.status === 'SUCCESS'" @click="handleRerun(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M23 4v6h-6M1 20v-6h6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                再跑一次
              </button>
              <button class="action-btn pause" v-if="row.status === 'RUNNING'" @click="handlePause(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <rect x="6" y="4" width="4" height="16" stroke="currentColor" stroke-width="2"/>
                  <rect x="14" y="4" width="4" height="16" stroke="currentColor" stroke-width="2"/>
                </svg>
                暂停
              </button>
              <button class="action-btn cancel" v-if="row.status === 'RUNNING'" @click="handleCancel(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                  <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                取消
              </button>
              <button class="action-btn delete" v-if="row.status !== 'RUNNING' && row.status !== 'PENDING'" @click="handleDelete(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <polyline points="3 6 5 6 21 6" stroke="currentColor" stroke-width="2"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
                </svg>
                删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="showStartDialog"
      title="新建迁移任务"
      width="520px"
      class="create-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="taskForm" label-width="100px" class="task-form">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.name" placeholder="输入任务名称" size="large" />
        </el-form-item>
        <el-form-item label="选择流程">
          <el-select v-model="taskForm.flowDefinitionId" placeholder="选择流程定义" style="width: 100%" size="large">
            <el-option
              v-for="flow in flowStore.flows"
              :key="flow.id"
              :label="flow.name"
              :value="flow.id!"
            >
              <div class="flow-option">
                <span>{{ flow.name }}</span>
                <span class="flow-badge-small">{{ flow.enabled ? '启用' : '禁用' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStartDialog = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleStartTask" size="large" class="start-btn">
          开始执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTaskStore } from '../stores/task'
import { useFlowStore } from '../stores/flow'
import { getTaskStatusLabel, getTaskStatusColor } from '../types'
import type { MigrationTask } from '../types'

const router = useRouter()
const taskStore = useTaskStore()
const flowStore = useFlowStore()

const showStartDialog = ref(false)
const taskForm = ref({ name: '', flowDefinitionId: null as number | null })

const progressGradient = 'linear-gradient(90deg, #667eea 0%, #764ba2 100%)'

const stats = computed(() => [
  {
    label: '总任务数',
    value: taskStore.tasks.length,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" stroke="white" stroke-width="2"/><path d="M9 9h6M9 13h6M9 17h4" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  },
  {
    label: '运行中',
    value: runningTasks.value.length,
    gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="white" stroke-width="2"/><path d="M12 6v6l4 2" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  },
  {
    label: '已完成',
    value: completedTasks.value.length,
    gradient: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="white" stroke-width="2"/><path d="M22 4L12 14.01l-3-3" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  },
  {
    label: '失败',
    value: failedTasks.value.length,
    gradient: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="white" stroke-width="2"/><path d="M15 9l-6 6M9 9l6 6" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  }
])

const runningTasks = computed(() =>
  taskStore.tasks.filter(t => t.status === 'RUNNING' || t.status === 'PENDING')
)

const completedTasks = computed(() =>
  taskStore.tasks.filter(t => t.status === 'SUCCESS')
)

const failedTasks = computed(() =>
  taskStore.tasks.filter(t => t.status === 'FAILED')
)

const hasCompletedTasks = computed(() =>
  taskStore.tasks.some(t => t.status === 'SUCCESS' || t.status === 'FAILED' || t.status === 'CANCELLED')
)

onMounted(async () => {
  await flowStore.loadFlows()
  await taskStore.loadTasks()
  taskStore.initWebSocket()

  const flowId = router.currentRoute.value.query.flowId as string
  const flowName = router.currentRoute.value.query.flowName as string
  if (flowId) {
    taskForm.value.flowDefinitionId = Number(flowId)
    taskForm.value.name = flowName || ''
    showStartDialog.value = true
  }
})

function viewTask(task: MigrationTask) {
  router.push(`/tasks/${task.id}`)
}

async function handleStartTask() {
  if (!taskForm.value.name || !taskForm.value.flowDefinitionId) return
  await taskStore.startTask(taskForm.value.name, taskForm.value.flowDefinitionId)
  showStartDialog.value = false
  taskForm.value = { name: '', flowDefinitionId: null }
}

async function handleRestart(task: MigrationTask) {
  await taskStore.restartTask(task.id)
  ElMessage.success('任务已重新执行')
}

async function handleRerun(task: MigrationTask) {
  try {
    await ElMessageBox.confirm('确定要重新执行此任务吗？', '确认', { type: 'warning' })
    await taskStore.restartTask(task.id)
    ElMessage.success('任务已重新执行')
  } catch { /* cancelled */ }
}

async function handlePause(task: MigrationTask) {
  await taskStore.pauseTask(task.id)
}

async function handleCancel(task: MigrationTask) {
  await taskStore.cancelTask(task.id)
}

async function handleDelete(task: MigrationTask) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${task.name}」？此操作不可撤销。`, '确认删除', { type: 'warning' })
    await taskStore.deleteTask(task.id)
    ElMessage.success('删除成功')
  } catch { /* cancelled */ }
}

async function handleClearCompleted() {
  try {
    await ElMessageBox.confirm('确定清除所有已完成的任务记录？', '确认清除', { type: 'warning' })
    await taskStore.clearCompletedTasks()
    ElMessage.success('已清除已完成任务')
  } catch { /* cancelled */ }
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm('确定清除所有任务记录？运行中的任务不会被清除。', '确认清除', { type: 'warning' })
    await taskStore.clearAllTasks()
    ElMessage.success('已清除全部任务')
  } catch { /* cancelled */ }
}

function getProgressStatus(status: string) {
  if (status === 'FAILED') return 'exception'
  if (status === 'SUCCESS') return 'success'
  return ''
}

function tableRowClassName({ row }: { row: MigrationTask }) {
  if (row.status === 'RUNNING') return 'running-row'
  if (row.status === 'FAILED') return 'failed-row'
  return ''
}
</script>

<style scoped>
.task-monitor {
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
  gap: 24px;
}

.header-left { flex: 1; }

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.title-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.4);
}

.page-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 72px;
}

.create-btn {
  height: 52px;
  padding: 0 28px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.create-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 32px rgba(102, 126, 234, 0.5);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  animation: slideUp 0.5s ease forwards;
  opacity: 0;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.stat-content { flex: 1; position: relative; z-index: 1; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 500;
}

.stat-glow {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  opacity: 0.1;
  filter: blur(40px);
}

/* 运行中任务 */
.running-section { margin-bottom: 32px; }

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pulse-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: white;
}

.task-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.running-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 20px;
}

.task-card.running {
  border-radius: 20px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  position: relative;
  overflow: hidden;
}

.task-card.running:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(245, 158, 11, 0.3);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.task-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-name {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.task-badge.running {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #d97706;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.spinner {
  width: 10px;
  height: 10px;
  border: 2px solid #d97706;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.percentage-value {
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
}

.task-stats {
  display: flex;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.stat-item strong {
  color: #1e293b;
  font-weight: 700;
}

.stat-item.success strong { color: #10b981; }
.stat-item.failed strong { color: #ef4444; }

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.running-glow {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1) 0%, rgba(217, 119, 6, 0.1) 100%);
}

.task-card:hover .card-glow { opacity: 1; }

/* 任务列表容器 */
.tasks-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.tasks-container .section-header { margin-bottom: 24px; }
.tasks-container .section-title { color: #1e293b; }
.tasks-container .flow-count {
  background: #f1f5f9;
  color: #64748b;
}

.tasks-table { border-radius: 16px; overflow: hidden; }

:deep(.el-table__header th) {
  background: #f8fafc !important;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

:deep(.el-table__row) {
  transition: all 0.3s ease;
}

:deep(.el-table__row:hover) {
  background: #f8fafc !important;
}

:deep(.el-table__row.running-row) {
  background: rgba(245, 158, 11, 0.05);
}

:deep(.el-table__row.failed-row) {
  background: rgba(239, 68, 68, 0.05);
}

.task-name-cell { display: flex; align-items: center; gap: 8px; }
.task-name-text { font-weight: 600; color: #1e293b; }

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-badge.running {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #d97706;
}

.status-badge.success {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #16a34a;
}

.status-badge.failed {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
}

.status-badge.pending, .status-badge.cancelled {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell .el-progress { flex: 1; }

.progress-text {
  font-size: 13px;
  color: #64748b;
  min-width: 50px;
  text-align: right;
  font-weight: 500;
}

.records-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.record.success { color: #10b981; }
.record.failed { color: #ef4444; }
.record.total { color: #64748b; }
.record-divider { color: #cbd5e1; }

.time-text { font-size: 13px; color: #64748b; }

.action-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: none;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f1f5f9;
  color: #64748b;
}

.action-btn:hover { transform: translateY(-2px); }
.action-btn.view:hover { background: #667eea; color: white; }
.action-btn.restart:hover { background: #f59e0b; color: white; }
.action-btn.pause:hover { background: #10b981; color: white; }
.action-btn.cancel:hover { background: #ef4444; color: white; }
.action-btn.delete { color: #ef4444; }
.action-btn.delete:hover { background: #ef4444; color: white; }
.action-btn.rerun:hover { background: #667eea; color: white; }
.action-btn.clear-completed { color: #f59e0b; padding: 8px 16px; }
.action-btn.clear-completed:hover { background: #f59e0b; color: white; }
.action-btn.clear-all { color: #ef4444; padding: 8px 16px; }
.action-btn.clear-all:hover { background: #ef4444; color: white; }

.batch-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

/* 对话框 */
.flow-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.flow-badge-small {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
}

.start-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.start-btn:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

@media (max-width: 768px) {
  .page-header { flex-direction: column; }
  .create-btn { width: 100%; justify-content: center; }
  .page-title { font-size: 28px; flex-direction: column; align-items: flex-start; gap: 12px; }
  .page-desc { margin-left: 0; }
  .title-icon { width: 48px; height: 48px; }
  .stats-grid { grid-template-columns: 1fr; }
  .running-cards { grid-template-columns: 1fr; }
  .action-buttons { flex-wrap: wrap; }
}
</style>
