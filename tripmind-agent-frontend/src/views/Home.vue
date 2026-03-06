<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">TripMind</h1>
      <p class="subtitle">智能旅游规划助手</p>
    </div>
    
    <div class="content">
      <div class="input-section">
        <div class="form-card">
          <h2 class="section-title">请描述您的旅行需求</h2>
          
          <div class="form-group">
            <label class="form-label">旅行描述</label>
            <textarea 
              v-model="userPrompt"
              class="form-textarea" 
              placeholder="例如：我要去日本旅行5天，预算5000，喜欢美食和购物"
              rows="6"
            ></textarea>
            <span class="form-hint">请详细描述您的目的地、天数、预算、偏好和出发日期</span>
          </div>

          <div class="form-group">
            <label class="form-label">用户ID(可选)</label>
            <input 
              v-model="userId"
              type="text" 
              class="form-input" 
              placeholder="输入您的用户ID"
            >
          </div>

          <div class="button-group">
            <button 
              @click="startPlanning" 
              :disabled="isPlanning || !userPrompt.trim()"
              class="btn btn-primary"
            >
              {{ isPlanning ? '规划中...' : '开始规划' }}
            </button>
            <button 
              @click="clearForm" 
              :disabled="isPlanning"
              class="btn btn-secondary"
            >
              清空
            </button>
          </div>
        </div>
      </div>

      <!-- 进度区域 -->
      <div v-if="isPlanning" class="progress-section">
        <div class="progress-card">
          <h2 class="section-title">规划进行中</h2>
          <div class="progress-steps">
            <div 
              v-for="(step, index) in progressSteps" 
              :key="index"
              class="step-item"
            >
              <span class="step-dot"></span>
              <span class="step-text">{{ step }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 结果区域 -->
      <div v-if="planResult && !isPlanning" class="result-section">
        <div class="result-card">
          <div class="result-header">
            <h2 class="section-title">旅行规划</h2>
            <div class="result-actions">
              <button @click="copyResult" class="btn btn-text">复制</button>
              <button @click="newPlan" class="btn btn-secondary">新建规划</button>
            </div>
          </div>
          <div class="result-content" v-html="formatMarkdown(planResult)"></div>
        </div>
      </div>
    </div>
    
    <!-- 提示消息 -->
    <div v-if="toastMessage" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>
    
    <AppFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useHead } from '@vueuse/head'
import AppFooter from '../components/AppFooter.vue'
import { createTripPlan } from '../api/index.js'

useHead({
  title: 'TripMind 智能旅游规划平台',
  meta: [
    {
      name: 'description',
      content: 'TripMind 智能旅游规划平台，多智能体旅游规划助手'
    },
    {
      name: 'keywords',
      content: 'TripMind,智能旅游,旅游规划,AI助手'
    }
  ]
})

const userPrompt = ref('')
const userId = ref('')

const isPlanning = ref(false)
const progressSteps = ref([])
const planResult = ref('')
const toastMessage = ref('')
const toastType = ref('info')

const normalizeThinkContent = (text) => {
  if (!text) {
    return ''
  }
  return text
    .replace(/^💭\s*思考:\s*/u, '')
    .trim()
}

const addProgressStep = (text) => {
  if (!text) {
    return
  }
  progressSteps.value.push(text)
}

const startPlanning = () => {
  if (!userPrompt.value.trim()) {
    showToast('请输入旅行需求', 'error')
    return
  }

  isPlanning.value = true
  progressSteps.value = []
  planResult.value = ''

  let finished = false

  try {
    createTripPlan(
      {
        userPrompt: userPrompt.value,
        userId: userId.value || undefined
      },
      (message) => {
        const event = message?.event || 'message'
        const data = (message?.data || '').toString().trim()

        if (event === 'error') {
          finished = true
          isPlanning.value = false
          showToast(data || '规划失败', 'error')
          return
        }

        if (event === 'think') {
          const content = normalizeThinkContent(data)
          if (content) {
            planResult.value = content
          }
          addProgressStep(data)
          return
        }

        if (event === 'step_start' || event === 'act' || event === 'observe' || event === 'message') {
          addProgressStep(data)
          return
        }

        if (event === 'finished' || event === 'done') {
          if (finished) {
            return
          }
          finished = true
          isPlanning.value = false

          if (!planResult.value && progressSteps.value.length > 0) {
            planResult.value = progressSteps.value.join('\n\n')
          }

          showToast('规划完成', 'success')
          return
        }

        addProgressStep(data)
      },
      (error) => {
        finished = true
        isPlanning.value = false
        showToast(`连接后端失败: ${error?.message || 'unknown error'}`, 'error')
        console.error('SSE Error:', error)
      }
    )
  } catch (error) {
    isPlanning.value = false
    showToast(`请求失败: ${error?.message || 'unknown error'}`, 'error')
  }
}

const clearForm = () => {
  userPrompt.value = ''
  userId.value = ''
  planResult.value = ''
  progressSteps.value = []
}

const newPlan = () => {
  planResult.value = ''
  progressSteps.value = []
}

const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(planResult.value)
    showToast('已复制到剪贴板', 'success')
  } catch (error) {
    showToast('复制失败', 'error')
  }
}

const showToast = (message, type = 'info') => {
  toastMessage.value = message
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 3000)
}

const formatMarkdown = (text) => {
  if (!text) return ''

  return text
    .replace(/### (.*)/g, '<h3>$1</h3>')
    .replace(/## (.*)/g, '<h2>$1</h2>')
    .replace(/# (.*)/g, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #ffffff;
}

.header {
  padding: 40px 20px 30px;
  text-align: center;
  background: #ffffff;
  border-bottom: 1px solid #e5e5e5;
}

.title {
  font-size: 2.5rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1a1a1a;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 1rem;
  color: #666666;
  font-weight: 400;
}

.content {
  flex: 1;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 20px;
}

.input-section {
  margin-bottom: 30px;
}

.form-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 30px;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
}

.form-textarea,
.form-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 0.9375rem;
  color: #1a1a1a;
  background: #ffffff;
  border: 1px solid #d1d1d1;
  border-radius: 6px;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-textarea {
  resize: vertical;
  line-height: 1.6;
}

.form-textarea:focus,
.form-input:focus {
  outline: none;
  border-color: #666666;
}

.form-textarea::placeholder,
.form-input::placeholder {
  color: #999999;
}

.form-hint {
  display: block;
  font-size: 0.8125rem;
  color: #999999;
  margin-top: 6px;
}

.button-group {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 12px 24px;
  font-size: 0.9375rem;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-primary {
  background: #1a1a1a;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background: #333333;
}

.btn-primary:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #ffffff;
  color: #1a1a1a;
  border: 1px solid #d1d1d1;
}

.btn-secondary:hover:not(:disabled) {
  background: #f5f5f5;
}

.btn-secondary:disabled {
  color: #cccccc;
  cursor: not-allowed;
}

.btn-text {
  background: transparent;
  color: #666666;
  padding: 8px 16px;
  border: 1px solid #e5e5e5;
}

.btn-text:hover {
  background: #f5f5f5;
}

.progress-section {
  margin-bottom: 30px;
}

.progress-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 30px;
}

.progress-steps {
  margin-top: 20px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  color: #666666;
  font-size: 0.9375rem;
}

.step-dot {
  width: 6px;
  height: 6px;
  background: #1a1a1a;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}

.step-text {
  flex: 1;
  line-height: 1.6;
}

.result-section {
  margin-bottom: 30px;
}

.result-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 30px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.result-actions {
  display: flex;
  gap: 12px;
}

.result-content {
  color: #333333;
  font-size: 0.9375rem;
  line-height: 1.8;
}

.result-content :deep(h1) {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 16px;
}

.result-content :deep(h2) {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 20px 0 12px;
}

.result-content :deep(h3) {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a1a1a;
  margin: 16px 0 10px;
}

.result-content :deep(strong) {
  font-weight: 600;
  color: #1a1a1a;
}

.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  background: #1a1a1a;
  color: #ffffff;
  border-radius: 6px;
  font-size: 0.875rem;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.toast.success {
  background: #1a1a1a;
}

.toast.error {
  background: #d32f2f;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .title {
    font-size: 2rem;
  }
  
  .form-card,
  .progress-card,
  .result-card {
    padding: 20px;
  }
  
  .result-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .button-group {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
  }
}
</style>
