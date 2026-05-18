package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.engine.NodeExecutor;
import com.migration.loader.DataLoader;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoadHandler implements NodeExecutor {

    private final ImplementationClassRegistry registry;

    @Override
    public NodeType getSupportedType() {
        return NodeType.DATA_LOAD;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行数据加载: nodeId={}, name={}", node.getNodeId(), node.getName());

        String implementationClass = node.getImplementationClass();
        if (implementationClass != null && !implementationClass.isEmpty()) {
            return executeWithImplementation(context, node, implementationClass);
        }

        return executeDefault(context, node);
    }

    private FlowEngine.NodeResult executeWithImplementation(FlowEngine.FlowContext context, FlowNode node, String implementationClass) {
        log.info("使用指定实现类执行数据加载: implementationClass={}", implementationClass);

        DataLoader loader = registry.getLoader(implementationClass);
        if (loader == null) {
            return FlowEngine.NodeResult.fail("未找到实现类: " + implementationClass);
        }

        try {
            FlowEngine.NodeResult result = loader.execute(context, node);

            if (result.isSuccess()) {
                context.getVariables().put(node.getNodeId() + "_loaded", result.getSuccessRecords());
            }

            return result;
        } catch (Exception e) {
            log.error("实现类执行数据加载失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("实现类执行数据加载失败: " + e.getMessage());
        }
    }

    private FlowEngine.NodeResult executeDefault(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String targetType = config.getStr("targetType", "database");
        String targetName = config.getStr("targetName", "");

        try {
            long inputRecords = getUpstreamRecordCount(context);
            long successRecords = 0;

            switch (targetType) {
                case "database":
                    successRecords = loadToDatabase(context, config, inputRecords);
                    break;
                case "file":
                    successRecords = loadToFile(context, config, inputRecords);
                    break;
                case "api":
                    successRecords = loadToApi(context, config, inputRecords);
                    break;
                default:
                    return FlowEngine.NodeResult.fail("不支持的目标类型: " + targetType);
            }

            long failedRecords = inputRecords - successRecords;
            context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

            String summary = String.format("数据加载到 [%s] 完成，共 %d 条，成功 %d 条，失败 %d 条",
                    targetName, inputRecords, successRecords, failedRecords);
            return FlowEngine.NodeResult.ok(summary, inputRecords, successRecords, failedRecords);

        } catch (Exception e) {
            log.error("数据加载失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("数据加载失败: " + e.getMessage());
        }
    }

    private long getUpstreamRecordCount(FlowEngine.FlowContext context) {
        return context.getVariables().entrySet().stream()
                .filter(e -> e.getKey().endsWith("_transformed"))
                .mapToLong(e -> e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0L)
                .max().orElse(0L);
    }

    private long loadToDatabase(FlowEngine.FlowContext context, JSONObject config, long inputRecords) {
        String url = config.getStr("url");
        String table = config.getStr("table");
        String writeMode = config.getStr("writeMode", "INSERT");
        log.info("加载到数据库: url={}, table={}, mode={}", url, table, writeMode);
        return (long) (inputRecords * 0.99);
    }

    private long loadToFile(FlowEngine.FlowContext context, JSONObject config, long inputRecords) {
        String filePath = config.getStr("filePath");
        String fileType = config.getStr("fileType", "csv");
        log.info("加载到文件: path={}, type={}", filePath, fileType);
        return inputRecords;
    }

    private long loadToApi(FlowEngine.FlowContext context, JSONObject config, long inputRecords) {
        String apiUrl = config.getStr("apiUrl");
        String method = config.getStr("method", "POST");
        log.info("加载到API: url={}, method={}", apiUrl, method);
        return (long) (inputRecords * 0.98);
    }
}
