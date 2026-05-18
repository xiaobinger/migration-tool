// 流程节点类型
export type NodeTypeCode =
  | 'start'
  | 'end'
  | 'data_extract'
  | 'data_transform'
  | 'data_load'
  | 'validation'
  | 'condition'
  | 'parallel'
  | 'merge'
  | 'parallel_group'
  | 'notification'
  | 'script'
  | 'delay'

// 节点类型配置
export interface NodeTypeConfig {
  code: NodeTypeCode
  label: string
  icon: string
  color: string
  category: string
  description?: string
}

// 业务逻辑实现类
export interface ImplementationClass {
  className: string
  classFullName: string
  description: string
  parameters?: {
    name: string
    type: string
    required: boolean
    defaultValue?: string
    description?: string
  }[]
  exampleConfig?: string
}

// 流程节点
export interface FlowNode {
  nodeId: string
  nodeType: NodeTypeCode
  name: string
  description?: string
  config?: string
  positionX?: number
  positionY?: number
  implementationClass?: string
  implementationConfig?: Record<string, any>
  parentGroupId?: string
}

// 流程边
export interface FlowEdge {
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  condition?: string
  label?: string
  edgeStyle?: 'straight' | 'orthogonal' | 'curved'
}

// 流程定义
export interface FlowDefinition {
  id?: number
  name: string
  description?: string
  version?: number
  enabled?: boolean
  createdBy?: string
  nodes: FlowNode[]
  edges: FlowEdge[]
}

// 任务状态
export type TaskStatusCode =
  | 'PENDING'
  | 'RUNNING'
  | 'PAUSED'
  | 'SUCCESS'
  | 'FAILED'
  | 'PARTIAL_SUCCESS'
  | 'CANCELLED'
  | 'SKIPPED'

// 迁移任务
export interface MigrationTask {
  id: number
  name: string
  flowDefinitionId: number
  status: TaskStatusCode
  currentNodeId?: string
  totalNodes: number
  completedNodes: number
  totalRecords: number
  successRecords: number
  failedRecords: number
  errorMessage?: string
  restartFromNodeId?: string
  createdBy?: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
}

// 任务执行请求
export interface TaskExecutionRequest {
  name: string
  flowDefinitionId: number
  params?: Record<string, any>
  restart?: boolean
  restartFromNodeId?: string
}

// 节点执行记录
export interface NodeExecution {
  id: number
  taskId: number
  nodeId: string
  nodeName: string
  nodeType: string
  incomingEdgeId?: string
  status: TaskStatusCode
  retryCount: number
  maxRetry: number
  errorMessage?: string
  inputSummary?: string
  outputSummary?: string
  startedAt?: string
  finishedAt?: string
  duration: number
}

// 任务日志
export interface TaskLog {
  id: number
  taskId: number
  nodeId?: string
  nodeName?: string
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  message: string
  stackTrace?: string
  recordCount: number
  duration: number
  logTime: string
}

// WebSocket进度消息
export interface TaskProgressMessage {
  type: 'progress' | 'node_start' | 'node_complete' | 'node_error' | 'task_complete' | 'task_failed' | 'log'
  taskId: number
  nodeId?: string
  nodeName?: string
  taskStatus?: string
  nodeStatus?: string
  progress?: number
  totalRecords?: number
  successRecords?: number
  failedRecords?: number
  message?: string
  error?: string
  timestamp: number
}

// 错误分析
export interface ErrorAnalysis {
  taskId: number
  totalErrors: number
  errorsByNode: Record<string, TaskLog[]>
  failedNodes: NodeExecution[]
  errorTypes: Record<string, number>
  suggestions: string[]
}

// 节点类型配置列表
export const NODE_TYPE_LIST: NodeTypeConfig[] = [
  { code: 'start', label: '开始', icon: 'VideoPlay', color: '#67C23A', category: '控制', description: '流程起始节点' },
  { code: 'end', label: '结束', icon: 'CircleClose', color: '#F56C6C', category: '控制', description: '流程结束节点' },
  { code: 'data_extract', label: '数据提取', icon: 'Download', color: '#409EFF', category: '数据', description: '从源系统提取数据' },
  { code: 'data_transform', label: '数据转换', icon: 'Switch', color: '#E6A23C', category: '数据', description: '对数据进行转换处理' },
  { code: 'data_load', label: '数据加载', icon: 'Upload', color: '#67C23A', category: '数据', description: '将数据加载到目标系统' },
  { code: 'validation', label: '数据校验', icon: 'CircleCheck', color: '#909399', category: '数据', description: '校验数据完整性和准确性' },
  { code: 'condition', label: '条件判断', icon: 'Share', color: '#F56C6C', category: '控制', description: '根据条件分支执行' },
  { code: 'parallel', label: '并行网关', icon: 'Grid', color: '#409EFF', category: '控制', description: '并行执行多个分支' },
  { code: 'merge', label: '合并网关', icon: 'Merge', color: '#409EFF', category: '控制', description: '合并并行执行分支' },
  { code: 'parallel_group', label: '并行组', icon: 'Grid', color: '#9B59B6', category: '控制', description: '将多个节点组合为并行执行组' },
  { code: 'notification', label: '通知', icon: 'Bell', color: '#E6A23C', category: '辅助', description: '发送通知消息' },
  { code: 'script', label: '自定义脚本', icon: 'Document', color: '#909399', category: '辅助', description: '执行自定义脚本逻辑' },
  { code: 'delay', label: '延时等待', icon: 'Timer', color: '#909399', category: '辅助', description: '延时等待指定时间' },
]

// 业务逻辑实现类映射
export const IMPLEMENTATION_CLASSES: Record<NodeTypeCode, ImplementationClass[]> = {
  'start': [
    {
      className: 'StartNodeExecutor',
      classFullName: 'com.migration.executor.StartNodeExecutor',
      description: '标准开始节点执行器',
      parameters: [
        { name: 'timeout', type: 'Integer', required: false, defaultValue: '0', description: '超时时间（毫秒），0表示不限制' }
      ],
      exampleConfig: '{"timeout": 0}'
    }
  ],

  'end': [
    {
      className: 'EndNodeExecutor',
      classFullName: 'com.migration.executor.EndNodeExecutor',
      description: '标准结束节点执行器',
      parameters: [
        { name: 'sendNotification', type: 'Boolean', required: false, defaultValue: 'false', description: '是否发送完成通知' }
      ],
      exampleConfig: '{"sendNotification": false}'
    }
  ],

  'data_extract': [
    {
      className: 'MySQLDataExtractor',
      classFullName: 'com.migration.extractor.MySQLDataExtractor',
      description: 'MySQL数据库数据提取器',
      parameters: [
        { name: 'host', type: 'String', required: true, description: '数据库主机地址' },
        { name: 'port', type: 'Integer', required: true, defaultValue: '3306', description: '数据库端口' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'username', type: 'String', required: true, description: '数据库用户名' },
        { name: 'password', type: 'String', required: true, description: '数据库密码' },
        { name: 'table', type: 'String', required: true, description: '要提取的表名' },
        { name: 'batchSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量提取大小' },
        { name: 'whereClause', type: 'String', required: false, description: 'WHERE条件子句' }
      ],
      exampleConfig: '{\n  "host": "localhost",\n  "port": 3306,\n  "database": "source_db",\n  "username": "root",\n  "password": "***",\n  "table": "users",\n  "batchSize": 1000,\n  "whereClause": "status = 1"\n}'
    },
    {
      className: 'PostgreSQLDataExtractor',
      classFullName: 'com.migration.extractor.PostgreSQLDataExtractor',
      description: 'PostgreSQL数据库数据提取器',
      parameters: [
        { name: 'host', type: 'String', required: true, description: '数据库主机地址' },
        { name: 'port', type: 'Integer', required: true, defaultValue: '5432', description: '数据库端口' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'username', type: 'String', required: true, description: '数据库用户名' },
        { name: 'password', type: 'String', required: true, description: '数据库密码' },
        { name: 'table', type: 'String', required: true, description: '要提取的表名' },
        { name: 'batchSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量提取大小' }
      ],
      exampleConfig: '{\n  "host": "localhost",\n  "port": 5432,\n  "database": "source_db",\n  "username": "postgres",\n  "password": "***",\n  "table": "orders",\n  "batchSize": 1000\n}'
    },
    {
      className: 'MongoDBDataExtractor',
      classFullName: 'com.migration.extractor.MongoDBDataExtractor',
      description: 'MongoDB数据提取器',
      parameters: [
        { name: 'connectionString', type: 'String', required: true, description: 'MongoDB连接字符串' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'collection', type: 'String', required: true, description: '集合名称' },
        { name: 'query', type: 'String', required: false, description: '查询条件（JSON格式）' },
        { name: 'batchSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量提取大小' }
      ],
      exampleConfig: '{\n  "connectionString": "mongodb://localhost:27017",\n  "database": "app_db",\n  "collection": "logs",\n  "query": "{\\"level\\": \\"ERROR\\"}",\n  "batchSize": 1000\n}'
    },
    {
      className: 'RESTAPIDataExtractor',
      classFullName: 'com.migration.extractor.RESTAPIDataExtractor',
      description: 'REST API数据提取器',
      parameters: [
        { name: 'url', type: 'String', required: true, description: 'API端点URL' },
        { name: 'method', type: 'String', required: false, defaultValue: 'GET', description: 'HTTP方法（GET/POST）' },
        { name: 'headers', type: 'String', required: false, description: '请求头（JSON格式）' },
        { name: 'params', type: 'String', required: false, description: '查询参数（JSON格式）' },
        { name: 'pageSize', type: 'Integer', required: false, defaultValue: '100', description: '每页数据量' },
        { name: 'maxPages', type: 'Integer', required: false, defaultValue: '100', description: '最大页数' }
      ],
      exampleConfig: '{\n  "url": "https://api.example.com/data",\n  "method": "GET",\n  "headers": "{\\"Authorization\\": \\"Bearer xxx\\"}",\n  "pageSize": 100,\n  "maxPages": 100\n}'
    },
    {
      className: 'FileDataExtractor',
      classFullName: 'com.migration.extractor.FileDataExtractor',
      description: '文件数据提取器（CSV/JSON/Excel）',
      parameters: [
        { name: 'filePath', type: 'String', required: true, description: '文件路径' },
        { name: 'fileType', type: 'String', required: true, description: '文件类型（csv/json/excel）' },
        { name: 'encoding', type: 'String', required: false, defaultValue: 'UTF-8', description: '文件编码' },
        { name: 'hasHeader', type: 'Boolean', required: false, defaultValue: 'true', description: '是否有表头行' },
        { name: 'sheetName', type: 'String', required: false, description: 'Excel工作表名称' }
      ],
      exampleConfig: '{\n  "filePath": "/data/source/users.csv",\n  "fileType": "csv",\n  "encoding": "UTF-8",\n  "hasHeader": true\n}'
    }
  ],

  'data_transform': [
    {
      className: 'FieldMappingTransformer',
      classFullName: 'com.migration.transformer.FieldMappingTransformer',
      description: '字段映射转换器',
      parameters: [
        { name: 'mappings', type: 'String', required: true, description: '字段映射配置（JSON格式）' },
        { name: 'skipNull', type: 'Boolean', required: false, defaultValue: 'false', description: '是否跳过空值字段' }
      ],
      exampleConfig: '{\n  "mappings": [\n    {"source": "user_id", "target": "id"},\n    {"source": "user_name", "target": "name"},\n    {"source": "created_at", "target": "createTime"}\n  ],\n  "skipNull": false\n}'
    },
    {
      className: 'DataTypeConverter',
      classFullName: 'com.migration.transformer.DataTypeConverter',
      description: '数据类型转换器',
      parameters: [
        { name: 'conversions', type: 'String', required: true, description: '类型转换配置（JSON格式）' }
      ],
      exampleConfig: '{\n  "conversions": [\n    {"field": "age", "from": "String", "to": "Integer"},\n    {"field": "price", "from": "String", "to": "BigDecimal"},\n    {"field": "birthday", "from": "String", "to": "Date", "format": "yyyy-MM-dd"}\n  ]\n}'
    },
    {
      className: 'DataFilter',
      classFullName: 'com.migration.transformer.DataFilter',
      description: '数据过滤器',
      parameters: [
        { name: 'filterExpr', type: 'String', required: true, description: '过滤表达式' },
        { name: 'filterType', type: 'String', required: false, defaultValue: 'include', description: '过滤类型（include/exclude）' }
      ],
      exampleConfig: '{\n  "filterExpr": "age >= 18 && status == 1",\n  "filterType": "include"\n}'
    },
    {
      className: 'DataAggregator',
      classFullName: 'com.migration.transformer.DataAggregator',
      description: '数据聚合器',
      parameters: [
        { name: 'groupBy', type: 'String', required: true, description: '分组字段' },
        { name: 'aggregations', type: 'String', required: true, description: '聚合操作配置' }
      ],
      exampleConfig: '{\n  "groupBy": "department",\n  "aggregations": [\n    {"field": "salary", "operation": "SUM", "alias": "totalSalary"},\n    {"field": "salary", "operation": "AVG", "alias": "avgSalary"},\n    {"field": "id", "operation": "COUNT", "alias": "employeeCount"}\n  ]\n}'
    },
    {
      className: 'DataEnricher',
      classFullName: 'com.migration.transformer.DataEnricher',
      description: '数据富化器',
      parameters: [
        { name: 'enrichments', type: 'String', required: true, description: '富化配置' }
      ],
      exampleConfig: '{\n  "enrichments": [\n    {"field": "userId", "lookup": "userCache", "addFields": ["userName", "email"]},\n    {"field": "countryCode", "lookup": "countryMap", "addField": "countryName"}\n  ]\n}'
    },
    {
      className: 'GroovyScriptTransformer',
      classFullName: 'com.migration.transformer.GroovyScriptTransformer',
      description: 'Groovy脚本转换器',
      parameters: [
        { name: 'script', type: 'String', required: true, description: 'Groovy脚本内容' },
        { name: 'imports', type: 'String', required: false, description: '导入的类' }
      ],
      exampleConfig: '{\n  "imports": ["com.example.utils.DateUtil"],\n  "script": "def result = [:];\\nresult.name = data.userName.toUpperCase();\\nresult.age = data.age + 1;\\nreturn result;"\n}'
    }
  ],

  'data_load': [
    {
      className: 'MySQLDataLoader',
      classFullName: 'com.migration.loader.MySQLDataLoader',
      description: 'MySQL数据库数据加载器',
      parameters: [
        { name: 'host', type: 'String', required: true, description: '数据库主机地址' },
        { name: 'port', type: 'Integer', required: true, defaultValue: '3306', description: '数据库端口' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'username', type: 'String', required: true, description: '数据库用户名' },
        { name: 'password', type: 'String', required: true, description: '数据库密码' },
        { name: 'table', type: 'String', required: true, description: '目标表名' },
        { name: 'batchSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量写入大小' },
        { name: 'writeMode', type: 'String', required: false, defaultValue: 'INSERT', description: '写入模式（INSERT/UPDATE/UPSERT）' },
        { name: 'onDuplicateKey', type: 'String', required: false, description: 'UPSERT模式下的更新字段' }
      ],
      exampleConfig: '{\n  "host": "localhost",\n  "port": 3306,\n  "database": "target_db",\n  "username": "root",\n  "password": "***",\n  "table": "users",\n  "batchSize": 1000,\n  "writeMode": "UPSERT",\n  "onDuplicateKey": "name,email,updated_at"\n}'
    },
    {
      className: 'PostgreSQLDataLoader',
      classFullName: 'com.migration.loader.PostgreSQLDataLoader',
      description: 'PostgreSQL数据库数据加载器',
      parameters: [
        { name: 'host', type: 'String', required: true, description: '数据库主机地址' },
        { name: 'port', type: 'Integer', required: true, defaultValue: '5432', description: '数据库端口' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'username', type: 'String', required: true, description: '数据库用户名' },
        { name: 'password', type: 'String', required: true, description: '数据库密码' },
        { name: 'table', type: 'String', required: true, description: '目标表名' },
        { name: 'batchSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量写入大小' },
        { name: 'writeMode', type: 'String', required: false, defaultValue: 'INSERT', description: '写入模式' }
      ],
      exampleConfig: '{\n  "host": "localhost",\n  "port": 5432,\n  "database": "target_db",\n  "username": "postgres",\n  "password": "***",\n  "table": "orders",\n  "batchSize": 1000\n}'
    },
    {
      className: 'MongoDBDataLoader',
      classFullName: 'com.migration.loader.MongoDBDataLoader',
      description: 'MongoDB数据加载器',
      parameters: [
        { name: 'connectionString', type: 'String', required: true, description: 'MongoDB连接字符串' },
        { name: 'database', type: 'String', required: true, description: '数据库名称' },
        { name: 'collection', type: 'String', required: true, description: '集合名称' },
        { name: 'writeMode', type: 'String', required: false, defaultValue: 'INSERT', description: '写入模式（INSERT/UPSERT）' },
        { name: 'upsertKey', type: 'String', required: false, description: 'UPSERT模式的唯一键' }
      ],
      exampleConfig: '{\n  "connectionString": "mongodb://localhost:27017",\n  "database": "target_db",\n  "collection": "logs",\n  "writeMode": "UPSERT",\n  "upsertKey": "logId"\n}'
    },
    {
      className: 'ElasticsearchDataLoader',
      classFullName: 'com.migration.loader.ElasticsearchDataLoader',
      description: 'Elasticsearch数据加载器',
      parameters: [
        { name: 'hosts', type: 'String', required: true, description: 'ES集群地址，多个用逗号分隔' },
        { name: 'index', type: 'String', required: true, description: '索引名称' },
        { name: 'username', type: 'String', required: false, description: '用户名' },
        { name: 'password', type: 'String', required: false, description: '密码' },
        { name: 'bulkSize', type: 'Integer', required: false, defaultValue: '1000', description: '批量写入大小' }
      ],
      exampleConfig: '{\n  "hosts": "localhost:9200",\n  "index": "user_logs",\n  "bulkSize": 1000\n}'
    },
    {
      className: 'KafkaDataLoader',
      classFullName: 'com.migration.loader.KafkaDataLoader',
      description: 'Kafka消息队列数据加载器',
      parameters: [
        { name: 'bootstrapServers', type: 'String', required: true, description: 'Kafka服务器地址' },
        { name: 'topic', type: 'String', required: true, description: 'Topic名称' },
        { name: 'keyField', type: 'String', required: false, description: '消息key字段' },
        { name: 'compressionType', type: 'String', required: false, defaultValue: 'none', description: '压缩类型' }
      ],
      exampleConfig: '{\n  "bootstrapServers": "localhost:9092",\n  "topic": "migration_data",\n  "keyField": "id",\n  "compressionType": "snappy"\n}'
    }
  ],

  'validation': [
    {
      className: 'SchemaValidator',
      classFullName: 'com.migration.validator.SchemaValidator',
      description: '数据模式校验器',
      parameters: [
        { name: 'schema', type: 'String', required: true, description: '数据模式定义（JSON格式）' },
        { name: 'strictMode', type: 'Boolean', required: false, defaultValue: 'false', description: '严格模式' }
      ],
      exampleConfig: '{\n  "schema": {\n    "name": {"type": "String", "required": true, "maxLength": 100},\n    "age": {"type": "Integer", "required": true, "min": 0, "max": 150},\n    "email": {"type": "String", "pattern": "^\\\\w+@\\\\w+\\\\.\\\\w+$"}\n  },\n  "strictMode": false\n}'
    },
    {
      className: 'ReferentialIntegrityValidator',
      classFullName: 'com.migration.validator.ReferentialIntegrityValidator',
      description: '引用完整性校验器',
      parameters: [
        { name: 'references', type: 'String', required: true, description: '引用关系定义' }
      ],
      exampleConfig: '{\n  "references": [\n    {"field": "departmentId", "lookupTable": "departments", "lookupField": "id"},\n    {"field": "managerId", "lookupTable": "employees", "lookupField": "id"}\n  ]\n}'
    },
    {
      className: 'DuplicateValidator',
      classFullName: 'com.migration.validator.DuplicateValidator',
      description: '重复数据校验器',
      parameters: [
        { name: 'uniqueFields', type: 'String', required: true, description: '唯一性字段列表' },
        { name: 'action', type: 'String', required: false, defaultValue: 'REJECT', description: '处理方式（REJECT/SKIP/KEEP_FIRST/KEEP_LAST）' }
      ],
      exampleConfig: '{\n  "uniqueFields": ["email", "phone"],\n  "action": "KEEP_FIRST"\n}'
    },
    {
      className: 'RangeValidator',
      classFullName: 'com.migration.validator.RangeValidator',
      description: '数据范围校验器',
      parameters: [
        { name: 'ranges', type: 'String', required: true, description: '范围校验配置' }
      ],
      exampleConfig: '{\n  "ranges": [\n    {"field": "age", "min": 0, "max": 150, "errorMsg": "年龄超出有效范围"},\n    {"field": "price", "min": 0, "errorMsg": "价格不能为负数"},\n    {"field": "createDate", "minDate": "2020-01-01", "maxDate": "2030-12-31"}\n  ]\n}'
    }
  ],

  'condition': [
    {
      className: 'ExpressionConditionExecutor',
      classFullName: 'com.migration.condition.ExpressionConditionExecutor',
      description: '表达式条件执行器',
      parameters: [
        { name: 'expression', type: 'String', required: true, description: '条件表达式' },
        { name: 'trueBranch', type: 'String', required: true, description: '条件为真时的分支' },
        { name: 'falseBranch', type: 'String', required: false, description: '条件为假时的分支' }
      ],
      exampleConfig: '{\n  "expression": "data.amount > 1000",\n  "trueBranch": "high_value_process",\n  "falseBranch": "normal_process"\n}'
    },
    {
      className: 'DataQualityConditionExecutor',
      classFullName: 'com.migration.condition.DataQualityConditionExecutor',
      description: '数据质量条件执行器',
      parameters: [
        { name: 'qualityRules', type: 'String', required: true, description: '质量规则配置' }
      ],
      exampleConfig: '{\n  "qualityRules": [\n    {"field": "errorRate", "threshold": 0.05, "operator": "LESS_THAN", "trueBranch": "pass", "falseBranch": "manual_review"},\n    {"field": "nullCount", "threshold": 0, "operator": "EQUALS", "trueBranch": "pass", "falseBranch": "handle_nulls"}\n  ]\n}'
    }
  ],

  'parallel': [
    {
      className: 'ParallelGatewayExecutor',
      classFullName: 'com.migration.gateway.ParallelGatewayExecutor',
      description: '并行网关执行器',
      parameters: [
        { name: 'branches', type: 'String', required: true, description: '并行分支列表' },
        { name: 'maxConcurrency', type: 'Integer', required: false, defaultValue: '5', description: '最大并发数' }
      ],
      exampleConfig: '{\n  "branches": ["branch_1", "branch_2", "branch_3"],\n  "maxConcurrency": 5\n}'
    }
  ],

  'merge': [
    {
      className: 'MergeGatewayExecutor',
      classFullName: 'com.migration.gateway.MergeGatewayExecutor',
      description: '合并网关执行器',
      parameters: [
        { name: 'waitAll', type: 'Boolean', required: false, defaultValue: 'true', description: '是否等待所有分支完成' },
        { name: 'timeout', type: 'Integer', required: false, description: '等待超时时间（毫秒）' }
      ],
      exampleConfig: '{\n  "waitAll": true,\n  "timeout": 60000\n}'
    }
  ],

  'parallel_group': [
    {
      className: 'ParallelGroupExecutor',
      classFullName: 'com.migration.engine.ParallelGroupExecutor',
      description: '并行组执行器',
      parameters: [
        { name: 'successStrategy', type: 'String', required: false, defaultValue: 'ALL_SUCCESS', description: '成功策略：ALL_SUCCESS=所有子流程成功才算成功, ANY_SUCCESS=任一子流程成功即算成功' },
        { name: 'maxConcurrency', type: 'Integer', required: false, defaultValue: '4', description: '同时执行的子流程最大数量' },
        { name: 'retryOnFailure', type: 'Boolean', required: false, defaultValue: 'false', description: '子流程节点失败时是否自动重试' },
        { name: 'maxRetryCount', type: 'Integer', required: false, defaultValue: '3', description: '每个节点失败后的最大重试次数' }
      ],
      exampleConfig: '{\n  "successStrategy": "ALL_SUCCESS",\n  "maxConcurrency": 4,\n  "retryOnFailure": false,\n  "maxRetryCount": 3\n}'
    }
  ],

  'notification': [
    {
      className: 'EmailNotifier',
      classFullName: 'com.migration.notifier.EmailNotifier',
      description: '邮件通知器',
      parameters: [
        { name: 'smtpHost', type: 'String', required: true, description: 'SMTP服务器地址' },
        { name: 'smtpPort', type: 'Integer', required: true, defaultValue: '587', description: 'SMTP端口' },
        { name: 'username', type: 'String', required: true, description: '用户名' },
        { name: 'password', type: 'String', required: true, description: '密码' },
        { name: 'from', type: 'String', required: true, description: '发件人地址' },
        { name: 'to', type: 'String', required: true, description: '收件人地址，多个用逗号分隔' },
        { name: 'subject', type: 'String', required: true, description: '邮件主题' },
        { name: 'template', type: 'String', required: false, description: '邮件模板' }
      ],
      exampleConfig: '{\n  "smtpHost": "smtp.gmail.com",\n  "smtpPort": 587,\n  "username": "noreply@example.com",\n  "password": "***",\n  "from": "migration@example.com",\n  "to": "admin@example.com,support@example.com",\n  "subject": "数据迁移任务完成通知"\n}'
    },
    {
      className: 'WebhookNotifier',
      classFullName: 'com.migration.notifier.WebhookNotifier',
      description: 'Webhook通知器',
      parameters: [
        { name: 'url', type: 'String', required: true, description: 'Webhook URL' },
        { name: 'method', type: 'String', required: false, defaultValue: 'POST', description: 'HTTP方法' },
        { name: 'headers', type: 'String', required: false, description: '请求头' },
        { name: 'bodyTemplate', type: 'String', required: false, description: '请求体模板' }
      ],
      exampleConfig: '{\n  "url": "https://hooks.slack.com/services/xxx",\n  "method": "POST",\n  "headers": "{\\"Content-Type\\": \\"application/json\\"}",\n  "bodyTemplate": "{\\"text\\": \\"Task ${taskName} completed with ${status}\\"}"\n}'
    },
    {
      className: 'DingTalkNotifier',
      classFullName: 'com.migration.notifier.DingTalkNotifier',
      description: '钉钉通知器',
      parameters: [
        { name: 'webhook', type: 'String', required: true, description: '钉钉Webhook地址' },
        { name: 'secret', type: 'String', required: false, description: '加签密钥' },
        { name: 'atMobiles', type: 'String', required: false, description: '被@人的手机号' }
      ],
      exampleConfig: '{\n  "webhook": "https://oapi.dingtalk.com/robot/send?access_token=xxx",\n  "secret": "SECxxx",\n  "atMobiles": ["13800138000"]\n}'
    }
  ],

  'script': [
    {
      className: 'GroovyScriptExecutor',
      classFullName: 'com.migration.script.GroovyScriptExecutor',
      description: 'Groovy脚本执行器',
      parameters: [
        { name: 'script', type: 'String', required: true, description: '脚本内容' },
        { name: 'timeout', type: 'Integer', required: false, defaultValue: '60000', description: '执行超时时间' }
      ],
      exampleConfig: '{\n  "script": "println(\\"Processing record: \\" + data); return data;",\n  "timeout": 60000\n}'
    },
    {
      className: 'JavaScriptExecutor',
      classFullName: 'com.migration.script.JavaScriptExecutor',
      description: 'JavaScript脚本执行器（Nashorn）',
      parameters: [
        { name: 'script', type: 'String', required: true, description: 'JavaScript脚本' },
        { name: 'timeout', type: 'Integer', required: false, defaultValue: '60000', description: '执行超时时间' }
      ],
      exampleConfig: '{\n  "script": "var result = {}; result.processed = data.value * 2; result;",\n  "timeout": 60000\n}'
    },
    {
      className: 'PythonScriptExecutor',
      classFullName: 'com.migration.script.PythonScriptExecutor',
      description: 'Python脚本执行器',
      parameters: [
        { name: 'scriptPath', type: 'String', required: true, description: 'Python脚本路径' },
        { name: 'pythonPath', type: 'String', required: false, description: 'Python解释器路径' }
      ],
      exampleConfig: '{\n  "scriptPath": "/scripts/transform.py",\n  "pythonPath": "/usr/bin/python3"\n}'
    }
  ],

  'delay': [
    {
      className: 'FixedDelayExecutor',
      classFullName: 'com.migration.delay.FixedDelayExecutor',
      description: '固定延时执行器',
      parameters: [
        { name: 'delay', type: 'Long', required: true, description: '延时时间' },
        { name: 'unit', type: 'String', required: false, defaultValue: 'MILLISECONDS', description: '时间单位（MILLISECONDS/SECONDS/MINUTES/HOURS）' }
      ],
      exampleConfig: '{\n  "delay": 5000,\n  "unit": "MILLISECONDS"\n}'
    },
    {
      className: 'DynamicDelayExecutor',
      classFullName: 'com.migration.delay.DynamicDelayExecutor',
      description: '动态延时执行器',
      parameters: [
        { name: 'delayExpression', type: 'String', required: true, description: '延时时间表达式' }
      ],
      exampleConfig: '{\n  "delayExpression": "data.retryCount * 1000"\n}'
    }
  ]
}

export function getNodeTypeConfig(code: NodeTypeCode): NodeTypeConfig {
  return NODE_TYPE_LIST.find(n => n.code === code) || NODE_TYPE_LIST[0]
}

export function getImplementationClasses(nodeType: NodeTypeCode): ImplementationClass[] {
  return IMPLEMENTATION_CLASSES[nodeType] || []
}

export function getImplementationClass(nodeType: NodeTypeCode, className: string): ImplementationClass | undefined {
  const classes = IMPLEMENTATION_CLASSES[nodeType]
  return classes?.find(c => c.className === className)
}

export function getTaskStatusLabel(code: TaskStatusCode): string {
  const map: Record<TaskStatusCode, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    PAUSED: '已暂停',
    SUCCESS: '成功',
    FAILED: '失败',
    PARTIAL_SUCCESS: '部分成功',
    CANCELLED: '已取消',
    SKIPPED: '已跳过',
  }
  return map[code] || code
}

export function getTaskStatusColor(code: TaskStatusCode): string {
  const map: Record<TaskStatusCode, string> = {
    PENDING: '#909399',
    RUNNING: '#409EFF',
    PAUSED: '#E6A23C',
    SUCCESS: '#67C23A',
    FAILED: '#F56C6C',
    PARTIAL_SUCCESS: '#E6A23C',
    CANCELLED: '#909399',
    SKIPPED: '#909399',
  }
  return map[code] || '#909399'
}
