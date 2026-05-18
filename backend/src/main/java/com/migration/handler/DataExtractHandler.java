package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.engine.NodeExecutor;
import com.migration.extractor.DataExtractor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataExtractHandler implements NodeExecutor {

    private final ImplementationClassRegistry registry;

    @Override
    public NodeType getSupportedType() {
        return NodeType.DATA_EXTRACT;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行数据提取: nodeId={}, name={}", node.getNodeId(), node.getName());

        String implementationClass = node.getImplementationClass();
        if (implementationClass != null && !implementationClass.isEmpty()) {
            return executeWithImplementation(context, node, implementationClass);
        }

        return executeDefault(context, node);
    }

    private FlowEngine.NodeResult executeWithImplementation(FlowEngine.FlowContext context, FlowNode node, String implementationClass) {
        log.info("使用指定实现类执行数据提取: implementationClass={}", implementationClass);

        DataExtractor extractor = registry.getExtractor(implementationClass);
        if (extractor == null) {
            return FlowEngine.NodeResult.fail("未找到实现类: " + implementationClass);
        }

        try {
            FlowEngine.NodeResult result = extractor.execute(context, node);

            if (result.isSuccess()) {
                context.getVariables().put(node.getNodeId() + "_extracted", result.getTotalRecords());
                context.getVariables().put(node.getNodeId() + "_source", extractor.getName());
            }

            return result;
        } catch (Exception e) {
            log.error("实现类执行数据提取失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("实现类执行数据提取失败: " + e.getMessage());
        }
    }

    private FlowEngine.NodeResult executeDefault(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String sourceType = config.getStr("sourceType", "database");
        String sourceName = config.getStr("sourceName", "");

        try {
            long totalRecords = 0;

            switch (sourceType) {
                case "database":
                    totalRecords = extractFromDatabase(context, config);
                    break;
                case "file":
                    totalRecords = extractFromFile(context, config);
                    break;
                case "api":
                    totalRecords = extractFromApi(context, config);
                    break;
                default:
                    return FlowEngine.NodeResult.fail("不支持的数据源类型: " + sourceType);
            }

            context.getVariables().put(node.getNodeId() + "_extracted", totalRecords);
            context.getVariables().put(node.getNodeId() + "_source", sourceName);

            String summary = String.format("从 [%s] 提取数据完成，共 %d 条记录", sourceName, totalRecords);
            return FlowEngine.NodeResult.ok(summary, totalRecords, totalRecords, 0);

        } catch (Exception e) {
            log.error("数据提取失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("数据提取失败: " + e.getMessage());
        }
    }

    private long extractFromDatabase(FlowEngine.FlowContext context, JSONObject config) {
        String url = config.getStr("url");
        String sql = config.getStr("sql", config.getStr("query"));
        log.info("从数据库提取: url={}, sql={}", url, sql);
        return 10000L;
    }

    private long extractFromFile(FlowEngine.FlowContext context, JSONObject config) {
        String filePath = config.getStr("filePath");
        String fileType = config.getStr("fileType", "csv");
        log.info("从文件提取: path={}, type={}", filePath, fileType);
        return 5000L;
    }

    private long extractFromApi(FlowEngine.FlowContext context, JSONObject config) {
        String apiUrl = config.getStr("apiUrl");
        String method = config.getStr("method", "GET");
        log.info("从API提取: url={}, method={}", apiUrl, method);
        return 8000L;
    }
}
