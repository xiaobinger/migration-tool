<template>
  <div class="ds-list">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
              <ellipse cx="12" cy="6" rx="9" ry="3" stroke="currentColor" stroke-width="2"/>
              <path d="M3 6v6c0 1.657 4.03 3 9 3s9-1.343 9-3V6" stroke="currentColor" stroke-width="2"/>
              <path d="M3 12v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6" stroke="currentColor" stroke-width="2"/>
            </svg>
          </span>
          数据源管理
        </h1>
        <p class="page-desc">管理数据库连接配置</p>
      </div>
      <el-button type="primary" size="large" @click="handleCreate" class="create-btn">
        <span class="btn-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </span>
        新建连接
      </el-button>
    </div>

    <div class="stats-grid" v-if="dsStore.dataSources.length">
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

    <div class="ds-container" v-if="dsStore.dataSources.length">
      <div class="section-header">
        <h2 class="section-title">我的连接</h2>
        <span class="ds-count">{{ dsStore.dataSources.length }} 个连接</span>
      </div>

      <el-row :gutter="24">
        <el-col
          :xs="24"
          :sm="12"
          :md="8"
          v-for="(ds, index) in dsStore.dataSources"
          :key="ds.id"
          class="ds-col"
        >
          <el-card shadow="hover" class="ds-card" :style="{ animationDelay: (index + stats.length) * 0.1 + 's' }">
            <template #header>
              <div class="card-header">
                <div class="ds-name-row">
                  <h3 class="ds-name">{{ ds.name }}</h3>
                  <el-tag
                    :type="typeTagMap[ds.type]?.type || 'info'"
                    size="small"
                    class="type-tag"
                    effect="dark"
                    round
                  >
                    {{ typeTagMap[ds.type]?.label || ds.type }}
                  </el-tag>
                </div>
                <div class="card-actions">
                  <button class="icon-btn" @click.stop="handleTest(ds)" title="测试连接" :disabled="testingId === ds.id">
                    <svg v-if="testingId !== ds.id" width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="2"/>
                      <path d="M22 4L12 14.01l-3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                    <svg v-else class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </button>
                  <button class="icon-btn" @click.stop="handleEdit(ds)" title="编辑">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </button>
                  <button class="icon-btn danger" @click.stop="handleDelete(ds)" title="删除">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </button>
                </div>
              </div>
            </template>

            <div class="card-body">
              <div class="ds-info">
                <div class="info-item">
                  <div class="info-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <rect x="2" y="3" width="20" height="14" rx="2" stroke="currentColor" stroke-width="2"/>
                      <path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </div>
                  <span>{{ ds.host }}:{{ ds.port }}/{{ ds.database }}</span>
                </div>
                <div class="info-item">
                  <div class="info-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <span>{{ ds.username }}</span>
                </div>
              </div>

              <div class="ds-badges">
                <el-tag v-if="ds.sshEnabled" type="warning" size="small" effect="plain" round class="ssh-badge">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
                    <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke="currentColor" stroke-width="2"/>
                  </svg>
                  SSH
                </el-tag>
                <div class="status-badge" :class="ds.enabled ? 'enabled' : 'disabled'">
                  <span class="badge-dot"></span>
                  {{ ds.enabled ? '已启用' : '已禁用' }}
                </div>
              </div>

              <p class="ds-desc" v-if="ds.description">{{ ds.description }}</p>
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
            <ellipse cx="12" cy="6" rx="9" ry="3" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
            <path d="M3 6v6c0 1.657 4.03 3 9 3s9-1.343 9-3V6" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
            <path d="M3 12v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6" stroke="currentColor" stroke-width="1.5" opacity="0.3"/>
            <path d="M12 9v6M9 12h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
          </svg>
        </div>
      </template>
      <template #description>
        <div class="empty-text">
          <h3>还没有连接</h3>
          <p>点击下方按钮创建您的第一个数据库连接</p>
        </div>
      </template>
      <el-button type="primary" size="large" @click="handleCreate" class="create-first-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        新建连接
      </el-button>
    </el-empty>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑连接' : '新建连接'"
      width="640px"
      :close-on-click-modal="false"
      class="ds-dialog"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" size="default" class="ds-form">
        <div class="form-section">
          <div class="section-label">基本信息</div>
          <el-form-item label="连接名称" required>
            <el-input v-model="form.name" placeholder="请输入连接名称" />
          </el-form-item>
          <el-form-item label="数据库类型" required>
            <el-select v-model="form.type" @change="handleTypeChange" style="width: 100%">
              <el-option label="MySQL" value="MYSQL" />
              <el-option label="PostgreSQL" value="POSTGRESQL" />
              <el-option label="MongoDB" value="MONGODB" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="section-label">连接配置</div>
          <el-row :gutter="16">
            <el-col :span="16">
              <el-form-item label="主机地址" required>
                <el-input v-model="form.host" placeholder="如 127.0.0.1" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="端口" required>
                <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="数据库" required>
            <el-input v-model="form.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名" required>
                <el-input v-model="form.username" placeholder="请输入用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码" required>
                <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section ssh-section">
          <div class="ssh-header" @click="sshExpanded = !sshExpanded">
            <div class="section-label ssh-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke="currentColor" stroke-width="2"/>
              </svg>
              SSH 隧道
              <el-switch v-model="form.sshEnabled" size="small" @click.stop class="ssh-switch" />
            </div>
            <svg
              width="16" height="16" viewBox="0 0 24 24" fill="none"
              :class="{ rotated: sshExpanded }"
              class="expand-arrow"
            >
              <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="ssh-content" v-if="sshExpanded && form.sshEnabled">
            <el-row :gutter="16">
              <el-col :span="16">
                <el-form-item label="SSH 主机">
                  <el-input v-model="form.sshHost" placeholder="SSH 主机地址" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="SSH 端口">
                  <el-input-number v-model="form.sshPort" :min="1" :max="65535" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="SSH 用户名">
              <el-input v-model="form.sshUsername" placeholder="SSH 登录用户名" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="SSH 密码">
                  <el-input v-model="form.sshPassword" type="password" show-password placeholder="SSH 密码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="SSH 密钥">
                  <el-input v-model="form.sshAuthKey" type="textarea" :rows="1" placeholder="SSH 私钥内容" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <div class="form-section">
          <el-form-item label="启用状态">
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            :loading="testLoading"
            @click="handleTestInDialog"
            class="test-btn"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" v-if="!testLoading">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="2"/>
              <path d="M22 4L12 14.01l-3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            测试连接
          </el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataSourceStore } from '../stores/datasource'
import type { DataSourceConfig } from '../api/datasource'

const dsStore = useDataSourceStore()

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | undefined>(undefined)
const testLoading = ref(false)
const sshExpanded = ref(false)
const testingId = ref<number | undefined>(undefined)

const defaultForm = (): DataSourceConfig => ({
  name: '',
  type: 'MYSQL',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: '',
  sshEnabled: false,
  sshHost: '',
  sshPort: 22,
  sshUsername: '',
  sshPassword: '',
  sshAuthKey: '',
  enabled: true,
  description: ''
})

const form = reactive<DataSourceConfig>(defaultForm())

const typeTagMap: Record<string, { type: string; label: string }> = {
  MYSQL: { type: 'primary', label: 'MySQL' },
  POSTGRESQL: { type: 'success', label: 'PostgreSQL' },
  MONGODB: { type: 'warning', label: 'MongoDB' }
}

const stats = computed(() => [
  {
    label: '总连接数',
    value: dsStore.dataSources.length,
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><ellipse cx="12" cy="6" rx="9" ry="3" stroke="white" stroke-width="2"/><path d="M3 6v6c0 1.657 4.03 3 9 3s9-1.343 9-3V6" stroke="white" stroke-width="2"/><path d="M3 12v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6" stroke="white" stroke-width="2"/></svg>'
  },
  {
    label: 'MySQL连接',
    value: dsStore.dataSources.filter(ds => ds.type === 'MYSQL').length,
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><ellipse cx="12" cy="6" rx="9" ry="3" stroke="white" stroke-width="2"/><path d="M3 6v6c0 1.657 4.03 3 9 3s9-1.343 9-3V6" stroke="white" stroke-width="2"/><path d="M3 12v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6" stroke="white" stroke-width="2"/></svg>'
  },
  {
    label: 'PostgreSQL连接',
    value: dsStore.dataSources.filter(ds => ds.type === 'POSTGRESQL').length,
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><ellipse cx="12" cy="6" rx="9" ry="3" stroke="white" stroke-width="2"/><path d="M3 6v6c0 1.657 4.03 3 9 3s9-1.343 9-3V6" stroke="white" stroke-width="2"/><path d="M3 12v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6" stroke="white" stroke-width="2"/></svg>'
  }
])

onMounted(() => {
  dsStore.loadDataSources()
})

function handleCreate() {
  isEdit.value = false
  editId.value = undefined
  Object.assign(form, defaultForm())
  sshExpanded.value = false
  dialogVisible.value = true
}

function handleEdit(ds: DataSourceConfig) {
  isEdit.value = true
  editId.value = ds.id
  Object.assign(form, {
    name: ds.name,
    type: ds.type,
    host: ds.host,
    port: ds.port,
    database: ds.database,
    username: ds.username,
    password: '',
    sshEnabled: ds.sshEnabled || false,
    sshHost: ds.sshHost || '',
    sshPort: ds.sshPort || 22,
    sshUsername: ds.sshUsername || '',
    sshPassword: '',
    sshAuthKey: ds.sshAuthKey || '',
    enabled: ds.enabled !== false,
    description: ds.description || ''
  })
  sshExpanded.value = !!ds.sshEnabled
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.host || !form.database || !form.username) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await dsStore.updateDataSource(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await dsStore.createDataSource({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(ds: DataSourceConfig) {
  await ElMessageBox.confirm(
    `确定删除连接「${ds.name}」？此操作不可撤销。`,
    '确认删除',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  try {
    await dsStore.deleteDataSource(ds.id!)
    ElMessage.success('删除成功')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleTest(ds: DataSourceConfig) {
  testingId.value = ds.id
  try {
    const result = await dsStore.testConnection(ds.id!)
    if (result.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(`连接失败: ${result.message}`)
    }
  } catch {
    ElMessage.error('连接测试异常')
  } finally {
    testingId.value = undefined
  }
}

async function handleTestInDialog() {
  testLoading.value = true
  try {
    const result = await dsStore.testDirect({ ...form })
    if (result.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(`连接失败: ${result.message}`)
    }
  } catch {
    ElMessage.error('连接测试异常')
  } finally {
    testLoading.value = false
  }
}

function handleTypeChange(val: string) {
  const portMap: Record<string, number> = {
    MYSQL: 3306,
    POSTGRESQL: 5432,
    MONGODB: 27017
  }
  form.port = portMap[val] || 3306
}
</script>

<style scoped>
.ds-list {
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

.ds-container {
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

.ds-container .section-header::after {
  content: '';
  flex: 1;
}

.ds-count {
  font-size: 14px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.ds-col { margin-bottom: 24px; }

.ds-card {
  border-radius: 20px;
  border: none;
  background: white;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  position: relative;
}

.ds-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
}

.ds-card :deep(.el-card__header) {
  padding: 20px 24px 16px;
  border-bottom: none;
  background: transparent;
}

.ds-card :deep(.el-card__body) {
  padding: 0 24px 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ds-name {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spin {
  animation: spin 1s linear infinite;
}

.card-body { padding-top: 8px; }

.ds-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.info-icon {
  color: #94a3b8;
  display: flex;
  align-items: center;
}

.ds-badges {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.ssh-badge {
  display: inline-flex;
  align-items: center;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  letter-spacing: 0.5px;
}

.status-badge.enabled {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #16a34a;
}

.status-badge.disabled {
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

.ds-desc {
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
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

.ds-card:hover .card-glow {
  opacity: 1;
}

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

.ds-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.ds-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 24px;
  margin-right: 0;
}

.ds-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 700;
  font-size: 18px;
}

.ds-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(255, 255, 255, 0.8);
}

.ds-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.ds-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
}

.ds-form .form-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.ds-form .form-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-label {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ssh-section {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border-radius: 12px;
  padding: 16px;
  border-bottom: none !important;
  margin-bottom: 24px;
}

.ssh-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background 0.3s ease;
  border-radius: 8px;
  padding: 4px 8px;
  margin: -4px -8px;
}

.ssh-header:hover {
  background: rgba(102, 126, 234, 0.15);
}

.ssh-label {
  color: #e2e8f0;
  margin-bottom: 0;
}

.ssh-switch {
  margin-left: 12px;
}

.expand-arrow {
  color: #64748b;
  transition: transform 0.3s ease;
}

.expand-arrow.rotated {
  transform: rotate(180deg);
}

.ssh-content {
  padding-top: 16px;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.ssh-content :deep(.el-form-item__label) {
  color: #94a3b8;
}

.ssh-content :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: none;
}

.ssh-content :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3);
}

.ssh-content :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
}

.ssh-content :deep(.el-input__inner) {
  color: #e2e8f0;
}

.ssh-content :deep(.el-input-number) {
  background: rgba(255, 255, 255, 0.08);
}

.ssh-content :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #e2e8f0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.test-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .page-header { flex-direction: column; }
  .create-btn { width: 100%; justify-content: center; }
  .page-title { font-size: 28px; flex-direction: column; align-items: flex-start; gap: 12px; }
  .page-desc { margin-left: 0; }
  .title-icon { width: 48px; height: 48px; }
  .stats-grid { grid-template-columns: 1fr; }
  .ds-dialog :deep(.el-dialog) { width: 95% !important; }
}
</style>
