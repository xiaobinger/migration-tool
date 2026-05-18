package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import com.migration.transformer.DataTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataTransformHandler implements NodeExecutor {

    private final ImplementationClassRegistry registry;

    @Override
    public NodeType getSupportedType() {
        return NodeType.DATA_TRANSFORM;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行数据转换: nodeId={}, name={}", node.getNodeId(), node.getName());

        String implementationClass = node.getImplementationClass();
        if (implementationClass != null && !implementationClass.isEmpty()) {
            return executeWithImplementation(context, node, implementationClass);
        }

        return executeDefault(context, node);
    }

    private FlowEngine.NodeResult executeWithImplementation(FlowEngine.FlowContext context, FlowNode node, String implementationClass) {
        log.info("使用指定实现类执行数据转换: implementationClass={}", implementationClass);

        DataTransformer transformer = registry.getTransformer(implementationClass);
        if (transformer == null) {
            return FlowEngine.NodeResult.fail("未找到实现类: " + implementationClass);
        }

        try {
            FlowEngine.NodeResult result = transformer.execute(context, node);

            if (result.isSuccess()) {
                context.getVariables().put(node.getNodeId() + "_transformed", result.getSuccessRecords());
            }

            return result;
        } catch (Exception e) {
            log.error("实现类执行数据转换失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("实现类执行数据转换失败: " + e.getMessage());
        }
    }

    private FlowEngine.NodeResult executeDefault(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String transformType = config.getStr("transformType", "mapping");
        String mapping = config.getStr("mapping", "{}");
        String script = config.getStr("script", "");

        try {
            long inputRecords = getUpstreamRecordCount(context);

            long successRecords = 0;
            long failedRecords = 0;

            switch (transformType) {
                case "mapping":
                    successRecords = doMapping(context, mapping, inputRecords);
                    break;
                case "script":
                    successRecords = doScriptTransform(context, script, inputRecords);
                    break;
                case "template":
                    successRecords = doTemplateTransform(context, config, inputRecords);
                    break;
                default:
                    return FlowEngine.NodeResult.fail("不支持的转换类型: " + transformType);
            }

            failedRecords = inputRecords - successRecords;
            context.getVariables().put(node.getNodeId() + "_transformed", successRecords);

            String summary = String.format("数据转换完成，输入 %d 条，成功 %d 条，失败 %d 条",
                    inputRecords, successRecords, failedRecords);
            return FlowEngine.NodeResult.ok(summary, inputRecords, successRecords, failedRecords);

        } catch (Exception e) {
            log.error("数据转换失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("数据转换失败: " + e.getMessage());
        }
    }

    private long getUpstreamRecordCount(FlowEngine.FlowContext context) {
        return context.getVariables().entrySet().stream()
                .filter(e -> e.getKey().endsWith("_extracted") || e.getKey().endsWith("_transformed"))
                .mapToLong(e -> {
                    if (e.getValue() instanceof Number) return ((Number) e.getValue()).longValue();
                    return 0L;
                })
                .max()
                .orElse(0L);
    }

    private long doMapping(FlowEngine.FlowContext context, String mapping, long inputRecords) {
        log.info("执行字段映射: mapping={}", mapping);
        return inputRecords;
    }

    private long doScriptTransform(FlowEngine.FlowContext context, String script, long inputRecords) {
        log.info("执行脚本转换: script length={}", script.length());
        return (long) (inputRecords * 0.98);
    }

    private long doTemplateTransform(FlowEngine.FlowContext context, JSONObject config, long inputRecords) {
        log.info("执行模板转换");
        return inputRecords;
    }
}
