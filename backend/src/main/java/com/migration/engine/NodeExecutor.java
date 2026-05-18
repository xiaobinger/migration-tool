package com.migration.engine;

import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;

/**
 * 节点执行器接口
 */
public interface NodeExecutor {

    /**
     * 获取支持的节点类型
     */
    NodeType getSupportedType();

    /**
     * 执行节点逻辑
     */
    FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node);
}
