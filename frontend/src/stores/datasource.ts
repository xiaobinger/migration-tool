import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dataSourceApi, type DataSourceConfig } from '../api/datasource'

export const useDataSourceStore = defineStore('datasource', () => {
  const dataSources = ref<DataSourceConfig[]>([])
  const loading = ref(false)

  async function loadDataSources() {
    loading.value = true
    try {
      dataSources.value = await dataSourceApi.list()
    } finally {
      loading.value = false
    }
  }

  async function loadEnabledDataSources() {
    try {
      dataSources.value = await dataSourceApi.listEnabled()
    } catch {
      dataSources.value = await dataSourceApi.list()
    }
  }

  async function createDataSource(config: DataSourceConfig) {
    const created = await dataSourceApi.create(config)
    dataSources.value.push(created)
    return created
  }

  async function updateDataSource(id: number, config: DataSourceConfig) {
    const updated = await dataSourceApi.update(id, config)
    const idx = dataSources.value.findIndex(ds => ds.id === id)
    if (idx >= 0) dataSources.value[idx] = updated
    return updated
  }

  async function deleteDataSource(id: number) {
    await dataSourceApi.delete(id)
    dataSources.value = dataSources.value.filter(ds => ds.id !== id)
  }

  async function testConnection(id: number) {
    return dataSourceApi.testConnection(id)
  }

  async function testDirect(config: DataSourceConfig) {
    return dataSourceApi.testDirect(config)
  }

  async function getTables(id: number) {
    return dataSourceApi.getTables(id)
  }

  async function getColumns(id: number, tableName: string) {
    return dataSourceApi.getColumns(id, tableName)
  }

  function getDataSourceById(id: number) {
    return dataSources.value.find(ds => ds.id === id)
  }

  return {
    dataSources,
    loading,
    loadDataSources,
    loadEnabledDataSources,
    createDataSource,
    updateDataSource,
    deleteDataSource,
    testConnection,
    testDirect,
    getTables,
    getColumns,
    getDataSourceById
  }
})
