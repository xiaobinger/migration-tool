import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { taskApi } from '../api/task'
import { wsClient } from '../utils/websocket'
import type { MigrationTask, TaskProgressMessage, TaskStatusCode } from '../types'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<MigrationTask[]>([])
  const currentTask = ref<MigrationTask | null>(null)
  const loading = ref(false)

  // 节点执行状态映射 (nodeId -> status)
  const nodeStatusMap = reactive<Record<string, TaskStatusCode>>({})
  // 当前执行到的节点
  const activeNodeId = ref<string | null>(null)
  // 进度
  const progress = ref(0)

  async function loadTasks(flowDefinitionId?: number) {
    loading.value = true
    try {
      tasks.value = await taskApi.list(flowDefinitionId)
    } finally {
      loading.value = false
    }
  }

  async function loadTask(id: number) {
    loading.value = true
    try {
      currentTask.value = await taskApi.get(id)
    } finally {
      loading.value = false
    }
  }

  async function startTask(name: string, flowDefinitionId: number) {
    const task = await taskApi.start({ name, flowDefinitionId })
    currentTask.value = task
    tasks.value.unshift(task)
    return task
  }

  async function restartTask(id: number, restartFromNodeId?: string) {
    const task = await taskApi.restart(id, restartFromNodeId)
    if (currentTask.value?.id === id) {
      currentTask.value = task
    }
    return task
  }

  async function pauseTask(id: number) {
    const task = await taskApi.pause(id)
    if (currentTask.value?.id === id) {
      currentTask.value = task
    }
    return task
  }

  async function cancelTask(id: number) {
    const task = await taskApi.cancel(id)
    if (currentTask.value?.id === id) {
      currentTask.value = task
    }
    return task
  }

  async function deleteTask(id: number) {
    await taskApi.delete(id)
    tasks.value = tasks.value.filter(t => t.id !== id)
    if (currentTask.value?.id === id) {
      currentTask.value = null
    }
  }

  async function clearCompletedTasks() {
    await taskApi.clearCompleted()
    tasks.value = tasks.value.filter(t => t.status === 'RUNNING' || t.status === 'PENDING' || t.status === 'PAUSED')
  }

  async function clearAllTasks() {
    await taskApi.clearAll()
    tasks.value = []
  }

  // WebSocket消息处理
  function initWebSocket() {
    wsClient.connect()
    wsClient.onMessage(handleProgressMessage)
  }

  function handleProgressMessage(msg: TaskProgressMessage) {
    if (currentTask.value && msg.taskId === currentTask.value.id) {
      switch (msg.type) {
        case 'progress':
          progress.value = msg.progress || 0
          activeNodeId.value = msg.nodeId || null
          if (msg.extractedRecords != null) currentTask.value.extractedRecords = msg.extractedRecords
          if (msg.loadedRecords != null) currentTask.value.loadedRecords = msg.loadedRecords
          if (msg.loadedSuccessRecords != null) currentTask.value.loadedSuccessRecords = msg.loadedSuccessRecords
          if (msg.loadedFailedRecords != null) currentTask.value.loadedFailedRecords = msg.loadedFailedRecords
          break
        case 'node_start':
          nodeStatusMap[msg.nodeId!] = 'RUNNING'
          activeNodeId.value = msg.nodeId || null
          break
        case 'node_complete':
          nodeStatusMap[msg.nodeId!] = 'SUCCESS'
          if (msg.progress != null) progress.value = msg.progress
          if (msg.extractedRecords != null) currentTask.value.extractedRecords = msg.extractedRecords
          if (msg.loadedRecords != null) currentTask.value.loadedRecords = msg.loadedRecords
          if (msg.loadedSuccessRecords != null) currentTask.value.loadedSuccessRecords = msg.loadedSuccessRecords
          if (msg.loadedFailedRecords != null) currentTask.value.loadedFailedRecords = msg.loadedFailedRecords
          break
        case 'node_error':
          nodeStatusMap[msg.nodeId!] = 'FAILED'
          break
        case 'task_complete':
          if (currentTask.value) {
            currentTask.value.status = 'SUCCESS'
            if (msg.extractedRecords != null) currentTask.value.extractedRecords = msg.extractedRecords
            if (msg.loadedRecords != null) currentTask.value.loadedRecords = msg.loadedRecords
            if (msg.loadedSuccessRecords != null) currentTask.value.loadedSuccessRecords = msg.loadedSuccessRecords
            if (msg.loadedFailedRecords != null) currentTask.value.loadedFailedRecords = msg.loadedFailedRecords
          }
          progress.value = 100
          break
        case 'task_failed':
          if (currentTask.value) {
            currentTask.value.status = 'FAILED'
            currentTask.value.errorMessage = msg.error
            if (msg.extractedRecords != null) currentTask.value.extractedRecords = msg.extractedRecords
            if (msg.loadedRecords != null) currentTask.value.loadedRecords = msg.loadedRecords
            if (msg.loadedSuccessRecords != null) currentTask.value.loadedSuccessRecords = msg.loadedSuccessRecords
            if (msg.loadedFailedRecords != null) currentTask.value.loadedFailedRecords = msg.loadedFailedRecords
          }
          break
      }
    }
  }

  function resetProgress() {
    Object.keys(nodeStatusMap).forEach(k => delete nodeStatusMap[k])
    activeNodeId.value = null
    progress.value = 0
  }

  return {
    tasks, currentTask, loading, nodeStatusMap, activeNodeId, progress,
    loadTasks, loadTask, startTask, restartTask, pauseTask, cancelTask,
    deleteTask, clearCompletedTasks, clearAllTasks,
    initWebSocket, resetProgress, handleProgressMessage,
  }
})
