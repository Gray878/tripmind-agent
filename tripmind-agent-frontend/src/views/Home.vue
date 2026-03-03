<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">TripMind</h1>
      <p class="subtitle">智能旅游规划平台</p>
    </div>
    
    <div class="content">
      <div class="welcome-card">
        <div class="icon">🚀</div>
        <h2>项目开发中</h2>
        <p class="description">
          TripMind 是一个基于多智能体架构的智能旅游规划平台<br/>
          正在按照架构设计方案逐步实现中...
        </p>
        <div class="features">
          <div class="feature">✓ 多智能体协同</div>
          <div class="feature">✓ 智能行程规划</div>
          <div class="feature">✓ 预算优化建议</div>
          <div class="feature">✓ 实时天气查询</div>
        </div>
        <div class="status">
          <span class="dot"></span>
          开发进行中
        </div>
      </div>
    </div>
    
    <AppFooter />
  </div>
</template>

<script setup>
import { useHead } from '@vueuse/head'
import AppFooter from '../components/AppFooter.vue'

useHead({
  title: 'TripMind 智能旅游规划平台',
  meta: [
    {
      name: 'description',
      content: 'TripMind 智能旅游规划平台，基于多智能体架构的AI旅游规划助手'
    },
    {
      name: 'keywords',
      content: 'TripMind,智能旅游,旅游规划,AI助手,多智能体,行程规划'
    }
  ]
})
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  padding: 60px 20px 40px;
  text-align: center;
  color: white;
}

.title {
  font-size: 3.5rem;
  font-weight: bold;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
}

.content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.welcome-card {
  background: white;
  border-radius: 20px;
  padding: 50px 40px;
  max-width: 600px;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.welcome-card h2 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 20px;
}

.description {
  font-size: 1.1rem;
  color: #666;
  line-height: 1.8;
  margin-bottom: 30px;
}

.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.feature {
  padding: 12px;
  background: #f0f4ff;
  border-radius: 8px;
  color: #667eea;
  font-weight: 500;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #e8f0fe;
  border-radius: 20px;
  color: #667eea;
  font-weight: 500;
}

.dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@media (max-width: 768px) {
  .title {
    font-size: 2.5rem;
  }
  
  .welcome-card {
    padding: 40px 30px;
  }
  
  .features {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .welcome-card {
    padding: 30px 20px;
  }
  
  .icon {
    font-size: 3rem;
  }
  
  .welcome-card h2 {
    font-size: 1.5rem;
  }
  
  .description {
    font-size: 1rem;
  }
}
</style>
