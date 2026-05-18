<!-- frontend/src/components/FlowHelpTip.vue -->
<template>
  <div class="flow-help-tip">
    <el-card shadow="hover" class="help-card">
      <template #header>
        <div class="card-header">
          <span>💡 操作指南</span>
          <el-button text @click="$emit('close')">✕</el-button>
        </div>
      </template>
      <div class="help-content">
        <h4>📌 基本操作</h4>
        <ul>
          <li><strong>添加节点</strong>：从左侧面板拖拽节点到画布</li>
          <li><strong>移动节点</strong>：鼠标拖动节点到任意位置</li>
          <li><strong>编辑属性</strong>：单击节点，在右侧面板编辑属性</li>
        </ul>
        
        <h4>🔗 连接节点</h4>
        <ul>
          <li><strong>方式1</strong>：从一个节点的蓝色连接点拖拽到另一个节点</li>
          <li><strong>方式2</strong>：双击节点，然后移动到目标节点点击</li>
          <li><strong>删除连线</strong>：右键点击连线即可删除</li>
        </ul>
        
        <h4>⚡ 执行顺序</h4>
        <ul>
          <li><strong>串行</strong>：A → B → C（依次执行）</li>
          <li><strong>并行</strong>：A → B 和 A → C（B和C同时执行）</li>
          <li>使用「条件分支」节点实现判断逻辑</li>
          <li>使用「并行处理」节点实现多路并发</li>
        </ul>
        
        <h4>🎯 节点类型</h4>
        <ul>
          <li>▶ <strong>开始</strong>：流程起点</li>
          <li>⬇ <strong>数据抽取</strong>：从源系统提取数据</li>
          <li>⇄ <strong>数据转换</strong>：清洗、转换数据</li>
          <li>⬆ <strong>数据加载</strong>：写入目标系统</li>
          <li>✓ <strong>数据校验</strong>：验证数据质量</li>
          <li>◇ <strong>条件分支</strong>：根据条件选择路径</li>
          <li>⊞ <strong>并行处理</strong>：同时执行多个分支</li>
          <li>⊟ <strong>合并</strong>：等待所有分支完成</li>
          <li>■ <strong>结束</strong>：流程终点</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineEmits<{
  'close': []
}>()
</script>

<style scoped>
.flow-help-tip {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 1000;
  width: 380px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}
.help-card {
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.help-content {
  font-size: 13px;
  line-height: 1.8;
}
.help-content h4 {
  margin: 12px 0 8px 0;
  color: #409EFF;
  font-size: 14px;
}
.help-content ul {
  margin: 0;
  padding-left: 18px;
}
.help-content li {
  margin: 4px 0;
  color: #606266;
}
.help-content strong {
  color: #303133;
}
</style>
