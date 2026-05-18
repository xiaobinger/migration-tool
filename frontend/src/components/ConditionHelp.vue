<!-- frontend/src/components/ConditionHelp.vue -->
<template>
  <el-dialog title="条件分支使用指南" v-model="visible" width="700px">
    <div class="condition-help-content">
      <h3>📋 什么是条件分支？</h3>
      <p>条件分支允许流程根据数据或变量的值选择不同的执行路径。</p>

      <h3>🔧 如何配置？</h3>
      <ol>
        <li>添加一个「条件判断」节点</li>
        <li>从该节点拉出多条连线到不同的目标节点</li>
        <li>点击每条连线，设置条件表达式</li>
      </ol>

      <h3>💡 条件表达式示例</h3>
      <el-table :data="examples" border stripe>
        <el-table-column prop="desc" label="说明" width="200" />
        <el-table-column prop="code" label="表达式" />
      </el-table>

      <h3>⚠️ 注意事项</h3>
      <ul>
        <li>条件表达式必须返回布尔值（true/false）</li>
        <li>可以使用 <code>data</code> 变量访问迁移数据</li>
        <li>可以使用之前节点存储的变量</li>
        <li>如果所有条件都不满足，流程会停止</li>
        <li>建议最后一条连线不设置条件作为默认路径</li>
      </ul>
    </div>

    <template #footer>
      <el-button type="primary" @click="visible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)

const examples = [
  { desc: '金额大于1000', code: 'data.amount > 1000' },
  { desc: '状态为成功', code: 'data.status === "SUCCESS"' },
  { desc: '记录数不为0', code: 'data.count !== 0' },
  { desc: '类型是用户', code: 'data.type == "user"' },
  { desc: '多个条件', code: 'data.age >= 18 && data.country === "CN"' },
]

function show() {
  visible.value = true
}

defineExpose({ show })
</script>

<style scoped>
.condition-help-content {
  line-height: 1.8;
}
.condition-help-content h3 {
  margin: 16px 0 8px 0;
  color: #409EFF;
}
.condition-help-content p, .condition-help-content li {
  color: #606266;
  font-size: 14px;
}
.condition-help-content code {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  color: #E6A23C;
}
</style>
