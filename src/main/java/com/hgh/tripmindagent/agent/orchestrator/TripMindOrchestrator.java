package com.hgh.tripmindagent.agent.orchestrator;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.infrastructure.registry.AgentLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main orchestrator agent.
 */
@Slf4j
@Component
public class TripMindOrchestrator extends ToolCallAgent {

    private AgentLocator agentLocator;

    @Autowired
    @Lazy
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

    public TripMindOrchestrator(
            @Qualifier("allAgentTools") Object[] allTools,
            @Qualifier("openaiChatModel") ChatModel chatModel,
            com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {

        super("orchestrator",
                buildConfig(configProperties),
                ChatClient.builder(chatModel).build(),
                allTools);
    }

    private static AgentConfig buildConfig(com.hgh.tripmindagent.config.AgentConfigProperties configProperties) {
        var configItem = configProperties.getConfig("orchestrator");
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
                .agentId("orchestrator")
                .name("旅游规划主管")
                .description("协调多个智能体完成复杂旅游规划任务")
                .skills(List.of("任务拆解", "并行调度", "结果汇总"))
                .domains(List.of("旅游规划", "任务协调"))
                .build();
    }

    /**
     * Execute subtasks in parallel using the target agents.
     */
    public AgentResult executeParallel(List<SubTask> tasks) {
        log.info("Start executing {} subtasks in parallel", tasks.size());

        List<CompletableFuture<AgentResult>> futures = tasks.stream()
                .map(task -> {
                    try {
                        var agent = agentLocator.getAgent(task.getAgentId());
                        AgentRequest request = AgentRequest.builder()
                                .userPrompt(task.getQuery())
                                .params(task.getParams())
                                .build();
                        return agent.runAsync(request);
                    } catch (Exception e) {
                        log.error("Failed to create subtask for agent {}", task.getAgentId(), e);
                        return CompletableFuture.completedFuture(
                                AgentResult.failure("智能体不存在: " + task.getAgentId())
                        );
                    }
                })
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<AgentResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return aggregateResults(results, tasks);
    }

    private AgentResult aggregateResults(List<AgentResult> results, List<SubTask> tasks) {
        StringBuilder finalResult = new StringBuilder();
        finalResult.append("# 旅游规划方案\n\n");

        for (int i = 0; i < results.size(); i++) {
            AgentResult result = results.get(i);
            SubTask task = tasks.get(i);

            if (result.isSuccess()) {
                String title = getTaskTitle(task.getAgentId());
                finalResult.append("## ").append(i + 1).append(". ").append(title).append("\n\n");
                finalResult.append(result.getResult()).append("\n\n");
            } else {
                finalResult.append("## ").append(i + 1).append(". 执行失败\n\n");
                finalResult.append("错误: ").append(result.getErrorMessage()).append("\n\n");
            }
        }

        return AgentResult.builder()
                .success(true)
                .result(finalResult.toString())
                .stepCount(results.size())
                .build();
    }

    private String getTaskTitle(String agentId) {
        switch (agentId) {
            case "research":
                return "景点调研";
            case "budget":
                return "预算规划";
            case "weather":
                return "天气预报";
            case "itinerary":
                return "行程安排";
            default:
                return "未知任务";
        }
    }
}
