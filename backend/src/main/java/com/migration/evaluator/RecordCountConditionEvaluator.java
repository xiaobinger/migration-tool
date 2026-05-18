package com.migration.evaluator;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class RecordCountConditionEvaluator implements ConditionEvaluator {

    @Override
    public String getName() { return "RecordCountConditionEvaluator"; }

    @Override
    public String getFullName() { return "com.migration.evaluator.RecordCountConditionEvaluator"; }

    @Override
    public String getDescription() { return "记录数条件评估器，基于上游数据量阈值判断"; }

    @Override
    public boolean evaluate(FlowEngine.FlowContext context, FlowNode node) {
        String operator = getConfig(node, "operator", "ge");
        long threshold = Long.parseLong(getConfig(node, "threshold", "0"));
        String sourceNodeId = getConfig(node, "sourceNodeId", "");

        log.info("记录数条件评估: operator={}, threshold={}, sourceNodeId={}", operator, threshold, sourceNodeId);

        long actualCount = getRecordCount(context, sourceNodeId);
        boolean result = compareCount(actualCount, threshold, operator);

        log.info("记录数评估结果: actual={}, threshold={}, operator={}, result={}", actualCount, threshold, operator, result);
        return result;
    }

    private long getRecordCount(FlowEngine.FlowContext context, String sourceNodeId) {
        Map<String, Object> variables = context.getVariables();

        if (!sourceNodeId.isEmpty()) {
            Object value = variables.get(sourceNodeId + "_extracted");
            if (value == null) value = variables.get(sourceNodeId + "_transformed");
            if (value == null) value = variables.get(sourceNodeId + "_loaded");
            if (value instanceof Number) return ((Number) value).longValue();
        }

        return variables.entrySet().stream()
                .filter(e -> e.getKey().endsWith("_extracted") || e.getKey().endsWith("_transformed"))
                .mapToLong(e -> e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0L)
                .max().orElse(0L);
    }

    private boolean compareCount(long actual, long threshold, String operator) {
        switch (operator) {
            case "eq": return actual == threshold;
            case "ne": return actual != threshold;
            case "gt": return actual > threshold;
            case "ge": return actual >= threshold;
            case "lt": return actual < threshold;
            case "le": return actual <= threshold;
            default:
                log.warn("未知操作符: {}", operator);
                return false;
        }
    }
}
