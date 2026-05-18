<template>
  <svg
      class="flow-canvas"
      ref="svgRef"
      :style="{ cursor: canvasCursor }"
      @mousedown="handleCanvasMouseDown"
      @mousemove="handleCanvasMouseMove"
      @mouseup="handleCanvasMouseUp"
      @wheel.prevent="handleWheel"
      @drop.prevent="handleDrop"
      @dragover.prevent
  >
    <defs>
      <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
        <polygon points="0 0, 10 3.5, 0 7" fill="#C0C4CC" />
      </marker>
      <marker id="arrowhead-active" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
        <polygon points="0 0, 10 3.5, 0 7" fill="#409EFF" />
      </marker>
      <marker id="arrowhead-success" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
        <polygon points="0 0, 10 3.5, 0 7" fill="#67C23A" />
      </marker>
      <marker id="arrowhead-failed" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
        <polygon points="0 0, 10 3.5, 0 7" fill="#F56C6C" />
      </marker>
      <filter id="glow">
        <feGaussianBlur stdDeviation="3" result="coloredBlur" />
        <feMerge>
          <feMergeNode in="coloredBlur" />
          <feMergeNode in="SourceGraphic" />
        </feMerge>
      </filter>
      <g id="success-icon">
        <circle cx="0" cy="0" r="10" fill="#67C23A" />
        <path d="M -5 0 L -2 3 L 5 -4" stroke="white" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />
      </g>
      <g id="failed-icon">
        <circle cx="0" cy="0" r="10" fill="#F56C6C" />
        <line x1="-4" y1="-4" x2="4" y2="4" stroke="white" stroke-width="2" stroke-linecap="round" />
        <line x1="-4" y1="4" x2="4" y2="-4" stroke="white" stroke-width="2" stroke-linecap="round" />
      </g>
      <g id="running-icon">
        <circle cx="0" cy="0" r="10" fill="#409EFF" opacity="0.3">
          <animate attributeName="r" values="8;12;8" dur="1.5s" repeatCount="indefinite" />
          <animate attributeName="opacity" values="0.3;0.6;0.3" dur="1.5s" repeatCount="indefinite" />
        </circle>
        <circle cx="0" cy="0" r="6" fill="#409EFF" />
      </g>
      <g id="skipped-icon">
        <circle cx="0" cy="0" r="10" fill="#C0C4CC" />
        <line x1="-4" y1="-4" x2="4" y2="4" stroke="white" stroke-width="2" stroke-linecap="round" />
        <line x1="4" y1="-4" x2="-4" y2="4" stroke="white" stroke-width="2" stroke-linecap="round" />
      </g>
      <radialGradient id="particle-gradient">
        <stop offset="0%" stop-color="#409EFF" stop-opacity="0" />
        <stop offset="50%" stop-color="#409EFF" stop-opacity="1" />
        <stop offset="100%" stop-color="#409EFF" stop-opacity="0" />
      </radialGradient>
      <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
        <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#f0f0f0" stroke-width="0.5" />
      </pattern>
    </defs>

    <!-- 背景（不随缩放平移） -->
    <rect width="100%" height="100%" fill="#fafbfc" />

    <!-- 缩放/平移容器 -->
    <g :transform="`translate(${panX}, ${panY}) scale(${zoomLevel})`">
      <!-- 网格背景 -->
      <rect :x="gridBounds.x" :y="gridBounds.y" :width="gridBounds.w" :height="gridBounds.h" fill="url(#grid)" />

      <!-- 并行组容器 -->
      <g v-for="[groupId, bounds] in parallelGroupBounds" :key="'pg_' + groupId">
        <rect
            :x="bounds.x" :y="bounds.y" :width="bounds.w" :height="bounds.h"
            rx="12" ry="12"
            fill="rgba(155, 89, 182, 0.06)"
            stroke="#9B59B6"
            stroke-width="2"
            stroke-dasharray="8,4"
            opacity="0.8"
        />
        <text
            :x="bounds.x + 12" :y="bounds.y + 18"
            fill="#9B59B6" font-size="11" font-weight="600" opacity="0.7"
        >并行组: {{ getParallelGroupName(groupId) }}</text>
      </g>

      <!-- 框选矩形 -->
      <rect
          v-if="rubberBand && isRubberBanding"
          :x="Math.min(rubberBand.startX, rubberBand.endX)"
          :y="Math.min(rubberBand.startY, rubberBand.endY)"
          :width="Math.abs(rubberBand.endX - rubberBand.startX)"
          :height="Math.abs(rubberBand.endY - rubberBand.startY)"
          fill="rgba(64, 158, 255, 0.08)"
          stroke="#409EFF"
          stroke-width="1.5"
          stroke-dasharray="6,3"
          rx="4"
      />

      <!-- 边 -->
      <g v-for="edge in flow.edges" :key="edge.edgeId">
        <path
            :d="getEdgePath(edge.sourceNodeId, edge.targetNodeId, edge.edgeStyle || 'straight')"
            :stroke="getEdgeColor(edge)"
            :stroke-width="isEdgeActive(edge) ? 3 : 2"
            :marker-end="getEdgeMarker(edge)"
            :stroke-dasharray="isEdgeActive(edge) ? ''  : 'none'"
            class="edge-line"
            fill="none"
            @click.stop="$emit('edge-click', edge)"
            @click.right.prevent="$emit('delete-edge', edge.edgeId)"
        />

        <circle
            v-if="shouldShowAnimation(edge)"
            r="4"
            :fill="getEdgeParticleColor(edge)"
            filter="url(#glow)"
        >
          <animateMotion
              :dur="`${getAnimationDuration(edge)}s`"
              :path="getEdgePath(edge.sourceNodeId, edge.targetNodeId, edge.edgeStyle || 'straight')"
              repeatCount="indefinite"
              keyPoints="0;1"
              keyTimes="0;1"
              calcMode="linear"
          />
        </circle>

        <g v-if="edge.label || edge.condition">
          <rect
              v-if="edge.condition"
              :x="(getNodePosition(edge.sourceNodeId).x + getNodePosition(edge.targetNodeId).x) / 2 - 50"
              :y="(getNodePosition(edge.sourceNodeId).y + getNodePosition(edge.targetNodeId).y) / 2 - 12"
              width="100"
              height="24"
              rx="6"
              fill="#FFF7E6"
              stroke="#FAAD14"
              stroke-width="1.5"
              opacity="0.95"
          />
          <text
              v-if="edge.condition"
              :x="(getNodePosition(edge.sourceNodeId).x + getNodePosition(edge.targetNodeId).x) / 2"
              :y="(getNodePosition(edge.sourceNodeId).y + getNodePosition(edge.targetNodeId).y) / 2 + 4"
              text-anchor="middle"
              fill="#D46B08"
              font-size="11"
              font-weight="600"
          >{{ edge.condition }}</text>
          <text
              v-else-if="edge.label"
              :x="(getNodePosition(edge.sourceNodeId).x + getNodePosition(edge.targetNodeId).x) / 2"
              :y="(getNodePosition(edge.sourceNodeId).y + getNodePosition(edge.targetNodeId).y) / 2 - 8"
              text-anchor="middle"
              fill="#909399"
              font-size="11"
          >{{ edge.label }}</text>
        </g>
      </g>

      <!-- 连线中的临时线 -->
      <path
          v-if="connecting"
          :d="getConnectingPath()"
          stroke="#409EFF"
          stroke-width="2"
          stroke-dasharray="5,5"
          fill="none"
          marker-end="url(#arrowhead-active)"
      />

      <!-- 节点 -->
      <g
          v-for="node in flow.nodes"
          :key="node.nodeId"
          :transform="`translate(${node.positionX || 0}, ${node.positionY || 0})`"
          @mousedown="handleNodeMouseDown($event, node.nodeId)"
          @click.stop="$emit('node-click', node.nodeId)"
          @dblclick.stop="$emit('node-dblclick', node.nodeId)"
          @contextmenu.prevent="handleNodeContextMenu($event, node.nodeId)"
          class="node-group"
      >
        <rect
            :x="-70" :y="-25" width="140" height="50" rx="8"
            :fill="getNodeBgColor(node)"
            :stroke="selectedNodeIds.has(node.nodeId) ? '#409EFF' : getNodeBorderColor(node)"
            :stroke-width="selectedNodeIds.has(node.nodeId) ? 3 : (isNodeActive(node.nodeId) ? 3 : 1.5)"
            :filter="isNodeActive(node.nodeId) ? 'url(#glow)' : ''"
            class="node-rect"
        />
        <g
            v-if="!isNodeActive(node.nodeId)"
            class="delete-btn"
            @click.stop="$emit('delete-node', node.nodeId)"
        >
          <circle cx="60" cy="-15" r="8" fill="#F56C6C" opacity="0" />
          <text x="60" y="-11" text-anchor="middle" fill="white" font-size="12" opacity="0">×</text>
        </g>
        <circle
            cx="-45" cy="0" r="12"
            :fill="getNodeTypeConfig(node.nodeType).color"
            opacity="0.2"
        />
        <text x="-45" y="5" text-anchor="middle" :fill="getNodeTypeConfig(node.nodeType).color" font-size="14">
          {{ getNodeIcon(node.nodeType) }}
        </text>
        <text x="5" y="5" text-anchor="middle" :fill="getNodeStatus(node.nodeId) === 'SKIPPED' ? '#C0C4CC' : '#303133'" font-size="13" font-weight="500">
          {{ node.name }}
        </text>
        <g v-if="getNodeStatus(node.nodeId)" :transform="`translate(55, -15)`">
          <use v-if="getNodeStatus(node.nodeId) === 'SUCCESS'" href="#success-icon" />
          <use v-else-if="getNodeStatus(node.nodeId) === 'FAILED'" href="#failed-icon" />
          <use v-else-if="getNodeStatus(node.nodeId) === 'RUNNING'" href="#running-icon" />
          <use v-else-if="getNodeStatus(node.nodeId) === 'SKIPPED'" href="#skipped-icon" />
          <circle v-else cx="0" cy="0" r="6" :fill="getStatusColor(getNodeStatus(node.nodeId)!)" />
        </g>
        <circle
            cx="0" cy="-25" r="6"
            fill="#409EFF"
            opacity="0.8"
            class="port port-top"
            @mouseenter="handlePortEnter($event)"
            @mouseleave="handlePortLeave($event)"
            @mousedown="handlePortMouseDown($event, node.nodeId, 'top')"
            @mouseup.stop="handlePortMouseUp($event, node.nodeId)"
        />
        <circle
            cx="0" cy="25" r="6"
            fill="#409EFF"
            opacity="0.8"
            class="port port-bottom"
            @mouseenter="handlePortEnter($event)"
            @mouseleave="handlePortLeave($event)"
            @mousedown="handlePortMouseDown($event, node.nodeId, 'bottom')"
            @mouseup.stop="handlePortMouseUp($event, node.nodeId)"
        />
        <circle
            cx="-70" cy="0" r="6"
            fill="#409EFF"
            opacity="0.8"
            class="port port-left"
            @mouseenter="handlePortEnter($event)"
            @mouseleave="handlePortLeave($event)"
            @mousedown="handlePortMouseDown($event, node.nodeId, 'left')"
            @mouseup.stop="handlePortMouseUp($event, node.nodeId)"
        />
        <circle
            cx="70" cy="0" r="6"
            fill="#409EFF"
            opacity="0.8"
            class="port port-right"
            @mouseenter="handlePortEnter($event)"
            @mouseleave="handlePortLeave($event)"
            @mousedown="handlePortMouseDown($event, node.nodeId, 'right')"
            @mouseup.stop="handlePortMouseUp($event, node.nodeId)"
        />
      </g>
    </g>
  </svg>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getNodeTypeConfig } from '../types'
import type { NodeTypeCode, TaskStatusCode } from '../types'
import type { FlowDefinition, FlowNode } from '../stores/flow'

const props = defineProps<{
  flow: FlowDefinition
  nodeStatusMap: Record<string, TaskStatusCode>
  activeNodeId: string | null
  showAnimation?: boolean
  executedEdgeIds?: Set<string>
}>()

const emit = defineEmits<{
  'node-click': [nodeId: string]
  'node-dblclick': [nodeId: string]
  'node-move': [nodeId: string, x: number, y: number]
  'connect': [sourceId: string, targetId: string]
  'delete-node': [nodeId: string]
  'delete-edge': [edgeId: string]
  'edge-click': [edge: any]
  'selection-change': [nodeIds: string[]]
}>()

const svgRef = ref<SVGSVGElement | null>(null)
const zoomLevel = ref(1)
const panX = ref(0)
const panY = ref(0)
const panning = ref<{ startX: number; startY: number; startPanX: number; startPanY: number } | null>(null)
const spacePressed = ref(false)

const canvasCursor = computed(() => {
  if (panning.value) return 'grabbing'
  if (spacePressed.value) return 'grab'
  return 'default'
})

const dragging = ref<{ nodeId: string; startX: number; startY: number; nodeStartX: number; nodeStartY: number } | null>(null)
const connecting = ref<{
  sourceId: string;
  sourcePort: string;
  startX: number;
  startY: number;
  currentX: number;
  currentY: number;
} | null>(null)

const selectedNodeIds = ref<Set<string>>(new Set())
const rubberBand = ref<{ startX: number; startY: number; endX: number; endY: number } | null>(null)
const isRubberBanding = ref(false)
const rubberBandThreshold = 5

const parallelGroups = computed(() => {
  const groups = new Map<string, FlowNode[]>()
  for (const node of props.flow.nodes) {
    if (node.parentGroupId) {
      if (!groups.has(node.parentGroupId)) {
        groups.set(node.parentGroupId, [])
      }
      groups.get(node.parentGroupId)!.push(node)
    }
  }
  return groups
})

const parallelGroupBounds = computed(() => {
  const bounds = new Map<string, { x: number; y: number; w: number; h: number }>()
  for (const [groupId, children] of parallelGroups.value.entries()) {
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const child of children) {
      const x = child.positionX || 0
      const y = child.positionY || 0
      minX = Math.min(minX, x - 90)
      minY = Math.min(minY, y - 40)
      maxX = Math.max(maxX, x + 90)
      maxY = Math.max(maxY, y + 40)
    }
    bounds.set(groupId, { x: minX, y: minY, w: maxX - minX, h: maxY - minY })
  }
  return bounds
})

const gridBounds = computed(() => {
  const padding = 2000
  return {
    x: -panX.value / zoomLevel.value - padding,
    y: -panY.value / zoomLevel.value - padding,
    w: (svgRef.value?.clientWidth || 1200) / zoomLevel.value + padding * 2,
    h: (svgRef.value?.clientHeight || 600) / zoomLevel.value + padding * 2,
  }
})

function screenToSvg(clientX: number, clientY: number): { x: number; y: number } {
  if (!svgRef.value) return { x: clientX, y: clientY }
  const rect = svgRef.value.getBoundingClientRect()
  return {
    x: (clientX - rect.left - panX.value) / zoomLevel.value,
    y: (clientY - rect.top - panY.value) / zoomLevel.value,
  }
}

function getNodePosition(nodeId: string) {
  const node = props.flow.nodes.find(n => n.nodeId === nodeId)
  return { x: node?.positionX || 0, y: node?.positionY || 0 }
}

function getNodeStatus(nodeId: string): TaskStatusCode | undefined {
  return props.nodeStatusMap[nodeId]
}

function isNodeActive(nodeId: string): boolean {
  return props.activeNodeId === nodeId || props.nodeStatusMap[nodeId] === 'RUNNING'
}

function isEdgeActive(edge: any): boolean {
  if (props.executedEdgeIds && props.executedEdgeIds.size > 0) {
    return props.executedEdgeIds.has(edge.edgeId)
  }
  const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
  return sourceStatus === 'SUCCESS' || sourceStatus === 'RUNNING' || sourceStatus === 'FAILED'
}

function shouldShowAnimation(edge: any): boolean {
  if (props.executedEdgeIds && props.executedEdgeIds.size > 0) {
    if (!props.executedEdgeIds.has(edge.edgeId)) return false
    const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
    if (sourceStatus === 'FAILED') return false
    if (props.showAnimation) return true
    return true
  }
  const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
  if (sourceStatus === 'FAILED' || sourceStatus === 'SKIPPED') return false
  if (props.showAnimation) return true
  return isEdgeActive(edge)
}

function getEdgeParticleColor(edge: any): string {
  const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
  if (sourceStatus === 'SUCCESS') return '#67C23A'
  if (sourceStatus === 'FAILED') return '#F56C6C'
  return '#409EFF'
}

function getNodeBgColor(node: FlowNode): string {
  const status = props.nodeStatusMap[node.nodeId]
  if (status === 'RUNNING') return '#ECF5FF'
  if (status === 'SUCCESS') return '#F0F9EB'
  if (status === 'FAILED') return '#FEF0F0'
  if (status === 'SKIPPED') return '#F5F7FA'
  return '#FFFFFF'
}

function getNodeBorderColor(node: FlowNode): string {
  const status = props.nodeStatusMap[node.nodeId]
  if (status === 'RUNNING') return '#409EFF'
  if (status === 'SUCCESS') return '#67C23A'
  if (status === 'FAILED') return '#F56C6C'
  if (status === 'SKIPPED') return '#C0C4CC'
  return getNodeTypeConfig(node.nodeType).color
}

function getEdgeColor(edge: any): string {
  if (props.executedEdgeIds && props.executedEdgeIds.size > 0) {
    if (!props.executedEdgeIds.has(edge.edgeId)) return '#E4E7ED'
    const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
    if (sourceStatus === 'SUCCESS') return '#67C23A'
    if (sourceStatus === 'RUNNING') return '#409EFF'
    if (sourceStatus === 'FAILED') return '#F56C6C'
    return '#67C23A'
  }
  const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
  if (sourceStatus === 'SUCCESS') return '#67C23A'
  if (sourceStatus === 'RUNNING') return '#409EFF'
  if (sourceStatus === 'FAILED') return '#F56C6C'
  if (sourceStatus === 'SKIPPED') return '#E4E7ED'
  return '#C0C4CC'
}

function getEdgeMarker(edge: any): string {
  if (props.executedEdgeIds && props.executedEdgeIds.size > 0) {
    if (!props.executedEdgeIds.has(edge.edgeId)) return 'url(#arrowhead)'
    const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
    if (sourceStatus === 'SUCCESS') return 'url(#arrowhead-success)'
    if (sourceStatus === 'FAILED') return 'url(#arrowhead-failed)'
    if (sourceStatus === 'RUNNING') return 'url(#arrowhead-active)'
    return 'url(#arrowhead-success)'
  }
  const sourceStatus = props.nodeStatusMap[edge.sourceNodeId]
  if (sourceStatus === 'SUCCESS') return 'url(#arrowhead-success)'
  if (sourceStatus === 'FAILED') return 'url(#arrowhead-failed)'
  if (sourceStatus === 'RUNNING') return 'url(#arrowhead-active)'
  if (sourceStatus === 'SKIPPED') return 'url(#arrowhead)'
  return 'url(#arrowhead)'
}

function getStatusColor(status: TaskStatusCode): string {
  const map: Record<string, string> = {
    RUNNING: '#409EFF', SUCCESS: '#67C23A', FAILED: '#F56C6C', PAUSED: '#E6A23C', SKIPPED: '#C0C4CC',
  }
  return map[status] || '#909399'
}

function getNodeIcon(type: NodeTypeCode): string {
  const icons: Record<string, string> = {
    start: '▶', end: '■', data_extract: '⬇', data_transform: '⇄',
    data_load: '⬆', validation: '✓', condition: '◇', parallel: '⊞',
    merge: '⊟', parallel_group: '⧉', notification: '🔔', script: '⟨⟩', delay: '⏱',
  }
  return icons[type] || '○'
}

function getEdgePath(sourceId: string, targetId: string, style: 'straight' | 'orthogonal' | 'curved' = 'straight'): string {
  const source = getNodePosition(sourceId)
  const target = getNodePosition(targetId)

  if (style === 'straight') {
    return `M ${source.x} ${source.y} L ${target.x} ${target.y}`
  } else if (style === 'orthogonal') {
    const midX = (source.x + target.x) / 2
    return `M ${source.x} ${source.y} L ${midX} ${source.y} L ${midX} ${target.y} L ${target.x} ${target.y}`
  } else {
    const dx = target.x - source.x
    const dy = target.y - source.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    const controlOffset = Math.min(150, Math.max(80, distance / 3))
    return `M ${source.x} ${source.y} C ${source.x} ${source.y + controlOffset}, ${target.x} ${target.y - controlOffset}, ${target.x} ${target.y}`
  }
}

function getConnectingPath(): string {
  if (!connecting.value) return ''
  const { startX, startY, currentX, currentY } = connecting.value
  const dx = currentX - startX
  const dy = currentY - startY
  const controlOffset = Math.min(100, Math.max(50, Math.sqrt(dx * dx + dy * dy) / 3))
  return `M ${startX} ${startY} C ${startX} ${startY + controlOffset}, ${currentX} ${currentY - controlOffset}, ${currentX} ${currentY}`
}

function getAnimationDuration(edge: any): number {
  const source = getNodePosition(edge.sourceNodeId)
  const target = getNodePosition(edge.targetNodeId)
  const distance = Math.sqrt(Math.pow(target.x - source.x, 2) + Math.pow(target.y - source.y, 2))
  return Math.max(1.5, Math.min(4, distance / 150))
}

function getPortCoordinates(nodeId: string, port: string) {
  const pos = getNodePosition(nodeId)
  switch (port) {
    case 'top': return { x: pos.x, y: pos.y - 25 }
    case 'bottom': return { x: pos.x, y: pos.y + 25 }
    case 'left': return { x: pos.x - 70, y: pos.y }
    case 'right': return { x: pos.x + 70, y: pos.y }
    default: return pos
  }
}

function handlePortEnter(event: MouseEvent) {
  const target = event.target as SVGCircleElement
  target.setAttribute('r', '8')
  target.style.opacity = '1'
  target.style.filter = 'drop-shadow(0 0 4px #409EFF)'
}

function handlePortLeave(event: MouseEvent) {
  const target = event.target as SVGCircleElement
  target.setAttribute('r', '6')
  target.style.opacity = '0.8'
  target.style.filter = ''
}

function handleNodeMouseDown(event: MouseEvent, nodeId: string) {
  if (spacePressed.value || event.button === 1) return
  event.stopPropagation()

  if (event.ctrlKey || event.metaKey) {
    const newSet = new Set(selectedNodeIds.value)
    if (newSet.has(nodeId)) {
      newSet.delete(nodeId)
    } else {
      newSet.add(nodeId)
    }
    selectedNodeIds.value = newSet
    emit('selection-change', Array.from(newSet))
    return
  }

  if (!selectedNodeIds.value.has(nodeId)) {
    selectedNodeIds.value = new Set([nodeId])
    emit('selection-change', [nodeId])
  } else if (selectedNodeIds.value.size > 1) {
    selectedNodeIds.value = new Set([nodeId])
    emit('selection-change', [nodeId])
  }

  const pos = getNodePosition(nodeId)
  dragging.value = {
    nodeId,
    startX: event.clientX,
    startY: event.clientY,
    nodeStartX: pos.x,
    nodeStartY: pos.y,
  }
}

function handlePortMouseDown(event: MouseEvent, nodeId: string, port: string) {
  if (spacePressed.value || event.button === 1) return
  event.stopPropagation()
  const portPos = getPortCoordinates(nodeId, port)
  connecting.value = {
    sourceId: nodeId,
    sourcePort: port,
    startX: portPos.x,
    startY: portPos.y,
    currentX: portPos.x,
    currentY: portPos.y,
  }
}

function handlePortMouseUp(event: MouseEvent, nodeId: string) {
  event.stopPropagation()
  if (connecting.value && connecting.value.sourceId !== nodeId) {
    emit('connect', connecting.value.sourceId, nodeId)
    connecting.value = null
  }
}

function handleCanvasMouseDown(event: MouseEvent) {
  connecting.value = null
  if (event.button !== 0 && event.button !== 1) return

  if (event.button === 0 && event.shiftKey) {
    const svgPoint = screenToSvg(event.clientX, event.clientY)
    rubberBand.value = {
      startX: svgPoint.x,
      startY: svgPoint.y,
      endX: svgPoint.x,
      endY: svgPoint.y,
    }
    isRubberBanding.value = false
    return
  }

  panning.value = {
    startX: event.clientX,
    startY: event.clientY,
    startPanX: panX.value,
    startPanY: panY.value,
  }
}

function handleCanvasMouseMove(event: MouseEvent) {
  if (rubberBand.value && !dragging.value && !connecting.value) {
    const svgPoint = screenToSvg(event.clientX, event.clientY)
    rubberBand.value = {
      ...rubberBand.value,
      endX: svgPoint.x,
      endY: svgPoint.y,
    }

    if (!isRubberBanding.value) {
      const dx = Math.abs(svgPoint.x - rubberBand.value.startX)
      const dy = Math.abs(svgPoint.y - rubberBand.value.startY)
      if (dx > rubberBandThreshold || dy > rubberBandThreshold) {
        isRubberBanding.value = true
        panning.value = null
      }
    }

    if (isRubberBanding.value) {
      updateRubberBandSelection()
    }
  }
  if (panning.value) {
    panX.value = panning.value.startPanX + (event.clientX - panning.value.startX)
    panY.value = panning.value.startPanY + (event.clientY - panning.value.startY)
    return
  }
  if (dragging.value) {
    const dx = (event.clientX - dragging.value.startX) / zoomLevel.value
    const dy = (event.clientY - dragging.value.startY) / zoomLevel.value
    emit('node-move', dragging.value.nodeId, dragging.value.nodeStartX + dx, dragging.value.nodeStartY + dy)
  }
  if (connecting.value) {
    const svgPoint = screenToSvg(event.clientX, event.clientY)
    connecting.value.currentX = svgPoint.x
    connecting.value.currentY = svgPoint.y
  }
}

function updateRubberBandSelection() {
  if (!rubberBand.value) return
  const rb = rubberBand.value
  const left = Math.min(rb.startX, rb.endX)
  const right = Math.max(rb.startX, rb.endX)
  const top = Math.min(rb.startY, rb.endY)
  const bottom = Math.max(rb.startY, rb.endY)

  if (Math.abs(right - left) < 5 && Math.abs(bottom - top) < 5) return

  const selected = new Set<string>()
  for (const node of props.flow.nodes) {
    const x = node.positionX || 0
    const y = node.positionY || 0
    if (x >= left && x <= right && y >= top && y <= bottom) {
      selected.add(node.nodeId)
    }
  }
  if (selected.size > 0) {
    selectedNodeIds.value = selected
    emit('selection-change', Array.from(selected))
  }
}

function getParallelGroupName(groupId: string): string {
  const groupNode = props.flow.nodes.find(n => n.nodeId === groupId)
  return groupNode ? groupNode.name : groupId
}

function handleCanvasMouseUp(_event: MouseEvent) {
  if (isRubberBanding.value) {
    // 框选完成，保持选中状态，不做任何清除
  } else if (panning.value && !dragging.value && !connecting.value) {
    const dx = Math.abs(_event.clientX - panning.value.startX)
    const dy = Math.abs(_event.clientY - panning.value.startY)
    if (dx < 3 && dy < 3 && !_event.ctrlKey && !_event.metaKey) {
      selectedNodeIds.value = new Set()
      emit('selection-change', [])
    }
  }
  panning.value = null
  dragging.value = null
  connecting.value = null
  rubberBand.value = null
  isRubberBanding.value = false
}

function handleWheel(event: WheelEvent) {
  if (!svgRef.value) return
  const rect = svgRef.value.getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const mouseY = event.clientY - rect.top
  const oldZoom = zoomLevel.value
  const delta = event.deltaY > 0 ? 0.9 : 1.1
  const newZoom = Math.min(3, Math.max(0.2, oldZoom * delta))
  panX.value = mouseX - (mouseX - panX.value) * (newZoom / oldZoom)
  panY.value = mouseY - (mouseY - panY.value) * (newZoom / oldZoom)
  zoomLevel.value = newZoom
}

function handleNodeContextMenu(event: MouseEvent, nodeId: string) {
  if (confirm('确定要删除这个节点吗？')) {
    emit('delete-node', nodeId)
  }
}

function handleDrop(event: DragEvent) {
  const nodeType = event.dataTransfer?.getData('nodeType') as NodeTypeCode
  if (nodeType) {
    emit('node-click', `new_${nodeType}_${Date.now()}`)
  }
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.code === 'Space' && !event.repeat) {
    event.preventDefault()
    spacePressed.value = true
  }
}

function handleKeyUp(event: KeyboardEvent) {
  if (event.code === 'Space') {
    spacePressed.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
})

function zoomIn() {
  const center = getViewportCenter()
  const oldZoom = zoomLevel.value
  const newZoom = Math.min(3, oldZoom * 1.2)
  panX.value = center.x - (center.x - panX.value) * (newZoom / oldZoom)
  panY.value = center.y - (center.y - panY.value) * (newZoom / oldZoom)
  zoomLevel.value = newZoom
}

function zoomOut() {
  const center = getViewportCenter()
  const oldZoom = zoomLevel.value
  const newZoom = Math.max(0.2, oldZoom / 1.2)
  panX.value = center.x - (center.x - panX.value) * (newZoom / oldZoom)
  panY.value = center.y - (center.y - panY.value) * (newZoom / oldZoom)
  zoomLevel.value = newZoom
}

function fitToView() {
  if (!svgRef.value || props.flow.nodes.length === 0) return
  const rect = svgRef.value.getBoundingClientRect()
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  for (const node of props.flow.nodes) {
    const x = node.positionX || 0
    const y = node.positionY || 0
    minX = Math.min(minX, x - 80)
    minY = Math.min(minY, y - 30)
    maxX = Math.max(maxX, x + 80)
    maxY = Math.max(maxY, y + 30)
  }
  const contentW = maxX - minX
  const contentH = maxY - minY
  const padding = 60
  const scaleX = (rect.width - padding * 2) / contentW
  const scaleY = (rect.height - padding * 2) / contentH
  const newZoom = Math.min(2, Math.max(0.2, Math.min(scaleX, scaleY)))
  const centerX = (minX + maxX) / 2
  const centerY = (minY + maxY) / 2
  zoomLevel.value = newZoom
  panX.value = rect.width / 2 - centerX * newZoom
  panY.value = rect.height / 2 - centerY * newZoom
}

function resetZoom() {
  zoomLevel.value = 1
  panX.value = 0
  panY.value = 0
}

function getViewportCenter() {
  if (!svgRef.value) return { x: 0, y: 0 }
  const rect = svgRef.value.getBoundingClientRect()
  return { x: rect.width / 2, y: rect.height / 2 }
}

function clearSelection() {
  selectedNodeIds.value = new Set()
  emit('selection-change', [])
}

defineExpose({
  zoomLevel,
  zoomIn,
  zoomOut,
  fitToView,
  resetZoom,
  clearSelection,
})
</script>

<style scoped>
.flow-canvas {
  width: 100%;
  height: 100%;
  cursor: default;
  user-select: none;
  -webkit-user-select: none;
  outline: none;
}
.node-group { cursor: move; }
.node-rect { transition: fill 0.3s, stroke 0.3s; }
.node-group:hover .node-rect { filter: drop-shadow(0 2px 4px rgba(0,0,0,0.12)); }
.node-group:hover .delete-btn circle,
.node-group:hover .delete-btn text { opacity: 1; transition: opacity 0.2s; }
.port {
  opacity: 0.3;
  transition: opacity 0.2s;
  cursor: crosshair;
}
.node-group:hover .port { opacity: 0.8; }
.edge-line { cursor: pointer; transition: stroke-width 0.2s; }
.edge-line:hover { stroke-width: 4; }
</style>
