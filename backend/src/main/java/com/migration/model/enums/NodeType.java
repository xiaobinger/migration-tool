package com.migration.model.enums;

import lombok.Getter;

/**
 * 流程节点类型
 */
@Getter
public enum NodeType {
    START("start", "开始节点"),
    END("end", "结束节点"),
    DATA_EXTRACT("data_extract", "数据提取"),
    DATA_TRANSFORM("data_transform", "数据转换"),
    DATA_LOAD("data_load", "数据加载"),
    VALIDATION("validation", "数据校验"),
    CONDITION("condition", "条件判断"),
    PARALLEL("parallel", "并行网关"),
    MERGE("merge", "合并网关"),
    PARALLEL_GROUP("parallel_group", "并行组"),
    NOTIFICATION("notification", "通知"),
    SCRIPT("script", "自定义脚本"),
    DELAY("delay", "延时等待");

    private final String code;
    private final String label;

    NodeType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static NodeType fromCode(String code) {
        for (NodeType type : values()) {
            if (type.code.equals(code)) return type;
        }
        throw new IllegalArgumentException("Unknown node type: " + code);
    }
}
