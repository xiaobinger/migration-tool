package com.migration.controller;

import com.migration.model.dto.TaskExecutionRequest;
import com.migration.model.entity.MigrationTask;
import com.migration.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<MigrationTask>> listTasks(
            @RequestParam(required = false) Long flowDefinitionId) {
        return ResponseEntity.ok(taskService.listTasks(flowDefinitionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MigrationTask> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping
    public ResponseEntity<MigrationTask> startTask(@RequestBody TaskExecutionRequest request) {
        return ResponseEntity.ok(taskService.startTask(request));
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<MigrationTask> restartTask(
            @PathVariable Long id,
            @RequestParam(required = false) String restartFromNodeId) {
        return ResponseEntity.ok(taskService.restartTask(id, restartFromNodeId));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<MigrationTask> pauseTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.pauseTask(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MigrationTask> cancelTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.cancelTask(id));
    }

    @GetMapping("/running")
    public ResponseEntity<List<MigrationTask>> getRunningTasks() {
        return ResponseEntity.ok(taskService.getRunningTasks());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/completed")
    public ResponseEntity<Void> clearCompletedTasks() {
        taskService.clearCompletedTasks();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAllTasks() {
        taskService.clearAllTasks();
        return ResponseEntity.ok().build();
    }
}
