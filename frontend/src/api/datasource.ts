import api from './http'

export interface DataSourceConfig {
  id?: number
  name: string
  type: 'MYSQL' | 'POSTGRESQL' | 'MONGODB'
  host: string
  port: number
  database: string
  username: string
  password?: string
  properties?: string
  sshEnabled?: boolean
  sshHost?: string
  sshPort?: number
  sshUsername?: string
  sshPassword?: string
  sshAuthKey?: string
  enabled?: boolean
  description?: string
  createdAt?: string
  updatedAt?: string
}

export const dataSourceApi = {
  list: () => api.get('/datasources') as Promise<DataSourceConfig[]>,

  listEnabled: () => api.get('/datasources/enabled') as Promise<DataSourceConfig[]>,

  get: (id: number) => api.get(`/datasources/${id}`) as Promise<DataSourceConfig>,

  create: (data: DataSourceConfig) => api.post('/datasources', data) as Promise<DataSourceConfig>,

  update: (id: number, data: DataSourceConfig) => api.put(`/datasources/${id}`, data) as Promise<DataSourceConfig>,

  delete: (id: number) => api.delete(`/datasources/${id}`) as Promise<void>,

  testConnection: (id: number) => api.post(`/datasources/${id}/test`) as Promise<{ success: boolean; message: string }>,

  testDirect: (data: DataSourceConfig) => api.post('/datasources/test', data) as Promise<{ success: boolean; message: string }>,

  getTables: (id: number) => api.get(`/datasources/${id}/tables`) as Promise<string[]>,

  getColumns: (id: number, tableName: string) => api.get(`/datasources/${id}/tables/${tableName}/columns`) as Promise<Record<string, any>[]>
}
