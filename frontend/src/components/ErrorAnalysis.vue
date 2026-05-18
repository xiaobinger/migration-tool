<template>
  <div class="error-analysis" v-if="analysis">
    <!-- 错误概览 -->
    <div class="section">
      <h4>错误概览</h4>
      <p>共 <strong>{{ analysis.totalErrors }}</strong> 个错误，
        涉及 <strong>{{ analysis.failedNodes?.length || 0 }}</strong> 个节点</p>
    </div>

    <!-- 错误类型分布 -->
    <div class="section" v-if="analysis.errorTypes && Object.keys(analysis.errorTypes).length">
      <h4>错误类型</h4>
      <div v-for="(count, type) in analysis.errorTypes" :key="type" class="error-type-item">
        <el-tag type="danger" size="small">{{ type }}</el-tag>
        <span class="error-count">{{ count }} 次</span>
      </div>
    </div>

    <!-- 失败节点 -->
    <div class="section" v-if="analysis.failedNodes?.length">
      <h4>失败节点</h4>
      <div v-for="node in analysis.failedNodes" :key="node.nodeId" class="failed-node">
        <el-tag type="danger" size="small">{{ node.nodeName || node.nodeId }}</el-tag>
        <span class="error-msg">{{ node.errorMessage }}</span>
        <span class="retry-info" v-if="node.retryCount > 0">
          已重试 {{ node.retryCount }}/{{ node.maxRetry }} 次
        </span>
      </div>
    </div>

    <!-- 修复建议 -->
    <div class="section" v-if="analysis.suggestions?.length">
      <h4>修复建议</h4>
      <div v-for="(suggestion, index) in analysis.suggestions" :key="index" class="suggestion-item">
        <span class="info-icon">ℹ</span>
        <span>{{ suggestion }}</span>
      </div>
    </div>
  </div>
  <el-empty v-else description="点击「分析」按钮进行错误分析" :image-size="60" />
</template>

<script setup lang="ts">
import type { ErrorAnalysis } from '../types'

defineProps<{ analysis: ErrorAnalysis | null }>()
</script>

<style scoped>
.error-analysis { font-size: 13px; }
.section { margin-bottom: 16px; }
.section h4 { font-size: 13px; color: #303133; margin-bottom: 8px; }
.error-type-item { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.error-count { color: #F56C6C; font-weight: 600; }
.failed-node { padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.error-msg { color: #F56C6C; font-size: 12px; margin-left: 8px; }
.retry-info { color: #E6A23C; font-size: 11px; margin-left: 8px; }
.suggestion-item {
  display: flex; align-items: flex-start; gap: 6px; padding: 4px 0;
  color: #606266; line-height: 1.5;
}
.info-icon { color: #409EFF; font-size: 14px; margin-top: 2px; flex-shrink: 0; }
</style>
