package com.hgh.tripmindagent.agent.weather;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import com.hgh.tripmindagent.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 天气智能体
 *
 * @author hgh
 */
@Slf4j
@Component
public class WeatherAgent extends ToolCallAgent {
    
    public WeatherAgent(
        @Qualifier("webSearchTool") WebSearchTool webSearchTool,
        @Qualifier("openaiChatModel") ChatModel chatModel,
        com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        
        super("weather",
              buildConfig(configProperties),
              ChatClient.builder(chatModel).build(),
              new Object[]{webSearchTool});
    }
    
    /**
     * 从配置文件构建 AgentConfig
     */
    private static AgentConfig buildConfig(com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        var configItem = configProperties.getConfig("weather");
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
            .agentId("weather")
            .name("天气顾问")
            .description("查询天气预报，提供穿衣和出行建议")
            .skills(List.of("天气查询", "穿衣建议"))
            .domains(List.of("天气预报", "出行建议"))
            .tools(List.of("web_search"))
            .build();
    }
}
