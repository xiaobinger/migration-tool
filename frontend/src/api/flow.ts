import api from './http'
import type { FlowDefinition } from '../types'

export const flowApi = {
  list: () => api.get<any, FlowDefinition[]>('/flows'),

  get: (id: number) => api.get<any, FlowDefinition>(`/flows/${id}`),

  create: (data: FlowDefinition) => api.post<any, FlowDefinition>('/flows', data),

  update: (id: number, data: FlowDefinition) => api.put<any, FlowDefinition>(`/flows/${id}`, data),

  delete: (id: number) => api.delete(`/flows/${id}`),
}
