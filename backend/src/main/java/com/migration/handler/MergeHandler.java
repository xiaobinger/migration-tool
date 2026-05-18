package com.migration.handler;

import com.migration.engine.FlowEngine;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合并网关处理器
 */
@Slf4j
@Component
public class MergeHandler implements NodeExecutor {

    @Override
    public NodeType getSupportedType() {
        return NodeType.MERGE;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行合并网关: nodeId={}, name={}", node.getNodeId(), node.getName());
        return FlowEngine.NodeResult.ok("合并网关");
    }
}
