package com.migration.model.dto;

import lombok.Data;

import java.util.Map;

/**
 * 任务执行请求
 */
@Data
public class TaskExecutionRequest {

    /** 任务名称 */
    private String name;

    /** 流程定义ID */
    private Long flowDefinitionId;

    /** 执行参数 */
    private Map<String, Object> params;

    /** 是否从断点重启 */
    private boolean restart = false;

    /** 重启的节点ID（断点续传） */
    private String restartFromNodeId;
}
