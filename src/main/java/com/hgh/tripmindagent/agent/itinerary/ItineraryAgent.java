package com.hgh.tripmindagent.agent.itinerary;

import com.hgh.tripmindagent.advisor.MyLoggerAdvisor;
import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 行程智能体
 * 
 * 职责：
 * 1. 根据景点位置、开放时间，优化游览顺序
 * 2. 合理安排每天的行程，避免过度疲劳
 * 3. 检测时间冲突（景点关闭、交通拥堵等）
 * 4. 根据用户偏好（美食优先、文化优先等）调整行程
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class ItineraryAgent extends ToolCallAgent {
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    private static final String SYSTEM_PROMPT = """
        你是行程规划专家，负责优化旅游行程安排。
        
        你的职责：
        1. 根据景点位置、开放时间，优化游览顺序
        2. 合理安排每天的行程，避免过度疲劳
        3. 检测时间冲突（景点关闭、交通拥堵等）
        4. 根据用户偏好（美食优先、文化优先等）调整行程
        
        输出格式（时间轴）：
        ### 📅 Day 1: 浅草寺 + 築地市场
        
        **08:00-10:00** 🏛️ 浅草寺参观
        - 地址：东京都台东区浅草2-3-1
        - 门票：免费
        - 特色：东京最古老的寺庙
        
        **10:30-12:00** 🍜 築地外场美食
        - 地址：东京都中央区築地
        - 人均：¥150
        - 推荐：海鲜丼、寿司
        
        **12:30-14:00** 🚇 地铁前往涩谷
        - 路线：浅草站 → 涩谷站
        - 费用：¥200
        - 时长：30分钟
        
        **14:00-18:00** 🛍️ 涩谷购物
        - 地址：东京都涩谷区
        - 推荐：涩谷109、东急百货
        
        **18:30-20:00** 🍱 晚餐 + 入住酒店
        - 餐厅：一兰拉面
        - 酒店：涩谷东急酒店
        - 费用：¥800
        
        ### 📅 Day 2: ...
        
        注意事项：
        - 景点间距离不超过 30 分钟车程
        - 每天安排 2-3 个主要景点
        - 预留用餐和休息时间
        - 避开高峰时段
        """;
    
    public ItineraryAgent(ChatModel chatModel, @Autowired(required = false) VectorStore vectorStore) {
        super("itinerary",
              AgentConfig.builder()
                  .name("ItineraryAgent")
                  .description("行程规划专家")
                  .systemPrompt(SYSTEM_PROMPT)
                  .maxSteps(8)
                  .timeout(120000)
                  .build(),
              buildChatClient(chatModel, vectorStore),
              new ToolCallback[]{});
        
        this.vectorStore = vectorStore;
    }
    
    /**
     * 构建 ChatClient（带 RAG 增强）
     */
    private static ChatClient buildChatClient(ChatModel chatModel, VectorStore vectorStore) {
        var builder = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor());
        
        // 如果有向量数据库，添加 RAG 增强
        if (vectorStore != null) {
            builder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore));
        }
        
        return builder.build();
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
            .agentId("itinerary")
            .name("行程规划专家")
            .description("优化旅游行程，检测时间冲突，提供最佳游览顺序")
            .skills(List.of("行程优化", "时间管理", "路线规划"))
            .domains(List.of("行程规划", "时间安排"))
            .build();
    }
}
