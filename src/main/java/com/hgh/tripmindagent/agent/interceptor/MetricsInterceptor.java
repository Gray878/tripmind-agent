package com.hgh.tripmindagent.agent.interceptor;

import com.hgh.tripmindagent.agent.base.AgentContext;
import com.hgh.tripmindagent.agent.base.AgentInterceptor;
import com.hgh.tripmindagent.agent.base.AgentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 性能监控拦截器
 * 记录智能体执行的性能指标
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class MetricsInterceptor implements AgentInterceptor {
    
    @Override
    public void beforeRun(AgentContext context) {
        // 记录开始时间
        context.set("metrics.startTime", System.currentTimeMillis());
        
        // 记录开始内存
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        context.set("metrics.startMemory", startMemory);
    }
    
    @Override
    public void afterRun(AgentContext context, AgentResult result) {
        // 计算执行时长
        long startTime = context.get("metrics.startTime", Long.class);
        long duration = System.currentTimeMillis() - startTime;
        
        // 计算内存使用
        long startMemory = context.get("metrics.startMemory", Long.class);
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = endMemory - startMemory;
        
        // 记录性能指标
        log.debug("智能体 [{}] 性能指标 - 耗时: {}ms, 内存: {}KB, Token: {}", 
            context.getAgentId(), 
            duration, 
            memoryUsed / 1024,
            result.getTokenUsage());
        
        // TODO: 发送到 Prometheus/Micrometer
        // Counter.builder("agent.execution.count")
        //     .tag("agent", context.getAgentId())
        //     .tag("status", result.isSuccess() ? "success" : "failure")
        //     .register(meterRegistry)
        //     .increment();
    }
    
    @Override
    public int getOrder() {
        return 50; // 中等优先级
    }
}
