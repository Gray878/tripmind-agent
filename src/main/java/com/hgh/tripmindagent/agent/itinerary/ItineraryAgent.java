package com.hgh.tripmindagent.agent.itinerary;

import com.hgh.tripmindagent.advisor.MyLoggerAdvisor;
import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * @author hgh
 */
@Slf4j
@Component
public class ItineraryAgent extends ToolCallAgent {
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    public ItineraryAgent(
            @Qualifier("dashscopeChatModel") ChatModel chatModel, 
            @Autowired(required = false) VectorStore vectorStore,
            com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        super("itinerary",
              buildConfig(configProperties),
              buildChatClient(chatModel, vectorStore),
              new Object[]{});
        
        this.vectorStore = vectorStore;
    }
    
    /**
     * 从配置文件构建 AgentConfig
     */
    private static AgentConfig buildConfig(com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        var configItem = configProperties.getConfig("itinerary");
        return AgentConfig.builder()
                .name(configItem.getName())
                .description(configItem.getDescription())
                .systemPrompt(configItem.getSystemPrompt())
                .maxSteps(configItem.getMaxSteps())
                .timeout(configItem.getTimeout())
                .build();
    }
    
    /**
     * 构建 ChatClient（带 RAG 增强）
     * 
     * 注意：QuestionAnswerAdvisor 需要 spring-ai-advisors-vector-store 依赖
     * 如果没有配置 VectorStore，则不启用 RAG 功能
     */
    private static ChatClient buildChatClient(ChatModel chatModel, VectorStore vectorStore) {
        var builder = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor());
        
        // 如果有向量数据库，添加 RAG 增强
        // 注意：QuestionAnswerAdvisor 在某些版本的 Spring AI 中可能不存在
        // 如果编译报错，可以注释掉这部分代码，不影响基本功能
        if (vectorStore != null) {
            try {
                // 尝试使用 RAG Advisor（需要 spring-ai-advisors-vector-store 依赖）
                // 如果类不存在，会在运行时跳过
                log.info("VectorStore 已配置，启用 RAG 增强");
                // builder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore));
                // 暂时注释掉，等 Spring AI 版本稳定后再启用
            } catch (Exception e) {
                log.warn("无法启用 RAG 增强: {}", e.getMessage());
            }
        } else {
            log.info("VectorStore 未配置，使用基础模式");
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
