package com.migration.ai;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AiService {

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(aiConfig.getTimeout());
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String userMessage, List<AiMessage> history) {
        if (!aiConfig.isEnabled()) {
            return "AI功能未启用，请在配置文件中设置 migration.ai.enabled=true";
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", aiConfig.getModel());
            body.put("max_tokens", aiConfig.getMaxTokens());
            body.put("temperature", aiConfig.getTemperature());

            List<Map<String, String>> messages = new ArrayList<>();

            String systemPrompt = buildSystemPrompt();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            for (AiMessage msg : history) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }

            messages.add(Map.of("role", "user", "content", userMessage));

            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
                headers.setBearerAuth(aiConfig.getApiKey());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    aiConfig.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("AI返回格式异常: {}", response.getBody());
                return "AI服务响应异常，请检查API配置";
            }

            String content = choices.get(0).path("message").path("content").asText("");
            log.info("AI回复长度: {}", content.length());
            return content;

        } catch (Exception e) {
            log.error("AI调用失败: {}", e.getMessage(), e);
            return "AI调用失败: " + e.getMessage();
        }
    }

    public String chat(String userMessage) {
        return chat(userMessage, Collections.emptyList());
    }

    private String buildSystemPrompt() {
        if (aiConfig.getSystemPrompt() != null && !aiConfig.getSystemPrompt().isEmpty()) {
            return aiConfig.getSystemPrompt();
        }
        return """
                你是一个数据迁移工具的AI助手，专门帮助用户设计数据迁移流程。
                
                你可以做的事情：
                1. 根据用户描述的需求，自动生成完整的数据迁移流程定义（包含节点和连线）
                2. 分析用户的数据源结构，给出字段映射建议
                3. 推荐最佳的数据清洗和转换方案
                4. 回答关于数据迁移的技术问题
                
                流程节点类型说明：
                - start: 开始节点（每个流程必须有一个）
                - end: 结束节点（每个流程必须有一个）
                - data_extract: 数据提取节点（支持MySQL/PostgreSQL/MongoDB/REST API/文件）
                - data_transform: 数据转换节点（字段映射/脚本转换等）
                - data_load: 数据加载节点（支持MySQL/PostgreSQL/MongoDB/文件/REST API）
                - validation: 数据校验节点
                - condition: 条件判断节点
                - parallel: 并行网关
                - merge: 合并网关
                - notification: 通知节点
                - script: 自定义脚本节点
                - delay: 延时等待节点
                
                当需要生成流程时，请严格按照JSON格式输出流程定义，包含nodes数组和edges数组。
                每个node包含：nodeId、nodeType、name、description、config(JSON)、positionX、positionY、implementationClass
                每个edge包含：edgeId、sourceNodeId、targetNodeId
                
                请用中文回复用户的问题。如果生成流程定义，请将JSON放在 ```json 代码块中输出。
                """;
    }
}
