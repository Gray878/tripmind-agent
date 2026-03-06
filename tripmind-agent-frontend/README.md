# TripMind 前端项目

TripMind 智能旅游规划平台的前端应用，基于 Vue 3 + Vite 构建。

## 技术栈

- **框架**: Vue 3
- **构建工具**: Vite 4
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **样式**: 原生 CSS（白色调简洁设计）

## 设计理念

- 纯白色调，简洁清爽
- 无表情图标，无渐变色
- 注重可读性和用户体验
- 响应式设计，适配多端

## 项目结构

```
tripmind-agent-frontend/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   ├── router/         # 路由配置
│   ├── views/          # 页面组件
│   ├── App.vue         # 根组件
│   ├── main.js         # 入口文件
│   └── style.css       # 全局样式
├── index.html          # HTML 模板
├── package.json        # 项目配置
└── vite.config.js      # Vite 配置
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 功能特性

### 已实现

- 旅游规划表单输入
- 流式 SSE 实时进度展示
- Markdown 格式结果渲染
- 复制结果到剪贴板
- 响应式布局设计

### 待实现

- PDF 下载功能
- 历史规划查询
- 用户认证系统
- 规划分享功能

## API 接口

### 后端服务地址

- 开发环境: http://localhost:8123/api
- 生产环境: /api（相对路径）

### 主要接口

- `POST /trip/plan/stream` - 创建旅游规划（流式）
- `POST /trip/plan` - 创建旅游规划（同步）
- `GET /trip/plan/{id}` - 获取规划详情
- `GET /trip/plan/{id}/pdf` - 下载 PDF

## 环境变量

在项目根目录创建 `.env` 文件：

```env
# API 基础地址（可选，默认使用代码中的配置）
VITE_API_BASE_URL=http://localhost:8123/api
```

## Docker 部署

### 构建镜像

```bash
docker build -t tripmind-frontend .
```

### 运行容器

```bash
docker run -p 80:80 tripmind-frontend
```

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 开发规范

### 代码风格

- 使用 2 空格缩进
- 组件名使用 PascalCase
- 文件名使用 kebab-case
- 遵循 Vue 3 Composition API 风格

### 样式规范

- 使用白色调为主色调
- 避免使用渐变色
- 不使用表情图标
- 保持简洁清爽的设计风格

### 提交规范

```
feat: 新功能
fix: 修复问题
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具链相关
```

## 常见问题

### 1. 开发环境跨域问题

在 `vite.config.js` 中配置代理：

```javascript
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true
      }
    }
  }
}
```

### 2. SSE 连接失败

确保后端服务已启动，并检查 API 地址配置是否正确。

### 3. 构建后静态资源 404

检查 `vite.config.js` 中的 `base` 配置是否正确。

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

- 项目地址: https://github.com/tripmind/tripmind-agent
- 问题反馈: https://github.com/tripmind/tripmind-agent/issues
