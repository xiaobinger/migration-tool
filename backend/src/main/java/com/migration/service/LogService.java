package com.migration.service;

import com.migration.model.enums.LogLevel;
import com.migration.model.enums.TaskStatus;
import com.migration.model.entity.NodeExecution;
import com.migration.model.entity.TaskLog;
import com.migration.repository.NodeExecutionRepository;
import com.migration.repository.TaskLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final TaskLogRepository taskLogRepository;
    private final NodeExecutionRepository nodeExecutionRepository;

    /**
     * 获取任务日志
     */
    public List<TaskLog> getTaskLogs(Long taskId) {
        return taskLogRepository.findByTaskIdOrderByLogTimeDesc(taskId);
    }

    /**
     * 获取节点日志
     */
    public List<TaskLog> getNodeLogs(Long taskId, String nodeId) {
        return taskLogRepository.findByTaskIdAndNodeIdOrderByLogTimeDesc(taskId, nodeId);
    }

    /**
     * 获取节点执行记录
     */
    public List<NodeExecution> getNodeExecutions(Long taskId) {
        return nodeExecutionRepository.findByTaskIdOrderByStartedAt(taskId);
    }

    /**
     * 错误分析 - 分析任务中的错误信息
     */
    public ErrorAnalysis analyzeErrors(Long taskId) {
        List<TaskLog> logs = taskLogRepository.findByTaskIdOrderByLogTimeDesc(taskId);
        List<NodeExecution> executions = nodeExecutionRepository.findByTaskIdOrderByStartedAt(taskId);

        ErrorAnalysis analysis = new ErrorAnalysis();
        analysis.setTaskId(taskId);

        // 统计错误日志
        List<TaskLog> errorLogs = logs.stream()
                .filter(l -> l.getLevel() == LogLevel.ERROR)
                .toList();
        analysis.setTotalErrors(errorLogs.size());

        // 按节点分组统计错误
        Map<String, List<TaskLog>> errorsByNode = errorLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getNodeName() != null ? l.getNodeName() : "unknown"));
        analysis.setErrorsByNode(errorsByNode);

        // 找出失败的节点
        List<NodeExecution> failedNodes = executions.stream()
                .filter(e -> e.getStatus() == TaskStatus.FAILED)
                .collect(Collectors.toList());
        analysis.setFailedNodes(failedNodes);

        // 错误类型分类
        Map<String, Integer> errorTypes = new HashMap<>();
        for (TaskLog errorLog : errorLogs) {
            String errorType = classifyError(errorLog.getMessage());
            errorTypes.merge(errorType, 1, Integer::sum);
        }
        analysis.setErrorTypes(errorTypes);

        // 生成建议
        analysis.setSuggestions(generateSuggestions(analysis));

        return analysis;
    }

    /**
     * 错误分类
     */
    private String classifyError(String message) {
        if (message == null) return "UNKNOWN";
        String lower = message.toLowerCase();
        if (lower.contains("connection") || lower.contains("timeout") || lower.contains("refused")) {
            return "CONNECTION_ERROR";
        }
        if (lower.contains("null") || lower.contains("npe")) {
            return "NULL_POINTER";
        }
        if (lower.contains("duplicate") || lower.contains("unique") || lower.contains("constraint")) {
            return "DATA_CONFLICT";
        }
        if (lower.contains("format") || lower.contains("parse") || lower.contains("convert")) {
            return "DATA_FORMAT_ERROR";
        }
        if (lower.contains("permission") || lower.contains("access") || lower.contains("denied")) {
            return "PERMISSION_ERROR";
        }
        if (lower.contains("out of memory") || lower.contains("oom")) {
            return "RESOURCE_ERROR";
        }
        return "OTHER";
    }

    /**
     * 生成修复建议
     */
    private List<String> generateSuggestions(ErrorAnalysis analysis) {
        List<String> suggestions = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : analysis.getErrorTypes().entrySet()) {
            switch (entry.getKey()) {
                case "CONNECTION_ERROR":
                    suggestions.add("检测到连接错误，建议：1) 检查数据库/服务连接配置 2) 确认网络连通性 3) 增加连接超时时间");
                    break;
                case "DATA_CONFLICT":
                    suggestions.add("检测到数据冲突，建议：1) 检查目标表唯一约束 2) 使用UPSERT模式替代INSERT 3) 先清理目标数据");
                    break;
                case "DATA_FORMAT_ERROR":
                    suggestions.add("检测到数据格式错误，建议：1) 检查字段映射配置 2) 增加数据转换节点 3) 添加数据校验节点");
                    break;
                case "PERMISSION_ERROR":
                    suggestions.add("检测到权限错误，建议：1) 检查数据库账号权限 2) 确认文件读写权限 3) 检查API认证配置");
                    break;
                case "RESOURCE_ERROR":
                    suggestions.add("检测到资源不足，建议：1) 减小批处理大小 2) 增加JVM内存 3) 分批执行迁移");
                    break;
                case "NULL_POINTER":
                    suggestions.add("检测到空指针异常，建议：1) 检查源数据是否包含空值 2) 增加空值处理逻辑 3) 配置默认值");
                    break;
            }
        }

        if (!analysis.getFailedNodes().isEmpty()) {
            suggestions.add(String.format("共有 %d 个节点执行失败，可使用「从断点重启」功能从失败节点继续执行",
                    analysis.getFailedNodes().size()));
        }

        return suggestions;
    }

    /**
     * 错误分析结果
     */
    @lombok.Data
    public static class ErrorAnalysis {
        private Long taskId;
        private int totalErrors;
        private Map<String, List<TaskLog>> errorsByNode;
        private List<NodeExecution> failedNodes;
        private Map<String, Integer> errorTypes;
        private List<String> suggestions;
    }
}
