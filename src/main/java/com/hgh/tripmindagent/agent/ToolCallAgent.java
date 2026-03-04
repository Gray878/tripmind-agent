package com.hgh.tripmindagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hgh.tripmindagent.agent.base.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具调用型智能体
 * 实现了完整的工具调用逻辑
 * 
 * @author TripMind Team
 */
@Slf4j
@Getter
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    /**
     * 构造函数
     */
    public ToolCallAgent(String agentId, AgentConfig config, ChatClient chatClient, ToolCallback[] tools) {
        super(agentId, config, chatClient);
        this.availableTools = tools != null ? tools : new ToolCallback[0];
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 思考：调用 LLM + 工具
     */
    @Override
    protected ThinkResult think(AgentContext context) {
        try {
            // 构建消息列表
            List<Message> messages = buildMessages(context);
            Prompt prompt = new Prompt(messages, this.chatOptions);
            
            // 调用 LLM（带工具）
            ChatResponse response = getChatClient()
                    .prompt(prompt)
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            
            AssistantMessage message = response.getResult().getOutput();
            context.addMessage(message);
            
            // 解析响应
            boolean hasToolCalls = message.getToolCalls() != null && !message.getToolCalls().isEmpty();
            boolean isFinished = !hasToolCalls && isTaskComplete(message.getContent(), context);
            
            // 保存工具调用信息到上下文
            if (hasToolCalls) {
                context.set("toolCallResponse", response);
            }
            
            return ThinkResult.builder()
                    .reasoning(message.getContent())
                    .nextAction(hasToolCalls ? "调用工具: " + message.getToolCalls().size() + " 个" : "完成任务")
                    .finished(isFinished)
                    .metadata(Map.of("toolCalls", message.getToolCalls()))
                    .build();
                    
        } catch (Exception e) {
            log.error("智能体 [{}] 思考过程出错", getAgentId(), e);
            return ThinkResult.builder()
                    .reasoning("思考失败: " + e.getMessage())
                    .finished(true)
                    .build();
        }
    }

    /**
     * 行动：执行工具调用
     */
    @Override
    protected ActResult act(AgentContext context, ThinkResult thinkResult) {
        if (thinkResult.isFinished()) {
            return ActResult.builder()
                    .action("无需行动")
                    .observation("任务已完成")
                    .terminated(true)
                    .build();
        }
        
        // 获取工具调用列表
        @SuppressWarnings("unchecked")
        List<AssistantMessage.ToolCall> toolCalls = 
            (List<AssistantMessage.ToolCall>) thinkResult.getMetadata().get("toolCalls");
        
        if (toolCalls == null || toolCalls.isEmpty()) {
            return ActResult.builder()
                    .action("无工具调用")
                    .observation("继续思考")
                    .terminated(false)
                    .build();
        }
        
        // 获取保存的响应
        ChatResponse toolCallResponse = context.get("toolCallResponse", ChatResponse.class);
        if (toolCallResponse == null) {
            return ActResult.builder()
                    .action("工具调用失败")
                    .observation("未找到工具调用响应")
                    .terminated(false)
                    .build();
        }
        
        // 执行工具调用
        StringBuilder actionLog = new StringBuilder();
        StringBuilder observationLog = new StringBuilder();
        
        try {
            // 构建 Prompt
            Prompt prompt = new Prompt(context.getMessageHistory(), this.chatOptions);
            
            // 执行工具调用
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallResponse);
            
            // 更新消息历史
            context.getMessageHistory().clear();
            context.getMessageHistory().addAll(toolExecutionResult.conversationHistory());
            
            // 获取工具响应
            ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
            
            // 判断是否调用了终止工具
            boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                    .anyMatch(response -> response.name().equals("doTerminate"));
            
            // 记录工具调用结果
            for (var response : toolResponseMessage.getResponses()) {
                actionLog.append(String.format("调用工具: %s\n", response.name()));
                observationLog.append(String.format("工具 %s 返回: %s\n", response.name(), response.responseData()));
            }
            
            return ActResult.builder()
                    .action(actionLog.toString())
                    .observation(observationLog.toString())
                    .terminated(terminateToolCalled)
                    .build();
                    
        } catch (Exception e) {
            log.error("工具调用失败", e);
            return ActResult.builder()
                    .action("工具调用异常")
                    .observation("错误: " + e.getMessage())
                    .terminated(false)
                    .build();
        }
    }
    
    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(AgentContext context) {
        List<Message> messages = new ArrayList<>();
        
        // 添加系统提示词
        if (getConfig().getSystemPrompt() != null) {
            messages.add(new UserMessage(getConfig().getSystemPrompt()));
        }
        
        // 添加用户输入
        if (context.getUserPrompt() != null) {
            messages.add(new UserMessage(context.getUserPrompt()));
        }
        
        // 添加历史消息
        messages.addAll(context.getMessageHistory());
        
        // 添加下一步提示词
        if (getConfig().getNextStepPrompt() != null && !context.getMessageHistory().isEmpty()) {
            messages.add(new UserMessage(getConfig().getNextStepPrompt()));
        }
        
        return messages;
    }
    
    /**
     * 判断任务是否完成（子类可重写）
     */
    protected boolean isTaskComplete(String response, AgentContext context) {
        if (response == null) return false;
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("任务完成") || 
               lowerResponse.contains("已完成") ||
               lowerResponse.contains("完成了");
    }
}
