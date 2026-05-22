# Migration Tool - 可视化流程配置数据迁移工具

一个基于 Spring Boot + Vue 3 的可视化数据迁移平台，通过拖拽式流程设计器编排数据提取、转换、加载（ETL）流程，支持多种数据源、条件分支、并行执行、断点续传、AI 智能生成流程等企业级特性。

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [功能特性](#功能特性)
- [后端架构](#后端架构)
  - [核心模块](#核心模块)
  - [流程引擎](#流程引擎)
  - [节点类型](#节点类型)
  - [数据模型](#数据模型)
  - [REST API](#rest-api)
- [前端架构](#前端架构)
  - [页面路由](#页面路由)
  - [状态管理](#状态管理)
  - [画布交互](#画布交互)
  - [组件清单](#组件清单)
- [配置说明](#配置说明)
- [AI 智能功能](#ai-智能功能)
- [数据库初始化](#数据库初始化)

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | JDK 17+ |
| Spring Boot | 3.2.5 | 核心框架 |
| Spring Data JPA | - | ORM 持久层（Hibernate） |
| Spring WebSocket | - | 实时进度推送 |
| Spring Boot Mail | - | 邮件通知 |
| MySQL | 8.x | 主数据库 |
| PostgreSQL | - | 数据源支持 |
| MongoDB Driver | 4.11.1 | 数据源支持 |
| Hutool | 5.8.26 | 工具库 |
| Lombok | - | 代码简化 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.27 | 核心框架 |
| TypeScript | 5.4.5 | 类型系统 |
| Vite | 5.2.11 | 构建工具 |
| Element Plus | 2.7.2 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.2 | 路由管理 |
| Axios | 1.6.8 | HTTP 客户端 |

---

## 项目结构

```
migration-tool/
├── backend/                          # 后端 Spring Boot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/migration/
│       │   ├── MigrationToolApplication.java
│       │   ├── config/               # 配置类
│       │   │   ├── AppConfig.java
│       │   │   └── WebSocketConfig.java
│       │   ├── controller/           # REST 控制器
│       │   │   ├── DataSourceConfigController.java
│       │   │   ├── FlowController.java
│       │   │   ├── LogController.java
│       │   │   └── TaskController.java
│       │   ├── ai/                   # AI 智能模块
│       │   │   ├── AiConfig.java          # AI 配置
│       │   │   ├── AiService.java         # AI 大模型调用服务
│       │   │   ├── AiMessage.java         # 对话消息模型
│       │   │   ├── AiController.java      # AI API 端点
│       │   │   ├── AiFlowGenerationService.java  # AI 流程生成服务
│       │   │   ├── SkillService.java      # Skills 学习与推荐服务
│       │   │   ├── SkillTemplate.java     # Skill 模板实体
│       │   │   ├── UserPreference.java    # 用户偏好实体
│       │   │   ├── SkillTemplateRepository.java
│       │   │   └── UserPreferenceRepository.java
│       │   ├── engine/               # 流程引擎核心
│       │   │   ├── FlowEngine.java
│       │   │   ├── ImplementationClassRegistry.java
│       │   │   ├── NodeExecutor.java
│       │   │   └── NodeHandlerRegistry.java
│       │   ├── evaluator/            # 条件评估器
│       │   │   ├── ConditionEvaluator.java
│       │   │   ├── DataQualityConditionEvaluator.java
│       │   │   ├── ExpressionConditionEvaluator.java
│       │   │   └── RecordCountConditionEvaluator.java
│       │   ├── extractor/            # 数据提取器
│       │   │   ├── DataExtractor.java
│       │   │   ├── FileDataExtractor.java
│       │   │   ├── MongoDBDataExtractor.java
│       │   │   ├── MySQLDataExtractor.java
│       │   │   ├── PostgreSQLDataExtractor.java
│       │   │   └── RESTAPIDataExtractor.java
│       │   ├── handler/              # 节点处理器
│       │   │   ├── ConditionHandler.java
│       │   │   ├── DataExtractHandler.java
│       │   │   ├── DataLoadHandler.java
│       │   │   ├── DataTransformHandler.java
│       │   │   ├── DelayHandler.java
│       │   │   ├── EndNodeHandler.java
│       │   │   ├── MergeHandler.java
│       │   │   ├── NotificationHandler.java
│       │   │   ├── ParallelHandler.java
│       │   │   ├── ScriptHandler.java
│       │   │   ├── StartNodeHandler.java
│       │   │   └── ValidationHandler.java
│       │   ├── loader/               # 数据加载器
│       │   │   ├── DataLoader.java
│       │   │   ├── FileDataLoader.java
│       │   │   ├── MongoDBDataLoader.java
│       │   │   ├── MySQLDataLoader.java
│       │   │   ├── PostgreSQLDataLoader.java
│       │   │   └── RESTAPIDataLoader.java
│       │   ├── model/
│       │   │   ├── dto/              # 数据传输对象
│       │   │   │   ├── FlowDefinitionDTO.java
│       │   │   │   ├── TaskExecutionRequest.java
│       │   │   │   └── TaskProgressMessage.java
│       │   │   ├── entity/           # JPA 实体
│       │   │   │   ├── DataSourceConfig.java
│       │   │   │   ├── FlowDefinition.java
│       │   │   │   ├── FlowEdge.java
│       │   │   │   ├── FlowNode.java
│       │   │   │   ├── MigrationTask.java
│       │   │   │   ├── NodeExecution.java
│       │   │   │   └── TaskLog.java
│       │   │   └── enums/            # 枚举类型
│       │   │       ├── DatabaseType.java
│       │   │       ├── LogLevel.java
│       │   │       ├── NodeType.java
│       │   │       └── TaskStatus.java
│       │   ├── notifier/             # 通知器
│       │   │   ├── DingTalkNotifier.java
│       │   │   ├── EmailNotifier.java
│       │   │   └── Notifier.java
│       │   ├── repository/           # JPA 仓库
│       │   │   ├── DataSourceConfigRepository.java
│       │   │   ├── FlowDefinitionRepository.java
│       │   │   ├── FlowEdgeRepository.java
│       │   │   ├── FlowNodeRepository.java
│       │   │   ├── LoadFailureRecordRepository.java
│       │   │   ├── MigrationTaskRepository.java
│       │   │   ├── NodeExecutionRepository.java
│       │   │   └── TaskLogRepository.java
│       │   ├── service/              # 业务服务
│       │   │   ├── DataSourceConfigService.java
│       │   │   ├── FlowService.java
│       │   │   ├── LogService.java
│       │   │   ├── TaskService.java
│       │   │   └── WebSocketNotificationService.java
│       │   ├── transformer/          # 数据转换器
│       │   │   ├── DataTransformer.java
│       │   │   ├── FieldMappingTransformer.java
│       │   │   └── GroovyScriptTransformer.java
│       │   └── websocket/            # WebSocket 端点
│       │       └── TaskProgressWebSocket.java
│       └── resources/
│           ├── application.yml
│           ├── schema.sql
│           └── data.sql
│
└── frontend/                         # 前端 Vue 3 项目
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    └── src/
        ├── App.vue
        ├── main.ts
        ├── api/                      # API 请求层
        │   ├── http.ts               # Axios 实例与拦截器
        │   ├── flow.ts               # 流程定义 API
        │   ├── task.ts               # 任务 API
        │   ├── datasource.ts         # 数据源 API
        │   ├── log.ts                # 日志 API
        │   └── ai.ts                 # AI 智能功能 API
        ├── components/               # 公共组件
        │   ├── FlowCanvas.vue        # SVG 流程画布（核心）
        │   ├── CanvasToolbar.vue     # 画布工具栏
        │   ├── NodePanel.vue         # 节点面板
        │   ├── AiAssistant.vue       # AI 助手面板
        │   ├── LogPanel.vue          # 日志面板
        │   ├── ErrorAnalysis.vue     # 错误分析
        │   ├── ConditionHelp.vue     # 条件帮助
        │   ├── FlowHelpTip.vue       # 流程帮助提示
        │   └── TaskAnimation.vue     # 任务动画
        ├── router/                   # 路由配置
        │   └── index.ts
        ├── stores/                   # Pinia 状态管理
        │   ├── flow.ts               # 流程 Store
        │   ├── task.ts               # 任务 Store
        │   └── datasource.ts         # 数据源 Store
        ├── types/                    # TypeScript 类型定义
        │   └── index.ts
        ├── utils/                    # 工具函数
        │   └── websocket.ts          # WebSocket 客户端
        └── views/                    # 页面视图
            ├── FlowList.vue          # 流程列表
            ├── FlowDesigner.vue      # 流程设计器
            ├── TaskMonitor.vue       # 任务监控
            ├── TaskDetail.vue        # 任务详情
            ├── DataSourceList.vue    # 数据源管理
            └── SkillManager.vue     # Skills 模板管理
```

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Maven 3.8+

### 1. 初始化数据库

创建 MySQL 数据库并执行初始化脚本：

```sql
CREATE DATABASE IF NOT EXISTS migration_tool
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

项目配置了 `ddl-auto: update`，启动后端时 Hibernate 会自动创建/更新表结构。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动在 `http://localhost:18080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认启动在 `http://localhost:5173`

### 4. 访问应用

浏览器打开 `http://localhost:5173`

---

## 功能特性

### 流程设计

- **可视化拖拽**：SVG 画布拖拽设计流程，支持节点拖拽、连线、缩放、平移
- **13 种节点类型**：开始/结束/数据提取/数据转换/数据加载/数据校验/条件判断/并行网关/合并网关/并行组/通知/脚本/延时
- **可插拔实现类**：每种节点类型支持多种实现类，后端动态注册参数元数据，前端自动渲染配置表单
- **条件分支**：连线支持条件表达式，支持 `nodeId.field` 语法引用任意节点执行结果
- **并行组**：Shift+左键框选多个节点组合为并行组，支持串行子流程链路自动识别、成功策略、并发线程数、失败重试配置
- **连线样式**：支持直线/折线/曲线三种连线样式

### 任务执行

- **实时进度**：WebSocket 推送节点级执行进度，前端实时更新节点状态和连线颜色
- **断点续传**：任务失败后可从断点节点重新执行，无需从头开始
- **执行状态可视化**：成功节点/连线变绿色，失败变红色，条件未走到变灰色
- **动画效果**：执行中的连线有粒子动画，直观展示数据流向

### 数据源管理

- **多数据库支持**：MySQL、PostgreSQL、MongoDB
- **SSH 隧道**：支持通过 SSH 隧道连接数据库
- **连接测试**：保存前可测试数据库连接是否可用
- **表/列浏览**：自动获取数据库表列表和列信息

### 通知

- **钉钉机器人**：支持加签模式，@指定人
- **邮件通知**：SMTP 邮件发送
- **Webhook**：自定义 HTTP 回调

### AI 智能功能

- **AI 对话助手**：在流程设计器中内置 AI 助手面板，自然语言描述需求即可获得建议
- **智能流程生成**：描述数据迁移需求，AI 自动生成完整的流程定义（节点+连线），一键应用到画布
- **需求分析**：AI 分析迁移需求，给出数据源选择、清洗策略、质量保障等专业建议
- **Skills 自动学习**：每次流程成功执行后自动提取模式，生成可复用的 Skill 模板
- **操作习惯学习**：自动记录常用数据源、表映射关系等偏好，AI 生成流程时优先参考
- **Skills 模板管理**：独立的 Skills 管理页面，支持查看/应用/编辑/删除模板
- **自定义模型**：支持通过 HTTP 调用任意兼容 OpenAI Chat Completions 格式的大模型（DeepSeek、Qwen、Ollama 等）

---

## 后端架构

### 核心模块

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller 层                           │
│  FlowController / TaskController / DataSourceController /    │
│  LogController / AiController                               │
├─────────────────────────────────────────────────────────────┤
│                      Service 层                              │
│  FlowService / TaskService / DataSourceConfigService /       │
│  LogService / WebSocketNotificationService                  │
├─────────────────────────────────────────────────────────────┤
│                      AI 智能层                                │
│  AiService ──→ AiFlowGenerationService ──→ SkillService     │
│       │                │                    │                │
│       │                ├── 流程生成           ├── Skill 模板  │
│       │                └── 需求分析           └── 用户偏好    │
│       └── AiConfig (自定义模型HTTP调用)                       │
├─────────────────────────────────────────────────────────────┤
│                      Engine 层                               │
│  FlowEngine ──→ NodeHandlerRegistry ──→ NodeExecutor        │
│       │                │                                     │
│       │                ├── StartNodeHandler                  │
│       │                ├── EndNodeHandler                    │
│       │                ├── DataExtractHandler ──→ Extractor  │
│       │                ├── DataTransformHandler ──→ Transformer│
│       │                ├── DataLoadHandler ──→ Loader        │
│       │                ├── ConditionHandler ──→ Evaluator    │
│       │                ├── ValidationHandler                 │
│       │                ├── ParallelHandler                   │
│       │                ├── MergeHandler                      │
│       │                ├── NotificationHandler ──→ Notifier  │
│       │                ├── ScriptHandler                     │
│       │                └── DelayHandler                      │
│       │                                                     │
│       └── ImplementationClassRegistry（参数元数据注册）        │
├─────────────────────────────────────────────────────────────┤
│                      Repository 层                           │
│  Spring Data JPA Repositories                               │
├─────────────────────────────────────────────────────────────┤
│                      持久层                                   │
│  MySQL (JPA) / MongoDB (Driver) / PostgreSQL (JDBC)         │
└─────────────────────────────────────────────────────────────┘
```

### 流程引擎

`FlowEngine` 是核心执行引擎，负责流程的完整生命周期：

1. **流程解析**：从数据库加载 FlowDefinition，构建节点图和邻接表
2. **拓扑排序**：从 START 节点开始，沿边逐步执行
3. **节点执行**：通过 `NodeHandlerRegistry` 分发到对应的 Handler
4. **条件分支**：`ConditionHandler` 调用 `ConditionEvaluator` 评估条件，决定走哪条边
5. **并行组执行**：`executeParallelGroup()` 识别串行子流程链路，使用 `CompletableFuture` 线程池并发执行
6. **断点续传**：失败时记录 `restartFromNodeId`，重启时恢复已执行节点的上下文
7. **进度推送**：每个节点状态变化通过 WebSocket 实时推送到前端

**执行流程图**：

```
START → DataExtract → DataTransform → Validation → Condition
                                                        ├── true  → DataLoad → END
                                                        └── false → Notification → END
```

**并行组执行模型**：

```
并行组 (PARALLEL_GROUP)
├── 链路1: Extract1 → Validate1 → Load1    ──┐
├── 链路2: Extract2 → Validate2 → Load2    ──┤── CompletableFuture 并发执行
└── 链路3: Extract3 → Transform3 → Load3    ──┘
```

### 节点类型

| 类型代码 | 名称 | 分类 | 说明 |
|----------|------|------|------|
| `start` | 开始节点 | 控制 | 流程入口，自动触发后续节点 |
| `end` | 结束节点 | 控制 | 流程出口，标记任务完成 |
| `data_extract` | 数据提取 | 数据 | 从源系统提取数据，支持 MySQL/PostgreSQL/MongoDB/REST API/文件 |
| `data_transform` | 数据转换 | 数据 | 字段映射/类型转换/过滤/聚合/脚本转换 |
| `data_load` | 数据加载 | 数据 | 写入目标系统，支持 MySQL/PostgreSQL/MongoDB/ES/Kafka |
| `validation` | 数据校验 | 数据 | 模式校验/引用完整性/重复检测/范围校验 |
| `condition` | 条件判断 | 控制 | 根据条件表达式走不同分支 |
| `parallel` | 并行网关 | 控制 | 并行执行多个分支 |
| `merge` | 合并网关 | 控制 | 合并并行分支 |
| `parallel_group` | 并行组 | 控制 | 框选组合多个节点为并行执行组 |
| `notification` | 通知 | 辅助 | 钉钉/邮件/Webhook 通知 |
| `script` | 自定义脚本 | 辅助 | Groovy/JavaScript/Python 脚本 |
| `delay` | 延时等待 | 辅助 | 固定/动态延时 |

### 数据模型

#### 实体关系图

```
FlowDefinition (1) ──→ (N) FlowNode
FlowDefinition (1) ──→ (N) FlowEdge
FlowDefinition (1) ──→ (N) MigrationTask
MigrationTask (1) ──→ (N) NodeExecution
MigrationTask (1) ──→ (N) TaskLog
DataSourceConfig (独立)
```

#### 数据库表结构

**flow_definition** - 流程定义

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(255) | 流程名称 |
| description | VARCHAR(255) | 描述 |
| version | INT | 版本号 |
| enabled | BIT | 是否启用 |
| created_by | VARCHAR(255) | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**flow_node** - 流程节点

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| node_id | VARCHAR(255) | 节点ID（流程内唯一） |
| flow_definition_id | BIGINT FK | 所属流程 |
| node_type | VARCHAR(255) | 节点类型枚举 |
| name | VARCHAR(255) | 节点名称 |
| description | VARCHAR(255) | 描述 |
| config | TEXT | 配置（JSON） |
| position_x | DOUBLE | 画布X坐标 |
| position_y | DOUBLE | 画布Y坐标 |
| sort_order | INT | 排序号 |
| implementation_class | VARCHAR(255) | 业务实现类全限定名 |
| parent_group_id | VARCHAR(255) | 所属并行组节点ID |

**flow_edge** - 流程连线

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| flow_definition_id | BIGINT FK | 所属流程 |
| edge_id | VARCHAR(255) | 边ID |
| source_node_id | VARCHAR(255) | 源节点ID |
| target_node_id | VARCHAR(255) | 目标节点ID |
| condition | VARCHAR(1024) | 条件表达式 |
| label | VARCHAR(255) | 标签 |
| edge_style | VARCHAR(255) | 连线样式 |

**migration_task** - 迁移任务

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(255) | 任务名称 |
| flow_definition_id | BIGINT FK | 关联流程 |
| status | VARCHAR(255) | 任务状态枚举 |
| current_node_id | VARCHAR(255) | 当前执行节点 |
| total_nodes | INT | 总节点数 |
| completed_nodes | INT | 已完成节点数 |
| extracted_records | BIGINT | 提取记录数 |
| loaded_records | BIGINT | 加载记录数 |
| loaded_success_records | BIGINT | 加载成功记录数 |
| loaded_failed_records | BIGINT | 加载失败记录数 |
| error_message | TEXT | 错误信息 |
| params | TEXT | 任务参数（JSON） |
| restart_from_node_id | VARCHAR(255) | 断点续传节点ID |
| created_by | VARCHAR(255) | 创建人 |
| created_at | DATETIME | 创建时间 |
| started_at | DATETIME | 开始时间 |
| finished_at | DATETIME | 结束时间 |

**node_execution** - 节点执行记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 关联任务 |
| node_id | VARCHAR(255) | 节点ID |
| node_name | VARCHAR(255) | 节点名称 |
| node_type | VARCHAR(255) | 节点类型 |
| incoming_edge_id | VARCHAR(255) | 进入该节点的边ID |
| status | VARCHAR(255) | 执行状态 |
| retry_count | INT | 重试次数 |
| max_retry | INT | 最大重试次数 |
| error_message | TEXT | 错误信息 |
| input_summary | TEXT | 输入摘要 |
| output_summary | TEXT | 输出摘要 |
| started_at | DATETIME | 开始时间 |
| finished_at | DATETIME | 结束时间 |
| duration | BIGINT | 耗时(ms) |

**task_log** - 任务日志

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 关联任务 |
| node_id | VARCHAR(255) | 节点ID |
| node_name | VARCHAR(255) | 节点名称 |
| level | VARCHAR(255) | 日志级别 |
| message | TEXT | 日志消息 |
| stack_trace | TEXT | 错误堆栈 |
| record_count | BIGINT | 处理记录数 |
| duration | BIGINT | 耗时(ms) |
| log_time | DATETIME | 日志时间 |

**data_source_config** - 数据源配置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(255) | 名称 |
| type | VARCHAR(255) | 数据库类型（MYSQL/POSTGRESQL/MONGODB） |
| host | VARCHAR(255) | 主机地址 |
| port | INT | 端口 |
| database | VARCHAR(255) | 数据库名 |
| username | VARCHAR(255) | 用户名 |
| password | VARCHAR(255) | 密码 |
| properties | TEXT | 扩展属性（JSON） |
| ssh_enabled | BIT | 是否启用SSH隧道 |
| ssh_host | VARCHAR(255) | SSH主机 |
| ssh_port | INT | SSH端口 |
| ssh_username | VARCHAR(255) | SSH用户名 |
| ssh_password | VARCHAR(255) | SSH密码 |
| ssh_auth_key | TEXT | SSH密钥 |
| enabled | BIT | 是否启用 |
| description | VARCHAR(255) | 描述 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**skill_template** - AI Skill 模板

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(255) | 模板名称 |
| description | VARCHAR(1024) | 描述 |
| category | VARCHAR(255) | 分类（simple_migration/transform_migration/parallel_migration等） |
| flow_definition_json | TEXT | 流程定义JSON |
| source_type | VARCHAR(255) | 源数据类型 |
| target_type | VARCHAR(255) | 目标数据类型 |
| tags | TEXT | 标签（逗号分隔） |
| usage_count | INT | 使用次数 |
| success_rate | DOUBLE | 成功率 |
| source_flow_id | BIGINT | 来源流程ID |
| auto_generated | BIT | 是否自动学习生成 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**user_preference** - 用户偏好

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| preference_type | VARCHAR(255) | 偏好类型（datasource/implementation/table等） |
| preference_key | VARCHAR(255) | 偏好键 |
| preference_value | TEXT | 偏好值 |
| frequency | INT | 使用频率 |
| last_used_at | DATETIME | 最后使用时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### REST API

#### 流程管理 `/api/flows`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/flows` | 获取流程列表 |
| GET | `/flows/{id}` | 获取流程详情（含节点和边） |
| POST | `/flows` | 创建流程 |
| PUT | `/flows/{id}` | 更新流程 |
| DELETE | `/flows/{id}` | 删除流程 |
| GET | `/flows/implementations` | 获取所有实现类及参数元数据 |
| GET | `/flows/implementations/{nodeType}` | 按节点类型获取实现类 |

#### 任务管理 `/api/tasks`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tasks` | 获取任务列表（可选 `flowDefinitionId` 过滤） |
| GET | `/tasks/{id}` | 获取任务详情 |
| POST | `/tasks` | 创建并启动任务 |
| POST | `/tasks/{id}/restart` | 重启任务（可选 `restartFromNodeId` 断点续传） |
| POST | `/tasks/{id}/pause` | 暂停任务 |
| POST | `/tasks/{id}/cancel` | 取消任务 |
| GET | `/tasks/running` | 获取运行中任务 |
| DELETE | `/tasks/{id}` | 删除任务 |
| DELETE | `/tasks/completed` | 清除已完成任务 |
| DELETE | `/tasks/all` | 清除所有任务 |

#### 数据源管理 `/api/datasources`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/datasources` | 获取数据源列表 |
| GET | `/datasources/enabled` | 获取已启用数据源 |
| GET | `/datasources/{id}` | 获取数据源详情 |
| POST | `/datasources` | 创建数据源 |
| PUT | `/datasources/{id}` | 更新数据源 |
| DELETE | `/datasources/{id}` | 删除数据源 |
| POST | `/datasources/{id}/test` | 测试已保存数据源连接 |
| POST | `/datasources/test` | 直接测试连接（无需保存） |
| GET | `/datasources/{id}/tables` | 获取表列表 |
| GET | `/datasources/{id}/tables/{tableName}/columns` | 获取列信息 |

#### 日志查询 `/api/logs`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/logs/task/{taskId}` | 获取任务日志 |
| GET | `/logs/task/{taskId}/node/{nodeId}` | 获取节点日志 |
| GET | `/logs/task/{taskId}/executions` | 获取节点执行记录 |
| GET | `/logs/task/{taskId}/error-analysis` | 获取错误分析 |

#### AI 智能功能 `/api/ai`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/chat` | AI 对话（支持历史消息上下文） |
| POST | `/ai/generate-flow` | AI 生成流程定义（可选 autoSave 直接保存） |
| POST | `/ai/analyze` | AI 需求分析 |
| GET | `/ai/skills` | 获取 Skill 模板列表（可选 category 筛选） |
| POST | `/ai/skills` | 创建 Skill 模板 |
| PUT | `/ai/skills/{id}` | 更新 Skill 模板 |
| DELETE | `/ai/skills/{id}` | 删除 Skill 模板 |
| POST | `/ai/skills/{id}/use` | 增加 Skill 使用次数 |
| GET | `/ai/preferences` | 获取用户偏好列表（可选 type 筛选） |
| POST | `/ai/learn/{flowDefinitionId}` | 从成功流程学习生成 Skill |
| GET | `/ai/config` | 获取 AI 配置状态 |

#### WebSocket `/ws/task-progress`

消息类型：

| type | 说明 | 关键字段 |
|------|------|----------|
| `progress` | 整体进度 | progress, extractedRecords, loadedRecords, loadedSuccessRecords, loadedFailedRecords |
| `node_start` | 节点开始执行 | nodeId, nodeName, nodeStatus |
| `node_complete` | 节点执行完成 | nodeId, nodeName, nodeStatus |
| `node_error` | 节点执行失败 | nodeId, nodeName, error |
| `task_complete` | 任务完成 | taskStatus |
| `task_failed` | 任务失败 | taskStatus, error |
| `log` | 日志消息 | message |

---

## 前端架构

### 页面路由

| 路径 | 组件 | 说明 |
|------|------|------|
| `/` | - | 重定向到 `/flows` |
| `/flows` | FlowList | 流程列表，支持新建/编辑/删除/立即执行 |
| `/flows/designer/:id?` | FlowDesigner | 流程设计器，可视化拖拽设计流程，内置 AI 助手 |
| `/tasks` | TaskMonitor | 任务监控列表，查看所有任务状态 |
| `/tasks/:id` | TaskDetail | 任务详情，实时查看执行进度和日志 |
| `/datasources` | DataSourceList | 数据源管理，CRUD + 连接测试 |
| `/skills` | SkillManager | Skills 模板管理，查看/应用/编辑/删除 |

### 状态管理

使用 Pinia Composition API 风格：

**flow.ts** - 流程状态管理

- `flows` - 流程列表
- `currentFlow` - 当前编辑的流程（含 nodes/edges）
- `implementations` - 实现类映射（按节点类型分组）
- 核心操作：`addNode` / `removeNode` / `updateNodePosition` / `addEdge` / `removeEdge` / `saveFlow`

**task.ts** - 任务状态管理

- `tasks` - 任务列表
- `currentTask` - 当前查看的任务
- `nodeStatusMap` - 节点执行状态映射（合并 API 数据和 WebSocket 实时数据）
- `activeNodeId` - 当前执行中的节点ID
- WebSocket 集成：`initWebSocket()` / `handleProgressMessage()`

**datasource.ts** - 数据源状态管理

- `dataSources` - 数据源列表
- 核心操作：CRUD / `testConnection` / `getTables` / `getColumns`

### 画布交互

FlowCanvas 是核心 SVG 画布组件，支持以下交互：

| 操作 | 行为 |
|------|------|
| 左键拖拽空白区域 | 平移画布 |
| Shift + 左键拖拽 | 框选节点（用于组合并行组） |
| 左键拖拽节点 | 移动节点位置 |
| 左键点击节点 | 选中节点（高亮） |
| 双击节点 | 弹出属性配置面板 |
| Ctrl/Cmd + 点击节点 | 多选/取消选中 |
| 鼠标滚轮 | 缩放画布 |
| 空格 + 拖拽 | 平移画布（备用） |
| 从端口拖拽到另一端口 | 创建连线 |
| 右键节点 | 删除节点 |
| 右键连线 | 删除连线 |

画布使用 SVG `<g transform="translate(panX, panY) scale(zoomLevel)">` 实现缩放平移，通过 `screenToSvg()` 函数进行坐标转换确保拖拽和框选在任意缩放级别下准确。

### 组件清单

| 组件 | 说明 |
|------|------|
| FlowCanvas.vue | SVG 流程画布，节点渲染/连线/框选/拖拽/缩放/平移 |
| CanvasToolbar.vue | 画布工具栏，缩放/适应/重置/连线样式/动画开关 |
| NodePanel.vue | 左侧节点面板，拖拽添加节点 |
| AiAssistant.vue | AI 助手面板，对话/生成流程/需求分析/Skills管理 |
| LogPanel.vue | 日志面板，展示任务执行日志 |
| ErrorAnalysis.vue | 错误分析面板，统计错误类型和建议 |
| ConditionHelp.vue | 条件表达式帮助 |
| FlowHelpTip.vue | 流程设计帮助提示 |
| TaskAnimation.vue | 任务动画效果 |

---

## 配置说明

### 后端配置 `application.yml`

```yaml
server:
  port: 18080                    # 服务端口

spring:
  datasource:
    url: jdbc:mysql://<host>:<port>/migration_tool?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: <username>
    password: <password>
  jpa:
    hibernate:
      ddl-auto: update           # 自动更新表结构
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

migration:
  thread-pool:
    core-size: 4                 # 线程池核心线程数
    max-size: 16                 # 线程池最大线程数
    queue-capacity: 100          # 队列容量
  ws-push-interval: 500          # WebSocket 推送间隔(ms)
  dingtalk:
    webhook-url: <webhook_url>   # 钉钉机器人 Webhook
    secret: <secret>             # 钉钉加签密钥
  ai:
    enabled: false               # 是否启用 AI 功能
    api-url: http://localhost:11434/v1/chat/completions  # AI 模型 API 地址（兼容 OpenAI 格式）
    api-key: ""                  # API 密钥（可选）
    model: deepseek-chat         # 模型名称
    max-tokens: 4096             # 最大生成 token 数
    temperature: 0.7             # 生成温度（0-1）
    timeout: 180000              # 请求超时(ms)
```

### 前端配置 `vite.config.ts`

```typescript
// API 代理配置
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:18080',
      changeOrigin: true
    },
    '/ws': {
      target: 'ws://localhost:18080',
      ws: true
    }
  }
}
```

---

## AI 智能功能

### 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端                                      │
│  FlowDesigner.vue ──→ AiAssistant.vue ──→ ai.ts API             │
│  SkillManager.vue ──→ ai.ts API                                 │
├─────────────────────────────────────────────────────────────────┤
│                        后端                                      │
│  AiController                                                   │
│    ├── AiService ──→ HTTP 调用大模型 API                          │
│    ├── AiFlowGenerationService ──→ 构建 Prompt + 解析响应         │
│    │       ├── 注入数据源信息、实现类列表                           │
│    │       ├── 注入相关 Skill 模板和用户偏好                        │
│    │       └── 解析 AI 返回的 JSON 流程定义                        │
│    └── SkillService                                             │
│            ├── learnFromSuccessfulFlow() ──→ 自动学习             │
│            ├── findRelevantSkills() ──→ 智能推荐                  │
│            └── recordPreference() ──→ 习惯记录                    │
├─────────────────────────────────────────────────────────────────┤
│  FlowEngine ──→ 任务成功时自动调用 SkillService.learnFromSuccessfulFlow()  │
└─────────────────────────────────────────────────────────────────┘
```

### AI 对话

通过 `/api/ai/chat` 端点与 AI 模型对话，支持传入历史消息实现多轮对话。系统会自动注入数据迁移领域的系统提示词，让 AI 扮演专业的数据迁移助手角色。

### 智能流程生成

1. 用户在 AI 助手中描述需求（如"将MySQL的users表数据清洗后迁移到PostgreSQL"）
2. 系统构建包含以下信息的 Prompt：
   - 可用数据源列表（ID、名称、类型、连接信息）
   - 可用实现类列表（提取器、加载器、转换器等）
   - 相关的 Skill 模板（基于需求关键词匹配）
   - 用户偏好（常用数据源、表映射等）
3. AI 返回 JSON 格式的流程定义（nodes + edges）
4. 前端解析后可一键应用到画布或保存为新流程

### Skills 自动学习

**触发时机**：每次流程任务成功完成时，FlowEngine 自动调用 `SkillService.learnFromSuccessfulFlow()`

**学习内容**：
- 流程结构模式（节点类型组合、连线关系）
- 数据源使用模式（源→目标类型）
- 节点实现类选择偏好
- 表和字段映射关系

**Skill 分类**：
| 分类 | 说明 |
|------|------|
| `simple_migration` | 简单提取→加载 |
| `transform_migration` | 包含数据转换 |
| `quality_aware_migration` | 包含数据校验 |
| `parallel_migration` | 并行执行 |
| `custom` | 自定义 |

### 用户偏好学习

系统自动记录用户的操作习惯，在 AI 生成流程时作为参考：

| 偏好类型 | 记录内容 | 用途 |
|----------|----------|------|
| `datasource` | 常用数据源 ID | AI 生成时优先选择 |
| `implementation` | 节点类型→实现类映射 | AI 生成时使用偏好实现类 |
| `table` | 常用表名 | AI 生成时优先填入 |
| `flow_generation` | 流程需求描述 | 优化 Prompt 构建 |

### 支持的 AI 模型

任何兼容 OpenAI Chat Completions API 格式的模型服务均可接入：

| 模型服务 | API 地址示例 | 说明 |
|----------|-------------|------|
| Ollama | `http://localhost:11434/v1/chat/completions` | 本地部署 |
| DeepSeek | `https://api.deepseek.com/v1/chat/completions` | 云端 API |
| 阿里通义千问 | 通过阿里云百炼平台 | 云端 API |
| OpenAI | `https://api.openai.com/v1/chat/completions` | 需海外网络 |
| 自部署模型 | 自定义 HTTP 地址 | 需兼容 OpenAI 格式 |

---

## 数据库初始化

项目提供了 `schema.sql` 作为参考，但实际运行时使用 Hibernate `ddl-auto: update` 自动管理表结构。

如需手动初始化，可执行 `backend/src/main/resources/schema.sql`，该脚本包含：
- 创建数据库 `migration_tool`
- 10 张表的完整建表语句（`IF NOT EXISTS` 安全模式）
- 所有表使用 `utf8mb4_unicode_ci` 字符集

**枚举值参考**：

NodeType 枚举：`start` / `end` / `data_extract` / `data_transform` / `data_load` / `validation` / `condition` / `parallel` / `merge` / `parallel_group` / `notification` / `script` / `delay`

TaskStatus 枚举：`PENDING` / `RUNNING` / `PAUSED` / `SUCCESS` / `FAILED` / `PARTIAL_SUCCESS` / `CANCELLED` / `SKIPPED`

DatabaseType 枚举：`MYSQL` / `POSTGRESQL` / `MONGODB`

LogLevel 枚举：`DEBUG` / `INFO` / `WARN` / `ERROR`
