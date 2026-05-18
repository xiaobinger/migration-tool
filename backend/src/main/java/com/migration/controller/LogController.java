package com.migration.controller;

import com.migration.model.entity.NodeExecution;
import com.migration.model.entity.TaskLog;
import com.migration.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

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
}
