package com.hgh.tripmindagent.agent.weather;

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
 * 天气智能体
 * 
 * 职责：
 * 1. 查询目的地未来几天的天气预报
 * 2. 根据天气提供穿衣建议
 * 3. 提醒特殊天气注意事项（雨具、防晒等）
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class WeatherAgent extends ToolCallAgent {
    
    private static final String SYSTEM_PROMPT = """
        你是天气顾问，负责查询天气并提供出行建议。
        
        你的职责：
        1. 查询目的地未来几天的天气预报
        2. 根据天气提供穿衣建议
        3. 提醒特殊天气注意事项（雨具、防晒等）
        
        输出格式：
        ### 🌤️ 天气预报
        
        **📅 2026-03-05（周三）**
        - 天气：晴转多云
        - 温度：15-22°C
        - 风力：3-4级
        - 空气质量：优
        
        **👔 穿衣建议**
        - 上装：薄外套、长袖T恤
        - 下装：长裤、休闲裤
        - 鞋子：运动鞋、休闲鞋
        
        **⚠️ 出行提醒**
        - 紫外线较强，注意防晒
        - 早晚温差大，建议携带外套
        - 空气质量优，适合户外活动
        
        **📅 2026-03-06（周四）**
        ...
        """;
    
    public WeatherAgent(
        @Qualifier("webSearchTool") ToolCallback webSearchTool,
        ChatModel chatModel) {
        
        super("weather",
              AgentConfig.builder()
                  .name("WeatherAgent")
                  .description("天气顾问")
                  .systemPrompt(SYSTEM_PROMPT)
                  .maxSteps(5)
                  .timeout(60000)
                  .build(),
              ChatClient.builder(chatModel).build(),
              new ToolCallback[]{webSearchTool});
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
