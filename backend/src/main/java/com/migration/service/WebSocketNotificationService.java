package com.migration.service;

import com.migration.engine.FlowEngine;
import com.migration.model.dto.TaskProgressMessage;
import com.migration.model.entity.MigrationTask;
import com.migration.model.entity.FlowNode;
import com.migration.websocket.TaskProgressWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final TaskProgressWebSocket webSocket;

    /**
     * 通知任务开始
     */
    public void notifyTaskStart(MigrationTask task) {
        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("task_start")
                .taskId(task.getId())
                .taskStatus(task.getStatus().name())
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播任务开始: taskId={}", task.getId());
    }

    /**
     * 通知任务完成
     */
    public void notifyTaskComplete(MigrationTask task) {
        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("task_complete")
                .taskId(task.getId())
                .taskStatus(task.getStatus().name())
                .progress(100)
                .extractedRecords(task.getExtractedRecords())
                .loadedRecords(task.getLoadedRecords())
                .loadedSuccessRecords(task.getLoadedSuccessRecords())
                .loadedFailedRecords(task.getLoadedFailedRecords())
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播任务完成: taskId={}", task.getId());
    }

    /**
     * 通知任务失败
     */
    public void notifyTaskFailed(MigrationTask task, String error) {
        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("task_failed")
                .taskId(task.getId())
                .taskStatus(task.getStatus().name())
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播任务失败: taskId={}, error={}", task.getId(), error);
    }

    /**
     * 通知节点开始
     */
    public void notifyNodeStart(MigrationTask task, FlowNode node) {
        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("node_start")
                .taskId(task.getId())
                .nodeId(node.getNodeId())
                .nodeName(node.getName())
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播节点开始: taskId={}, nodeId={}", task.getId(), node.getNodeId());
    }

    /**
     * 通知节点完成
     */
    public void notifyNodeComplete(MigrationTask task, FlowNode node, FlowEngine.NodeResult result) {
        int progress = task.getTotalNodes() > 0
                ? (task.getCompletedNodes() * 100) / task.getTotalNodes()
                : 0;

        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("node_complete")
                .taskId(task.getId())
                .nodeId(node.getNodeId())
                .nodeName(node.getName())
                .nodeStatus("SUCCESS")
                .progress(progress)
                .extractedRecords(task.getExtractedRecords())
                .loadedRecords(task.getLoadedRecords())
                .loadedSuccessRecords(task.getLoadedSuccessRecords())
                .loadedFailedRecords(task.getLoadedFailedRecords())
                .message(result.getSummary())
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播节点完成: taskId={}, nodeId={}", task.getId(), node.getNodeId());
    }

    /**
     * 通知节点错误
     */
    public void notifyNodeError(MigrationTask task, FlowNode node, String error) {
        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("node_error")
                .taskId(task.getId())
                .nodeId(node.getNodeId())
                .nodeName(node.getName())
                .nodeStatus("FAILED")
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
        log.info("广播节点错误: taskId={}, nodeId={}, error={}", task.getId(), node.getNodeId(), error);
    }

    /**
     * 发送任务进度
     */
    public void notifyProgress(MigrationTask task) {
        int progress = task.getTotalNodes() > 0
                ? (task.getCompletedNodes() * 100) / task.getTotalNodes()
                : 0;

        TaskProgressMessage message = TaskProgressMessage.builder()
                .type("progress")
                .taskId(task.getId())
                .taskStatus(task.getStatus().name())
                .progress(progress)
                .extractedRecords(task.getExtractedRecords())
                .loadedRecords(task.getLoadedRecords())
                .loadedSuccessRecords(task.getLoadedSuccessRecords())
                .loadedFailedRecords(task.getLoadedFailedRecords())
                .timestamp(System.currentTimeMillis())
                .build();
        webSocket.broadcast(message);
    }
}
