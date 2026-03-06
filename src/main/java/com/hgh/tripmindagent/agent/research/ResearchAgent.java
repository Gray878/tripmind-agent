package com.hgh.tripmindagent.agent.research;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import com.hgh.tripmindagent.tools.WebScrapingTool;
import com.hgh.tripmindagent.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 调研智能体
 * 
 * 职责：
 * 1. 搜索目的地的热门景点（评分、距离、特色、门票）
 * 2. 推荐当地美食（人均价格、地址、营业时间）
 * 3. 查询交通方案（地铁、公交、打车时间和费用）
 * 4. 收集最新的旅游攻略和用户评价
 * 
 * @author hgh
 */
@Slf4j
@Component
public class ResearchAgent extends ToolCallAgent {
    
    public ResearchAgent(
        @Qualifier("webSearchTool") WebSearchTool webSearchTool,
        @Qualifier("webScrapingTool") WebScrapingTool webScrapingTool,
        @Qualifier("openaiChatModel") ChatModel chatModel,
        com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        
        super("research",
              buildConfig(configProperties),
              ChatClient.builder(chatModel).build(),
              new Object[]{webSearchTool, webScrapingTool});
    }
    
    /**
     * 从配置文件构建 AgentConfig
     */
    private static AgentConfig buildConfig(com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        var configItem = configProperties.getConfig("research");
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
            .agentId("research")
            .name("旅游调研专家")
            .description("搜索和整理旅游景点、美食、交通等实时信息")
            .skills(List.of("信息搜索", "数据整理", "网页抓取"))
            .domains(List.of("景点调研", "美食推荐", "交通查询"))
            .tools(List.of("web_search", "web_scraping"))
            .build();
    }
}
