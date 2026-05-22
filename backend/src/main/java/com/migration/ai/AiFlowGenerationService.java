package com.migration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.engine.ImplementationClassRegistry;
import com.migration.model.dto.FlowDefinitionDTO;
import com.migration.model.entity.DataSourceConfig;
import com.migration.service.DataSourceConfigService;
import com.migration.service.FlowService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFlowGenerationService {

    private final AiService aiService;
    private final FlowService flowService;
    private final SkillService skillService;
    private final DataSourceConfigService dataSourceConfigService;
    private final ImplementationClassRegistry implementationClassRegistry;
    private final ObjectMapper objectMapper;

    public AiFlowResult generateFlow(String userRequirement) {
        String prompt = buildGenerationPrompt(userRequirement);
        String aiResponse = aiService.chat(prompt);
        FlowDefinitionDTO flowDef = parseFlowFromResponse(aiResponse);

        if (flowDef == null) {
            return AiFlowResult.fail("AI生成的流程格式无法解析，请重新描述需求或手动创建流程", aiResponse);
        }

        flowDef.setName(extractFlowName(userRequirement));
        flowDef.setDescription(userRequirement);

        return AiFlowResult.ok(flowDef, aiResponse);
    }

    public AiFlowResult generateAndSaveFlow(String userRequirement) {
        AiFlowResult result = generateFlow(userRequirement);
        if (result.isSuccess() && result.getFlowDefinition() != null) {
            try {
                FlowDefinitionDTO saved = flowService.createFlow(result.getFlowDefinition());
                result.setFlowDefinition(saved);
                skillService.recordPreference("flow_generation", userRequirement, "requirement");
            } catch (Exception e) {
                log.error("保存AI生成的流程失败: {}", e.getMessage(), e);
                result.setSuccess(false);
                result.setMessage("流程生成成功但保存失败: " + e.getMessage());
            }
        }
        return result;
    }

    public String analyzeRequirement(String userRequirement) {
        String prompt = buildAnalysisPrompt(userRequirement);
        return aiService.chat(prompt);
    }

    private String buildGenerationPrompt(String userRequirement) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下需求生成一个完整的数据迁移流程定义。\n\n");
        prompt.append("用户需求：").append(userRequirement).append("\n\n");

        prompt.append("可用数据源：\n");
        List<DataSourceConfig> dataSources = dataSourceConfigService.listAll();
        for (DataSourceConfig ds : dataSources) {
            prompt.append("- ID: ").append(ds.getId())
                    .append(", 名称: ").append(ds.getName())
                    .append(", 类型: ").append(ds.getType())
                    .append(", 主机: ").append(ds.getHost())
                    .append(":").append(ds.getPort())
                    .append(", 数据库: ").append(ds.getDatabase())
                    .append("\n");
        }
        prompt.append("\n");

        prompt.append("可用实现类：\n");
        Map<String, List<ImplementationClassRegistry.ImplementationInfo>> allImpls =
                implementationClassRegistry.getAllImplementations();
        for (Map.Entry<String, List<ImplementationClassRegistry.ImplementationInfo>> entry : allImpls.entrySet()) {
            prompt.append(entry.getKey()).append(":\n");
            for (ImplementationClassRegistry.ImplementationInfo info : entry.getValue()) {
                prompt.append("  - ").append(info.getFullClassName())
                        .append(": ").append(info.getDescription()).append("\n");
            }
        }
        prompt.append("\n");

        List<SkillTemplate> relevantSkills = skillService.findRelevantSkills(userRequirement);
        if (!relevantSkills.isEmpty()) {
            prompt.append("相关的历史成功流程模板（请参考这些模板的节点结构）：\n");
            for (SkillTemplate skill : relevantSkills.stream().limit(3).toList()) {
                prompt.append("- 模板: ").append(skill.getName())
                        .append(", 类别: ").append(skill.getCategory())
                        .append(", 使用次数: ").append(skill.getUsageCount())
                        .append("\n");
                prompt.append("  流程定义: ").append(skill.getFlowDefinitionJson()).append("\n");
            }
            prompt.append("\n");
        }

        List<UserPreference> prefs = skillService.getTopPreferences("datasource", 5);
        if (!prefs.isEmpty()) {
            prompt.append("用户常用数据源偏好：\n");
            for (UserPreference pref : prefs) {
                prompt.append("- ").append(pref.getPreferenceKey())
                        .append(" (使用").append(pref.getFrequency()).append("次)\n");
            }
            prompt.append("\n");
        }

        prompt.append("""
                请严格按照以下JSON格式输出流程定义（不要输出其他内容，只输出JSON）：
                {
                  "nodes": [
                    {
                      "nodeId": "node_start",
                      "nodeType": "start",
                      "name": "开始",
                      "config": "{}",
                      "positionX": 100,
                      "positionY": 200,
                      "implementationClass": ""
                    },
                    {
                      "nodeId": "data_extract_1",
                      "nodeType": "data_extract",
                      "name": "数据提取1",
                      "config": "{\\"connectionId\\":\\"1\\",\\"queryMode\\":\\"table\\",\\"table\\":\\"source_table\\"}",
                      "positionX": 300,
                      "positionY": 200,
                      "implementationClass": "com.migration.extractor.MySQLDataExtractor"
                    },
                    {
                      "nodeId": "data_transform_1",
                      "nodeType": "data_transform",
                      "name": "数据转换",
                      "config": "{\\"mappings\\":\\"[]\\"}",
                      "positionX": 500,
                      "positionY": 200,
                      "implementationClass": "com.migration.transformer.FieldMappingTransformer"
                    },
                    {
                      "nodeId": "data_load_1",
                      "nodeType": "data_load",
                      "name": "数据加载1",
                      "config": "{\\"connectionId\\":\\"2\\",\\"table\\":\\"target_table\\",\\"writeMode\\":\\"INSERT\\"}",
                      "positionX": 700,
                      "positionY": 200,
                      "implementationClass": "com.migration.loader.MySQLDataLoader"
                    },
                    {
                      "nodeId": "node_end",
                      "nodeType": "end",
                      "name": "结束",
                      "config": "{}",
                      "positionX": 900,
                      "positionY": 200,
                      "implementationClass": ""
                    }
                  ],
                  "edges": [
                    {"edgeId": "edge_1", "sourceNodeId": "node_start", "targetNodeId": "data_extract_1"},
                    {"edgeId": "edge_2", "sourceNodeId": "data_extract_1", "targetNodeId": "data_transform_1"},
                    {"edgeId": "edge_3", "sourceNodeId": "data_transform_1", "targetNodeId": "data_load_1"},
                    {"edgeId": "edge_4", "sourceNodeId": "data_load_1", "targetNodeId": "node_end"}
                  ]
                }
                
                注意事项：
                1. 每个流程必须包含start和end节点
                2. 数据提取节点必须指定implementationClass（如com.migration.extractor.MySQLDataExtractor）
                3. 数据加载节点必须指定implementationClass（如com.migration.loader.MySQLDataLoader）
                4. config中的connectionId要使用实际的数据源ID
                5. 节点位置要合理分布，从左到右排列
                6. 如果需要并行处理，使用parallel和merge节点
                """);

        return prompt.toString();
    }

    private String buildAnalysisPrompt(String userRequirement) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下数据迁移需求，给出专业建议：\n\n");
        prompt.append("需求：").append(userRequirement).append("\n\n");

        prompt.append("可用数据源：\n");
        List<DataSourceConfig> dataSources = dataSourceConfigService.listAll();
        for (DataSourceConfig ds : dataSources) {
            prompt.append("- ").append(ds.getName())
                    .append(" (").append(ds.getType()).append(": ")
                    .append(ds.getHost()).append(":").append(ds.getPort())
                    .append("/").append(ds.getDatabase()).append(")\n");
        }

        prompt.append("\n请从以下方面给出建议：\n");
        prompt.append("1. 数据源和目标选择建议\n");
        prompt.append("2. 数据清洗和转换策略\n");
        prompt.append("3. 数据质量保障方案\n");
        prompt.append("4. 性能优化建议\n");
        prompt.append("5. 潜在风险和注意事项\n");

        return prompt.toString();
    }

    private FlowDefinitionDTO parseFlowFromResponse(String aiResponse) {
        String jsonStr = extractJsonFromResponse(aiResponse);
        if (jsonStr == null) return null;

        try {
            JsonNode root = objectMapper.readTree(jsonStr);

            FlowDefinitionDTO dto = new FlowDefinitionDTO();

            List<FlowDefinitionDTO.NodeDTO> nodes = new ArrayList<>();
            JsonNode nodesArr = root.path("nodes");
            if (nodesArr.isArray()) {
                for (JsonNode nodeJson : nodesArr) {
                    FlowDefinitionDTO.NodeDTO node = new FlowDefinitionDTO.NodeDTO();
                    node.setNodeId(nodeJson.path("nodeId").asText("node_" + UUID.randomUUID().toString().substring(0, 8)));
                    node.setNodeType(nodeJson.path("nodeType").asText("data_extract"));
                    node.setName(nodeJson.path("name").asText(""));
                    node.setDescription(nodeJson.path("description").asText(""));
                    node.setConfig(nodeJson.path("config").asText("{}"));
                    node.setPositionX(nodeJson.path("positionX").asDouble(0));
                    node.setPositionY(nodeJson.path("positionY").asDouble(0));
                    node.setImplementationClass(nodeJson.path("implementationClass").asText(""));
                    node.setParentGroupId(nodeJson.path("parentGroupId").asText(null));
                    nodes.add(node);
                }
            }

            List<FlowDefinitionDTO.EdgeDTO> edges = new ArrayList<>();
            JsonNode edgesArr = root.path("edges");
            if (edgesArr.isArray()) {
                for (JsonNode edgeJson : edgesArr) {
                    FlowDefinitionDTO.EdgeDTO edge = new FlowDefinitionDTO.EdgeDTO();
                    edge.setEdgeId(edgeJson.path("edgeId").asText("edge_" + UUID.randomUUID().toString().substring(0, 8)));
                    edge.setSourceNodeId(edgeJson.path("sourceNodeId").asText(""));
                    edge.setTargetNodeId(edgeJson.path("targetNodeId").asText(""));
                    edge.setCondition(edgeJson.path("condition").asText(null));
                    edge.setLabel(edgeJson.path("label").asText(null));
                    edge.setEdgeStyle(edgeJson.path("edgeStyle").asText(null));
                    edges.add(edge);
                }
            }

            if (nodes.isEmpty()) return null;

            dto.setNodes(nodes);
            dto.setEdges(edges);
            return dto;

        } catch (Exception e) {
            log.error("解析AI生成的流程JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractJsonFromResponse(String response) {
        Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n([\\s\\S]*?)\\n```");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    private String extractFlowName(String requirement) {
        if (requirement.length() <= 50) return requirement;
        return requirement.substring(0, 50) + "...";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @Builder
    public static class AiFlowResult {
        private boolean success;
        private String message;
        private FlowDefinitionDTO flowDefinition;
        private String aiResponse;

        public static AiFlowResult ok(FlowDefinitionDTO flowDef, String aiResponse) {
            return AiFlowResult.builder()
                    .success(true)
                    .message("流程生成成功")
                    .flowDefinition(flowDef)
                    .aiResponse(aiResponse)
                    .build();
        }

        public static AiFlowResult fail(String message, String aiResponse) {
            return AiFlowResult.builder()
                    .success(false)
                    .message(message)
                    .aiResponse(aiResponse)
                    .build();
        }
    }
}
