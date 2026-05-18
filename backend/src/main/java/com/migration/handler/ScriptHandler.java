package com.migration.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.migration.engine.FlowEngine;
import com.migration.engine.NodeExecutor;
import com.migration.model.entity.FlowNode;
import com.migration.model.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义脚本节点处理器
 */
@Slf4j
@Component
public class ScriptHandler implements NodeExecutor {

    @Override
    public NodeType getSupportedType() {
        return NodeType.SCRIPT;
    }

    @Override
    public FlowEngine.NodeResult execute(FlowEngine.FlowContext context, FlowNode node) {
        log.info("执行自定义脚本: nodeId={}, name={}", node.getNodeId(), node.getName());

        JSONObject config = JSONUtil.parseObj(node.getConfig());
        String scriptType = config.getStr("scriptType", "groovy"); // groovy / js / sql
        String script = config.getStr("script", "");

        try {
            Object result = executeScript(scriptType, script, context);
            context.getVariables().put(node.getNodeId() + "_result", result);

            return FlowEngine.NodeResult.ok("脚本执行完成: " + result);

        } catch (Exception e) {
            log.error("脚本执行失败: {}", e.getMessage(), e);
            return FlowEngine.NodeResult.fail("脚本执行失败: " + e.getMessage());
        }
    }

    private Object executeScript(String type, String script, FlowEngine.FlowContext context) {
        // TODO: 集成脚本引擎
        // Groovy: new GroovyShell().evaluate(script)
        // JS: ScriptEngineManager.getEngineByName("javascript").eval(script)
        log.info("执行脚本: type={}, script length={}", type, script.length());
        return "ok";
    }
}
