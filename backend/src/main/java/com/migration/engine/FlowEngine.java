package com.migration.engine;

import cn.hutool.core.bean.BeanUtil;
import com.migration.model.entity.*;
import com.migration.model.enums.LogLevel;
import com.migration.model.enums.NodeType;
import com.migration.model.enums.TaskStatus;
import com.migration.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEngine {

    private final FlowDefinitionRepository flowDefinitionRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final FlowEdgeRepository flowEdgeRepository;
    private final MigrationTaskRepository migrationTaskRepository;
    private final NodeExecutionRepository nodeExecutionRepository;
    private final TaskLogRepository taskLogRepository;
    private final NodeHandlerRegistry nodeHandlerRegistry;

    private final Map<Long, FlowContext> runningTasks = new ConcurrentHashMap<>();

    @Transactional
    public MigrationTask pauseTask(Long taskId) {
        FlowContext context = runningTasks.get(taskId);
        if (context != null) {
            context.setPaused(true);
            log.info("任务已暂停: taskId={}", taskId);
        }
        MigrationTask task = migrationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        task.setStatus(TaskStatus.PAUSED);
        return migrationTaskRepository.save(task);
    }

    @Transactional
    public MigrationTask cancelTask(Long taskId) {
        FlowContext context = runningTasks.get(taskId);
        if (context != null) {
            context.setCancelled(true);
            log.info("任务已取消: taskId={}", taskId);
        }
        MigrationTask task = migrationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        task.setStatus(TaskStatus.CANCELLED);
        task.setFinishedAt(LocalDateTime.now());
        return migrationTaskRepository.save(task);
    }

    public void execute(MigrationTask task, String restartFromNodeId) {
        executeFlow(task.getId(), task.getFlowDefinitionId(), restartFromNodeId);
    }

    public MigrationTask executeFlow(Long taskId, Long flowDefinitionId, String restartFromNodeId) {
        MigrationTask task = migrationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        task.setCompletedNodes(0);
        task.setTotalRecords(0L);
        task.setSuccessRecords(0L);
        task.setFailedRecords(0L);
        task.setExtractedRecords(0L);
        task.setLoadedRecords(0L);
        task.setLoadedSuccessRecords(0L);
        task.setLoadedFailedRecords(0L);
        migrationTaskRepository.save(task);

        try {
            List<FlowNode> nodes = flowNodeRepository.findByFlowDefinitionIdOrderBySortOrder(flowDefinitionId);
            List<FlowEdge> edges = flowEdgeRepository.findByFlowDefinitionId(flowDefinitionId);

            task.setTotalNodes(nodes.size());
            migrationTaskRepository.save(task);

            Map<String, FlowNode> nodeMap = nodes.stream()
                    .collect(Collectors.toMap(FlowNode::getNodeId, n -> n));

            Map<String, List<FlowEdge>> outgoingEdges = new HashMap<>();
            for (FlowEdge edge : edges) {
                outgoingEdges.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            }

            FlowContext context = new FlowContext();
            context.setTaskId(taskId);
            context.setTask(task);
            context.setNodeResults(new HashMap<>());
            context.setVariables(new HashMap<>());
            context.setData(new ArrayList<>());
            context.setPaused(false);
            context.setCancelled(false);

            runningTasks.put(taskId, context);

            Set<String> executedNodes = new HashSet<>();
            Set<String> executedEdgeIds = new HashSet<>();

            String startNodeId;
            if (restartFromNodeId != null && !restartFromNodeId.isEmpty()) {
                startNodeId = restartFromNodeId;
                List<NodeExecution> previousExecutions = nodeExecutionRepository.findByTaskIdAndStatus(taskId, TaskStatus.SUCCESS);
                for (NodeExecution prevExec : previousExecutions) {
                    executedNodes.add(prevExec.getNodeId());
                    context.getNodeResults().put(prevExec.getNodeId(), NodeResult.ok(prevExec.getOutputSummary() != null ? prevExec.getOutputSummary() : "已完成"));
                    if (prevExec.getIncomingEdgeId() != null) {
                        executedEdgeIds.add(prevExec.getIncomingEdgeId());
                    }
                }
                executedNodes.remove(restartFromNodeId);
                deleteNodeExecutions(taskId, List.of(restartFromNodeId));
                log.info("从断点续传: restartFromNodeId={}, 已跳过节点数={}", restartFromNodeId, executedNodes.size());
            } else {
                startNodeId = findStartNode(nodes, edges);
            }

            if (startNodeId == null) {
                throw new RuntimeException("流程缺少开始节点");
            }

            executeNode(startNodeId, null, nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds);

            task.setStatus(TaskStatus.SUCCESS);
            task.setFinishedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("流程执行失败", e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
        } finally {
            runningTasks.remove(taskId);
        }

        return migrationTaskRepository.save(task);
    }

    @Transactional
    public void deleteNodeExecutions(Long taskId, List<String> nodeIds) {
        nodeExecutionRepository.deleteByTaskIdAndNodeIdIn(taskId, nodeIds);
    }

    private String findStartNode(List<FlowNode> nodes, List<FlowEdge> edges) {
        Set<String> targetNodes = edges.stream()
                .map(FlowEdge::getTargetNodeId)
                .collect(Collectors.toSet());

        return nodes.stream()
                .filter(n -> !targetNodes.contains(n.getNodeId()))
                .findFirst()
                .map(FlowNode::getNodeId)
                .orElse(null);
    }

    private void executeNode(String nodeId, String incomingEdgeId,
                              Map<String, FlowNode> nodeMap,
                              Map<String, List<FlowEdge>> outgoingEdges,
                              FlowContext context, MigrationTask task,
                              Set<String> executedNodes, Set<String> executedEdgeIds) {

        if (context.isInterrupted()) {
            if (context.isCancelled()) {
                throw new RuntimeException("任务已被取消");
            } else if (context.isPaused()) {
                throw new RuntimeException("任务已被暂停");
            }
        }

        if (executedNodes.contains(nodeId)) {
            log.debug("节点已执行过，跳过: {}", nodeId);
            return;
        }
        executedNodes.add(nodeId);

        FlowNode node = nodeMap.get(nodeId);
        if (node == null) {
            throw new RuntimeException("节点不存在: " + nodeId);
        }

        log.info("执行节点: {}", nodeId);

        NodeExecution record = NodeExecution.builder()
                .taskId(task.getId())
                .nodeId(nodeId)
                .nodeName(node.getName())
                .nodeType(node.getNodeType().name())
                .incomingEdgeId(incomingEdgeId)
                .status(TaskStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();

        taskLogRepository.save(TaskLog.builder()
                .taskId(task.getId())
                .nodeId(nodeId)
                .nodeName(node.getName())
                .level(LogLevel.INFO)
                .message(String.format("节点 [%s] 开始执行, 类型: %s", node.getName(), node.getNodeType().getLabel()))
                .build());

        try {
            NodeExecutor executor = nodeHandlerRegistry.getHandler(node.getNodeType());
            NodeResult result;
            if (node.getNodeType() == NodeType.PARALLEL_GROUP) {
                result = executeParallelGroup(nodeId, node, nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds);
            } else if (executor != null) {
                result = executor.execute(context, node);
            } else {
                result = NodeResult.ok("节点执行完成", 0, 0, 0);
            }

            context.getNodeResults().put(nodeId, result);
            record.setStatus(TaskStatus.SUCCESS);
            record.setOutputSummary(result.summary);
            record.setFinishedAt(LocalDateTime.now());
            record.setDuration(Duration.between(record.getStartedAt(), record.getFinishedAt()).toMillis());

            task.setCompletedNodes(task.getCompletedNodes() + 1);

            if (node.getNodeType() == NodeType.DATA_EXTRACT) {
                task.setExtractedRecords(task.getExtractedRecords() + result.totalRecords);
                task.setTotalRecords(task.getTotalRecords() + result.totalRecords);
                task.setSuccessRecords(task.getSuccessRecords() + result.successRecords);
                task.setFailedRecords(task.getFailedRecords() + result.failedRecords);
            } else if (node.getNodeType() == NodeType.DATA_LOAD) {
                task.setLoadedRecords(task.getLoadedRecords() + result.totalRecords);
                task.setLoadedSuccessRecords(task.getLoadedSuccessRecords() + result.successRecords);
                task.setLoadedFailedRecords(task.getLoadedFailedRecords() + result.failedRecords);
                task.setTotalRecords(task.getTotalRecords() + result.totalRecords);
                task.setSuccessRecords(task.getSuccessRecords() + result.successRecords);
                task.setFailedRecords(task.getFailedRecords() + result.failedRecords);
            }

            taskLogRepository.save(TaskLog.builder()
                    .taskId(task.getId())
                    .nodeId(nodeId)
                    .nodeName(node.getName())
                    .level(LogLevel.INFO)
                    .message(result.summary)
                    .recordCount((node.getNodeType() == NodeType.DATA_EXTRACT || node.getNodeType() == NodeType.DATA_LOAD) ? result.totalRecords : 0)
                    .duration(record.getDuration())
                    .build());

        } catch (Exception e) {
            log.error("节点执行失败: {}", nodeId, e);
            record.setStatus(TaskStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            record.setDuration(Duration.between(record.getStartedAt(), record.getFinishedAt()).toMillis());
            context.getNodeResults().put(nodeId, NodeResult.fail(e.getMessage()));

            task.setCompletedNodes(task.getCompletedNodes() + 1);
            task.setRestartFromNodeId(nodeId);
            migrationTaskRepository.save(task);

            taskLogRepository.save(TaskLog.builder()
                    .taskId(task.getId())
                    .nodeId(nodeId)
                    .nodeName(node.getName())
                    .level(LogLevel.ERROR)
                    .message(String.format("节点 [%s] 执行失败: %s", node.getName(), e.getMessage()))
                    .stackTrace(getStackTrace(e))
                    .build());

            throw e;
        } finally {
            nodeExecutionRepository.save(record);
        }

        List<FlowEdge> edges = outgoingEdges.get(nodeId);
        if (edges != null && !edges.isEmpty()) {
            List<FlowEdge> nextEdges = resolveNextEdges(nodeId, edges, context);
            for (FlowEdge nextEdge : nextEdges) {
                executedEdgeIds.add(nextEdge.getEdgeId());
                executeNode(nextEdge.getTargetNodeId(), nextEdge.getEdgeId(), nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds);
            }
        }
    }

    private List<FlowEdge> resolveNextEdges(String currentNodeId, List<FlowEdge> edges, FlowContext context) {
        if (edges.size() == 1) {
            FlowEdge edge = edges.get(0);
            if (edge.getCondition() != null && !edge.getCondition().isEmpty()) {
                if (evaluateEdgeCondition(edge.getCondition(), context, currentNodeId)) {
                    return List.of(edge);
                }
                return Collections.emptyList();
            }
            return List.of(edges.get(0));
        }

        List<FlowEdge> matched = new ArrayList<>();
        FlowEdge defaultEdge = null;

        for (FlowEdge edge : edges) {
            String condition = edge.getCondition();
            String label = edge.getLabel();

            if ((condition == null || condition.isEmpty()) && (label == null || label.isEmpty())) {
                defaultEdge = edge;
                continue;
            }

            String evalKey = condition;
            if (evalKey == null || evalKey.isEmpty()) {
                evalKey = label;
            }

            if (evalKey != null && !evalKey.isEmpty()) {
                Object condResult = context.getVariables().get(currentNodeId + "_condition");
                String condStr = condResult != null ? String.valueOf(condResult) : null;

                if (condStr != null && matchesCondition(evalKey, condStr)) {
                    matched.add(edge);
                } else if (condStr == null && evaluateEdgeCondition(evalKey, context, currentNodeId)) {
                    matched.add(edge);
                }
            }
        }

        if (matched.isEmpty() && defaultEdge != null) {
            matched.add(defaultEdge);
        }

        if (matched.isEmpty() && !edges.isEmpty()) {
            matched.add(edges.get(0));
        }

        return matched;
    }

    private boolean matchesCondition(String edgeCondition, String conditionResult) {
        String normalized = edgeCondition.trim().toLowerCase();
        String normalizedResult = conditionResult.trim().toLowerCase();

        if (normalized.equals(normalizedResult)) return true;
        if (normalized.equals("true") && normalizedResult.equals("true")) return true;
        if (normalized.equals("false") && normalizedResult.equals("false")) return true;
        if (normalized.equals("是") && normalizedResult.equals("true")) return true;
        if (normalized.equals("否") && normalizedResult.equals("false")) return true;
        if (normalized.equals("yes") && normalizedResult.equals("true")) return true;
        if (normalized.equals("no") && normalizedResult.equals("false")) return true;

        try {
            return Double.parseDouble(normalized) == Double.parseDouble(normalizedResult);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean evaluateEdgeCondition(String condition, FlowContext context, String sourceNodeId) {
        if (condition == null || condition.isEmpty()) return true;

        try {
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val != null && String.valueOf(val).equals(parts[1].trim());
                }
            } else if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val == null || !String.valueOf(val).equals(parts[1].trim());
                }
            } else if (condition.contains(">=")) {
                String[] parts = condition.split(">=");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val != null && Double.parseDouble(String.valueOf(val)) >= Double.parseDouble(parts[1].trim());
                }
            } else if (condition.contains("<=")) {
                String[] parts = condition.split("<=");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val != null && Double.parseDouble(String.valueOf(val)) <= Double.parseDouble(parts[1].trim());
                }
            } else if (condition.contains(">") && !condition.contains(">=")) {
                String[] parts = condition.split(">");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val != null && Double.parseDouble(String.valueOf(val)) > Double.parseDouble(parts[1].trim());
                }
            } else if (condition.contains("<") && !condition.contains("<=")) {
                String[] parts = condition.split("<");
                if (parts.length == 2) {
                    Object val = getNodeResultVal(context, parts[0].trim(), sourceNodeId);
                    return val != null && Double.parseDouble(String.valueOf(val)) < Double.parseDouble(parts[1].trim());
                }
            } else {
                Object val = getNodeResultVal(context, condition.trim(), sourceNodeId);
                if (val instanceof Boolean) return (Boolean) val;
                if (val instanceof String) return !"false".equals(val) && !"0".equals(val);
                return val != null;
            }
        } catch (Exception e) {
            log.warn("条件表达式评估失败: condition={}, error={}", condition, e.getMessage());
        }
        return false;
    }

    private static Object getNodeResultVal(FlowContext context, String fieldRef, String sourceNodeId) {
        String targetNodeId = sourceNodeId;
        String fieldName = fieldRef;

        if (fieldRef.contains(".")) {
            int dotIdx = fieldRef.indexOf('.');
            targetNodeId = fieldRef.substring(0, dotIdx);
            fieldName = fieldRef.substring(dotIdx + 1);
        }

        NodeResult result = context.getNodeResults().get(targetNodeId);
        if (result == null) return null;
        return BeanUtil.getFieldValue(result, fieldName);
    }

    private NodeResult executeParallelGroup(String groupId, FlowNode groupNode,
                                             Map<String, FlowNode> nodeMap,
                                             Map<String, List<FlowEdge>> outgoingEdges,
                                             FlowContext context, MigrationTask task,
                                             Set<String> executedNodes, Set<String> executedEdgeIds) {
        cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(groupNode.getConfig() != null ? groupNode.getConfig() : "{}");
        String successStrategy = config.getStr("successStrategy", "ALL_SUCCESS");
        int maxConcurrency = config.getInt("maxConcurrency", 4);
        boolean retryOnFailure = config.getBool("retryOnFailure", false);
        int maxRetryCount = config.getInt("maxRetryCount", 3);

        List<FlowNode> childNodes = nodeMap.values().stream()
                .filter(n -> groupId.equals(n.getParentGroupId()))
                .collect(Collectors.toList());

        if (childNodes.isEmpty()) {
            return NodeResult.ok("并行组无子节点");
        }

        Set<String> childNodeIds = childNodes.stream().map(FlowNode::getNodeId).collect(Collectors.toSet());

        List<FlowEdge> internalEdges = outgoingEdges.entrySet().stream()
                .filter(e -> childNodeIds.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .filter(e -> childNodeIds.contains(e.getTargetNodeId()))
                .collect(Collectors.toList());

        Set<String> nodesWithIncoming = internalEdges.stream()
                .map(FlowEdge::getTargetNodeId)
                .collect(Collectors.toSet());

        List<String> entryNodeIds = childNodes.stream()
                .map(FlowNode::getNodeId)
                .filter(id -> !nodesWithIncoming.contains(id))
                .collect(Collectors.toList());

        if (entryNodeIds.isEmpty()) {
            entryNodeIds = childNodes.stream().map(FlowNode::getNodeId).collect(Collectors.toList());
        }

        Map<String, List<FlowEdge>> internalOutgoing = new HashMap<>();
        for (FlowEdge edge : internalEdges) {
            internalOutgoing.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
        }

        List<List<String>> chains = new ArrayList<>();
        for (String entryId : entryNodeIds) {
            List<String> chain = new ArrayList<>();
            buildChain(entryId, internalOutgoing, childNodeIds, chain, new HashSet<>());
            chains.add(chain);
        }

        Set<String> chainedNodes = chains.stream().flatMap(List::stream).collect(Collectors.toSet());
        for (FlowNode child : childNodes) {
            if (!chainedNodes.contains(child.getNodeId())) {
                chains.add(List.of(child.getNodeId()));
            }
        }

        log.info("并行组 [{}] 识别出 {} 条子流程链路, 最大并发: {}, 重试: {}, 策略: {}",
                groupNode.getName(), chains.size(), maxConcurrency, retryOnFailure, successStrategy);

        taskLogRepository.save(TaskLog.builder()
                .taskId(task.getId())
                .nodeId(groupId)
                .nodeName(groupNode.getName())
                .level(LogLevel.INFO)
                .message(String.format("并行组 [%s] 识别出 %d 条子流程链路, 最大并发=%d, 重试=%s, 策略=%s",
                        groupNode.getName(), chains.size(), maxConcurrency, retryOnFailure, successStrategy))
                .build());

        for (int i = 0; i < chains.size(); i++) {
            List<String> chain = chains.get(i);
            String chainDesc = chain.stream()
                    .map(id -> nodeMap.containsKey(id) ? nodeMap.get(id).getName() : id)
                    .collect(Collectors.joining(" -> "));
            log.info("  链路 {}: {}", i + 1, chainDesc);
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(chains.size(), maxConcurrency));
        List<CompletableFuture<NodeResult>> futures = new ArrayList<>();

        for (List<String> chain : chains) {
            CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> {
                return executeChain(chain, internalOutgoing, nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds, retryOnFailure, maxRetryCount);
            }, executor);
            futures.add(future);
        }

        List<NodeResult> results;
        try {
            results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }

        long successCount = results.stream().filter(NodeResult::isSuccess).count();
        long failCount = results.size() - successCount;

        boolean groupSuccess;
        if ("ANY_SUCCESS".equals(successStrategy)) {
            groupSuccess = successCount >= 1;
        } else {
            groupSuccess = failCount == 0;
        }

        String summary = String.format("并行组执行完成: %d/%d 链路成功, 策略=%s, 结果=%s",
                successCount, results.size(), successStrategy, groupSuccess ? "成功" : "失败");

        taskLogRepository.save(TaskLog.builder()
                .taskId(task.getId())
                .nodeId(groupId)
                .nodeName(groupNode.getName())
                .level(groupSuccess ? LogLevel.INFO : LogLevel.ERROR)
                .message(summary)
                .build());

        if (!groupSuccess) {
            throw new RuntimeException(summary);
        }

        return NodeResult.ok(summary);
    }

    private void buildChain(String nodeId, Map<String, List<FlowEdge>> internalOutgoing,
                             Set<String> childNodeIds, List<String> chain, Set<String> visited) {
        if (visited.contains(nodeId)) return;
        visited.add(nodeId);
        chain.add(nodeId);

        List<FlowEdge> edges = internalOutgoing.get(nodeId);
        if (edges != null && !edges.isEmpty()) {
            FlowEdge nextEdge = edges.get(0);
            buildChain(nextEdge.getTargetNodeId(), internalOutgoing, childNodeIds, chain, visited);
        }
    }

    private NodeResult executeChain(List<String> chain,
                                     Map<String, List<FlowEdge>> internalOutgoing,
                                     Map<String, FlowNode> nodeMap,
                                     Map<String, List<FlowEdge>> outgoingEdges,
                                     FlowContext context, MigrationTask task,
                                     Set<String> executedNodes, Set<String> executedEdgeIds,
                                     boolean retryOnFailure, int maxRetryCount) {
        for (int i = 0; i < chain.size(); i++) {
            String nodeId = chain.get(i);
            String incomingEdgeId = null;
            if (i > 0) {
                String prevNodeId = chain.get(i - 1);
                List<FlowEdge> edges = internalOutgoing.get(prevNodeId);
                if (edges != null) {
                    incomingEdgeId = edges.stream()
                            .filter(e -> e.getTargetNodeId().equals(nodeId))
                            .map(FlowEdge::getEdgeId)
                            .findFirst()
                            .orElse(null);
                }
            }

            NodeResult result = executeChildNodeWithRetry(nodeId, incomingEdgeId, nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds, retryOnFailure, maxRetryCount);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return NodeResult.ok("链路执行完成");
    }

    private NodeResult executeChildNodeWithRetry(String nodeId, String incomingEdgeId,
                                                   Map<String, FlowNode> nodeMap,
                                                   Map<String, List<FlowEdge>> outgoingEdges,
                                                   FlowContext context, MigrationTask task,
                                                   Set<String> executedNodes, Set<String> executedEdgeIds,
                                                   boolean retryOnFailure, int maxRetryCount) {
        int attempts = retryOnFailure ? maxRetryCount + 1 : 1;
        NodeResult result = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) {
                synchronized (executedNodes) {
                    executedNodes.remove(nodeId);
                }
                log.info("节点 {} 第 {}/{} 次重试", nodeId, attempt - 1, maxRetryCount);
                taskLogRepository.save(TaskLog.builder()
                        .taskId(task.getId())
                        .nodeId(nodeId)
                        .level(LogLevel.WARN)
                        .message(String.format("节点 [%s] 第 %d/%d 次重试", nodeMap.get(nodeId) != null ? nodeMap.get(nodeId).getName() : nodeId, attempt - 1, maxRetryCount))
                        .build());
            }

            result = executeChildNode(nodeId, incomingEdgeId, nodeMap, outgoingEdges, context, task, executedNodes, executedEdgeIds);
            if (result.isSuccess()) {
                return result;
            }
        }

        return result;
    }

    private NodeResult executeChildNode(String nodeId, String incomingEdgeId,
                                         Map<String, FlowNode> nodeMap,
                                         Map<String, List<FlowEdge>> outgoingEdges,
                                         FlowContext context, MigrationTask task,
                                         Set<String> executedNodes, Set<String> executedEdgeIds) {
        synchronized (executedNodes) {
            if (executedNodes.contains(nodeId)) {
                return NodeResult.ok("已执行");
            }
            executedNodes.add(nodeId);
        }

        FlowNode node = nodeMap.get(nodeId);
        if (node == null) {
            return NodeResult.fail("节点不存在: " + nodeId);
        }

        NodeExecution record = NodeExecution.builder()
                .taskId(task.getId())
                .nodeId(nodeId)
                .nodeName(node.getName())
                .nodeType(node.getNodeType().name())
                .incomingEdgeId(incomingEdgeId)
                .status(TaskStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();

        try {
            NodeExecutor executor = nodeHandlerRegistry.getHandler(node.getNodeType());
            NodeResult result;
            if (executor != null) {
                result = executor.execute(context, node);
            } else {
                result = NodeResult.ok("节点执行完成", 0, 0, 0);
            }

            synchronized (context) {
                context.getNodeResults().put(nodeId, result);
            }

            record.setStatus(TaskStatus.SUCCESS);
            record.setOutputSummary(result.summary);
            record.setFinishedAt(LocalDateTime.now());
            record.setDuration(Duration.between(record.getStartedAt(), record.getFinishedAt()).toMillis());

            synchronized (task) {
                task.setCompletedNodes(task.getCompletedNodes() + 1);
                if (node.getNodeType() == NodeType.DATA_EXTRACT) {
                    task.setExtractedRecords(task.getExtractedRecords() + result.totalRecords);
                } else if (node.getNodeType() == NodeType.DATA_LOAD) {
                    task.setLoadedRecords(task.getLoadedRecords() + result.totalRecords);
                    task.setLoadedSuccessRecords(task.getLoadedSuccessRecords() + result.successRecords);
                    task.setLoadedFailedRecords(task.getLoadedFailedRecords() + result.failedRecords);
                }
            }

            return result;
        } catch (Exception e) {
            record.setStatus(TaskStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            record.setDuration(Duration.between(record.getStartedAt(), record.getFinishedAt()).toMillis());
            synchronized (context) {
                context.getNodeResults().put(nodeId, NodeResult.fail(e.getMessage()));
            }
            return NodeResult.fail(e.getMessage());
        } finally {
            nodeExecutionRepository.save(record);
        }
    }

    private boolean isDataNode(NodeType nodeType) {
        return nodeType == NodeType.DATA_EXTRACT || nodeType == NodeType.DATA_LOAD;
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        String trace = sw.toString();
        if (trace.length() > 4000) {
            return trace.substring(0, 4000);
        }
        return trace;
    }

    @lombok.Data
    public static class FlowContext {
        private Long taskId;
        private MigrationTask task;
        private Map<String, NodeResult> nodeResults;
        private Map<String, Object> variables;
        private List<Object> data;
        private boolean paused;
        private boolean cancelled;

        public boolean isInterrupted() {
            return paused || cancelled;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NodeResult {
        private boolean success;
        private String summary;
        private long totalRecords;
        private long successRecords;
        private long failedRecords;
        private String errorMessage;

        public static NodeResult ok(String summary, long total, long success, long failed) {
            return NodeResult.builder()
                    .success(true)
                    .summary(summary)
                    .totalRecords(total)
                    .successRecords(success)
                    .failedRecords(failed)
                    .build();
        }

        public static NodeResult ok(String summary) {
            return NodeResult.builder()
                    .success(true)
                    .summary(summary)
                    .totalRecords(0)
                    .successRecords(0)
                    .failedRecords(0)
                    .build();
        }

        public static NodeResult fail(String errorMessage) {
            return NodeResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
