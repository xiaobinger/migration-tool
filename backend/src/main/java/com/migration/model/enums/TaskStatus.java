package com.migration.model.enums;

import lombok.Getter;

/**
 * 任务执行状态
 */
@Getter
public enum TaskStatus {
    PENDING("pending", "待执行"),
    RUNNING("running", "执行中"),
    PAUSED("paused", "已暂停"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    PARTIAL_SUCCESS("partial_success", "部分成功"),
    CANCELLED("cancelled", "已取消"),
    SKIPPED("skipped", "已跳过");

    private final String code;
    private final String label;

    TaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
