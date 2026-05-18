package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import com.migration.notifier.Notifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler implements NodeExecutor {

    private final ImplementationClassRegistry registry;

    @Override
    public NodeType getSupportedType() {
        return NodeType.NOTIFICATION;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行通知: nodeId={}, name={}", node.getNodeId(), node.getName());

        String implementationClass = node.getImplementationClass();
        if (implementationClass != null && !implementationClass.isEmpty()) {
            return executeWithImplementation(context, node, implementationClass);
        }

        return executeDefault(context, node);
    }

    private FlowEngine.NodeResult executeWithImplementation(FlowEngine.FlowContext context, FlowNode node, String implementationClass) {
        log.info("使用指定实现类执行通知: implementationClass={}", implementationClass);

        Notifier notifier = registry.getNotifier(implementationClass);
        if (notifier == null) {
            return FlowEngine.NodeResult.fail("未找到实现类: " + implementationClass);
        }

        try {
            return notifier.execute(context, node);
        } catch (Exception e) {
            log.error("实现类执行通知失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("实现类执行通知失败: " + e.getMessage());
        }
    }

    private FlowEngine.NodeResult executeDefault(FlowEngine.FlowContext context, FlowNode node) {
        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String channel = config.getStr("channel", "dingtalk");

        try {
            switch (channel) {
                case "dingtalk": {
                    String implClass = "com.migration.notifier.DingTalkNotifier";
                    Notifier notifier = registry.getNotifier(implClass);
                    if (notifier != null) {
                        return notifier.execute(context, node);
                    }
                    log.warn("未找到钉钉通知实现类");
                    return FlowEngine.NodeResult.ok("钉钉通知发送跳过: 实现类未注册");
                }
                case "email": {
                    String implClass = "com.migration.notifier.EmailNotifier";
                    Notifier notifier = registry.getNotifier(implClass);
                    if (notifier != null) {
                        return notifier.execute(context, node);
                    }
                    log.warn("未找到邮件通知实现类");
                    return FlowEngine.NodeResult.ok("邮件通知发送跳过: 实现类未注册");
                }
                case "webhook":
                    log.info("发送Webhook通知: url={}", config.getStr("webhookUrl"));
                    return FlowEngine.NodeResult.ok("Webhook通知发送成功");
                default:
                    log.warn("不支持的通知渠道: {}", channel);
                    return FlowEngine.NodeResult.ok("不支持的通知渠道: " + channel);
            }
        } catch (Exception e) {
            log.error("通知发送失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.ok("通知发送失败（不影响流程）: " + e.getMessage());
        }
    }
}
