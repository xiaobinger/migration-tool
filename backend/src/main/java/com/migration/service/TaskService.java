package com.migration.service;

import com.migration.engine.FlowEngine;
import com.migration.model.dto.TaskExecutionRequest;
import com.migration.model.entity.MigrationTask;
import com.migration.model.enums.TaskStatus;
import com.migration.repository.MigrationTaskRepository;
import com.migration.repository.NodeExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final MigrationTaskRepository taskRepository;
    private final NodeExecutionRepository nodeExecutionRepository;
    private final FlowEngine flowEngine;

    /**
     * 创建并启动迁移任务
     */
    public MigrationTask startTask(TaskExecutionRequest request) {
        MigrationTask task = MigrationTask.builder()
                .name(request.getName())
                .flowDefinitionId(request.getFlowDefinitionId())
                .status(TaskStatus.PENDING)
                .createdBy("system")
                .build();
        task = taskRepository.save(task);

        // 异步执行
        executeAsync(task, request.isRestart() ? request.getRestartFromNodeId() : null);

        return task;
    }

    /**
     * 重启任务（从断点续传）
     */
    public MigrationTask restartTask(Long taskId, String restartFromNodeId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

        if (task.getStatus() != TaskStatus.FAILED && task.getStatus() != TaskStatus.PAUSED && task.getStatus() != TaskStatus.CANCELLED) {
            throw new RuntimeException("只能重启失败、暂停或取消的任务，当前状态: " + task.getStatus().getLabel());
        }

        String effectiveRestartNodeId = restartFromNodeId;
        if (effectiveRestartNodeId == null || effectiveRestartNodeId.isEmpty()) {
            effectiveRestartNodeId = task.getRestartFromNodeId();
        }

        task.setStatus(TaskStatus.PENDING);
        task.setErrorMessage(null);
        task.setFinishedAt(null);
        task.setRestartFromNodeId(effectiveRestartNodeId);
        task = taskRepository.save(task);

        executeAsync(task, effectiveRestartNodeId);

        return task;
    }

    /**
     * 暂停任务
     */
    public MigrationTask pauseTask(Long taskId) {
        flowEngine.pauseTask(taskId);
        return taskRepository.findById(taskId).orElseThrow();
    }

    /**
     * 取消任务
     */
    public MigrationTask cancelTask(Long taskId) {
        flowEngine.cancelTask(taskId);
        return taskRepository.findById(taskId).orElseThrow();
    }

    /**
     * 获取任务详情
     */
    public MigrationTask getTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow();
    }

    /**
     * 获取任务列表
     */
    public List<MigrationTask> listTasks(Long flowDefinitionId) {
        if (flowDefinitionId != null) {
            return taskRepository.findByFlowDefinitionId(flowDefinitionId);
        }
        return taskRepository.findAll();
    }

    /**
     * 获取运行中的任务
     */
    public List<MigrationTask> getRunningTasks() {
        return taskRepository.findByStatusIn(List.of(TaskStatus.RUNNING, TaskStatus.PENDING));
    }

    /**
     * 异步执行任务
     */
    @Async("taskExecutor")
    public void executeAsync(MigrationTask task, String restartFromNodeId) {
        flowEngine.execute(task, restartFromNodeId);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new RuntimeException("运行中的任务不能删除");
        }
        nodeExecutionRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public void clearCompletedTasks() {
        List<MigrationTask> completed = taskRepository.findByStatusIn(
                List.of(TaskStatus.SUCCESS, TaskStatus.FAILED, TaskStatus.CANCELLED, TaskStatus.PARTIAL_SUCCESS)
        );
        for (MigrationTask task : completed) {
            nodeExecutionRepository.deleteByTaskId(task.getId());
        }
        taskRepository.deleteAll(completed);
    }

    @Transactional
    public void clearAllTasks() {
        List<MigrationTask> running = taskRepository.findByStatusIn(
                List.of(TaskStatus.RUNNING, TaskStatus.PENDING)
        );
        if (!running.isEmpty()) {
            throw new RuntimeException("存在运行中的任务，请先停止后再清除");
        }
        nodeExecutionRepository.deleteAll();
        taskRepository.deleteAll();
    }
}
