package com.migration.engine;

import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 节点处理器注册中心
 */
@Slf4j
@Component
public class NodeHandlerRegistry {

    private final Map<NodeType, NodeExecutor> handlers = new EnumMap<>(NodeType.class);

    public NodeHandlerRegistry(List<NodeExecutor> executorList) {
        for (NodeExecutor executor : executorList) {
            handlers.put(executor.getSupportedType(), executor);
            log.info("注册节点处理器: {} -> {}", executor.getSupportedType(), executor.getClass().getSimpleName());
        }
    }

    public NodeExecutor getHandler(NodeType type) {
        return handlers.get(type);
    }
}
