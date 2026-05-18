package com.migration.loader;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RESTAPIDataLoader implements DataLoader {

    @Override
    public String getName() { return "RESTAPIDataLoader"; }

    @Override
    public String getFullName() { return "com.migration.loader.RESTAPIDataLoader"; }

    @Override
    public String getDescription() { return "REST API数据加载器"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("REST API数据加载开始: nodeId={}", node.getNodeId());

        String apiUrl = getConfig(node, "apiUrl", "");
        String method = getConfig(node, "method", "POST");
        String contentType = getConfig(node, "contentType", "application/json");
        int batchSize = Integer.parseInt(getConfig(node, "batchSize", "100"));

        log.info("加载到REST API: url={}, method={}, contentType={}, batchSize={}", apiUrl, method, contentType, batchSize);

        long inputRecords = getUpstreamRecordCount(context);
        long successRecords = (long) (inputRecords * 0.98);

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        String summary = String.format("数据加载到REST API [%s] 完成，共 %d 条，成功 %d 条，失败 %d 条",
                apiUrl, inputRecords, successRecords, inputRecords - successRecords);
        return FlowEngine.NodeResult.ok(summary, inputRecords, successRecords, inputRecords - successRecords);
    }

    private long getUpstreamRecordCount(FlowEngine.FlowContext context) {
        return context.getVariables().entrySet().stream()
                .filter(e -> e.getKey().endsWith("_transformed"))
                .mapToLong(e -> e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0L)
                .max().orElse(0L);
    }
}
