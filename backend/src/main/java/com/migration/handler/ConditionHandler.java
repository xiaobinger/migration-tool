package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.engine.NodeExecutor;
import com.migration.evaluator.ConditionEvaluator;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionHandler implements NodeExecutor {

    private final ImplementationClassRegistry registry;

    @Override
    public NodeType getSupportedType() {
        return NodeType.CONDITION;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行条件判断: nodeId={}, name={}", node.getNodeId(), node.getName());

        String implementationClass = node.getImplementationClass();
        if (implementationClass != null && !implementationClass.isEmpty()) {
            return executeWithImplementation(context, node, implementationClass);
        }

        return executeDefault(context, node);
    }

    private FlowEngine.NodeResult executeWithImplementation(FlowEngine.FlowContext context, FlowNode node, String implementationClass) {
        log.info("使用指定实现类执行条件判断: implementationClass={}", implementationClass);

        ConditionEvaluator evaluator = registry.getEvaluator(implementationClass);
        if (evaluator == null) {
            return FlowEngine.NodeResult.fail("未找到实现类: " + implementationClass);
        }

        try {
            boolean result = evaluator.evaluate(context, node);
            String branch = result ? "true" : "false";
            context.getVariables().put(node.getNodeId() + "_condition", branch);

            String summary = String.format("条件判断完成（%s）: %s → %s", evaluator.getName(), node.getName(), branch);
            return FlowEngine.NodeResult.ok(summary);
        } catch (Exception e) {
            log.error("实现类执行条件判断失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("实现类执行条件判断失败: " + e.getMessage());
        }
    }

    private FlowEngine.NodeResult executeDefault(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String conditionType = config.getStr("conditionType", "simple");
        String expression = config.getStr("expression", "");

        try {
            boolean result = false;

            switch (conditionType) {
                case "simple":
                    result = evaluateSimpleCondition(context, config);
                    break;
                case "expression":
                    result = evaluateExpression(context, expression);
                    break;
                case "data_quality":
                    result = evaluateDataQuality(context, config);
                    break;
                default:
                    return FlowEngine.NodeResult.fail("不支持的条件类型: " + conditionType);
            }

            String branch = result ? "true" : "false";
            context.getVariables().put(node.getNodeId() + "_condition", branch);

            String summary = String.format("条件判断完成: %s → %s", node.getName(), branch);
            return FlowEngine.NodeResult.ok(summary);

        } catch (Exception e) {
            log.error("条件判断失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("条件判断失败: " + e.getMessage());
        }
    }

    private boolean evaluateSimpleCondition(FlowEngine.FlowContext context, JSONObject config) {
        String variable = config.getStr("variable", "");
        String operator = config.getStr("operator", "eq");
        String value = config.getStr("value", "");

        Object varValue = context.getVariables().get(variable);
        if (varValue == null) return false;

        String varStr = String.valueOf(varValue);
        switch (operator) {
            case "eq": return varStr.equals(value);
            case "ne": return !varStr.equals(value);
            case "gt": return Double.parseDouble(varStr) > Double.parseDouble(value);
            case "lt": return Double.parseDouble(varStr) < Double.parseDouble(value);
            case "ge": return Double.parseDouble(varStr) >= Double.parseDouble(value);
            case "le": return Double.parseDouble(varStr) <= Double.parseDouble(value);
            case "contains": return varStr.contains(value);
            default: return false;
        }
    }

    private boolean evaluateExpression(FlowEngine.FlowContext context, String expression) {
        log.info("评估表达式: {}", expression);
        return true;
    }

    private boolean evaluateDataQuality(FlowEngine.FlowContext context, JSONObject config) {
        String metric = config.getStr("metric", "success_rate");
        double threshold = config.getDouble("threshold", 0.95);
        log.info("评估数据质量: metric={}, threshold={}", metric, threshold);
        return true;
    }
}
