import api from './http'
import type { MigrationTask, TaskExecutionRequest } from '../types'

export const taskApi = {
  list: (flowDefinitionId?: number) =>
    api.get<any, MigrationTask[]>('/tasks', { params: { flowDefinitionId } }),

  get: (id: number) => api.get<any, MigrationTask>(`/tasks/${id}`),

  start: (data: TaskExecutionRequest) => api.post<any, MigrationTask>('/tasks', data),

  restart: (id: number, restartFromNodeId?: string) =>
    api.post<any, MigrationTask>(`/tasks/${id}/restart`, null, {
      params: restartFromNodeId ? { restartFromNodeId } : undefined,
    }),

  pause: (id: number) => api.post<any, MigrationTask>(`/tasks/${id}/pause`),

  cancel: (id: number) => api.post<any, MigrationTask>(`/tasks/${id}/cancel`),

  running: () => api.get<any, MigrationTask[]>('/tasks/running'),

  delete: (id: number) => api.delete<any, void>(`/tasks/${id}`),

  clearCompleted: () => api.delete<any, void>('/tasks/completed'),

  clearAll: () => api.delete<any, void>('/tasks/all'),
}
