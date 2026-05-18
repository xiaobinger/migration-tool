package com.migration.notifier;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.model.entity.FlowNode;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class DingTalkNotifier implements Notifier {

    private final String webhookUrl;
    private final String secret;

    public DingTalkNotifier(String webhookUrl, String secret) {
        this.webhookUrl = webhookUrl;
        this.secret = secret;
    }

    @Override
    public String getName() { return "DingTalkNotifier"; }

    @Override
    public String getFullName() { return "com.migration.notifier.DingTalkNotifier"; }

    @Override
    public String getDescription() { return "钉钉机器人通知发送器，支持文本和Markdown消息"; }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行钉钉通知: nodeId={}", node.getNodeId());

        String msgType = getConfig(node, "msgType", "text");
        String content = getConfig(node, "message", "");
        String title = getConfig(node, "title", "迁移任务通知");
        String atMobiles = getConfig(node, "atMobiles", "");
        boolean atAll = Boolean.parseBoolean(getConfig(node, "atAll", "false"));

        content = replaceVariables(content, context.getVariables());
        title = replaceVariables(title, context.getVariables());

        try {
            String url = buildSignedUrl();
            JSONObject body = buildMessage(msgType, content, title, atMobiles, atAll);

            HttpResponse response = HttpRequest.post(url)
                    .body(body.toString())
                    .contentType("application/json")
                    .timeout(10000)
                    .execute();

            JSONObject resp = JSONUtil.parseObj(response.body());
            int errcode = resp.getInt("errcode", -1);

            if (errcode == 0) {
                log.info("钉钉通知发送成功: msgType={}", msgType);
                String summary = String.format("钉钉通知发送成功: 类型=%s, 标题=%s", msgType, title);
                return FlowEngine.NodeResult.ok(summary);
            } else {
                String errmsg = resp.getStr("errmsg", "未知错误");
                log.error("钉钉通知发送失败: errcode={}, errmsg={}", errcode, errmsg);
                return FlowEngine.NodeResult.fail("钉钉通知发送失败: " + errmsg);
            }

        } catch (Exception e) {
            log.error("钉钉通知发送异常: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("钉钉通知发送异常: " + e.getMessage());
        }
    }

    private String buildSignedUrl() throws Exception {
        if (secret == null || secret.isEmpty()) {
            return webhookUrl;
        }

        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

        return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
    }

    private JSONObject buildMessage(String msgType, String content, String title, String atMobiles, boolean atAll) {
        JSONObject body = new JSONObject();
        body.set("msgtype", msgType);

        if ("markdown".equals(msgType)) {
            JSONObject markdown = new JSONObject();
            markdown.set("title", title);
            markdown.set("text", content);
            body.set("markdown", markdown);
        } else if ("text".equals(msgType)) {
            JSONObject text = new JSONObject();
            text.set("content", content);
            body.set("text", text);
        }

        JSONObject at = new JSONObject();
        if (!atMobiles.isEmpty()) {
            at.set("atMobiles", atMobiles.split("[,;\\s]+"));
        }
        at.set("isAtAll", atAll);
        body.set("at", at);

        return body;
    }

    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        result = result.replace("${timestamp}", java.time.LocalDateTime.now().toString());
        return result;
    }
}
