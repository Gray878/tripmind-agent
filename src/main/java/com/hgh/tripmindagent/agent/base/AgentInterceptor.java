package com.hgh.tripmindagent.agent.base;

/**
 * 智能体拦截器接口
 * 用于实现横切关注点（日志、监控、审计等）
 * 
 * @author TripMind Team
 */
public interface AgentInterceptor {
    
    /**
     * 执行前拦截
     * 
     * @param context 执行上下文
     */
    default void beforeRun(AgentContext context) {}
    
    /**
     * 执行后拦截
     * 
     * @param context 执行上下文
     * @param result 执行结果
     */
    default void afterRun(AgentContext context, AgentResult result) {}
    
    /**
     * 异常拦截
     * 
     * @param context 执行上下文
     * @param e 异常
     */
    default void onError(AgentContext context, Exception e) {}
    
    /**
     * 拦截器优先级（数字越小优先级越高）
     * 
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}
