package com.migration.evaluator;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DataQualityConditionEvaluator implements ConditionEvaluator {

    @Override
    public String getName() { return "DataQualityConditionEvaluator"; }

    @Override
    public String getFullName() { return "com.migration.evaluator.DataQualityConditionEvaluator"; }

    @Override
    public String getDescription() { return "数据质量条件评估器，基于数据完整性/准确率/阈值判断"; }

    @Override
    public boolean evaluate(FlowEngine.FlowContext context, FlowNode node) {
        String metric = getConfig(node, "metric", "success_rate");
        double threshold = Double.parseDouble(getConfig(node, "threshold", "0.95"));
        String sourceNodeId = getConfig(node, "sourceNodeId", "");

        log.info("数据质量条件评估: metric={}, threshold={}, sourceNodeId={}", metric, threshold, sourceNodeId);

        double actualValue = getMetricValue(context, metric, sourceNodeId);
        boolean result = actualValue >= threshold;

        log.info("数据质量评估结果: {} = {} (阈值: {}), 结果: {}", metric, actualValue, threshold, result);
        return result;
    }

    private double getMetricValue(FlowEngine.FlowContext context, String metric, String sourceNodeId) {
        Map<String, Object> variables = context.getVariables();
        Map<String, FlowEngine.NodeResult> nodeResults = context.getNodeResults();

        switch (metric) {
            case "success_rate": {
                FlowEngine.NodeResult nodeResult = findNodeResult(nodeResults, sourceNodeId);
                if (nodeResult == null || nodeResult.getTotalRecords() == 0) return 0.0;
                return (double) nodeResult.getSuccessRecords() / nodeResult.getTotalRecords();
            }
            case "failure_rate": {
                FlowEngine.NodeResult nodeResult = findNodeResult(nodeResults, sourceNodeId);
                if (nodeResult == null || nodeResult.getTotalRecords() == 0) return 0.0;
                return (double) nodeResult.getFailedRecords() / nodeResult.getTotalRecords();
            }
            case "record_count": {
                String key = sourceNodeId.isEmpty() ? "" : sourceNodeId + "_extracted";
                Object value = variables.get(key);
                if (value instanceof Number) return ((Number) value).doubleValue();
                return 0.0;
            }
            case "completeness": {
                FlowEngine.NodeResult nodeResult = findNodeResult(nodeResults, sourceNodeId);
                if (nodeResult == null) return 0.0;
                long total = nodeResult.getTotalRecords();
                long success = nodeResult.getSuccessRecords();
                return total > 0 ? (double) success / total : 0.0;
            }
            default:
                log.warn("未知数据质量指标: {}", metric);
                return 0.0;
        }
    }

    private FlowEngine.NodeResult findNodeResult(Map<String, FlowEngine.NodeResult> nodeResults, String sourceNodeId) {
        if (!sourceNodeId.isEmpty()) {
            return nodeResults.get(sourceNodeId);
        }
        return nodeResults.values().stream()
                .reduce((a, b) -> b)
                .orElse(null);
    }
}
