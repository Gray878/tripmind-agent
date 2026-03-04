package com.hgh.tripmindagent.agent.interceptor;

import com.hgh.tripmindagent.agent.base.AgentContext;
import com.hgh.tripmindagent.agent.base.AgentInterceptor;
import com.hgh.tripmindagent.agent.base.AgentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志拦截器
 * 记录智能体执行的详细日志
 * 
 * @author hgh
 */
@Slf4j
@Component
public class LoggingInterceptor implements AgentInterceptor {
    
    @Override
    public void beforeRun(AgentContext context) {
        log.info("智能体 [{}] 开始执行，会话ID: {}, 用户ID: {}", 
            context.getAgentId(), 
            context.getSessionId(), 
            context.getUserId());
        log.debug("用户输入: {}", context.getUserPrompt());
    }
    
    @Override
    public void afterRun(AgentContext context, AgentResult result) {
        long duration = System.currentTimeMillis() - context.getStartTime();
        log.info("智能体 [{}] 执行完成，耗时: {}ms，成功: {}, 步骤数: {}", 
            context.getAgentId(), 
            duration, 
            result.isSuccess(),
            result.getStepCount());
        
        if (!result.isSuccess()) {
            log.error("智能体 [{}] 执行失败: {}", 
                context.getAgentId(), 
                result.getErrorMessage());
        }
    }
    
    @Override
    public void onError(AgentContext context, Exception e) {
        log.error("智能体 [{}] 执行异常", context.getAgentId(), e);
    }
    
    @Override
    public int getOrder() {
        return 100; // 较低优先级，最后执行
    }
}
