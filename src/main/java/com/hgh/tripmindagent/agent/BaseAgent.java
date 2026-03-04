package com.hgh.tripmindagent.agent;

import com.hgh.tripmindagent.agent.base.*;
import com.hgh.tripmindagent.agent.model.AgentState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 智能体基类（生产级）
 * 
 * 核心特性：
 * 1. 并发安全：使用 AtomicReference + ReentrantLock
 * 2. 流式输出：集成 SseEmitter
 * 3. 拦截器机制：支持日志、监控、审计等横切关注点
 * 4. 模板方法：定义执行骨架，子类实现细节
 * 
 * @author TripMind Team
 */
@Slf4j
@Getter
public abstract class BaseAgent {

    // 核心属性（不可变）
    private final String agentId;
    private final AgentConfig config;
    private final ChatClient chatClient;
    
    // 扩展机制
    private final List<AgentInterceptor> interceptors = new ArrayList<>();
    
    // 执行状态（并发安全）
    private final AtomicReference<AgentState> state = new AtomicReference<>(AgentState.IDLE);
    private final ReentrantLock executionLock = new ReentrantLock();
    
    /**
     * 构造函数
     */
    protected BaseAgent(String agentId, AgentConfig config, ChatClient chatClient) {
        this.agentId = agentId;
        this.config = config;
        this.chatClient = chatClient;
        
        // 验证配置
        if (config != null) {
            config.validate();
        }
    }
    
    /**
     * 添加拦截器
     */
    public void addInterceptor(AgentInterceptor interceptor) {
        interceptors.add(interceptor);
        // 按优先级排序
        interceptors.sort(Comparator.comparingInt(AgentInterceptor::getOrder));
    }
    
    /**
     * 添加多个拦截器
     */
    public void addInterceptors(List<AgentInterceptor> interceptorList) {
        interceptors.addAll(interceptorList);
        // 按优先级排序
        interceptors.sort(Comparator.comparingInt(AgentInterceptor::getOrder));
    }

    /**
     * 同步执行
     */
    public final AgentResult run(AgentRequest request) {
        // 尝试获取锁
        if (!executionLock.tryLock()) {
            return AgentResult.failure("智能体正在运行中，请稍后重试");
        }
        
        try {
            return executeWithLifecycle(request, null);
        } finally {
            executionLock.unlock();
        }
    }

    /**
     * 流式执行（SSE）
     */
    public final SseEmitter runStream(AgentRequest request) {
        SseEmitter emitter = new SseEmitter(config.getTimeout());
        
        CompletableFuture.runAsync(() -> {
            // 尝试获取锁
            if (!executionLock.tryLock()) {
                sendError(emitter, "智能体正在运行中");
                return;
            }
            
            try {
                executeWithLifecycle(request, emitter);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                executionLock.unlock();
            }
        });
        
        return emitter;
    }
    
    /**
     * 异步执行
     */
    public CompletableFuture<AgentResult> runAsync(AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> run(request));
    }

    /**
     * 生命周期管理（核心执行逻辑）
     */
    private AgentResult executeWithLifecycle(AgentRequest request, SseEmitter emitter) {
        // 验证请求
        request.validate();
        
        // 创建上下文
        AgentContext context = createContext(request, emitter);
        
        try {
            // 前置拦截
            interceptors.forEach(i -> i.beforeRun(context));
            
            // 状态转换（CAS 操作保证原子性）
            if (!state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)) {
                return AgentResult.failure("状态转换失败");
            }
            
            // 执行核心逻辑
            AgentResult result = doRun(context);
            
            // 后置拦截
            interceptors.forEach(i -> i.afterRun(context, result));
            
            // 更新状态
            state.set(AgentState.FINISHED);
            return result;
            
        } catch (Exception e) {
            log.error("智能体执行失败: {}", agentId, e);
            state.set(AgentState.ERROR);
            
            // 异常拦截
            interceptors.forEach(i -> i.onError(context, e));
            
            return AgentResult.failure(e.getMessage());
        } finally {
            // 清理资源
            cleanup(context);
            // 重置状态
            state.set(AgentState.IDLE);
        }
    }
    
    /**
     * 创建执行上下文
     */
    private AgentContext createContext(AgentRequest request, SseEmitter emitter) {
        return AgentContext.builder()
                .agentId(agentId)
                .sessionId(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .userPrompt(request.getUserPrompt())
                .params(request.getParams())
                .startTime(System.currentTimeMillis())
                .emitter(emitter)
                .build();
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
    
    /**
     * 子类实现核心执行逻辑
     */
    protected abstract AgentResult doRun(AgentContext context);
    
    /**
     * 获取智能体能力描述
     */
    public abstract AgentCapability getCapability();
    
    /**
     * 清理资源（子类可重写）
     */
    protected void cleanup(AgentContext context) {
        // 子类可以重写此方法来清理资源
    }
}
