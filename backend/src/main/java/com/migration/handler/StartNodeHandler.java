package com.migration.handler;

import com.migration.engine.FlowEngine;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开始节点处理器
 */
@Slf4j
@Component
public class StartNodeHandler implements NodeExecutor {

    @Override
    public NodeType getSupportedType() {
        return NodeType.START;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("流程开始执行: taskId={}", context.getTask().getId());
        return FlowEngine.NodeResult.ok("流程开始");
    }
}
