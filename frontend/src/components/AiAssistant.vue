<template>
  <div class="ai-assistant">
    <div class="ai-header">
      <div class="ai-title">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>AI 助手</span>
      </div>
      <div class="ai-header-actions">
        <button class="action-icon-btn" @click="showSkillsPanel = !showSkillsPanel" :class="{ active: showSkillsPanel }" title="Skills管理">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
        <button class="action-icon-btn" @click="clearChat" title="清空对话">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <polyline points="3 6 5 6 21 6" stroke="currentColor" stroke-width="2"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
        <button class="action-icon-btn" @click="$emit('close')" title="关闭">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/>
            <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="ai-body" v-if="!showSkillsPanel">
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#c0c4cc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <p>描述你的数据迁移需求，AI 将为你生成流程</p>
          <div class="quick-prompts">
            <button v-for="prompt in quickPrompts" :key="prompt" class="quick-prompt-btn" @click="sendQuickPrompt(prompt)">
              {{ prompt }}
            </button>
          </div>
        </div>
        <div v-for="(msg, index) in messages" :key="index" class="chat-message" :class="msg.role">
          <div class="message-avatar">
            <svg v-if="msg.role === 'user'" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2"/>
              <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.role === 'assistant' && msg.flowData" class="flow-actions">
              <button class="apply-flow-btn" @click="applyFlow(msg.flowData)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <polyline points="20 6 9 17 4 12" stroke="currentColor" stroke-width="2"/>
                </svg>
                应用到画布
              </button>
              <button class="save-flow-btn" @click="saveFlow(msg.flowData)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" stroke="currentColor" stroke-width="2"/>
                  <polyline points="17 21 17 13 7 13 7 21" stroke="currentColor" stroke-width="2"/>
                </svg>
                保存为新流程
              </button>
            </div>
          </div>
        </div>
        <div v-if="loading" class="chat-message assistant">
          <div class="message-avatar">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="input-actions">
          <button class="input-action-btn" @click="handleAnalyze" :disabled="!inputText.trim() || loading" title="需求分析">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65" stroke="currentColor" stroke-width="2"/>
            </svg>
            分析
          </button>
          <button class="input-action-btn primary" @click="handleGenerate" :disabled="!inputText.trim() || loading" title="AI生成流程">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <polygon points="5 3 19 12 5 21 5 3" stroke="currentColor" stroke-width="2" fill="none"/>
            </svg>
            生成流程
          </button>
        </div>
        <div class="input-row">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="描述你的数据迁移需求，例如：将MySQL的users表数据清洗后迁移到PostgreSQL..."
            @keydown.enter.ctrl="handleSend"
            :disabled="loading"
          />
          <button class="send-btn" @click="handleSend" :disabled="!inputText.trim() || loading">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <line x1="22" y1="2" x2="11" y2="13" stroke="currentColor" stroke-width="2"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2" stroke="currentColor" stroke-width="2" fill="none"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="ai-body" v-else>
      <div class="skills-panel">
        <div class="skills-header">
          <h3>Skills 管理</h3>
          <span class="skills-count">{{ skills.length }} 个模板</span>
        </div>
        <div class="skills-list">
          <div v-for="skill in skills" :key="skill.id" class="skill-card">
            <div class="skill-info">
              <div class="skill-name">{{ skill.name }}</div>
              <div class="skill-meta">
                <span class="skill-category">{{ skill.category }}</span>
                <span class="skill-usage">使用 {{ skill.usageCount }} 次</span>
              </div>
              <div class="skill-desc" v-if="skill.description">{{ skill.description }}</div>
            </div>
            <div class="skill-actions">
              <button class="skill-action-btn" @click="applySkill(skill)" title="应用此模板">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <polyline points="20 6 9 17 4 12" stroke="currentColor" stroke-width="2"/>
                </svg>
              </button>
              <button class="skill-action-btn danger" @click="deleteSkill(skill)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <polyline points="3 6 5 6 21 6" stroke="currentColor" stroke-width="2"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
                </svg>
              </button>
            </div>
          </div>
          <div v-if="skills.length === 0" class="empty-skills">
            <p>暂无Skills模板</p>
            <p class="hint">执行成功的流程会自动学习生成模板</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { aiApi, type AiChatMessage, type SkillTemplate } from '../api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'apply-flow', flow: any): void
}>()

const messages = ref<Array<AiChatMessage & { flowData?: any }>>([])
const inputText = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement>()
const showSkillsPanel = ref(false)
const skills = ref<SkillTemplate[]>([])

const quickPrompts = [
  '将MySQL数据迁移到PostgreSQL',
  '从MongoDB提取数据写入MySQL',
  '数据清洗并迁移到目标库',
  '并行提取多表数据合并加载',
]

onMounted(() => {
  loadSkills()
})

async function loadSkills() {
  try {
    skills.value = await aiApi.getSkills() as any
  } catch (e) {
    console.error('加载Skills失败', e)
  }
}

function sendQuickPrompt(prompt: string) {
  inputText.value = prompt
  handleSend()
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const history = messages.value.slice(-10).map(m => ({ role: m.role, content: m.content }))
    const res = await aiApi.chat(text, history as AiChatMessage[])
    messages.value.push({ role: 'assistant', content: res.message })
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '请求失败: ' + (e.message || '未知错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function handleGenerate() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: `请生成流程：${text}` })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiApi.generateFlow(text) as any
    if (res.success && res.flowDefinition) {
      let content = '已成功生成流程定义！\n\n'
      if (res.flowDefinition.nodes) {
        content += `包含 ${res.flowDefinition.nodes.length} 个节点：\n`
        for (const node of res.flowDefinition.nodes) {
          content += `- ${node.name} (${node.nodeType})\n`
        }
      }
      messages.value.push({ role: 'assistant', content, flowData: res.flowDefinition })
    } else {
      messages.value.push({ role: 'assistant', content: res.message || '流程生成失败，请重新描述需求' })
      if (res.aiResponse) {
        messages.value[messages.value.length - 1].content += '\n\nAI原始回复:\n' + res.aiResponse
      }
    }
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '生成失败: ' + (e.message || '未知错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function handleAnalyze() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: `请分析需求：${text}` })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiApi.analyze(text) as any
    messages.value.push({ role: 'assistant', content: res.analysis })
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '分析失败: ' + (e.message || '未知错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function applyFlow(flowData: any) {
  emit('apply-flow', flowData)
  ElMessage.success('流程已应用到画布')
}

async function saveFlow(flowData: any) {
  try {
    const res = await aiApi.generateFlow(flowData.description || 'AI生成流程', true) as any
    if (res.success) {
      ElMessage.success('流程已保存')
    } else {
      ElMessage.warning(res.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function applySkill(skill: SkillTemplate) {
  if (!skill.flowDefinitionJson) {
    ElMessage.warning('该模板没有流程定义数据')
    return
  }
  try {
    const flowData = JSON.parse(skill.flowDefinitionJson)
    emit('apply-flow', flowData)
    if (skill.id) {
      aiApi.incrementSkillUsage(skill.id).catch(() => {})
    }
    ElMessage.success('模板已应用到画布')
  } catch (e) {
    ElMessage.error('模板数据解析失败')
  }
}

async function deleteSkill(skill: SkillTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板 "${skill.name}" 吗？`, '确认删除', { type: 'warning' })
    if (skill.id) {
      await aiApi.deleteSkill(skill.id)
      skills.value = skills.value.filter(s => s.id !== skill.id)
      ElMessage.success('已删除')
    }
  } catch {}
}

function clearChat() {
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\n/g, '<br/>')
  return html
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-left: 1px solid #e4e7ed;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.ai-header-actions {
  display: flex;
  gap: 4px;
}

.action-icon-btn {
  background: rgba(255,255,255,0.15);
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}

.action-icon-btn:hover, .action-icon-btn.active {
  background: rgba(255,255,255,0.3);
}

.ai-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 12px;
}

.empty-state p {
  font-size: 14px;
}

.quick-prompts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  width: 100%;
  max-width: 320px;
}

.quick-prompt-btn {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.quick-prompt-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-message.user .message-avatar {
  background: #409eff;
  color: #fff;
}

.chat-message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.message-content {
  max-width: 85%;
}

.message-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-message.user .message-text {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-message.assistant .message-text {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.chat-message.assistant .message-text :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 12px;
}

.chat-message.assistant .message-text :deep(code) {
  background: #e8eaed;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'Consolas', monospace;
}

.chat-message.assistant .message-text :deep(pre code) {
  background: none;
  padding: 0;
}

.flow-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.apply-flow-btn, .save-flow-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.apply-flow-btn {
  background: #67c23a;
  color: #fff;
}

.apply-flow-btn:hover {
  background: #5daf34;
}

.save-flow-btn {
  background: #409eff;
  color: #fff;
}

.save-flow-btn:hover {
  background: #337ecc;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.chat-input-area {
  border-top: 1px solid #e4e7ed;
  padding: 12px;
  background: #fafafa;
}

.input-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.input-action-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
}

.input-action-btn.primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.input-action-btn.primary:hover:not(:disabled) {
  background: #337ecc;
}

.input-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-row :deep(.el-textarea__inner) {
  border-radius: 8px;
  font-size: 13px;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.skills-panel {
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}

.skills-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.skills-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.skills-count {
  font-size: 12px;
  color: #909399;
}

.skills-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skill-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.skill-info {
  flex: 1;
  min-width: 0;
}

.skill-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.skill-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.skill-category {
  background: #ecf5ff;
  color: #409eff;
  padding: 1px 6px;
  border-radius: 4px;
}

.skill-desc {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-actions {
  display: flex;
  gap: 4px;
}

.skill-action-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.skill-action-btn:hover {
  background: #409eff;
  color: #fff;
}

.skill-action-btn.danger:hover {
  background: #f56c6c;
  color: #fff;
}

.empty-skills {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.empty-skills .hint {
  font-size: 12px;
  margin-top: 8px;
}
</style>
