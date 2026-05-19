import api from './http'
import type { TaskLog, NodeExecution, ErrorAnalysis, LoadFailureRecord } from '../types'

export const logApi = {
  taskLogs: (taskId: number) => api.get<any, TaskLog[]>(`/logs/task/${taskId}`),

  nodeLogs: (taskId: number, nodeId: string) =>
    api.get<any, TaskLog[]>(`/logs/task/${taskId}/node/${nodeId}`),

  nodeExecutions: (taskId: number) =>
    api.get<any, NodeExecution[]>(`/logs/task/${taskId}/executions`),

  errorAnalysis: (taskId: number) =>
    api.get<any, ErrorAnalysis>(`/logs/task/${taskId}/error-analysis`),

  loadFailures: (taskId: number) =>
    api.get<any, LoadFailureRecord[]>(`/logs/task/${taskId}/failures`),

  loadFailuresByNode: (taskId: number, nodeId: string) =>
    api.get<any, LoadFailureRecord[]>(`/logs/task/${taskId}/failures/${nodeId}`),

  retryLoadFailures: (taskId: number) =>
    api.post<any, any>(`/logs/task/${taskId}/failures/retry`),

  clearLoadFailures: (taskId: number) =>
    api.delete<any, any>(`/logs/task/${taskId}/failures`),
}
