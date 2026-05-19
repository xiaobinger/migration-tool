<template>
  <div class="flow-designer" v-if="flowStore.currentFlow">
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <button class="back-btn" @click="$router.push('/flows')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>返回</span>
        </button>
        <div class="toolbar-divider"></div>
        <el-input
          v-model="flowStore.currentFlow.name"
          style="width: 240px"
          placeholder="输入流程名称"
          size="large"
          class="flow-name-input"
        />
      </div>

      <div class="toolbar-center">
        <button class="tool-btn" @click="showHelp = !showHelp" :class="{ active: showHelp }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="17" r="0.5" fill="currentColor"/>
          </svg>
          <span>{{ showHelp ? '隐藏' : '显示' }}帮助</span>
        </button>
      </div>

      <div class="toolbar-right">
        <button class="tool-btn execute" @click="handleExecute" :disabled="executing">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <polygon points="5 3 19 12 5 21 5 3" stroke="currentColor" stroke-width="2" fill="none"/>
          </svg>
          <span>{{ executing ? '执行中...' : '立即执行' }}</span>
        </button>
        <button class="tool-btn save" @click="handleSave">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" stroke="currentColor" stroke-width="2"/>
            <polyline points="17 21 17 13 7 13 7 21" stroke="currentColor" stroke-width="2"/>
            <polyline points="7 3 7 8 15 8" stroke="currentColor" stroke-width="2"/>
          </svg>
          <span>保存</span>
        </button>
      </div>
    </div>

    <div class="designer-body">
      <NodePanel @add-node="handleAddNode" />

      <div class="canvas-wrapper" ref="canvasWrapper">
        <CanvasToolbar
            v-model:edge-style="edgeStyle"
            v-model:show-animation="showAnimation"
            :zoom-level="canvasZoomLevel"
            @zoom-in="flowCanvasRef?.zoomIn()"
            @zoom-out="flowCanvasRef?.zoomOut()"
            @fit-to-view="flowCanvasRef?.fitToView()"
            @reset-zoom="flowCanvasRef?.resetZoom()"
        />
        <div class="multi-select-bar" v-if="multiSelectedNodeIds.length > 1">
          <span class="select-info">已选中 {{ multiSelectedNodeIds.length }} 个节点</span>
          <button class="action-btn group-btn" @click="handleGroupAsParallel">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
              <rect x="14" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
              <rect x="3" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
              <rect x="14" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
            </svg>
            组合为并行组
          </button>
          <button class="action-btn cancel-btn" @click="handleClearSelection">取消选择</button>
        </div>
        <FlowCanvas
            ref="flowCanvasRef"
            :flow="flowStore.currentFlow"
            :node-status-map="{}"
            :active-node-id="null"
            :show-animation="showAnimation"
            @node-click="handleNodeClick"
            @node-dblclick="handleNodeDblClick"
            @node-move="handleNodeMove"
            @connect="handleConnect"
            @delete-node="handleDeleteNode"
            @delete-edge="handleDeleteEdge"
            @edge-click="handleEdgeClick"
            @selection-change="handleSelectionChange"
        />
      </div>

      <transition name="slide-panel">
        <div class="property-panel" v-if="selectedNode || selectedEdge">
          <template v-if="selectedNode">
            <div class="panel-header">
              <h3>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="2"/>
                </svg>
                节点属性
              </h3>
              <button class="close-btn" @click="selectedNode = null">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>

            <div class="panel-body">
              <div class="form-section">
                <div class="section-title">基本信息</div>
                <el-form label-width="80px" size="default">
                  <el-form-item label="节点ID">
                    <el-input :model-value="selectedNode.nodeId" disabled />
                  </el-form-item>
                  <el-form-item label="节点类型">
                    <el-tag effect="dark" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none;">
                      {{ flowStore.getNodeTypeName(selectedNode.nodeType) }}
                    </el-tag>
                  </el-form-item>
                  <el-form-item label="名称">
                    <el-input v-model="selectedNode.name" placeholder="输入节点名称" />
                  </el-form-item>
                  <el-form-item label="描述">
                    <el-input
                        v-model="selectedNode.description"
                        type="textarea"
                        :rows="2"
                        placeholder="节点描述"
                    />
                  </el-form-item>
                </el-form>
              </div>

              <div class="form-section" v-if="hasImplementation(selectedNode.nodeType)">
                <div class="section-title">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2"/>
                    <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
                  </svg>
                  业务实现类
                </div>

                <div v-if="selectedNode.nodeType === 'parallel_group'" class="parallel-group-info">
                  <div class="pg-children">
                    <div class="pg-children-title">子流程链路</div>
                    <div v-for="(chain, idx) in getParallelGroupChains(selectedNode.nodeId)" :key="idx" class="pg-chain">
                      <div class="pg-chain-header">链路 {{ idx + 1 }}</div>
                      <div class="pg-chain-nodes">
                        <template v-for="(node, nIdx) in chain" :key="node.nodeId">
                          <div class="pg-chain-node">
                            <span class="pg-child-icon">{{ getNodeIcon(node.nodeType) }}</span>
                            <span class="pg-child-name">{{ node.name }}</span>
                          </div>
                          <span v-if="nIdx < chain.length - 1" class="pg-chain-arrow">→</span>
                        </template>
                      </div>
                    </div>
                    <div v-if="getParallelGroupChains(selectedNode.nodeId).length === 0" class="pg-empty">
                      暂无子节点，请框选节点后组合
                    </div>
                  </div>
                </div>

                <div class="impl-class-selector">
                  <label>选择实现类</label>
                  <el-select
                    v-model="selectedNode.implementationClass"
                    placeholder="请选择实现类"
                    style="width: 100%"
                    size="default"
                    @change="handleImplClassChange"
                  >
                    <el-option
                      v-for="impl in currentImplementations"
                      :key="impl.fullClassName"
                      :label="impl.className"
                      :value="impl.fullClassName"
                    >
                      <div class="impl-option">
                        <span class="impl-name">{{ impl.className }}</span>
                        <span class="impl-desc">{{ impl.description }}</span>
                      </div>
                    </el-option>
                  </el-select>
                </div>

                <div class="impl-brief" v-if="selectedNode.implementationClass && selectedImplementation">
                  <div class="impl-brief-name">{{ selectedImplementation.className }}</div>
                  <div class="impl-brief-desc">{{ selectedImplementation.description }}</div>
                </div>

                <template v-if="selectedNode.implementationClass && selectedImplementation?.parameters?.length">
                  <div
                    v-for="group in parameterGroups"
                    :key="group.name"
                    class="param-group"
                  >
                    <div class="param-group-title">{{ group.name }}</div>
                    <el-form label-width="100px" size="default" class="param-form">
                      <template v-for="param in group.params" :key="param.name">
                        <el-form-item
                          v-if="isParamVisible(param)"
                          :label="param.label"
                          :required="param.required"
                        >
                          <template v-if="param.inputType === 'switch'">
                            <el-switch
                              v-model="configForm[param.name]"
                              @change="onParamChange(param.name)"
                            />
                          </template>
                          <template v-else-if="param.inputType === 'datasource'">
                            <el-select
                              v-model="configForm[param.name]"
                              style="width: 100%"
                              placeholder="选择数据源连接"
                              @change="onDataSourceChange(param.name)"
                            >
                              <el-option
                                v-for="ds in dataSourceList"
                                :key="ds.id"
                                :label="`${ds.name} (${ds.type})`"
                                :value="String(ds.id)"
                              >
                                <div class="ds-option">
                                  <span class="ds-name">{{ ds.name }}</span>
                                  <el-tag size="small" :type="ds.type === 'MYSQL' ? 'primary' : ds.type === 'POSTGRESQL' ? 'success' : 'warning'" effect="dark">{{ ds.type }}</el-tag>
                                </div>
                              </el-option>
                            </el-select>
                            <div class="param-hint" v-if="!dataSourceList.length">
                              <router-link to="/datasources" class="ds-link">前往配置数据源 →</router-link>
                            </div>
                          </template>
                          <template v-else-if="param.inputType === 'table-select'">
                            <el-select
                              v-model="configForm[param.name]"
                              style="width: 100%"
                              placeholder="选择表"
                              :disabled="!configForm.connectionId"
                              :loading="tableLoading"
                              filterable
                              @change="onParamChange(param.name)"
                            >
                              <el-option
                                v-for="t in tableList"
                                :key="t"
                                :label="t"
                                :value="t"
                              />
                            </el-select>
                          </template>
                          <template v-else-if="param.inputType === 'select'">
                            <el-select
                              v-model="configForm[param.name]"
                              style="width: 100%"
                              @change="onParamChange(param.name)"
                            >
                              <el-option
                                v-for="opt in param.options"
                                :key="opt.value"
                                :label="opt.label"
                                :value="opt.value"
                              />
                            </el-select>
                          </template>
                          <template v-else-if="param.inputType === 'number'">
                            <el-input-number
                              v-model="configForm[param.name]"
                              style="width: 100%"
                              :placeholder="param.defaultValue || '0'"
                              @change="onParamChange(param.name)"
                            />
                          </template>
                          <template v-else-if="param.inputType === 'textarea'">
                            <el-input
                              v-model="configForm[param.name]"
                              type="textarea"
                              :rows="3"
                              :placeholder="param.description || ''"
                              @input="onParamChange(param.name)"
                            />
                          </template>
                          <template v-else-if="param.inputType === 'password'">
                            <el-input
                              v-model="configForm[param.name]"
                              type="password"
                              show-password
                              :placeholder="param.required ? '必填' : '可选'"
                              @input="onParamChange(param.name)"
                            />
                          </template>
                          <template v-else>
                            <el-input
                              v-model="configForm[param.name]"
                              :placeholder="param.defaultValue || (param.required ? '必填' : '可选')"
                              @input="onParamChange(param.name)"
                            />
                          </template>
                          <div class="param-hint" v-if="param.description && param.inputType !== 'textarea' && param.inputType !== 'datasource'">
                            {{ param.description }}
                          </div>
                        </el-form-item>
                      </template>
                    </el-form>
                  </div>
                </template>

                <div class="extended-config" v-if="selectedNode.implementationClass">
                  <div class="extended-header" @click="showExtendedConfig = !showExtendedConfig">
                    <div class="extended-title">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <polyline points="16 18 22 12 16 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        <polyline points="8 6 2 12 8 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      </svg>
                      扩展配置 (JSON)
                    </div>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" :class="{ rotated: showExtendedConfig }">
                      <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </div>
                  <div class="extended-content" v-if="showExtendedConfig">
                    <el-input
                      v-model="selectedNode.config"
                      type="textarea"
                      :rows="6"
                      placeholder='{"key": "value"}'
                      class="config-input"
                      @input="onRawConfigChange"
                    />
                    <div class="config-hint">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                        <path d="M12 16v-4M12 8h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      </svg>
                      可直接编辑JSON补充额外参数，上方表单字段会同步更新
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-section" v-else>
                <div class="no-impl-hint">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                    <path d="M12 16v-4M12 8h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                  <span>此节点类型暂无可用实现类</span>
                </div>
              </div>

              <div class="panel-footer">
                <button v-if="selectedNode.nodeType === 'parallel_group'" class="ungroup-btn" @click="handleUngroup(selectedNode.nodeId)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <rect x="3" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
                    <rect x="14" y="3" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
                    <rect x="3" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
                    <rect x="14" y="14" width="7" height="7" rx="1" stroke="currentColor" stroke-width="2"/>
                    <line x1="10" y1="10" x2="14" y2="14" stroke="currentColor" stroke-width="2"/>
                  </svg>
                  取消并行组
                </button>
                <button class="delete-btn" @click="handleDeleteNode(selectedNode.nodeId)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                  删除节点
                </button>
              </div>
            </div>
          </template>

          <template v-if="selectedEdge">
            <div class="panel-header">
              <h3>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                连线属性
              </h3>
              <button class="close-btn" @click="selectedEdge = null">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>

            <el-form label-width="80px" size="default" class="property-form">
              <el-form-item label="连线ID">
                <el-input :model-value="selectedEdge.edgeId" disabled />
              </el-form-item>
              <el-form-item label="源节点">
                <el-tag size="small">{{ selectedEdge.sourceNodeId }}</el-tag>
              </el-form-item>
              <el-form-item label="目标节点">
                <el-tag size="small">{{ selectedEdge.targetNodeId }}</el-tag>
              </el-form-item>
              <el-form-item label="条件表达式">
                <el-input
                    v-model="selectedEdge.condition"
                    type="textarea"
                    :rows="4"
                    placeholder="例如：data.amount > 1000"
                />
                <div class="condition-help">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                    <path d="M12 16v-4M12 8h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                  <span>支持 JavaScript 表达式，可使用 data 变量</span>
                </div>
              </el-form-item>
              <el-form-item label="连线样式">
                <el-select v-model="selectedEdge.edgeStyle" style="width: 100%">
                  <el-option label="直线" value="straight" />
                  <el-option label="折线" value="orthogonal" />
                  <el-option label="曲线" value="curved" />
                </el-select>
              </el-form-item>
              <el-form-item label="标签">
                <el-input v-model="selectedEdge.label" placeholder="可选" />
              </el-form-item>
            </el-form>

            <div class="panel-footer">
              <button class="delete-btn" @click="handleDeleteSelectedEdge">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                删除连线
              </button>
            </div>
          </template>
        </div>
      </transition>

      <FlowHelpTip v-if="showHelp" @close="showHelp = false" />
    </div>
  </div>
  <div class="flow-designer loading-state" v-else>
    <div class="loading-content">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" class="loading-spinner">
        <circle cx="12" cy="12" r="10" stroke="#667eea" stroke-width="2" opacity="0.2"/>
        <path d="M12 2a10 10 0 0 1 10 10" stroke="#667eea" stroke-width="2" stroke-linecap="round">
          <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="1s" repeatCount="indefinite"/>
        </path>
      </svg>
      <span>加载流程中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFlowStore } from '../stores/flow'
import { useTaskStore } from '../stores/task'
import { useDataSourceStore } from '../stores/datasource'
import type { FlowNode, ImplementationInfo, ParameterInfo } from '../stores/flow'
import NodePanel from '../components/NodePanel.vue'
import FlowCanvas from '../components/FlowCanvas.vue'
import CanvasToolbar from '../components/CanvasToolbar.vue'
import FlowHelpTip from '../components/FlowHelpTip.vue'

const route = useRoute()
const router = useRouter()
const flowStore = useFlowStore()
const taskStore = useTaskStore()
const dataSourceStore = useDataSourceStore()
const selectedNode = ref<FlowNode | null>(null)
const selectedEdge = ref<any>(null)
const showHelp = ref(true)
const showExtendedConfig = ref(false)
const edgeStyle = ref<'straight' | 'orthogonal' | 'curved'>('straight')
const showAnimation = ref(false)
const currentImplementations = ref<ImplementationInfo[]>([])
const configForm = ref<Record<string, any>>({})
const tableList = ref<string[]>([])
const tableLoading = ref(false)
const flowCanvasRef = ref<InstanceType<typeof FlowCanvas> | null>(null)
const executing = ref(false)
const multiSelectedNodeIds = ref<string[]>([])

const selectedParallelGroupNodes = computed(() => {
  if (!flowStore.currentFlow) return []
  return flowStore.currentFlow.nodes.filter(n => n.parentGroupId)
})

const parallelGroupList = computed(() => {
  if (!flowStore.currentFlow) return []
  const groups = new Map<string, { id: string; name: string; childCount: number }>()
  for (const node of flowStore.currentFlow.nodes) {
    if (node.nodeType === 'parallel_group') {
      const childCount = flowStore.currentFlow.nodes.filter(n => n.parentGroupId === node.nodeId).length
      groups.set(node.nodeId, { id: node.nodeId, name: node.name, childCount })
    }
  }
  return Array.from(groups.values())
})

const canvasZoomLevel = computed(() => {
  const z = (flowCanvasRef.value as any)?.zoomLevel
  return z != null ? (typeof z === 'object' && 'value' in z ? z.value : z) : 1
})

const dataSourceList = computed(() => dataSourceStore.dataSources)

const typesWithImplementation = ['data_extract', 'data_transform', 'data_load', 'condition', 'validation', 'notification', 'script', 'delay', 'parallel_group']

const hasImplementation = (nodeType: string) => {
  return typesWithImplementation.includes(nodeType)
}

const selectedImplementation = computed(() => {
  if (!selectedNode.value?.implementationClass) return null
  return currentImplementations.value.find(impl => impl.fullClassName === selectedNode.value?.implementationClass)
})

const parameterGroups = computed(() => {
  const params = selectedImplementation.value?.parameters
  if (!params?.length) return []
  const groupMap = new Map<string, ParameterInfo[]>()
  for (const p of params) {
    const g = p.group || '通用配置'
    if (!groupMap.has(g)) groupMap.set(g, [])
    groupMap.get(g)!.push(p)
  }
  return Array.from(groupMap.entries()).map(([name, params]) => ({ name, params }))
})

function isParamVisible(param: ParameterInfo): boolean {
  if (param.name === 'table' && param.inputType === 'table-select') {
    const params = selectedImplementation.value?.parameters || []
    const hasQueryMode = params.some(p => p.name === 'queryMode')
    if (hasQueryMode) {
      return configForm.value.queryMode === 'table'
    }
    return true
  }
  if (param.name === 'script' && param.inputType === 'textarea') {
    const params = selectedImplementation.value?.parameters || []
    const hasQueryMode = params.some(p => p.name === 'queryMode')
    if (hasQueryMode) {
      return configForm.value.queryMode === 'script'
    }
  }
  if (param.name === 'whereClause') {
    const params = selectedImplementation.value?.parameters || []
    const hasQueryMode = params.some(p => p.name === 'queryMode')
    if (hasQueryMode) {
      return configForm.value.queryMode === 'table'
    }
  }
  return true
}

async function onDataSourceChange(_paramName: string) {
  onParamChange(_paramName)
  tableList.value = []
  configForm.value.table = ''
  const connectionId = configForm.value.connectionId
  if (connectionId) {
    await loadTables(Number(connectionId))
  }
  syncConfig()
}

async function loadTables(connectionId: number) {
  tableLoading.value = true
  try {
    tableList.value = await dataSourceStore.getTables(connectionId)
  } catch {
    tableList.value = []
  } finally {
    tableLoading.value = false
  }
}

function parseConfig() {
  if (!selectedNode.value) return
  const raw = selectedNode.value.config
  let obj: Record<string, any> = {}
  if (raw) {
    try { obj = JSON.parse(raw) } catch { return }
  }
  const form: Record<string, any> = {}
  const params = selectedImplementation.value?.parameters || []
  for (const p of params) {
    if (p.name in obj) {
      if (p.type === 'integer' || p.type === 'long') {
        form[p.name] = Number(obj[p.name]) || 0
      } else if (p.type === 'boolean') {
        form[p.name] = obj[p.name] === true || obj[p.name] === 'true'
      } else {
        form[p.name] = obj[p.name]
      }
    } else if (p.defaultValue !== undefined && p.defaultValue !== null && p.defaultValue !== '') {
      if (p.type === 'integer' || p.type === 'long') {
        form[p.name] = Number(p.defaultValue) || 0
      } else if (p.type === 'boolean') {
        form[p.name] = p.defaultValue === 'true'
      } else {
        form[p.name] = p.defaultValue
      }
    } else {
      if (p.type === 'boolean') {
        form[p.name] = false
      } else if (p.type === 'integer' || p.type === 'long') {
        form[p.name] = undefined
      } else {
        form[p.name] = ''
      }
    }
  }
  for (const [k, v] of Object.entries(obj)) {
    if (!(k in form)) {
      form[k] = v
    }
  }
  configForm.value = form
  if (form.connectionId) {
    loadTables(Number(form.connectionId))
  }
}

function syncConfig() {
  if (!selectedNode.value) return
  const params = selectedImplementation.value?.parameters || []
  const paramNames = new Set(params.map(p => p.name))
  let existing: Record<string, any> = {}
  if (selectedNode.value.config) {
    try { existing = JSON.parse(selectedNode.value.config) } catch { /* ignore */ }
  }
  const merged: Record<string, any> = {}
  for (const [k, v] of Object.entries(existing)) {
    if (!paramNames.has(k)) {
      merged[k] = v
    }
  }
  for (const p of params) {
    if (!isParamVisible(p)) continue
    const val = configForm.value[p.name]
    if (val !== undefined && val !== '') {
      merged[p.name] = val
    }
  }
  selectedNode.value.config = JSON.stringify(merged, null, 2)
}

function onParamChange(_paramName: string) {
  if (_paramName === 'queryMode') {
    const mode = configForm.value.queryMode
    if (mode === 'script') {
      configForm.value.table = ''
      configForm.value.whereClause = ''
    } else {
      configForm.value.script = ''
    }
  }
  syncConfig()
}

function onRawConfigChange() {
  parseConfig()
}

watch(selectedNode, async (node) => {
  if (node) {
    await loadImplementations(node.nodeType)
    parseConfig()
  }
})

watch(() => selectedNode.value?.implementationClass, () => {
  parseConfig()
})

async function loadImplementations(nodeType: string) {
  if (!hasImplementation(nodeType)) {
    currentImplementations.value = []
    return
  }
  const implementations = await flowStore.loadImplementationsByType(nodeType)
  currentImplementations.value = implementations || []
}

const demoNodeStatus = computed(() => {
  if (!showAnimation.value || !flowStore.currentFlow) return {}

  const status: Record<string, any> = {}
  const nodes = flowStore.currentFlow.nodes
  if (nodes.length > 0) {
    status[nodes[0].nodeId] = 'SUCCESS'
    if (nodes.length > 1) {
      status[nodes[1].nodeId] = 'RUNNING'
    }
  }
  return status
})

onMounted(async () => {
  const id = route.params.id as string
  if (id) {
    await flowStore.loadFlow(Number(id))
  }
  if (!flowStore.currentFlow) {
    flowStore.createNewFlow()
  }
  await flowStore.loadImplementations()
  try {
    await dataSourceStore.loadDataSources()
  } catch { /* ignore */ }
})

function handleAddNode(type: string) {
  const x = 300 + Math.random() * 200
  const y = 100 + Math.random() * 400
  flowStore.addNode(type as any, x, y)
}

function handleNodeClick(nodeId: string) {
  selectedEdge.value = null
}

function handleNodeDblClick(nodeId: string) {
  const node = flowStore.currentFlow?.nodes.find(n => n.nodeId === nodeId)
  selectedNode.value = node || null
  showExtendedConfig.value = false
}

function handleEdgeClick(edge: any) {
  selectedEdge.value = edge
  selectedNode.value = null
}

function handleSelectionChange(nodeIds: string[]) {
  multiSelectedNodeIds.value = nodeIds
  if (nodeIds.length === 0) {
    // 不清除已打开的配置面板，只清除多选
  }
}

function handleGroupAsParallel() {
  if (!flowStore.currentFlow || multiSelectedNodeIds.value.length < 2) return

  const groupCount = flowStore.currentFlow.nodes.filter(n => n.nodeType === 'parallel_group').length + 1
  const groupId = `parallel_group_${groupCount}`

  const childNodes = flowStore.currentFlow.nodes.filter(n => multiSelectedNodeIds.value.includes(n.nodeId))
  if (childNodes.some(n => n.nodeType === 'start' || n.nodeType === 'end')) {
    ElMessage.warning('开始和结束节点不能加入并行组')
    return
  }

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  for (const child of childNodes) {
    minX = Math.min(minX, child.positionX || 0)
    minY = Math.min(minY, child.positionY || 0)
    maxX = Math.max(maxX, child.positionX || 0)
    maxY = Math.max(maxY, child.positionY || 0)
  }

  flowStore.currentFlow.nodes.push({
    nodeId: groupId,
    nodeType: 'parallel_group',
    name: `并行组 ${groupCount}`,
    positionX: (minX + maxX) / 2,
    positionY: minY - 80,
    config: JSON.stringify({ successStrategy: 'ALL_SUCCESS' }),
    implementationClass: 'com.migration.engine.ParallelGroupExecutor',
  })

  for (const child of childNodes) {
    child.parentGroupId = groupId
  }

  multiSelectedNodeIds.value = []
  ElMessage.success(`已创建并行组，包含 ${childNodes.length} 个节点`)
}

function handleClearSelection() {
  multiSelectedNodeIds.value = []
  flowCanvasRef.value?.clearSelection()
}

function handleUngroup(groupId: string) {
  if (!flowStore.currentFlow) return
  for (const node of flowStore.currentFlow.nodes) {
    if (node.parentGroupId === groupId) {
      node.parentGroupId = undefined
    }
  }
  flowStore.removeNode(groupId)
  ElMessage.success('已取消并行组')
}

function getChildNodes(groupId: string) {
  if (!flowStore.currentFlow) return []
  return flowStore.currentFlow.nodes.filter(n => n.parentGroupId === groupId)
}

function getParallelGroupChains(groupId: string) {
  if (!flowStore.currentFlow) return []
  const children = flowStore.currentFlow.nodes.filter(n => n.parentGroupId === groupId)
  if (children.length === 0) return []

  const childIds = new Set(children.map(n => n.nodeId))
  const edges = flowStore.currentFlow.edges.filter(
    e => childIds.has(e.sourceNodeId) && childIds.has(e.targetNodeId)
  )

  const nodesWithIncoming = new Set(edges.map(e => e.targetNodeId))
  const entryNodes = children.filter(n => !nodesWithIncoming.has(n.nodeId))

  const outgoingMap = new Map<string, string[]>()
  for (const edge of edges) {
    if (!outgoingMap.has(edge.sourceNodeId)) {
      outgoingMap.set(edge.sourceNodeId, [])
    }
    outgoingMap.get(edge.sourceNodeId)!.push(edge.targetNodeId)
  }

  const chains: Array<Array<typeof children[0]>> = []
  const visited = new Set<string>()

  for (const entry of entryNodes) {
    const chain: typeof children = []
    let currentId: string | undefined = entry.nodeId
    while (currentId && !visited.has(currentId)) {
      visited.add(currentId)
      const node = children.find(n => n.nodeId === currentId)
      if (node) chain.push(node)
      const nextIds = outgoingMap.get(currentId)
      currentId = nextIds && nextIds.length > 0 ? nextIds[0] : undefined
    }
    if (chain.length > 0) chains.push(chain)
  }

  for (const child of children) {
    if (!visited.has(child.nodeId)) {
      chains.push([child])
    }
  }

  return chains
}

function getNodeIcon(type: string): string {
  const icons: Record<string, string> = {
    start: '▶', end: '■', data_extract: '⬇', data_transform: '⇄',
    data_load: '⬆', validation: '✓', condition: '◇', parallel: '⊞',
    merge: '⊟', parallel_group: '⧉', notification: '🔔', script: '⟨⟩', delay: '⏱',
  }
  return icons[type] || '○'
}

function handleDeleteSelectedEdge() {
  if (selectedEdge.value) {
    flowStore.removeEdge(selectedEdge.value.edgeId)
    selectedEdge.value = null
  }
}

function handleNodeMove(nodeId: string, x: number, y: number) {
  flowStore.updateNodePosition(nodeId, x, y)
}

function handleConnect(sourceId: string, targetId: string) {
  const exists = flowStore.currentFlow?.edges.some(
      e => e.sourceNodeId === sourceId && e.targetNodeId === targetId
  )
  if (exists) {
    ElMessage.warning('该连接已存在')
    return
  }
  flowStore.addEdge(sourceId, targetId, undefined, edgeStyle.value)
  ElMessage.success('连接成功')
}

function handleDeleteNode(nodeId: string) {
  if (confirm('确定要删除这个节点吗？相关的连线也会被删除。')) {
    flowStore.removeNode(nodeId)
    if (selectedNode.value?.nodeId === nodeId) {
      selectedNode.value = null
    }
  }
}

function handleDeleteEdge(edgeId: string) {
  flowStore.removeEdge(edgeId)
}

function handleImplClassChange(className: string) {
  if (selectedNode.value) {
    selectedNode.value.implementationClass = className
    selectedNode.value.config = ''
    configForm.value = {}
    showExtendedConfig.value = false
    parseConfig()
    syncConfig()
  }
}

async function handleSave() {
  if (!flowStore.currentFlow) return
  try {
    await flowStore.saveFlow(flowStore.currentFlow)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handleExecute() {
  if (!flowStore.currentFlow) return

  const hasNodes = flowStore.currentFlow.nodes.length > 0
  if (!hasNodes) {
    ElMessage.warning('流程中没有任何节点，请先设计流程')
    return
  }

  try {
    await ElMessageBox.confirm(
      '执行前会先保存当前流程，确认要立即执行吗？',
      '确认执行',
      { confirmButtonText: '确认执行', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  executing.value = true
  try {
    await flowStore.saveFlow(flowStore.currentFlow)
    const flowId = flowStore.currentFlow.id
    if (!flowId) {
      ElMessage.error('流程未保存，无法执行')
      return
    }
    const task = await taskStore.startTask(
      flowStore.currentFlow.name + ' - 执行',
      flowId
    )
    ElMessage.success('任务已创建，正在执行...')
    router.push(`/tasks/${task.id}`)
  } catch (e: any) {
    ElMessage.error('执行失败: ' + (e.message || '未知错误'))
  } finally {
    executing.value = false
  }
}
</script>

<style scoped>
.flow-designer {
  height: calc(100vh - 136px);
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.toolbar-left, .toolbar-center, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #667eea;
  color: white;
  transform: translateX(-2px);
}

.toolbar-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

.flow-name-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: none;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.flow-name-input :deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

.flow-name-input :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tool-btn:hover {
  background: #667eea;
  color: white;
}

.tool-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.tool-btn.save {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.tool-btn.save:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.tool-btn.execute {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.3);
}

.tool-btn.execute:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
}

.tool-btn.execute:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.designer-body {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
  position: relative;
}

.canvas-wrapper {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  position: relative;
}

.multi-select-bar {
  position: absolute;
  top: 52px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  border: 1px solid #e2e8f0;
}

.select-info {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.group-btn {
  background: linear-gradient(135deg, #9B59B6 0%, #8E44AD 100%);
  color: white;
}

.group-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(155, 89, 182, 0.4);
}

.cancel-btn {
  background: #f1f5f9;
  color: #64748b;
}

.cancel-btn:hover {
  background: #e2e8f0;
}

.parallel-group-info {
  margin-bottom: 16px;
}

.pg-children {
  background: linear-gradient(135deg, #f5f0ff 0%, #ede5ff 100%);
  border-radius: 10px;
  padding: 12px;
  border-left: 3px solid #9B59B6;
}

.pg-children-title {
  font-size: 13px;
  font-weight: 600;
  color: #9B59B6;
  margin-bottom: 8px;
}

.pg-child-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 12px;
}

.pg-child-icon {
  font-size: 14px;
}

.pg-child-name {
  flex: 1;
  font-weight: 500;
  color: #1e293b;
}

.pg-child-type {
  color: #9B59B6;
  font-size: 11px;
}

.pg-empty {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  padding: 8px;
}

.pg-chain {
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  padding: 8px 10px;
  border-left: 3px solid #9B59B6;
}

.pg-chain-header {
  font-size: 11px;
  font-weight: 700;
  color: #9B59B6;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.pg-chain-nodes {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.pg-chain-node {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(155, 89, 182, 0.08);
  border-radius: 4px;
  font-size: 11px;
}

.pg-chain-arrow {
  color: #9B59B6;
  font-size: 12px;
  font-weight: bold;
  opacity: 0.6;
}

.ungroup-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #f0e6ff 0%, #e6d5ff 100%);
  color: #9B59B6;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
}

.ungroup-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(155, 89, 182, 0.3);
}

.property-panel {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.panel-body {
  padding: 20px 24px;
  flex: 1;
}

.form-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.property-form {
  padding: 20px 24px;
  flex: 1;
}

.property-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
}

.property-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #e2e8f0;
}

.property-form :deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

.property-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.impl-class-selector {
  margin-bottom: 16px;
}

.impl-class-selector label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 8px;
}

.impl-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.impl-name {
  font-weight: 600;
  color: #1e293b;
}

.impl-desc {
  font-size: 12px;
  color: #64748b;
}

.impl-brief {
  background: linear-gradient(135deg, #f0f4ff 0%, #e8edff 100%);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border-left: 3px solid #667eea;
}

.impl-brief-name {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 4px;
}

.impl-brief-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.param-group {
  margin-bottom: 20px;
}

.param-group-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.param-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.param-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.param-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.param-form :deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

.param-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.param-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
  font-size: 13px;
}

.param-form :deep(.el-textarea__inner:hover) {
  border-color: #667eea;
}

.param-form :deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.param-form :deep(.el-input-number) {
  width: 100%;
}

.param-form :deep(.el-input-number .el-input__wrapper) {
  border-radius: 8px;
  padding-left: 8px;
  padding-right: 8px;
}

.param-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

.param-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  line-height: 1.4;
}

.ds-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ds-name {
  font-weight: 500;
  color: #1e293b;
}

.ds-link {
  color: #667eea;
  text-decoration: none;
  font-size: 12px;
  font-weight: 500;
}

.ds-link:hover {
  text-decoration: underline;
}

.extended-config {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
}

.extended-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.extended-header:hover {
  background: rgba(102, 126, 234, 0.15);
}

.extended-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
}

.extended-header svg:last-child {
  color: #64748b;
  transition: transform 0.3s ease;
}

.extended-header svg.rotated {
  transform: rotate(180deg);
}

.extended-content {
  padding: 0 16px 16px;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.no-impl-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  color: #64748b;
  font-size: 13px;
}

.no-impl-hint svg {
  color: #667eea;
}

.config-input :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #e2e8f0;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
}

.config-input :deep(.el-textarea__inner:focus) {
  background: rgba(255, 255, 255, 0.15);
  border-color: #667eea;
}

.config-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
  line-height: 1.5;
}

.config-hint svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #667eea;
}

.condition-help {
  margin-top: 8px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.condition-help svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #667eea;
}

.panel-footer {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  position: sticky;
  bottom: 0;
  background: white;
}

.delete-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.3);
}

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: all 0.3s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

@media (max-width: 1200px) {
  .property-panel {
    width: 360px;
  }
}

@media (max-width: 992px) {
  .property-panel {
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    z-index: 10;
    border-radius: 16px 0 0 16px;
  }
}

@media (max-width: 768px) {
  .designer-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left, .toolbar-center, .toolbar-right {
    justify-content: center;
    flex-wrap: wrap;
  }

  .property-panel {
    width: 100%;
  }

  .tool-btn span, .back-btn span {
    display: none;
  }
}

.loading-state {
  justify-content: center;
  align-items: center;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
