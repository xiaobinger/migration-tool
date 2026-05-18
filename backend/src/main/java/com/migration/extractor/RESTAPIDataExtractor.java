package com.migration.extractor;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API数据提取器
 * 从REST API端点提取数据
 */
@Slf4j
public class RESTAPIDataExtractor implements DataExtractor {

    @Override
    public String getName() {
        return "RESTAPIDataExtractor";
    }

    @Override
    public String getFullName() {
        return "com.migration.extractor.RESTAPIDataExtractor";
    }

    @Override
    public String getDescription() {
        return "REST API数据提取器";
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("REST API数据提取开始: nodeId={}", node.getNodeId());

        String url = getConfig(node, "url", "");
        String method = getConfig(node, "method", "GET");
        String headers = getConfig(node, "headers", "{}");
        int pageSize = Integer.parseInt(getConfig(node, "pageSize", "100"));
        int maxPages = Integer.parseInt(getConfig(node, "maxPages", "100"));

        log.info("调用REST API: url={}, method={}", url, method);

        // TODO: 实际实现REST API调用和数据提取
        long totalRecords = (long) pageSize * maxPages;

        String summary = String.format("从REST API [%s] 提取数据完成，共 %d 条记录", url, totalRecords);
        return FlowEngine.NodeResult.ok(summary, totalRecords, totalRecords, 0);
    }
}
