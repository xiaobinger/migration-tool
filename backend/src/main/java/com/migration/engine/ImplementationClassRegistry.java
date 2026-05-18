package com.migration.engine;

import com.migration.evaluator.ConditionEvaluator;
import com.migration.evaluator.DataQualityConditionEvaluator;
import com.migration.evaluator.ExpressionConditionEvaluator;
import com.migration.evaluator.RecordCountConditionEvaluator;
import com.migration.extractor.DataExtractor;
import com.migration.extractor.FileDataExtractor;
import com.migration.extractor.MongoDBDataExtractor;
import com.migration.extractor.MySQLDataExtractor;
import com.migration.extractor.PostgreSQLDataExtractor;
import com.migration.extractor.RESTAPIDataExtractor;
import com.migration.loader.DataLoader;
import com.migration.loader.FileDataLoader;
import com.migration.loader.MongoDBDataLoader;
import com.migration.loader.MySQLDataLoader;
import com.migration.loader.PostgreSQLDataLoader;
import com.migration.loader.RESTAPIDataLoader;
import com.migration.model.enums.NodeType;
import com.migration.notifier.DingTalkNotifier;
import com.migration.notifier.EmailNotifier;
import com.migration.notifier.Notifier;
import com.migration.service.DataSourceConfigService;
import com.migration.transformer.DataTransformer;
import com.migration.transformer.FieldMappingTransformer;
import com.migration.transformer.GroovyScriptTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ImplementationClassRegistry {

    private final Map<String, DataExtractor> extractorMap = new ConcurrentHashMap<>();
    private final Map<String, DataTransformer> transformerMap = new ConcurrentHashMap<>();
    private final Map<String, DataLoader> loaderMap = new ConcurrentHashMap<>();
    private final Map<String, ConditionEvaluator> evaluatorMap = new ConcurrentHashMap<>();
    private final Map<String, Notifier> notifierMap = new ConcurrentHashMap<>();
    private final Map<NodeType, List<ImplementationInfo>> implementations = new ConcurrentHashMap<>();
    private final DataSourceConfigService dataSourceConfigService;

    public ImplementationClassRegistry(JavaMailSender mailSender,
                                       @Value("${migration.dingtalk.webhook-url:}") String dingtalkWebhookUrl,
                                       @Value("${migration.dingtalk.secret:}") String dingtalkSecret,
                                       DataSourceConfigService dataSourceConfigService) {
        this.dataSourceConfigService = dataSourceConfigService;
        registerExtractors();
        registerTransformers();
        registerLoaders();
        registerEvaluators();
        registerNotifiers(mailSender, dingtalkWebhookUrl, dingtalkSecret);
        log.info("实现类注册完成: extractors={}, transformers={}, loaders={}, evaluators={}, notifiers={}",
                extractorMap.size(), transformerMap.size(), loaderMap.size(), evaluatorMap.size(), notifierMap.size());
    }

    private void registerExtractors() {
        List<ImplementationInfo> extractorInfos = new ArrayList<>();

        DataExtractor mysql = new MySQLDataExtractor();
        mysql.setDataSourceConfigService(dataSourceConfigService);
        extractorMap.put(mysql.getFullName(), mysql);
        extractorInfos.add(new ImplementationInfo(mysql.getName(), mysql.getFullName(), mysql.getDescription(), NodeType.DATA_EXTRACT, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的数据库连接", null, "数据源"),
                new ParameterInfo("queryMode", "查询模式", "string", "select", true, "table", "选择表或自定义SQL", List.of(new OptionItem("选择表", "table"), new OptionItem("自定义SQL", "script")), "数据源"),
                new ParameterInfo("table", "表名", "string", "table-select", false, null, "选择要提取的表", null, "提取配置"),
                new ParameterInfo("script", "SQL脚本", "string", "textarea", false, "", "自定义SQL查询语句", null, "提取配置"),
                new ParameterInfo("batchSize", "批量提取大小", "integer", "number", false, "1000", "批量提取大小", null, "提取配置"),
                new ParameterInfo("whereClause", "WHERE条件子句", "string", "textarea", false, "", "WHERE条件子句", null, "提取配置")
        )));

        DataExtractor postgresql = new PostgreSQLDataExtractor();
        postgresql.setDataSourceConfigService(dataSourceConfigService);
        extractorMap.put(postgresql.getFullName(), postgresql);
        extractorInfos.add(new ImplementationInfo(postgresql.getName(), postgresql.getFullName(), postgresql.getDescription(), NodeType.DATA_EXTRACT, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的数据库连接", null, "数据源"),
                new ParameterInfo("queryMode", "查询模式", "string", "select", true, "table", "选择表或自定义SQL", List.of(new OptionItem("选择表", "table"), new OptionItem("自定义SQL", "script")), "数据源"),
                new ParameterInfo("table", "表名", "string", "table-select", false, null, "选择要提取的表", null, "提取配置"),
                new ParameterInfo("script", "SQL脚本", "string", "textarea", false, "", "自定义SQL查询语句", null, "提取配置"),
                new ParameterInfo("batchSize", "批量提取大小", "integer", "number", false, "1000", "批量提取大小", null, "提取配置"),
                new ParameterInfo("whereClause", "WHERE条件子句", "string", "textarea", false, "", "WHERE条件子句", null, "提取配置")
        )));

        DataExtractor mongodb = new MongoDBDataExtractor();
        mongodb.setDataSourceConfigService(dataSourceConfigService);
        extractorMap.put(mongodb.getFullName(), mongodb);
        extractorInfos.add(new ImplementationInfo(mongodb.getName(), mongodb.getFullName(), mongodb.getDescription(), NodeType.DATA_EXTRACT, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的MongoDB连接", null, "数据源"),
                new ParameterInfo("collection", "集合名称", "string", "text", true, null, "集合名称", null, "提取配置"),
                new ParameterInfo("query", "查询条件(JSON)", "string", "textarea", false, "", "查询条件(JSON)", null, "提取配置"),
                new ParameterInfo("batchSize", "批量提取大小", "integer", "number", false, "1000", "批量提取大小", null, "提取配置")
        )));

        DataExtractor restapi = new RESTAPIDataExtractor();
        extractorMap.put(restapi.getFullName(), restapi);
        extractorInfos.add(new ImplementationInfo(restapi.getName(), restapi.getFullName(), restapi.getDescription(), NodeType.DATA_EXTRACT, List.of(
                new ParameterInfo("url", "API端点URL", "string", "text", true, null, "API端点URL", null, "请求配置"),
                new ParameterInfo("method", "HTTP方法", "string", "select", false, "GET", "HTTP方法", List.of(new OptionItem("GET", "GET"), new OptionItem("POST", "POST")), "请求配置"),
                new ParameterInfo("headers", "请求头(JSON)", "string", "textarea", false, "", "请求头(JSON)", null, "请求配置"),
                new ParameterInfo("params", "查询参数(JSON)", "string", "textarea", false, "", "查询参数(JSON)", null, "请求配置"),
                new ParameterInfo("pageSize", "每页数据量", "integer", "number", false, "100", "每页数据量", null, "分页配置"),
                new ParameterInfo("maxPages", "最大页数", "integer", "number", false, "100", "最大页数", null, "分页配置")
        )));

        DataExtractor file = new FileDataExtractor();
        extractorMap.put(file.getFullName(), file);
        extractorInfos.add(new ImplementationInfo(file.getName(), file.getFullName(), file.getDescription(), NodeType.DATA_EXTRACT, List.of(
                new ParameterInfo("filePath", "文件路径", "string", "text", true, null, "文件路径", null, "文件配置"),
                new ParameterInfo("fileType", "文件类型", "string", "select", true, "csv", "文件类型", List.of(new OptionItem("csv", "csv"), new OptionItem("json", "json"), new OptionItem("excel", "excel")), "文件配置"),
                new ParameterInfo("encoding", "文件编码", "string", "text", false, "UTF-8", "文件编码", null, "文件配置"),
                new ParameterInfo("hasHeader", "是否有表头", "boolean", "switch", false, "true", "是否有表头", null, "文件配置"),
                new ParameterInfo("sheetName", "Excel工作表名称", "string", "text", false, "", "Excel工作表名称", null, "文件配置")
        )));

        implementations.put(NodeType.DATA_EXTRACT, extractorInfos);
    }

    private void registerTransformers() {
        List<ImplementationInfo> transformerInfos = new ArrayList<>();

        DataTransformer fieldMapping = new FieldMappingTransformer();
        transformerMap.put(fieldMapping.getFullName(), fieldMapping);
        transformerInfos.add(new ImplementationInfo(fieldMapping.getName(), fieldMapping.getFullName(), fieldMapping.getDescription(), NodeType.DATA_TRANSFORM, List.of(
                new ParameterInfo("mappings", "字段映射配置(JSON)", "string", "textarea", true, "", "字段映射配置(JSON)", null, "映射配置"),
                new ParameterInfo("skipNull", "跳过空值字段", "boolean", "switch", false, "false", "跳过空值字段", null, "映射配置")
        )));

        DataTransformer groovyScript = new GroovyScriptTransformer();
        transformerMap.put(groovyScript.getFullName(), groovyScript);
        transformerInfos.add(new ImplementationInfo(groovyScript.getName(), groovyScript.getFullName(), groovyScript.getDescription(), NodeType.DATA_TRANSFORM, List.of(
                new ParameterInfo("script", "Groovy脚本内容", "string", "textarea", true, "", "Groovy脚本内容", null, "脚本配置"),
                new ParameterInfo("imports", "导入的类(逗号分隔)", "string", "text", false, "", "导入的类(逗号分隔)", null, "脚本配置")
        )));

        implementations.put(NodeType.DATA_TRANSFORM, transformerInfos);
    }

    private void registerLoaders() {
        List<ImplementationInfo> loaderInfos = new ArrayList<>();

        DataLoader mysqlLoader = new MySQLDataLoader();
        mysqlLoader.setDataSourceConfigService(dataSourceConfigService);
        loaderMap.put(mysqlLoader.getFullName(), mysqlLoader);
        loaderInfos.add(new ImplementationInfo(mysqlLoader.getName(), mysqlLoader.getFullName(), mysqlLoader.getDescription(), NodeType.DATA_LOAD, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的数据库连接", null, "数据源"),
                new ParameterInfo("table", "目标表名", "string", "table-select", true, null, "选择目标表", null, "写入配置"),
                new ParameterInfo("batchSize", "批量写入大小", "integer", "number", false, "1000", "批量写入大小", null, "写入配置"),
                new ParameterInfo("writeMode", "写入模式", "string", "select", false, "INSERT", "写入模式", List.of(new OptionItem("INSERT", "INSERT"), new OptionItem("UPDATE", "UPDATE"), new OptionItem("UPSERT", "UPSERT")), "写入配置"),
                new ParameterInfo("onDuplicateKey", "UPSERT更新字段", "string", "text", false, "", "UPSERT更新字段", null, "写入配置")
        )));

        DataLoader postgresqlLoader = new PostgreSQLDataLoader();
        postgresqlLoader.setDataSourceConfigService(dataSourceConfigService);
        loaderMap.put(postgresqlLoader.getFullName(), postgresqlLoader);
        loaderInfos.add(new ImplementationInfo(postgresqlLoader.getName(), postgresqlLoader.getFullName(), postgresqlLoader.getDescription(), NodeType.DATA_LOAD, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的数据库连接", null, "数据源"),
                new ParameterInfo("table", "目标表名", "string", "table-select", true, null, "选择目标表", null, "写入配置"),
                new ParameterInfo("batchSize", "批量写入大小", "integer", "number", false, "1000", "批量写入大小", null, "写入配置"),
                new ParameterInfo("writeMode", "写入模式", "string", "select", false, "INSERT", "写入模式", List.of(new OptionItem("INSERT", "INSERT"), new OptionItem("UPDATE", "UPDATE"), new OptionItem("UPSERT", "UPSERT")), "写入配置")
        )));

        DataLoader mongodbLoader = new MongoDBDataLoader();
        mongodbLoader.setDataSourceConfigService(dataSourceConfigService);
        loaderMap.put(mongodbLoader.getFullName(), mongodbLoader);
        loaderInfos.add(new ImplementationInfo(mongodbLoader.getName(), mongodbLoader.getFullName(), mongodbLoader.getDescription(), NodeType.DATA_LOAD, List.of(
                new ParameterInfo("connectionId", "数据源连接", "string", "datasource", true, null, "选择已配置的MongoDB连接", null, "数据源"),
                new ParameterInfo("collection", "集合名称", "string", "text", true, null, "集合名称", null, "写入配置"),
                new ParameterInfo("writeMode", "写入模式", "string", "select", false, "INSERT", "写入模式", List.of(new OptionItem("INSERT", "INSERT"), new OptionItem("UPSERT", "UPSERT")), "写入配置"),
                new ParameterInfo("upsertKey", "UPSERT唯一键", "string", "text", false, "", "UPSERT唯一键", null, "写入配置")
        )));

        DataLoader fileLoader = new FileDataLoader();
        loaderMap.put(fileLoader.getFullName(), fileLoader);
        loaderInfos.add(new ImplementationInfo(fileLoader.getName(), fileLoader.getFullName(), fileLoader.getDescription(), NodeType.DATA_LOAD, List.of(
                new ParameterInfo("filePath", "输出文件路径", "string", "text", true, null, "输出文件路径", null, "文件配置"),
                new ParameterInfo("fileType", "文件类型", "string", "select", true, "csv", "文件类型", List.of(new OptionItem("csv", "csv"), new OptionItem("json", "json"), new OptionItem("excel", "excel")), "文件配置"),
                new ParameterInfo("writeMode", "写入模式", "string", "select", false, "OVERWRITE", "写入模式", List.of(new OptionItem("OVERWRITE", "OVERWRITE"), new OptionItem("APPEND", "APPEND")), "文件配置"),
                new ParameterInfo("encoding", "文件编码", "string", "text", false, "UTF-8", "文件编码", null, "文件配置")
        )));

        DataLoader restapiLoader = new RESTAPIDataLoader();
        loaderMap.put(restapiLoader.getFullName(), restapiLoader);
        loaderInfos.add(new ImplementationInfo(restapiLoader.getName(), restapiLoader.getFullName(), restapiLoader.getDescription(), NodeType.DATA_LOAD, List.of(
                new ParameterInfo("url", "API端点URL", "string", "text", true, null, "API端点URL", null, "请求配置"),
                new ParameterInfo("method", "HTTP方法", "string", "select", false, "POST", "HTTP方法", List.of(new OptionItem("POST", "POST"), new OptionItem("PUT", "PUT"), new OptionItem("PATCH", "PATCH")), "请求配置"),
                new ParameterInfo("headers", "请求头(JSON)", "string", "textarea", false, "", "请求头(JSON)", null, "请求配置"),
                new ParameterInfo("bodyTemplate", "请求体模板", "string", "textarea", false, "", "请求体模板", null, "请求配置")
        )));

        implementations.put(NodeType.DATA_LOAD, loaderInfos);
    }

    private void registerEvaluators() {
        List<ImplementationInfo> evaluatorInfos = new ArrayList<>();

        ConditionEvaluator expression = new ExpressionConditionEvaluator();
        evaluatorMap.put(expression.getFullName(), expression);
        evaluatorInfos.add(new ImplementationInfo(expression.getName(), expression.getFullName(), expression.getDescription(), NodeType.CONDITION, List.of(
                new ParameterInfo("expression", "条件表达式", "string", "textarea", true, "", "条件表达式", null, "表达式配置")
        )));

        ConditionEvaluator dataQuality = new DataQualityConditionEvaluator();
        evaluatorMap.put(dataQuality.getFullName(), dataQuality);
        evaluatorInfos.add(new ImplementationInfo(dataQuality.getName(), dataQuality.getFullName(), dataQuality.getDescription(), NodeType.CONDITION, List.of(
                new ParameterInfo("qualityRules", "质量规则配置(JSON)", "string", "textarea", true, "", "质量规则配置(JSON)", null, "质量规则")
        )));

        ConditionEvaluator recordCount = new RecordCountConditionEvaluator();
        evaluatorMap.put(recordCount.getFullName(), recordCount);
        evaluatorInfos.add(new ImplementationInfo(recordCount.getName(), recordCount.getFullName(), recordCount.getDescription(), NodeType.CONDITION, List.of(
                new ParameterInfo("operator", "比较操作符", "string", "select", true, "ge", "比较操作符", List.of(new OptionItem("等于", "eq"), new OptionItem("不等于", "ne"), new OptionItem("大于", "gt"), new OptionItem("大于等于", "ge"), new OptionItem("小于", "lt"), new OptionItem("小于等于", "le")), "条件配置"),
                new ParameterInfo("threshold", "阈值", "integer", "number", true, "0", "阈值", null, "条件配置"),
                new ParameterInfo("sourceNodeId", "数据来源节点ID", "string", "text", false, "", "数据来源节点ID", null, "条件配置")
        )));

        implementations.put(NodeType.CONDITION, evaluatorInfos);
    }

    private void registerNotifiers(JavaMailSender mailSender, String dingtalkWebhookUrl, String dingtalkSecret) {
        List<ImplementationInfo> notifierInfos = new ArrayList<>();

        EmailNotifier emailNotifier = new EmailNotifier(mailSender);
        notifierMap.put(emailNotifier.getFullName(), emailNotifier);
        notifierInfos.add(new ImplementationInfo(emailNotifier.getName(), emailNotifier.getFullName(), emailNotifier.getDescription(), NodeType.NOTIFICATION, List.of(
                new ParameterInfo("recipients", "收件人(逗号分隔)", "string", "text", true, null, "收件人(逗号分隔)", null, "收件配置"),
                new ParameterInfo("cc", "抄送(逗号分隔)", "string", "text", false, "", "抄送(逗号分隔)", null, "收件配置"),
                new ParameterInfo("from", "发件人地址", "string", "text", false, "migration-tool@example.com", "发件人地址", null, "收件配置"),
                new ParameterInfo("subject", "邮件主题", "string", "text", true, null, "邮件主题", null, "内容配置"),
                new ParameterInfo("content", "邮件内容", "string", "textarea", true, "", "邮件内容", null, "内容配置"),
                new ParameterInfo("html", "HTML格式", "boolean", "switch", false, "true", "HTML格式", null, "内容配置"),
                new ParameterInfo("template", "邮件模板", "string", "text", false, "", "邮件模板", null, "内容配置")
        )));

        DingTalkNotifier dingTalkNotifier = new DingTalkNotifier(dingtalkWebhookUrl, dingtalkSecret);
        notifierMap.put(dingTalkNotifier.getFullName(), dingTalkNotifier);
        notifierInfos.add(new ImplementationInfo(dingTalkNotifier.getName(), dingTalkNotifier.getFullName(), dingTalkNotifier.getDescription(), NodeType.NOTIFICATION, List.of(
                new ParameterInfo("message", "消息内容", "string", "textarea", true, "", "消息内容", null, "消息配置"),
                new ParameterInfo("msgType", "消息类型", "string", "select", false, "text", "消息类型", List.of(new OptionItem("文本", "text"), new OptionItem("Markdown", "markdown")), "消息配置"),
                new ParameterInfo("title", "消息标题", "string", "text", false, "迁移任务通知", "Markdown消息标题", null, "消息配置"),
                new ParameterInfo("atMobiles", "@人手机号(逗号分隔)", "string", "text", false, "", "@人手机号(逗号分隔)", null, "消息配置"),
                new ParameterInfo("atAll", "@所有人", "boolean", "switch", false, "false", "@所有人", null, "消息配置")
        )));

        implementations.put(NodeType.NOTIFICATION, notifierInfos);

        List<ImplementationInfo> parallelGroupInfos = new ArrayList<>();
        parallelGroupInfos.add(new ImplementationInfo("ParallelGroupExecutor", "com.migration.engine.ParallelGroupExecutor", "并行组执行器", NodeType.PARALLEL_GROUP, List.of(
                new ParameterInfo("successStrategy", "成功策略", "string", "select", false, "ALL_SUCCESS", "并行组成功判定策略", List.of(new OptionItem("所有子流程成功才算成功", "ALL_SUCCESS"), new OptionItem("任一子流程成功即算成功", "ANY_SUCCESS")), "并行组配置"),
                new ParameterInfo("maxConcurrency", "最大并发线程数", "integer", "number", false, "4", "同时执行的子流程最大数量", null, "并行组配置"),
                new ParameterInfo("retryOnFailure", "失败重试", "boolean", "switch", false, "false", "子流程节点失败时是否自动重试", null, "并行组配置"),
                new ParameterInfo("maxRetryCount", "最大重试次数", "integer", "number", false, "3", "每个节点失败后的最大重试次数", null, "并行组配置")
        )));
        implementations.put(NodeType.PARALLEL_GROUP, parallelGroupInfos);
    }

    public DataExtractor getExtractor(String fullClassName) {
        return extractorMap.get(fullClassName);
    }

    public DataTransformer getTransformer(String fullClassName) {
        return transformerMap.get(fullClassName);
    }

    public DataLoader getLoader(String fullClassName) {
        return loaderMap.get(fullClassName);
    }

    public ConditionEvaluator getEvaluator(String fullClassName) {
        return evaluatorMap.get(fullClassName);
    }

    public Notifier getNotifier(String fullClassName) {
        return notifierMap.get(fullClassName);
    }

    public List<ImplementationInfo> getImplementations(NodeType nodeType) {
        return implementations.getOrDefault(nodeType, Collections.emptyList());
    }

    public Map<String, List<ImplementationInfo>> getAllImplementations() {
        Map<String, List<ImplementationInfo>> result = new HashMap<>();
        implementations.forEach((type, infos) -> result.put(type.name(), infos));
        return result;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ImplementationInfo {
        private String className;
        private String fullClassName;
        private String description;
        private NodeType nodeType;
        private List<ParameterInfo> parameters;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ParameterInfo {
        private String name;
        private String label;
        private String type;
        private String inputType;
        private boolean required;
        private String defaultValue;
        private String description;
        private List<OptionItem> options;
        private String group;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OptionItem {
        private String label;
        private String value;
    }
}
