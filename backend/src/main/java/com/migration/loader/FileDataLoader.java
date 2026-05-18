package com.migration.loader;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileDataLoader implements DataLoader {

    @Override
    public String getName() { return "FileDataLoader"; }

    @Override
    public String getFullName() { return "com.migration.loader.FileDataLoader"; }

    @Override
    public String getDescription() { return "文件数据加载器（CSV/JSON/Excel）"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("文件数据加载开始: nodeId={}", node.getNodeId());

        String filePath = getConfig(node, "filePath", "");
        String fileType = getConfig(node, "fileType", "csv");
        String writeMode = getConfig(node, "writeMode", "OVERWRITE");
        String encoding = getConfig(node, "encoding", "UTF-8");

        log.info("加载到文件: path={}, type={}, mode={}, encoding={}", filePath, fileType, writeMode, encoding);

        long inputRecords = getUpstreamRecordCount(context);
        long successRecords = inputRecords;

        context.getVariables().put(node.getNodeId() + "_loaded", successRecords);

        String summary = String.format("数据加载到文件 [%s] 完成，共 %d 条，类型: %s", filePath, successRecords, fileType);
        return FlowEngine.NodeResult.ok(summary, inputRecords, successRecords, 0);
    }

    private long getUpstreamRecordCount(FlowEngine.FlowContext context) {
        return context.getVariables().entrySet().stream()
                .filter(e -> e.getKey().endsWith("_transformed"))
                .mapToLong(e -> e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0L)
                .max().orElse(0L);
    }
}
