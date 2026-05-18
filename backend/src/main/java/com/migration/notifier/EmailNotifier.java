package com.migration.notifier;

import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;

@Slf4j
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;

    public EmailNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getName() { return "EmailNotifier"; }

    @Override
    public String getFullName() { return "com.migration.notifier.EmailNotifier"; }

    @Override
    public String getDescription() { return "邮件通知发送器，支持HTML邮件和模板变量替换"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行邮件通知: nodeId={}", node.getNodeId());

        String recipients = getConfig(node, "recipients", "");
        String subject = getConfig(node, "subject", "迁移任务通知");
        String content = getConfig(node, "content", "");
        boolean html = Boolean.parseBoolean(getConfig(node, "html", "true"));

        if (recipients.isEmpty()) {
            return FlowEngine.NodeResult.fail("邮件收件人不能为空");
        }

        content = replaceVariables(content, context.getVariables());
        subject = replaceVariables(subject, context.getVariables());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getConfig(node, "from", "migration-tool@example.com"));
            helper.setTo(recipients.split("[,;\\s]+"));
            helper.setSubject(subject);
            helper.setText(content, html);

            String cc = getConfig(node, "cc", "");
            if (!cc.isEmpty()) {
                helper.setCc(cc.split("[,;\\s]+"));
            }

            mailSender.send(message);

            log.info("邮件发送成功: to={}, subject={}", recipients, subject);
            String summary = String.format("邮件通知发送成功: 收件人=%s, 主题=%s", recipients, subject);
            return FlowEngine.NodeResult.ok(summary);

        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("邮件发送失败: " + e.getMessage());
        }
    }

    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        result = result.replace("${taskId}", String.valueOf(variables.getOrDefault("taskId", "")));
        result = result.replace("${taskName}", String.valueOf(variables.getOrDefault("taskName", "")));
        result = result.replace("${timestamp}", java.time.LocalDateTime.now().toString());
        return result;
    }
}
