package com.hgh.tripmindagent.agent.orchestrator;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.infrastructure.registry.AgentRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 主管智能体（协调者）
 * 
 * 职责：
 * 1. 任务分解：分析用户需求，提取关键信息
 * 2. 并行调度：分配任务给专业智能体
 * 3. 结果汇总：整合所有子智能体的输出
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class TripMindOrchestrator extends ToolCallAgent {
    
    @Autowired
    private AgentRegistry agentRegistry;
    
    private static final String SYSTEM_PROMPT = """
        你是 TripMind 旅游规划大脑，负责协调多个专业智能体完成旅游规划任务。
        
        你的职责：
        1. 分析用户需求，提取关键信息（目的地、天数、预算、偏好、日期）
        2. 将复杂任务分解为子任务，分配给专业智能体
        3. 并行调度子智能体执行
        4. 汇总所有结果，生成完整的旅游规划方案
        
        可用的子智能体：
        - research: 调研景点、美食、交通信息
        - budget: 预算规划和费用计算
        - weather: 天气查询和穿衣建议
        - itinerary: 行程优化和时间安排
        
        输出格式：JSON 任务列表
        {
          "tasks": [
            {"agentId": "research", "query": "...", "priority": 1},
            {"agentId": "budget", "query": "...", "priority": 1},
            {"agentId": "weather", "query": "...", "priority": 2},
            {"agentId": "itinerary", "query": "...", "priority": 3}
          ]
        }
        """;
    
    public TripMindOrchestrator(ToolCallback[] allTools, ChatModel chatModel) {
        super("orchestrator", 
              AgentConfig.builder()
                  .name("TripMindOrchestrator")
                  .description("旅游规划主管智能体")
                  .systemPrompt(SYSTEM_PROMPT)
                  .maxSteps(15)
                  .timeout(300000)
                  .build(),
              ChatClient.builder(chatModel).build(),
              allTools);
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
            .agentId("orchestrator")
            .name("旅游规划主管")
            .description("协调多个智能体完成复杂的旅游规划任务")
            .skills(List.of("任务分解", "并行调度", "结果汇总"))
            .domains(List.of("旅游规划", "任务协调"))
            .build();
    }
    
    /**
     * 并行执行子任务
     */
    public AgentResult executeParallel(List<SubTask> tasks) {
        log.info("开始并行执行 {} 个子任务", tasks.size());
        
        List<CompletableFuture<AgentResult>> futures = tasks.stream()
            .map(task -> {
                try {
                    var agent = agentRegistry.getAgent(task.getAgentId());
                    AgentRequest request = AgentRequest.builder()
                        .userPrompt(task.getQuery())
                        .params(task.getParams())
                        .build();
                    return agent.runAsync(request);
                } catch (Exception e) {
                    log.error("创建子任务失败: {}", task.getAgentId(), e);
                    return CompletableFuture.completedFuture(
                        AgentResult.failure("智能体不存在: " + task.getAgentId())
                    );
                }
            })
            .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 汇总结果
        List<AgentResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        return aggregateResults(results, tasks);
    }
    
    /**
     * 汇总结果
     */
    private AgentResult aggregateResults(List<AgentResult> results, List<SubTask> tasks) {
        StringBuilder finalResult = new StringBuilder();
        finalResult.append("# 🌍 旅游规划方案\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            AgentResult result = results.get(i);
            SubTask task = tasks.get(i);
            
            if (result.isSuccess()) {
                String title = getTaskTitle(task.getAgentId());
                finalResult.append("## ").append(i + 1).append(". ").append(title).append("\n\n");
                finalResult.append(result.getResult()).append("\n\n");
            } else {
                finalResult.append("## ").append(i + 1).append(". ❌ 执行失败\n\n");
                finalResult.append("错误: ").append(result.getErrorMessage()).append("\n\n");
            }
        }
        
        return AgentResult.builder()
                .success(true)
                .result(finalResult.toString())
                .stepCount(results.size())
                .build();
    }
    
    /**
     * 获取任务标题
     */
    private String getTaskTitle(String agentId) {
        return switch (agentId) {
            case "research" -> "📍 景点调研";
            case "budget" -> "💰 预算规划";
            case "weather" -> "🌤️ 天气预报";
            case "itinerary" -> "📅 行程安排";
            default -> "未知任务";
        };
    }
}
