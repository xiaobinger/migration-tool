package com.migration.controller;

import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.loader.DataLoader;
import com.migration.model.entity.*;
import com.migration.repository.FlowNodeRepository;
import com.migration.repository.LoadFailureRecordRepository;
import com.migration.repository.MigrationTaskRepository;
import com.migration.repository.NodeExecutionRepository;
import com.migration.service.LogService;
import com.migration.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;
    private final LoadFailureRecordRepository loadFailureRecordRepository;
    private final MigrationTaskRepository migrationTaskRepository;
    private final NodeExecutionRepository nodeExecutionRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final ImplementationClassRegistry implementationClassRegistry;
    private final WebSocketNotificationService webSocketNotificationService;

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskLog>> getTaskLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(logService.getTaskLogs(taskId));
    }

    @GetMapping("/task/{taskId}/node/{nodeId}")
    public ResponseEntity<List<TaskLog>> getNodeLogs(
            @PathVariable Long taskId, @PathVariable String nodeId) {
        return ResponseEntity.ok(logService.getNodeLogs(taskId, nodeId));
    }

    @GetMapping("/task/{taskId}/executions")
    public ResponseEntity<List<NodeExecution>> getNodeExecutions(@PathVariable Long taskId) {
        return ResponseEntity.ok(logService.getNodeExecutions(taskId));
    }

    @GetMapping("/task/{taskId}/error-analysis")
    public ResponseEntity<LogService.ErrorAnalysis> analyzeErrors(@PathVariable Long taskId) {
        return ResponseEntity.ok(logService.analyzeErrors(taskId));
    }

    @GetMapping("/task/{taskId}/failures")
    public ResponseEntity<List<LoadFailureRecord>> getLoadFailures(@PathVariable Long taskId) {
        return ResponseEntity.ok(loadFailureRecordRepository.findByTaskId(taskId));
    }

    @GetMapping("/task/{taskId}/failures/{nodeId}")
    public ResponseEntity<List<LoadFailureRecord>> getLoadFailuresByNode(
            @PathVariable Long taskId, @PathVariable String nodeId) {
        return ResponseEntity.ok(loadFailureRecordRepository.findByTaskIdAndNodeId(taskId, nodeId));
    }

    @PostMapping("/task/{taskId}/failures/retry")
    public ResponseEntity<?> retryLoadFailures(@PathVariable Long taskId) {
        List<LoadFailureRecord> unretried = loadFailureRecordRepository.findByTaskIdAndRetriedFalse(taskId);
        if (unretried.isEmpty()) {
            return ResponseEntity.ok().body(Map.of(
                    "message", "没有需要重试的失败记录",
                    "totalCount", 0,
                    "successCount", 0,
                    "failCount", 0
            ));
        }

        MigrationTask task = migrationTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "任务不存在"));
        }

        Map<String, List<LoadFailureRecord>> byNode = unretried.stream()
                .collect(Collectors.groupingBy(LoadFailureRecord::getNodeId));

        long totalSuccess = 0;
        long totalFail = 0;
        List<LoadFailureRecord> stillFailed = new ArrayList<>();

        for (Map.Entry<String, List<LoadFailureRecord>> entry : byNode.entrySet()) {
            String nodeId = entry.getKey();
            List<LoadFailureRecord> nodeFailures = entry.getValue();

            List<FlowNode> nodes = flowNodeRepository.findByFlowDefinitionIdOrderBySortOrder(task.getFlowDefinitionId());
            FlowNode node = nodes.stream()
                    .filter(n -> n.getNodeId().equals(nodeId))
                    .findFirst()
                    .orElse(null);

            if (node == null || node.getImplementationClass() == null || node.getImplementationClass().isEmpty()) {
                for (LoadFailureRecord r : nodeFailures) {
                    r.setRetried(true);
                    r.setRetriedAt(LocalDateTime.now());
                    stillFailed.add(r);
                    totalFail++;
                }
                continue;
            }

            DataLoader loader = implementationClassRegistry.getLoader(node.getImplementationClass());
            if (loader == null) {
                for (LoadFailureRecord r : nodeFailures) {
                    r.setRetried(true);
                    r.setRetriedAt(LocalDateTime.now());
                    stillFailed.add(r);
                    totalFail++;
                }
                continue;
            }

            FlowEngine.FlowContext context = new FlowEngine.FlowContext();
            context.setTaskId(taskId);
            context.setTask(task);
            context.setNodeResults(new java.util.concurrent.ConcurrentHashMap<>());
            context.setVariables(new java.util.concurrent.ConcurrentHashMap<>());

            List<Object> rowDataList = new ArrayList<>();
            for (LoadFailureRecord r : nodeFailures) {
                try {
                    Object parsed = cn.hutool.json.JSONUtil.parse(r.getRowData());
                    if (parsed instanceof Map) {
                        rowDataList.add(parsed);
                    }
                } catch (Exception e) {
                    r.setRetried(true);
                    r.setRetriedAt(LocalDateTime.now());
                    r.setErrorMessage("重试解析行数据失败: " + e.getMessage());
                    stillFailed.add(r);
                    totalFail++;
                }
            }
            context.setData(rowDataList);

            try {
                FlowEngine.NodeResult result = loader.execute(context, node);

                Set<String> stillFailedRowData = new HashSet<>();
                if (result.getFailedRows() != null) {
                    for (FlowEngine.FailedRow fr : result.getFailedRows()) {
                        stillFailedRowData.add(fr.getRowData());
                    }
                }

                long nodeRetrySuccess = 0;
                for (LoadFailureRecord r : nodeFailures) {
                    r.setRetried(true);
                    r.setRetriedAt(LocalDateTime.now());
                    if (stillFailedRowData.contains(r.getRowData())) {
                        String newError = result.getFailedRows().stream()
                                .filter(fr -> fr.getRowData().equals(r.getRowData()))
                                .map(FlowEngine.FailedRow::getErrorMessage)
                                .findFirst()
                                .orElse("重试仍失败");
                        r.setErrorMessage("重试仍失败: " + newError);
                        stillFailed.add(r);
                        totalFail++;
                    } else {
                        nodeRetrySuccess++;
                        totalSuccess++;
                    }
                }

                if (nodeRetrySuccess > 0) {
                    synchronized (task) {
                        task.setLoadedSuccessRecords(task.getLoadedSuccessRecords() + nodeRetrySuccess);
                        task.setLoadedFailedRecords(Math.max(0, task.getLoadedFailedRecords() - nodeRetrySuccess));
                        migrationTaskRepository.save(task);
                    }
                    webSocketNotificationService.notifyProgress(task);
                }
            } catch (Exception e) {
                for (LoadFailureRecord r : nodeFailures) {
                    r.setRetried(true);
                    r.setRetriedAt(LocalDateTime.now());
                    r.setErrorMessage("重试异常: " + e.getMessage());
                    stillFailed.add(r);
                }
                totalFail += nodeFailures.size();
            }
        }

        loadFailureRecordRepository.saveAll(unretried);

        return ResponseEntity.ok().body(Map.of(
                "message", String.format("重试完成: 成功 %d 条，仍失败 %d 条", totalSuccess, totalFail),
                "totalCount", unretried.size(),
                "successCount", totalSuccess,
                "failCount", totalFail
        ));
    }

    @DeleteMapping("/task/{taskId}/failures")
    public ResponseEntity<?> clearLoadFailures(@PathVariable Long taskId) {
        List<LoadFailureRecord> all = loadFailureRecordRepository.findByTaskId(taskId);
        loadFailureRecordRepository.deleteAll(all);
        return ResponseEntity.ok().body(Map.of("message", "已清除所有失败记录"));
    }
}
