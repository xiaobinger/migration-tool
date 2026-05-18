package com.migration.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket推送的任务进度消息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskProgressMessage {

    /** 消息类型: progress / node_start / node_complete / node_error / task_complete / task_failed / log */
    private String type;

    /** 任务ID */
    private Long taskId;

    /** 当前节点ID */
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 任务状态 */
    private String taskStatus;

    /** 节点状态 */
    private String nodeStatus;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 总记录数 */
    private Long totalRecords;

    /** 成功记录数 */
    private Long successRecords;

    /** 失败记录数 */
    private Long failedRecords;

    /** 消息内容 */
    private String message;

    /** 错误信息 */
    private String error;

    /** 时间戳 */
    private Long timestamp;

    public static TaskProgressMessage progress(Long taskId, int progress, String currentNodeId) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("progress");
        msg.setTaskId(taskId);
        msg.setProgress(progress);
        msg.setNodeId(currentNodeId);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage nodeStart(Long taskId, String nodeId, String nodeName) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("node_start");
        msg.setTaskId(taskId);
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setNodeStatus("running");
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage nodeComplete(Long taskId, String nodeId, String nodeName) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("node_complete");
        msg.setTaskId(taskId);
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setNodeStatus("success");
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage nodeError(Long taskId, String nodeId, String nodeName, String error) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("node_error");
        msg.setTaskId(taskId);
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setNodeStatus("failed");
        msg.setError(error);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage taskComplete(Long taskId) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("task_complete");
        msg.setTaskId(taskId);
        msg.setTaskStatus("success");
        msg.setProgress(100);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage taskFailed(Long taskId, String error) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("task_failed");
        msg.setTaskId(taskId);
        msg.setTaskStatus("failed");
        msg.setError(error);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static TaskProgressMessage log(Long taskId, String nodeId, String message) {
        TaskProgressMessage msg = new TaskProgressMessage();
        msg.setType("log");
        msg.setTaskId(taskId);
        msg.setNodeId(nodeId);
        msg.setMessage(message);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
