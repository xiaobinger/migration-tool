package com.migration.evaluator;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;

public interface ConditionEvaluator {

    String getName();

    String getFullName();

    String getDescription();

    boolean evaluate(FlowEngine.FlowContext context, FlowNode node);

    default String getConfig(FlowNode node, String key, String defaultValue) {
        if (node.getConfig() == null || node.getConfig().isEmpty()) {
            return defaultValue;
        }
        try {
            cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(node.getConfig());
            return config.getStr(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
