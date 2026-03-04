package com.hgh.tripmindagent.agent.research;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
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
 * @author TripMind Team
 */
@Slf4j
@Component
public class ResearchAgent extends ToolCallAgent {
    
    private static final String SYSTEM_PROMPT = """
        你是旅游调研专家，负责搜索和整理旅游相关信息。
        
        你的职责：
        1. 搜索目的地的热门景点（评分、距离、特色、门票）
        2. 推荐当地美食（人均价格、地址、营业时间）
        3. 查询交通方案（地铁、公交、打车时间和费用）
        4. 收集最新的旅游攻略和用户评价
        
        输出要求：
        - 数据必须真实可信，来源于 2026 年的最新信息
        - 使用结构化格式输出（Markdown）
        - 包含评分、价格、地址等关键信息
        
        可用工具：
        - web_search: 联网搜索
        - web_scraping: 网页抓取
        
        输出格式示例：
        ### 🏛️ 热门景点
        1. **景点名称** ⭐ 4.8/5.0
           - 地址：xxx
           - 门票：¥xxx
           - 特色：xxx
           
        ### 🍜 美食推荐
        1. **餐厅名称** ⭐ 4.5/5.0
           - 人均：¥xxx
           - 地址：xxx
           - 营业时间：xxx
        """;
    
    public ResearchAgent(
        @Qualifier("webSearchTool") ToolCallback webSearchTool,
        @Qualifier("webScrapingTool") ToolCallback webScrapingTool,
        ChatModel chatModel) {
        
        super("research",
              AgentConfig.builder()
                  .name("ResearchAgent")
                  .description("旅游调研专家")
                  .systemPrompt(SYSTEM_PROMPT)
                  .maxSteps(10)
                  .timeout(120000)
                  .build(),
              ChatClient.builder(chatModel).build(),
              new ToolCallback[]{webSearchTool, webScrapingTool});
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
