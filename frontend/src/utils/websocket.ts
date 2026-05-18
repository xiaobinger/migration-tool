import type { TaskProgressMessage } from '../types'

type MessageHandler = (msg: TaskProgressMessage) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private handlers: MessageHandler[] = []
  private reconnectTimer: number | null = null
  private url = 'ws://localhost:18080/ws/task-progress'

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return

    this.ws = new WebSocket(this.url)

    this.ws.onopen = () => {
      console.log('[WS] 连接建立')
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }

    this.ws.onmessage = (event) => {
      try {
        const msg: TaskProgressMessage = JSON.parse(event.data)
        this.handlers.forEach(handler => handler(msg))
      } catch (e) {
        console.error('[WS] 消息解析失败:', e)
      }
    }

    this.ws.onclose = () => {
      console.log('[WS] 连接关闭，5秒后重连...')
      this.reconnectTimer = window.setTimeout(() => this.connect(), 5000)
    }

    this.ws.onerror = (err) => {
      console.error('[WS] 连接错误:', err)
      this.ws?.close()
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }

  onMessage(handler: MessageHandler) {
    this.handlers.push(handler)
    return () => {
      this.handlers = this.handlers.filter(h => h !== handler)
    }
  }
}

export const wsClient = new WebSocketClient()
