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
 * @author hgh
 */
@Slf4j
@Getter
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final Object[] availableTools;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    /**
     * 构造函数
     * 
     * @param tools 工具数组，可以是任何带 @Tool 注解的对象
     */
    public ToolCallAgent(String agentId, AgentConfig config, ChatClient chatClient, Object[] tools) {
        super(agentId, config, chatClient);
        this.availableTools = tools != null ? tools : new Object[0];
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
            
            // 调试日志：打印消息结构
            if (log.isDebugEnabled()) {
                log.debug("=== 发送给 LLM 的消息列表 ===");
                for (int i = 0; i < messages.size(); i++) {
                    Message msg = messages.get(i);
                    String type = msg.getClass().getSimpleName();
                    String preview = msg.getText() != null && msg.getText().length() > 50
                        ? msg.getText().substring(0, 50) + "..."
                        : msg.getText();
                    log.debug("[{}] {} - {}", i, type, preview);
                    
                    if (msg instanceof AssistantMessage assistantMsg && assistantMsg.getToolCalls() != null) {
                        log.debug("    ToolCalls: {}", assistantMsg.getToolCalls().size());
                    }
                }
            }
            
            Prompt prompt = new Prompt(messages, this.chatOptions);
            
            // 调用 LLM（带工具）
            ChatResponse response = getChatClient()
                    .prompt(prompt)
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            
            AssistantMessage message = response.getResult().getOutput();
            
            // 解析响应
            boolean hasToolCalls = message.getToolCalls() != null && !message.getToolCalls().isEmpty();
            boolean isFinished = !hasToolCalls && isTaskComplete(message.getText(), context);
            
            // 保存工具调用信息到上下文（不立即添加到消息历史）
            if (hasToolCalls) {
                context.set("toolCallResponse", response);
                // 将 AssistantMessage 添加到消息历史
                context.addMessage(message);
                log.debug("添加 AssistantMessage (with {} tool calls) 到消息历史", message.getToolCalls().size());
            } else {
                // 没有工具调用时，也添加到消息历史
                context.addMessage(message);
                log.debug("添加 AssistantMessage (no tool calls) 到消息历史");
            }
            
            return ThinkResult.builder()
                    .reasoning(message.getText())
                    .nextAction(hasToolCalls ? "调用工具: " + message.getToolCalls().size() + " 个" : "完成任务")
                    .finished(isFinished)
                    .metadata(Map.of("toolCalls", message.getToolCalls() != null ? message.getToolCalls() : List.of()))
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
     * 
     * 关键流程：
     * 1. 从 context 中获取 toolCallResponse（包含 AssistantMessage with tool_calls）
     * 2. 使用 ToolCallingManager 执行工具调用
     * 3. ToolCallingManager 会返回完整的对话历史（包含 ToolResponseMessage）
     * 4. 用返回的完整历史替换 context 中的消息历史
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
            // 构建 Prompt（使用当前的消息历史）
            List<Message> currentMessages = new ArrayList<>(context.getMessageHistory());
            Prompt prompt = new Prompt(currentMessages, this.chatOptions);
            
            // 执行工具调用
            // ToolCallingManager 会：
            // 1. 执行所有工具调用
            // 2. 创建 ToolResponseMessage
            // 3. 返回完整的对话历史（原消息 + ToolResponseMessage）
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallResponse);
            
            // 获取工具响应消息（最后一条）
            List<Message> updatedHistory = toolExecutionResult.conversationHistory();
            ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(updatedHistory);
            
            // 只添加 ToolResponseMessage 到消息历史
            // 注意：AssistantMessage 已经在 think() 方法中添加过了
            context.addMessage(toolResponseMessage);
            
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
     * 
     * 关键规则：
     * 1. 系统提示词必须在最前面
     * 2. 用户输入在系统提示词之后
     * 3. 历史消息（包含 AssistantMessage + ToolResponseMessage）必须保持完整
     * 4. 不能在 AssistantMessage(with tool_calls) 和 ToolResponseMessage 之间插入其他消息
     */
    private List<Message> buildMessages(AgentContext context) {
        List<Message> messages = new ArrayList<>();
        
        // 1. 添加系统提示词（作为 UserMessage）
        if (getConfig().getSystemPrompt() != null) {
            messages.add(new UserMessage(getConfig().getSystemPrompt()));
        }
        
        // 2. 添加用户输入（仅在第一次调用时添加）
        if (context.getUserPrompt() != null && context.getMessageHistory().isEmpty()) {
            messages.add(new UserMessage(context.getUserPrompt()));
        }
        
        // 3. 添加历史消息（包含完整的 Assistant + Tool 对话）
        messages.addAll(context.getMessageHistory());
        
        // 4. 添加下一步提示词（仅在没有待处理的工具调用时添加）
        // 注意：不能在 AssistantMessage(with tool_calls) 后面添加 UserMessage
        if (getConfig().getNextStepPrompt() != null && 
            !context.getMessageHistory().isEmpty() &&
            !hasUnresolvedToolCalls(context.getMessageHistory())) {
            messages.add(new UserMessage(getConfig().getNextStepPrompt()));
        }
        
        return messages;
    }
    
    /**
     * 检查消息历史中是否有未解决的工具调用
     * 
     * 规则：如果最后一条消息是 AssistantMessage 且包含 tool_calls，
     * 则认为有未解决的工具调用
     */
    private boolean hasUnresolvedToolCalls(List<Message> messageHistory) {
        if (messageHistory.isEmpty()) {
            return false;
        }
        
        Message lastMessage = messageHistory.get(messageHistory.size() - 1);
        if (lastMessage instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getToolCalls() != null && 
                   !assistantMessage.getToolCalls().isEmpty();
        }
        
        return false;
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


    /**
     * 获取智能体能力描述
     * 子类应该重写此方法提供具体的能力描述
     */
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
                .agentId(getAgentId())
                .name(getConfig().getName())
                .description(getConfig().getDescription())
                .skills(List.of(
                    "工具调用",
                    "多轮对话",
                    "ReAct 推理",
                    "流式输出"
                ))
                .domains(List.of("通用"))
                .tools(List.of())
                .build();
    }

}
