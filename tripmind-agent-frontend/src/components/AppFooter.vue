<template>
  <footer class="app-footer">
    <div class="footer-content">
      <p class="footer-text">© {{ currentYear }} TripMind. All rights reserved.</p>
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
  background-color: #ffffff;
  padding: 24px 20px;
  border-top: 1px solid #e5e5e5;
  width: 100%;
  margin-top: auto;
}

.footer-content {
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
}

.footer-text {
  font-size: 0.875rem;
  color: #999999;
  margin: 0;
}

@media (max-width: 768px) {
  .app-footer {
    padding: 20px;
  }
  
  .footer-text {
    font-size: 0.8125rem;
  }
}
</style>
