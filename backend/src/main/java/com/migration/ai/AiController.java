package com.migration.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiFlowGenerationService aiFlowGenerationService;
    private final SkillService skillService;
    private final AiConfig aiConfig;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> historyRaw = (List<Map<String, String>>) request.get("history");

        List<AiMessage> history = new java.util.ArrayList<>();
        if (historyRaw != null) {
            for (Map<String, String> m : historyRaw) {
                history.add(new AiMessage(m.get("role"), m.get("content")));
            }
        }

        String response = aiService.chat(message, history);
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PostMapping("/generate-flow")
    public ResponseEntity<?> generateFlow(@RequestBody Map<String, String> request) {
        String requirement = request.get("requirement");
        boolean autoSave = Boolean.parseBoolean(request.getOrDefault("autoSave", "false"));

        AiFlowGenerationService.AiFlowResult result;
        if (autoSave) {
            result = aiFlowGenerationService.generateAndSaveFlow(requirement);
        } else {
            result = aiFlowGenerationService.generateFlow(requirement);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeRequirement(@RequestBody Map<String, String> request) {
        String requirement = request.get("requirement");
        String analysis = aiFlowGenerationService.analyzeRequirement(requirement);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @GetMapping("/skills")
    public ResponseEntity<List<SkillTemplate>> listSkills(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(skillService.getSkillsByCategory(category));
        }
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillTemplate> createSkill(@RequestBody SkillTemplate skill) {
        return ResponseEntity.ok(skillService.createSkill(skill));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<SkillTemplate> updateSkill(@PathVariable Long id, @RequestBody SkillTemplate skill) {
        SkillTemplate updated = skillService.updateSkill(id, skill);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/skills/{id}/use")
    public ResponseEntity<Void> incrementSkillUsage(@PathVariable Long id) {
        skillService.incrementUsage(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<UserPreference>> listPreferences(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return ResponseEntity.ok(skillService.getTopPreferences(type, 100));
        }
        return ResponseEntity.ok(skillService.getAllPreferences());
    }

    @PostMapping("/learn/{flowDefinitionId}")
    public ResponseEntity<?> learnFromFlow(@PathVariable Long flowDefinitionId,
                                            @RequestParam String flowName) {
        skillService.learnFromSuccessfulFlow(flowDefinitionId, flowName);
        return ResponseEntity.ok(Map.of("message", "学习完成"));
    }

    @GetMapping("/config")
    public ResponseEntity<?> getAiConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", aiConfig.isEnabled(),
                "model", aiConfig.getModel(),
                "apiUrl", aiConfig.getApiUrl()
        ));
    }
}
