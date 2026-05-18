package com.migration.model.enums;

import lombok.Getter;

/**
 * 日志级别
 */
@Getter
public enum LogLevel {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn"),
    ERROR("error");

    private final String code;

    LogLevel(String code) {
        this.code = code;
    }
}
