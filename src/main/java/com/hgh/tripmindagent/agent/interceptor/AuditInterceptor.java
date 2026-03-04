package com.hgh.tripmindagent.agent.interceptor;

import com.hgh.tripmindagent.agent.base.AgentContext;
import com.hgh.tripmindagent.agent.base.AgentInterceptor;
import com.hgh.tripmindagent.agent.base.AgentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计拦截器
 * 记录智能体执行的审计日志
 * 
 * @author hgh
 */
@Slf4j
@Component
public class AuditInterceptor implements AgentInterceptor {
    
    @Override
    public void beforeRun(AgentContext context) {
        log.info("审计日志 - 用户: {}, 智能体: {}, 操作: 开始执行, 会话: {}", 
            context.getUserId(), 
            context.getAgentId(),
            context.getSessionId());
        
        // TODO: 保存到审计日志数据库
    }
    
    @Override
    public void afterRun(AgentContext context, AgentResult result) {
        log.info("审计日志 - 用户: {}, 智能体: {}, 操作: 执行完成, 结果: {}", 
            context.getUserId(), 
            context.getAgentId(),
            result.isSuccess() ? "成功" : "失败");
        
        // TODO: 保存到审计日志数据库
    }
    
    @Override
    public void onError(AgentContext context, Exception e) {
        log.error("审计日志 - 用户: {}, 智能体: {}, 操作: 执行异常, 错误: {}", 
            context.getUserId(), 
            context.getAgentId(),
            e.getMessage());
        
        // TODO: 保存到审计日志数据库
    }
    
    @Override
    public int getOrder() {
        return 10; // 最高优先级，最先执行
    }
}
