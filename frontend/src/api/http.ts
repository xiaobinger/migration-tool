import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:18080/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截
api.interceptors.request.use(config => config)

// 响应拦截
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default api
