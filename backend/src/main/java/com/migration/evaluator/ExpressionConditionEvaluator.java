package com.migration.evaluator;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ExpressionConditionEvaluator implements ConditionEvaluator {

    @Override
    public String getName() { return "ExpressionConditionEvaluator"; }

    @Override
    public String getFullName() { return "com.migration.evaluator.ExpressionConditionEvaluator"; }

    @Override
    public String getDescription() { return "表达式条件评估器，支持变量比较和逻辑运算"; }

    @Override
    public boolean evaluate(FlowEngine.FlowContext context, FlowNode node) {
        String expression = getConfig(node, "expression", "");
        String operator = getConfig(node, "operator", "eq");
        String leftVar = getConfig(node, "leftVariable", "");
        String rightValue = getConfig(node, "rightValue", "");

        log.info("表达式条件评估: expression={}, operator={}, leftVar={}, rightValue={}", expression, operator, leftVar, rightValue);

        if (!expression.isEmpty()) {
            return evaluateExpression(context, expression);
        }

        Object leftValue = context.getVariables().get(leftVar);
        if (leftValue == null) {
            log.warn("变量不存在: {}", leftVar);
            return false;
        }

        return compareValues(leftValue, rightValue, operator);
    }

    private boolean evaluateExpression(FlowEngine.FlowContext context, String expression) {
        Map<String, Object> variables = context.getVariables();
        String result = expression;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        if (result.contains("&&")) {
            String[] parts = result.split("&&");
            for (String part : parts) {
                if (!evaluateSimpleExpression(part.trim())) {
                    return false;
                }
            }
            return true;
        }

        if (result.contains("||")) {
            String[] parts = result.split("\\|\\|");
            for (String part : parts) {
                if (evaluateSimpleExpression(part.trim())) {
                    return true;
                }
            }
            return false;
        }

        return evaluateSimpleExpression(result);
    }

    private boolean evaluateSimpleExpression(String expr) {
        if (expr.contains(">=")) {
            String[] parts = expr.split(">=");
            return Double.parseDouble(parts[0].trim()) >= Double.parseDouble(parts[1].trim());
        } else if (expr.contains("<=")) {
            String[] parts = expr.split("<=");
            return Double.parseDouble(parts[0].trim()) <= Double.parseDouble(parts[1].trim());
        } else if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            return !parts[0].trim().equals(parts[1].trim());
        } else if (expr.contains(">")) {
            String[] parts = expr.split(">");
            return Double.parseDouble(parts[0].trim()) > Double.parseDouble(parts[1].trim());
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<");
            return Double.parseDouble(parts[0].trim()) < Double.parseDouble(parts[1].trim());
        } else if (expr.contains("==")) {
            String[] parts = expr.split("==");
            return parts[0].trim().equals(parts[1].trim());
        }
        return Boolean.parseBoolean(expr.trim());
    }

    private boolean compareValues(Object leftValue, String rightValue, String operator) {
        String leftStr = String.valueOf(leftValue);
        switch (operator) {
            case "eq": return leftStr.equals(rightValue);
            case "ne": return !leftStr.equals(rightValue);
            case "gt": return Double.parseDouble(leftStr) > Double.parseDouble(rightValue);
            case "ge": return Double.parseDouble(leftStr) >= Double.parseDouble(rightValue);
            case "lt": return Double.parseDouble(leftStr) < Double.parseDouble(rightValue);
            case "le": return Double.parseDouble(leftStr) <= Double.parseDouble(rightValue);
            case "contains": return leftStr.contains(rightValue);
            case "startsWith": return leftStr.startsWith(rightValue);
            case "endsWith": return leftStr.endsWith(rightValue);
            default:
                log.warn("未知操作符: {}", operator);
                return false;
        }
    }
}
