<template>
  <footer class="app-footer">
    <div class="footer-content">
      <div class="footer-section">
        <div class="footer-logo">
          <h3>TripMind 智能旅游规划平台</h3>
          <p class="footer-desc">基于多智能体架构的AI旅游规划助手</p>
        </div>
      </div>
      
      <div class="footer-section">
        <h4>项目信息</h4>
        <div class="footer-links">
          <a href="https://github.com/tripmind/tripmind-agent" target="_blank">GitHub 仓库</a>
          <a href="#">技术文档</a>
          <a href="#">API 文档</a>
        </div>
      </div>
      
      <div class="footer-section">
        <h4>相关链接</h4>
        <div class="footer-links">
          <a href="#">用户协议</a>
          <a href="#">隐私政策</a>
          <a href="#">关于我们</a>
        </div>
      </div>
    </div>
    
    <div class="footer-bottom">
      <p>© {{ currentYear }} TripMind - 智能旅游规划平台</p>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

// 计算当前年份
const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  background-color: #fff;
  padding: 40px 0 20px;
  color: #666;
  border-top: 1px solid #eee;
  width: 100%;
  margin-top: auto;
  position: relative;
  z-index: 2;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 0 20px;
}

.footer-section {
  flex: 1;
  min-width: 200px;
  margin-bottom: 30px;
  padding: 0 15px;
}

.footer-logo h3 {
  font-size: 1.25rem;
  margin-bottom: 10px;
  color: #333;
}

.footer-desc {
  font-size: 0.9rem;
  color: #999;
  line-height: 1.5;
}

.footer-section h4 {
  font-size: 1rem;
  margin-bottom: 15px;
  color: #333;
}

.footer-links {
  display: flex;
  flex-direction: column;
}

.footer-links a {
  margin-bottom: 10px;
  color: #666;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #667eea;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 0.9rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
  }
  
  .footer-section {
    width: 100%;
    margin-bottom: 20px;
    padding: 0;
  }
}

@media (max-width: 480px) {
  .app-footer {
    padding: 30px 0 15px;
  }
  
  .footer-logo h3 {
    font-size: 1.1rem;
  }
  
  .footer-desc {
    font-size: 0.85rem;
  }
  
  .footer-section h4 {
    font-size: 0.95rem;
  }
  
  .footer-links a {
    font-size: 0.9rem;
  }
}
</style>
