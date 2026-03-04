# TripMind 智能旅游规划平台

> 基于多智能体架构的 AI 旅游规划系统
> 
> 项目状态：开发中 🚧

## 📖 项目介绍

TripMind 是一个基于多智能体架构的智能旅游规划平台，通过协调多个专业智能体（调研、预算、天气、行程等），为用户提供个性化的旅游规划方案。

### 核心特性

- 🤖 **多智能体协同**：主管智能体协调多个专业智能体并行工作
- 🗺️ **智能行程规划**：基于用户需求自动生成优化的旅游行程
- 💰 **预算优化建议**：智能分配旅游预算，提供省钱建议
- 🌤️ **实时天气查询**：查询目的地天气，提供穿衣建议
- 🔍 **景点调研**：搜索热门景点、美食、交通信息
- 📄 **PDF 导出**：生成精美的旅游规划 PDF 文档

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户交互层                              │
│              (REST API + SSE 流式输出)                     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  智能体编排层                              │
│              TripMindOrchestrator                        │
│         (任务分解 + 并行调度 + 结果汇总)                     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  专业智能体层                              │
│  ResearchAgent | BudgetAgent | WeatherAgent             │
│              ItineraryAgent                              │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  工具调用层                                │
│  WebSearch | WebScraping | PDF | FileOperation          │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  基础设施层                                │
│  AgentRegistry | MessageBus | ToolRegistry | Cache      │
└─────────────────────────────────────────────────────────┘
```

## 🛠️ 技术栈

### 后端技术

- **框架**：Spring Boot 3.x + Spring AI
- **AI 模型**：通义千问 / DeepSeek
- **向量数据库**：PgVector（可选）
- **缓存**：Caffeine
- **监控**：Micrometer + Prometheus
- **文档**：Knife4j
- **工具库**：Jsoup（网页抓取）+ iText（PDF 生成）

### 前端技术

- **框架**：Vue 3 + Vite
- **UI 库**：Element Plus（待集成）
- **状态管理**：Pinia（待集成）
- **HTTP 客户端**：Axios
- **实时通信**：Server-Sent Events (SSE)

## 📦 项目结构

```
tripmind-agent/
├── docs/                                    # 项目文档
│   ├── TripMind多智能体架构设计方案.md        # 架构设计文档
│   ├── TODO.md                              # 开发任务清单
│   └── AI旅游规划多智能体.md                  # 需求分析文档
├── src/
│   ├── main/
│   │   ├── java/com/hgh/tripmindagent/
│   │   │   ├── agent/                       # 智能体层
│   │   │   │   ├── BaseAgent.java           # 智能体基类
│   │   │   │   ├── ReActAgent.java          # ReAct 模式智能体
│   │   │   │   └── ToolCallAgent.java       # 工具调用型智能体
│   │   │   ├── tools/                       # 工具层
│   │   │   │   ├── WebSearchTool.java       # 联网搜索
│   │   │   │   ├── WebScrapingTool.java     # 网页抓取
│   │   │   │   ├── PDFGenerationTool.java   # PDF 生成
│   │   │   │   └── ...                      # 其他工具
│   │   │   ├── advisor/                     # 拦截器/增强器
│   │   │   ├── chatmemory/                  # 对话记忆
│   │   │   ├── config/                      # 配置管理
│   │   │   ├── controller/                  # 控制器层
│   │   │   ├── rag/                         # RAG 相关
│   │   │   └── constant/                    # 常量定义
│   │   └── resources/
│   │       ├── application.yml              # 应用配置
│   │       └── application-prod.yml         # 生产环境配置
│   └── test/                                # 测试代码
├── tripmind-agent-frontend/                 # 前端项目
│   ├── src/
│   │   ├── views/                           # 页面组件
│   │   ├── components/                      # 通用组件
│   │   ├── router/                          # 路由配置
│   │   ├── api/                             # API 接口
│   │   └── App.vue                          # 根组件
│   └── package.json
├── pom.xml                                  # Maven 配置
└── README.md                                # 项目说明
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Node.js 16+
- PostgreSQL 14+（可选，用于 PgVector）

### 后端启动

1. **克隆项目**

```bash
git clone https://github.com/liyupi/tripmind-agent.git
cd tripmind-agent
```

2. **配置 API Key**

编辑 `src/main/resources/application.yml`，配置你的 AI 模型 API Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
```

3. **启动后端**

```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8123` 启动。

### 前端启动

1. **安装依赖**

```bash
cd tripmind-agent-frontend
npm install
```

2. **启动开发服务器**

```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

### 访问应用

- 前端页面：http://localhost:3000
- API 文档：http://localhost:8123/doc.html
- 健康检查：http://localhost:8123/api/health

## 📚 开发文档

### 核心文档

- [架构设计方案](docs/TripMind多智能体架构设计方案.md) - 详细的系统架构设计
- [开发任务清单](docs/TODO.md) - 分阶段的开发任务
- [需求分析文档](docs/AI旅游规划多智能体.md) - 项目需求和功能规划

### 开发指南

#### 1. 智能体开发

所有智能体继承自 `BaseAgent` 或其子类：

```java
@Component
public class MyAgent extends ToolCallAgent {
    public MyAgent(ToolCallback[] tools, ChatModel chatModel) {
        super(tools);
        this.setName("myAgent");
        this.setSystemPrompt("你的系统提示词");
        // 配置其他参数
    }
}
```

#### 2. 工具开发

实现 `ToolCallback` 接口或使用 `@Tool` 注解：

```java
@Component
public class MyTool implements ToolCallback {
    @Override
    public String getName() {
        return "myTool";
    }
    
    @Override
    public String getDescription() {
        return "工具描述";
    }
    
    @Override
    public String call(String toolInput) {
        // 工具逻辑
        return result;
    }
}
```

#### 3. API 开发

在 `controller` 包下创建控制器：

```java
@RestController
@RequestMapping("/api/trip")
public class TripPlanController {
    @PostMapping("/plan")
    public AgentResult createPlan(@RequestBody TripRequest request) {
        // 实现逻辑
    }
}
```

## 📋 开发进度

### 第一阶段：核心框架 ✅ 已完成

- [x] BaseAgent 实现（生产级：并发安全、流式输出、拦截器机制）
- [x] ReActAgent 实现（思考-行动循环）
- [x] ToolCallAgent 实现（完整工具调用逻辑）
- [x] 拦截器机制（日志、监控、审计）
- [x] 核心模型类（Config、Context、Request、Result、Capability等）

### 第二阶段：具体智能体 ✅ 已完成

- [x] TripMindOrchestrator（主管智能体 - 任务分解、并行调度、结果汇总）
- [x] ResearchAgent（调研智能体 - 景点、美食、交通）
- [x] BudgetAgent（预算智能体 - 费用计算、预算分配）
- [x] WeatherAgent（天气智能体 - 天气查询、穿衣建议）
- [x] ItineraryAgent（行程智能体 - 行程优化、RAG增强）

### 第三阶段：工具调用层与基础设施 ✅ 已完成

- [x] AgentRegistry（智能体注册中心 - 自动注册、能力匹配）
- [x] MessageBus（消息总线 - 发布订阅、请求响应）
- [x] InMemoryMessageBus（内存实现）
- [x] TripMindConfig（配置管理）

### 第四阶段：API 与前端 ✅ 已完成

- [x] TripPlanController（REST API）
  - [x] POST /api/trip/plan（同步创建规划）
  - [x] POST /api/trip/plan/stream（流式创建规划 - SSE）
  - [x] GET /api/trip/plan/{id}（查询规划 - 待实现）
  - [x] GET /api/trip/plan/{id}/pdf（下载PDF - 待实现）
- [x] Knife4j API 文档集成
- [x] 集成测试

### 第五阶段：优化与扩展 📅 待开发

- [ ] 缓存优化（Caffeine）
- [ ] 监控指标（Prometheus）
- [ ] 性能优化
- [ ] PDF 生成功能完善
- [ ] 持久化存储
- [ ] 更多智能体（酒店、机票、地图等）

## 🤝 贡献指南

欢迎贡献代码、提出问题和建议！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 开源协议

本项目采用 MIT 协议开源。

## 🔗 相关链接

- [GitHub 仓库](https://github.com/liyupi/tripmind-agent)
- [问题反馈](https://github.com/liyupi/tripmind-agent/issues)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 Issue：https://github.com/liyupi/tripmind-agent/issues
- 邮件联系：[待补充]

---

**注意**：本项目正在积极开发中，部分功能尚未完成。欢迎关注项目进展！

⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！
