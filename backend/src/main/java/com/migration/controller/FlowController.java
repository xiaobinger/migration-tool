package com.migration.controller;

import com.migration.engine.ImplementationClassRegistry;
import com.migration.model.dto.FlowDefinitionDTO;
import com.migration.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flows")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;
    private final ImplementationClassRegistry implementationClassRegistry;

    @GetMapping
    public ResponseEntity<List<FlowDefinitionDTO>> listFlows() {
        return ResponseEntity.ok(flowService.listFlows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlowDefinitionDTO> getFlow(@PathVariable Long id) {
        return ResponseEntity.ok(flowService.getFlowDetail(id));
    }

    @PostMapping
    public ResponseEntity<FlowDefinitionDTO> createFlow(@RequestBody FlowDefinitionDTO dto) {
        return ResponseEntity.ok(flowService.createFlow(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlowDefinitionDTO> updateFlow(@PathVariable Long id, @RequestBody FlowDefinitionDTO dto) {
        return ResponseEntity.ok(flowService.updateFlow(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlow(@PathVariable Long id) {
        flowService.deleteFlow(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/implementations")
    public ResponseEntity<Map<String, List<ImplementationClassRegistry.ImplementationInfo>>> getImplementations() {
        return ResponseEntity.ok(implementationClassRegistry.getAllImplementations());
    }

    @GetMapping("/implementations/{nodeType}")
    public ResponseEntity<List<ImplementationClassRegistry.ImplementationInfo>> getImplementationsByType(@PathVariable String nodeType) {
        com.migration.model.enums.NodeType type;
        try {
            type = com.migration.model.enums.NodeType.valueOf(nodeType);
        } catch (IllegalArgumentException e) {
            type = com.migration.model.enums.NodeType.fromCode(nodeType);
        }
        return ResponseEntity.ok(implementationClassRegistry.getImplementations(type));
    }
}
