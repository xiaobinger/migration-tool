import { defineStore } from 'pinia'
import api from '../api/http'
import type { NodeTypeCode } from '../types'

export interface FlowNode {
  id?: number
  nodeId: string
  flowDefinitionId?: number
  nodeType: NodeTypeCode
  name: string
  description?: string
  config?: string
  positionX?: number
  positionY?: number
  sortOrder?: number
  implementationClass?: string
  parentGroupId?: string
}

export interface FlowEdge {
  id?: number
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  condition?: string
  label?: string
  edgeStyle?: 'straight' | 'orthogonal' | 'curved'
}

export interface FlowDefinition {
  id?: number
  name: string
  description?: string
  version?: number
  enabled?: boolean
  createdBy?: string
  nodes: FlowNode[]
  edges: FlowEdge[]
}

export interface OptionItem {
  label: string
  value: string
}

export interface ParameterInfo {
  name: string
  label: string
  type: string
  inputType: string
  required: boolean
  defaultValue?: string
  description?: string
  options?: OptionItem[]
  group?: string
}

export interface ImplementationInfo {
  className: string
  fullClassName: string
  description: string
  nodeType: string
  parameters?: ParameterInfo[]
}

export const useFlowStore = defineStore('flow', {
  state: () => ({
    flows: [] as FlowDefinition[],
    currentFlow: null as FlowDefinition | null,
    implementations: {} as Record<string, ImplementationInfo[]>
  }),

  actions: {
    async loadFlows() {
      try {
        const res = await api.get('/flows')
        this.flows = res as any
      } catch (error) {
        console.error('加载流程列表失败', error)
      }
    },

    async loadFlow(id: number) {
      try {
        const res = await api.get(`/flows/${id}`)
        this.currentFlow = res as any
      } catch (error) {
        console.error('加载流程失败', error)
      }
    },

    async loadImplementations() {
      try {
        const res = await api.get('/flows/implementations')
        this.implementations = res as any
      } catch (error) {
        console.error('加载实现类列表失败', error)
      }
    },

    async loadImplementationsByType(nodeType: string) {
      try {
        const res = await api.get(`/flows/implementations/${nodeType}`)
        this.implementations[nodeType.toUpperCase()] = res as any
        return res as any as ImplementationInfo[]
      } catch (error) {
        console.error('加载实现类列表失败', error)
        return []
      }
    },

    createNewFlow() {
      this.currentFlow = {
        name: '新建流程',
        description: '',
        version: 1,
        enabled: false,
        nodes: [
          {
            nodeId: 'node_start',
            nodeType: 'start',
            name: '开始',
            positionX: 100,
            positionY: 200
          }
        ],
        edges: []
      }
    },

    addNode(type: NodeTypeCode, x: number, y: number) {
      if (!this.currentFlow) return

      const typeCount = this.currentFlow.nodes.filter(n => n.nodeType === type).length + 1
      const nodeId = `${type}_${typeCount}`

      this.currentFlow.nodes.push({
        nodeId,
        nodeType: type,
        name: `${this.getNodeTypeName(type)} ${typeCount}`,
        positionX: x,
        positionY: y,
        implementationClass: ''
      })
    },

    removeNode(nodeId: string) {
      if (!this.currentFlow) return
      this.currentFlow.nodes = this.currentFlow.nodes.filter(n => n.nodeId !== nodeId)
      this.currentFlow.edges = this.currentFlow.edges.filter(
        e => e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId
      )
    },

    updateNodePosition(nodeId: string, x: number, y: number) {
      if (!this.currentFlow) return
      const node = this.currentFlow.nodes.find(n => n.nodeId === nodeId)
      if (node) {
        node.positionX = x
        node.positionY = y
      }
    },

    addEdge(sourceId: string, targetId: string, condition?: string, style?: string) {
      if (!this.currentFlow) return

      const edgeId = `edge_${Date.now()}`
      this.currentFlow.edges.push({
        edgeId,
        sourceNodeId: sourceId,
        targetNodeId: targetId,
        condition,
        edgeStyle: (style as 'straight' | 'orthogonal' | 'curved') || 'straight'
      })
    },

    removeEdge(edgeId: string) {
      if (!this.currentFlow) return
      this.currentFlow.edges = this.currentFlow.edges.filter(e => e.edgeId !== edgeId)
    },

    async saveFlow(flow: FlowDefinition) {
      try {
        if (flow.id) {
          await api.put(`/flows/${flow.id}`, flow)
        } else {
          const res = await api.post('/flows', flow)
          flow.id = (res as any).id
        }
        await this.loadFlows()
      } catch (error) {
        console.error('保存流程失败', error)
        throw error
      }
    },

    async deleteFlow(id: number) {
      try {
        await api.delete(`/flows/${id}`)
        await this.loadFlows()
      } catch (error) {
        console.error('删除流程失败', error)
        throw error
      }
    },

    getNodeTypeName(type: NodeTypeCode): string {
      const typeMap: Record<string, string> = {
        start: '开始',
        end: '结束',
        data_extract: '数据提取',
        data_transform: '数据转换',
        data_load: '数据加载',
        validation: '数据校验',
        condition: '条件判断',
        parallel: '并行网关',
        merge: '合并网关',
        parallel_group: '并行组',
        notification: '通知',
        script: '脚本',
        delay: '延时'
      }
      return typeMap[type] || type
    }
  }
})
