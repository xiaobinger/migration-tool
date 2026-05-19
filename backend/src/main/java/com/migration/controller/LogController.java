package com.migration.controller;

import com.migration.model.entity.LoadFailureRecord;
import com.migration.model.entity.NodeExecution;
import com.migration.model.entity.TaskLog;
import com.migration.repository.LoadFailureRecordRepository;
import com.migration.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;
    private final LoadFailureRecordRepository loadFailureRecordRepository;

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
        for (LoadFailureRecord record : unretried) {
            record.setRetried(true);
            record.setRetriedAt(LocalDateTime.now());
        }
        loadFailureRecordRepository.saveAll(unretried);
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "已标记 " + unretried.size() + " 条失败记录为待重试",
                "count", unretried.size()
        ));
    }

    @DeleteMapping("/task/{taskId}/failures")
    public ResponseEntity<?> clearLoadFailures(@PathVariable Long taskId) {
        List<LoadFailureRecord> all = loadFailureRecordRepository.findByTaskId(taskId);
        loadFailureRecordRepository.deleteAll(all);
        return ResponseEntity.ok().body(java.util.Map.of("message", "已清除所有失败记录"));
    }
}
