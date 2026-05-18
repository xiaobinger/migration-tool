package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 延时等待节点处理器
 */
@Slf4j
@Component
public class DelayHandler implements NodeExecutor {

    @Override
    public NodeType getSupportedType() {
        return NodeType.DELAY;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        int delaySeconds = config.getInt("delaySeconds", 5);

        log.info("延时等待: {}秒", delaySeconds);
        try {
            TimeUnit.SECONDS.sleep(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FlowEngine.NodeResult.fail("延时被中断");
        }

        return FlowEngine.NodeResult.ok("延时 " + delaySeconds + " 秒完成");
    }
}
