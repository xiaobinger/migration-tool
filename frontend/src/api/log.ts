import api from './http'
import type { TaskLog, NodeExecution, ErrorAnalysis } from '../types'

export const logApi = {
  taskLogs: (taskId: number) => api.get<any, TaskLog[]>(`/logs/task/${taskId}`),

  nodeLogs: (taskId: number, nodeId: string) =>
    api.get<any, TaskLog[]>(`/logs/task/${taskId}/node/${nodeId}`),

  nodeExecutions: (taskId: number) =>
    api.get<any, NodeExecution[]>(`/logs/task/${taskId}/executions`),

  errorAnalysis: (taskId: number) =>
    api.get<any, ErrorAnalysis>(`/logs/task/${taskId}/error-analysis`),
}
