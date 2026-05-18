package com.migration.extractor;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import com.migration.service.DataSourceConfigService;

/**
 * 数据提取器接口
 * 定义所有数据提取器的通用行为
 */
public interface DataExtractor {

    /**
     * 获取提取器名称
     */
    String getName();

    /**
     * 获取完整类名
     */
    String getFullName();

    /**
     * 获取描述
     */
    String getDescription();

    /**
     * 执行数据提取
     * @param context 流程上下文
     * @param node 节点配置
     * @return 执行结果
     */
    FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node);

    /**
     * 辅助方法：从节点配置中获取值
     */
    default String getConfig(FlowNode node, String key, String defaultValue) {
        if (node.getConfig() == null || node.getConfig().isEmpty()) {
            return defaultValue;
        }
        try {
            cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(node.getConfig());
            return config.getStr(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    default void setDataSourceConfigService(DataSourceConfigService service) {}
}
