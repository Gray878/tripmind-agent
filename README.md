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
- 💬 **实时对话**：支持流式输出的智能对话体验

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户交互层                              │
│         (Next.js Frontend + REST API + SSE)              │
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
- **AI 模型**：通义千问 / DeepSeek / OpenAI 兼容模型
- **向量数据库**：PgVector（可选）
- **缓存**：Caffeine
- **监控**：Micrometer + Prometheus
- **文档**：Knife4j
- **工具库**：Jsoup（网页抓取）+ iText（PDF 生成）

### 前端技术

- **框架**：Next.js 15 + React 19
- **UI 库**：shadcn/ui + Tailwind CSS
- **认证**：Supabase Auth
- **实时通信**：Server-Sent Events (SSE)
- **图标**：Lucide Icons
- **类型安全**：TypeScript

## 📦 项目结构

```
tripmind-agent/
├── docs/                                    # 项目文档
│   ├── TripMind多智能体架构设计方案.md        # 架构设计文档
│   ├── TODO.md                              # 开发任务清单
│   └── API文档说明.md                        # API 接口文档
├── src/
│   ├── main/
│   │   ├── java/com/hgh/tripmindagent/
│   │   │   ├── agent/                       # 智能体层
│   │   │   │   ├── base/                    # 基础类
│   │   │   │   ├── orchestrator/            # 编排器
│   │   │   │   ├── research/                # 调研智能体
│   │   │   │   ├── budget/                  # 预算智能体
│   │   │   │   ├── weather/                 # 天气智能体
│   │   │   │   └── itinerary/               # 行程智能体
│   │   │   ├── infrastructure/              # 基础设施
│   │   │   │   ├── registry/                # 注册中心
│   │   │   │   └── messaging/               # 消息总线
│   │   │   ├── tools/                       # 工具层
│   │   │   ├── advisor/                     # 拦截器/增强器
│   │   │   ├── config/                      # 配置管理
│   │   │   ├── controller/                  # 控制器层
│   │   │   └── rag/                         # RAG 相关
│   │   └── resources/
│   │       ├── application.yml              # 应用配置
│   │       ├── application-local.yml        # 本地环境配置
│   │       └── application-prod.yml         # 生产环境配置
│   └── test/                                # 测试代码
├── tripmind-agent-frontend/                 # 前端项目
│   ├── app/                                 # Next.js App Router
│   │   ├── page.tsx                         # 首页
│   │   ├── chat/                            # 对话页面
│   │   ├── auth/                            # 认证页面
│   │   └── layout.tsx                       # 根布局
│   ├── components/                          # 组件
│   │   ├── ui/                              # UI 组件
│   │   ├── hero.tsx                         # Hero 组件
│   │   └── theme-switcher.tsx               # 主题切换
│   ├── lib/                                 # 工具库
│   │   └── supabase/                        # Supabase 客户端
│   └── package.json
├── pom.xml                                  # Maven 配置
└── README.md                                # 项目说明
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+（可选，用于 PgVector）

### 后端启动

1. **克隆项目**

```bash
git clone https://github.com/liyupi/tripmind-agent.git
cd tripmind-agent
```

2. **配置 API Key**

编辑 `src/main/resources/application-local.yml`，配置你的 AI 模型 API Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
    openai:
      api-key: your-openai-api-key
      base-url: https://api.openai.com  # 或其他兼容的 API 地址
```

3. **启动后端**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端服务将在 `http://localhost:8123` 启动。

### 前端启动

1. **进入前端目录**

```bash
cd tripmind-agent-frontend
```

2. **配置环境变量**

复制 `.env.example` 为 `.env.local`，并配置 Supabase 相关信息：

```env
NEXT_PUBLIC_SUPABASE_URL=your-supabase-url
NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=your-supabase-key
```

3. **安装依赖**

```bash
npm install
```

4. **启动开发服务器**

```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

### 访问应用

- 前端页面：http://localhost:3000
- 对话页面：http://localhost:3000/chat
- API 文档：http://localhost:8123/doc.html
- 健康检查：http://localhost:8123/api/health

## 🎨 前端特性

### 页面设计

- **首页**：现代化的 Hero 区域，带有搜索框和快速标签
- **对话页面**：实时流式对话，支持多轮交互
- **主题切换**：支持亮色/暗色模式
- **响应式设计**：适配桌面和移动设备

### UI 组件

- 使用 shadcn/ui 组件库
- Tailwind CSS 样式系统
- Lucide Icons 图标库
- 渐变背景装饰效果

### 用户体验

- 未登录用户可以使用对话功能（无历史记录）
- 登录用户可以保存对话历史
- 流式输出，实时显示 AI 回复
- 支持 Enter 发送，Shift+Enter 换行

## 📚 开发文档

### 核心文档

- [架构设计方案](docs/TripMind多智能体架构设计方案.md) - 详细的系统架构设计
- [开发任务清单](docs/TODO.md) - 分阶段的开发任务
- [API 文档说明](docs/API文档说明.md) - API 接口文档

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
    
    @PostMapping("/plan/stream")
    public SseEmitter createPlanStream(@RequestBody TripRequest request) {
        // SSE 流式输出
    }
}
```

#### 4. 前端组件开发

创建 React 组件：

```tsx
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";

export function MyComponent() {
  const [state, setState] = useState("");
  
  return (
    <div>
      <Button onClick={() => setState("clicked")}>
        Click me
      </Button>
    </div>
  );
}
```

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

- [GitHub 仓库](https://github.com/Gray878/tripmind-agent)
- [问题反馈](https://github.com/liyupi/tripmind-agent/issues)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Next.js 文档](https://nextjs.org/docs)
- [shadcn/ui 文档](https://ui.shadcn.com/)

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 Issue：https://github.com/Gray878/tripmind-agent/issues
- 邮件联系：[待补充]

---

**注意**：本项目正在积极开发中，部分功能尚未完成。欢迎关注项目进展！

⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！
