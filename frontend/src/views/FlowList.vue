<template>
  <div class="flow-list">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
              <path d="M9 9h6M9 13h6M9 17h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </span>
          流程设计
        </h1>
        <p class="page-desc">管理和设计您的数据迁移流程</p>
      </div>
      <el-button type="primary" size="large" @click="handleCreate" class="create-btn">
        <span class="btn-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
        新建流程
      </el-button>
    </div>

    <div class="stats-grid" v-if="flowStore.flows.length">
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

    <div class="flows-container" v-if="flowStore.flows.length">
      <div class="section-header">
        <h2 class="section-title">我的流程</h2>
        <span class="flow-count">{{ flowStore.flows.length }} 个流程</span>
      </div>

      <el-row :gutter="24">
        <el-col
          :xs="24"
          :sm="12"
          :md="8"
          v-for="(flow, index) in flowStore.flows"
          :key="flow.id"
          class="flow-col"
        >
          <el-card shadow="hover" class="flow-card" :style="{ animationDelay: (index + stats.length) * 0.1 + 's' }">
            <template #header>
              <div class="card-header">
                <div class="flow-badge" :class="flow.enabled ? 'enabled' : 'disabled'">
                  <span class="badge-dot"></span>
                  {{ flow.enabled ? '启用' : '禁用' }}
                </div>
                <div class="card-actions">
                  <button class="icon-btn" @click.stop="handleEdit(flow)" title="编辑">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </button>
                  <button class="icon-btn danger" @click.stop="handleDelete(flow)" title="删除">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </button>
                </div>
              </div>
            </template>

            <div class="card-body">
              <h3 class="flow-name">{{ flow.name }}</h3>
              <p class="flow-desc">{{ flow.description || '暂无描述' }}</p>

              <div class="flow-stats">
                <div class="stat-item">
                  <div class="stat-icon-small">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <span>{{ flow.nodes?.length || 0 }} 节点</span>
                </div>
                <div class="stat-item">
                  <div class="stat-icon-small">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </div>
                  <span>v{{ flow.version }}</span>
                </div>
              </div>

              <el-button type="primary" class="action-btn run" @click="handleRun(flow)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <polygon points="5,3 19,12 5,21" stroke="currentColor" stroke-width="2" fill="none"/>
                </svg>
                立即执行
              </el-button>
            </div>

            <div class="card-glow"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-empty v-else class="empty-state">
      <template #image>
        <div class="empty-illustration">
          <svg width="120" height="120" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
            <path d="M9 12h6M12 9v6" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
          </svg>
        </div>
      </template>
      <template #description>
        <div class="empty-text">
          <h3>还没有流程</h3>
          <p>点击下方按钮创建您的第一个数据迁移流程</p>
        </div>
      </template>
      <el-button type="primary" size="large" @click="handleCreate" class="create-first-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        新建流程
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useFlowStore } from '../stores/flow'

const router = useRouter()
const flowStore = useFlowStore()

const stats = computed(() => [
  {
    label: '总流程数',
    value: flowStore.flows.length,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" stroke="white" stroke-width="2"/><path d="M9 9h6M9 13h6M9 17h4" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  },
  {
    label: '启用流程',
    value: flowStore.flows.filter(f => f.enabled).length,
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="white" stroke-width="2"/><path d="M22 4L12 14.01l-3-3" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  },
  {
    label: '节点总数',
    value: flowStore.flows.reduce((sum, f) => sum + (f.nodes?.length || 0), 0),
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="white" stroke-width="2"/><circle cx="12" cy="12" r="8" stroke="white" stroke-width="2"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4" stroke="white" stroke-width="2" stroke-linecap="round"/></svg>'
  }
])

onMounted(() => {
  flowStore.loadFlows()
})

function handleCreate() {
  flowStore.createNewFlow()
  router.push('/flows/designer')
}

function handleEdit(flow: any) {
  router.push(`/flows/designer/${flow.id}`)
}

function handleRun(flow: any) {
  router.push({ path: '/tasks', query: { flowId: flow.id, flowName: flow.name } })
}

async function handleDelete(flow: any) {
  await ElMessageBox.confirm(
    `确定删除流程「${flow.name}」？此操作不可撤销。`,
    '确认删除',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  await flowStore.deleteFlow(flow.id)
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.flow-list {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
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

.btn-icon {
  display: flex;
  align-items: center;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 20px;
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
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.stat-content { flex: 1; position: relative; z-index: 1; }

.stat-value {
  font-size: 36px;
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

/* 流程容器 */
.flows-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.flow-count {
  font-size: 14px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.flow-col { margin-bottom: 24px; }

.flow-card {
  border-radius: 20px;
  border: none;
  background: white;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  position: relative;
}

.flow-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
}

.flow-card :deep(.el-card__header) {
  padding: 20px 24px 16px;
  border-bottom: none;
  background: transparent;
}

.flow-card :deep(.el-card__body) {
  padding: 0 24px 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flow-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.flow-badge.enabled {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #16a34a;
}

.flow-badge.disabled {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.card-actions { display: flex; gap: 8px; }

.icon-btn {
  width: 32px;
  height: 32px;
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

.icon-btn:hover {
  background: #667eea;
  color: white;
  transform: scale(1.1);
}

.icon-btn.danger:hover {
  background: #ef4444;
}

.card-body { padding-top: 8px; }

.flow-name {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.flow-desc {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 20px;
  min-height: 40px;
  line-height: 1.6;
}

.flow-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}

.stat-icon-small {
  color: #cbd5e1;
}

.action-btn.run {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn.run:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.flow-card:hover .card-glow {
  opacity: 1;
}

/* 空状态 */
.empty-state {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 80px 40px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.empty-illustration {
  margin-bottom: 24px;
  color: #cbd5e1;
}

.empty-text h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.empty-text p {
  color: #64748b;
  font-size: 15px;
}

.create-first-btn {
  margin-top: 24px;
  height: 52px;
  padding: 0 32px;
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

.create-first-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 32px rgba(102, 126, 234, 0.5);
}

@media (max-width: 768px) {
  .page-header { flex-direction: column; }
  .create-btn { width: 100%; justify-content: center; }
  .page-title { font-size: 28px; flex-direction: column; align-items: flex-start; gap: 12px; }
  .page-desc { margin-left: 0; }
  .title-icon { width: 48px; height: 48px; }
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
