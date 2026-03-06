package com.hgh.tripmindagent.agent.budget;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预算智能体
 * 
 * @author hgh
 */
@Slf4j
@Component
public class BudgetAgent extends ToolCallAgent {
    
    public BudgetAgent(
            @Qualifier("openaiChatModel") ChatModel chatModel,
            com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        super("budget",
              buildConfig(configProperties),
              ChatClient.builder(chatModel).build(),
              new Object[]{});
    }
    
    /**
     * 从配置文件构建 AgentConfig
     */
    private static AgentConfig buildConfig(com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        var configItem = configProperties.getConfig("budget");
        return AgentConfig.builder()
                .name(configItem.getName())
                .description(configItem.getDescription())
                .systemPrompt(configItem.getSystemPrompt())
                .maxSteps(configItem.getMaxSteps())
                .timeout(configItem.getTimeout())
                .build();
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
            .agentId("budget")
            .name("预算规划专家")
            .description("精确计算旅游费用，提供预算分配方案")
            .skills(List.of("费用计算", "预算分配", "成本优化"))
            .domains(List.of("预算规划", "费用管理"))
            .build();
    }
}
