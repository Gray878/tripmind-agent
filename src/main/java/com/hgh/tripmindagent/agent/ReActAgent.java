package com.hgh.tripmindagent.agent;

import com.hgh.tripmindagent.agent.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

/**
 * ReAct (Reasoning and Acting) 模式的智能体
 * 实现了思考-行动的循环模式
 * 
 * @author hgh
 */
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    protected ReActAgent(String agentId, AgentConfig config, ChatClient chatClient) {
        super(agentId, config, chatClient);
    }
    
    /**
     * 执行核心逻辑（ReAct 循环）
     */
    @Override
    protected AgentResult doRun(AgentContext context) {
        StringBuilder resultBuilder = new StringBuilder();
        int step = 0;
        long startTime = System.currentTimeMillis();
        
        while (step < getConfig().getMaxSteps()) {
            // 检查超时
            if (System.currentTimeMillis() - startTime > getConfig().getTimeout()) {
                log.warn("智能体 [{}] 执行超时", getAgentId());
                resultBuilder.append("\n⚠️ 执行超时，已自动终止\n");
                break;
            }
            
            step++;
            log.info("智能体 [{}] 执行第 {} 步", getAgentId(), step);
            context.sendEvent("step_start", "Step " + step);
            
            // 思考
            ThinkResult thinkResult = think(context);
            String thinkMsg = String.format("💭 思考: %s\n", thinkResult.getReasoning());
            resultBuilder.append(thinkMsg);
            context.sendEvent("think", thinkMsg);
            
            // 判断是否完成
            if (thinkResult.isFinished()) {
                log.info("智能体 [{}] 判断任务已完成", getAgentId());
                resultBuilder.append("✅ 任务完成\n");
                context.sendEvent("finished", "任务完成");
                break;
            }
            
            // 行动
            ActResult actResult = act(context, thinkResult);
            String actMsg = String.format("🔧 行动: %s\n", actResult.getAction());
            String obsMsg = String.format("👀 观察: %s\n", actResult.getObservation());
            resultBuilder.append(actMsg).append(obsMsg);
            context.sendEvent("act", actMsg);
            context.sendEvent("observe", obsMsg);
            
            // 更新上下文
            context.addHistory(thinkResult, actResult);
            
            // 检查是否终止
            if (actResult.isTerminated()) {
                log.info("智能体 [{}] 主动终止", getAgentId());
                resultBuilder.append("🛑 智能体主动终止\n");
                break;
            }
        }
        
        if (step >= getConfig().getMaxSteps()) {
            resultBuilder.append("⚠️ 达到最大步数限制\n");
        }
        
        return AgentResult.builder()
                .success(true)
                .result(resultBuilder.toString())
                .stepCount(step)
                .build();
    }
    
    /**
     * 思考：处理当前状态并决定下一步行动
     */
    protected abstract ThinkResult think(AgentContext context);
    
    /**
     * 行动：执行决定的行动
     */
    protected abstract ActResult act(AgentContext context, ThinkResult thinkResult);
}
