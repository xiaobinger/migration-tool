package com.migration.handler;

import com.migration.engine.FlowEngine;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 并行网关处理器
 */
@Slf4j
@Component
public class ParallelHandler implements NodeExecutor {

    @Override
    public NodeType getSupportedType() {
        return NodeType.PARALLEL;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行并行网关: nodeId={}, name={}", node.getNodeId(), node.getName());
        // 并行网关的实际并行由FlowEngine调度实现
        return FlowEngine.NodeResult.ok("并行网关");
    }
}
