package com.hgh.tripmindagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.hgh.tripmindagent.agent.base.ActResult;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import com.hgh.tripmindagent.agent.base.AgentContext;
import com.hgh.tripmindagent.agent.base.ThinkResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent with explicit tool-call lifecycle control.
 */
@Slf4j
@Getter
public class ToolCallAgent extends ReActAgent {

    private static final int MAX_SAME_TOOL_CALL_ROUNDS = 3;
    private static final int MAX_HISTORY_MESSAGES = 12;
    private static final String DEFAULT_NEXT_STEP_PROMPT =
            "Please provide the final answer based on tool results. Call tools again only if key info is still missing.";

    private final Object[] availableTools;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;

    public ToolCallAgent(String agentId, AgentConfig config, ChatClient chatClient, Object[] tools) {
        super(agentId, config, chatClient);
        this.availableTools = normalizeTools(tools);
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    private Object[] normalizeTools(Object[] tools) {
        if (tools == null || tools.length == 0) {
            return new Object[0];
        }
        if (tools.length == 1) {
            Object first = tools[0];
            if (first instanceof List<?>) {
                return ((List<?>) first).toArray();
            }
            if (first instanceof Object[]) {
                return (Object[]) first;
            }
        }
        return tools;
    }

    @Override
    protected ThinkResult think(AgentContext context) {
        try {
            List<Message> messages = buildMessages(context);

            if (log.isDebugEnabled()) {
                log.debug("=== 发送给 LLM 的消息列表 ===");
                for (int i = 0; i < messages.size(); i++) {
                    Message msg = messages.get(i);
                    String type = msg.getClass().getSimpleName();
                    
                    // 对于 UserMessage，判断是否为 system-prompt
                    if (msg instanceof UserMessage) {
                        String text = msg.getText();
                        // 如果是第一条消息且内容较长，很可能是 system-prompt，不输出详细内容
                        if (i == 0 && text != null && text.length() > 200) {
                            log.debug("[{}] {} - [System Prompt, {} chars]", i, type, text.length());
                        } else {
                            String preview = text != null && text.length() > 80 ? text.substring(0, 80) + "..." : text;
                            log.debug("[{}] {} - {}", i, type, preview);
                        }
                    } else if (msg instanceof AssistantMessage) {
                        AssistantMessage assistant = (AssistantMessage) msg;
                        if (assistant.getToolCalls() != null && !assistant.getToolCalls().isEmpty()) {
                            log.debug("[{}] {} - [ToolCalls: {}]", i, type, assistant.getToolCalls().size());
                        } else {
                            String text = msg.getText();
                            String preview = text != null && text.length() > 80 ? text.substring(0, 80) + "..." : text;
                            log.debug("[{}] {} - {}", i, type, preview);
                        }
                    } else {
                        // ToolResponseMessage 等其他消息类型
                        log.debug("[{}] {}", i, type);
                    }
                }
            }

            Prompt prompt = new Prompt(messages, this.chatOptions);
            ChatResponse response = getChatClient()
                    .prompt(prompt)
                    .tools(availableTools)
                    .call()
                    .chatResponse();

            AssistantMessage message = response.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCalls =
                    message.getToolCalls() == null ? List.of() : message.getToolCalls();

            boolean hasToolCalls = !toolCalls.isEmpty();
            boolean isFinished = !hasToolCalls;

            if (hasToolCalls) {
                if (isRepeatedToolCallLoop(context, toolCalls)) {
                    log.warn("Agent [{}] detected repeated tool-call loop, force stop", getAgentId());
                    return ThinkResult.builder()
                            .reasoning("Repeated tool-call loop detected. Stop and summarize with current data.")
                            .nextAction("terminate_loop")
                            .finished(true)
                            .metadata(Map.of("toolCalls", toolCalls))
                            .build();
                }

                context.set("toolCallResponse", response);
                context.addMessage(message);
                log.debug("Added AssistantMessage with {} tool calls to history", toolCalls.size());
            } else {
                context.addMessage(message);
                log.debug("Added AssistantMessage without tool calls to history");
            }

            return ThinkResult.builder()
                    .reasoning(message.getText())
                    .nextAction(hasToolCalls ? "call_tools" : "finish")
                    .finished(isFinished)
                    .metadata(Map.of("toolCalls", toolCalls))
                    .build();
        } catch (Exception e) {
            log.error("Agent [{}] think stage failed", getAgentId(), e);
            return ThinkResult.builder()
                    .reasoning("Think failed: " + e.getMessage())
                    .nextAction("terminate")
                    .finished(true)
                    .metadata(Map.of("toolCalls", List.of()))
                    .build();
        }
    }

    @Override
    protected ActResult act(AgentContext context, ThinkResult thinkResult) {
        if (thinkResult.isFinished()) {
            return ActResult.builder()
                    .action("noop")
                    .observation("task_finished")
                    .terminated(true)
                    .build();
        }

        List<AssistantMessage.ToolCall> toolCalls = extractToolCalls(thinkResult);
        if (toolCalls.isEmpty()) {
            return ActResult.builder()
                    .action("no_tool_calls")
                    .observation("No executable tool calls found in think metadata")
                    .terminated(true)
                    .build();
        }

        ChatResponse toolCallResponse = context.get("toolCallResponse", ChatResponse.class);
        if (toolCallResponse == null) {
            return ActResult.builder()
                    .action("tool_call_failed")
                    .observation("Missing tool-call response in context")
                    .terminated(true)
                    .build();
        }

        try {
            List<Message> currentMessages = new ArrayList<>(context.getMessageHistory());
            Prompt prompt = new Prompt(currentMessages, this.chatOptions);
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallResponse);

            List<Message> updatedHistory = toolExecutionResult.conversationHistory();
            Message lastMessage = CollUtil.getLast(updatedHistory);
            if (!(lastMessage instanceof ToolResponseMessage)) {
                return ActResult.builder()
                        .action("tool_call_failed")
                        .observation("Tool execution did not produce ToolResponseMessage")
                        .terminated(true)
                        .build();
            }

            ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;
            if (!isToolResponseComplete(toolCalls, toolResponseMessage)) {
                return ActResult.builder()
                        .action("tool_response_incomplete")
                        .observation("ToolResponseMessage does not cover all tool_call_id values")
                        .terminated(true)
                        .build();
            }

            context.addMessage(toolResponseMessage);
            trimMessageHistory(context);

            StringBuilder actionLog = new StringBuilder();
            StringBuilder observationLog = new StringBuilder();
            boolean terminateToolCalled = false;

            for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                actionLog.append("tool=").append(response.name()).append("\n");
                observationLog.append("tool_result[").append(response.name()).append("]=")
                        .append(response.responseData()).append("\n");
                if ("doTerminate".equals(response.name())) {
                    terminateToolCalled = true;
                }
            }

            return ActResult.builder()
                    .action(actionLog.toString())
                    .observation(observationLog.toString())
                    .terminated(terminateToolCalled)
                    .build();
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return ActResult.builder()
                    .action("tool_call_exception")
                    .observation("error=" + e.getMessage())
                    .terminated(true)
                    .build();
        }
    }

    private List<Message> buildMessages(AgentContext context) {
        List<Message> messages = new ArrayList<>();
        List<Message> sanitizedHistory = sanitizeMessageHistory(context.getMessageHistory());

        if (sanitizedHistory.size() != context.getMessageHistory().size()) {
            context.getMessageHistory().clear();
            context.getMessageHistory().addAll(sanitizedHistory);
        }

        if (hasText(getConfig().getSystemPrompt())) {
            messages.add(new SystemMessage(getConfig().getSystemPrompt()));
        }

        if (hasText(context.getUserPrompt())) {
            messages.add(new UserMessage(context.getUserPrompt()));
        }

        messages.addAll(sanitizedHistory);

        if (!sanitizedHistory.isEmpty() && !hasUnresolvedToolCalls(sanitizedHistory)) {
            String nextStepPrompt = hasText(getConfig().getNextStepPrompt())
                    ? getConfig().getNextStepPrompt()
                    : DEFAULT_NEXT_STEP_PROMPT;
            messages.add(new UserMessage(nextStepPrompt));
        }

        return messages;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isRepeatedToolCallLoop(AgentContext context, List<AssistantMessage.ToolCall> toolCalls) {
        StringBuilder signatureBuilder = new StringBuilder();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            signatureBuilder.append(toolCall.name())
                    .append("::")
                    .append(String.valueOf(toolCall.arguments()))
                    .append("||");
        }
        String signature = signatureBuilder.toString();

        String lastSignature = context.get("lastToolCallSignature", String.class);
        Integer sameCount = context.get("sameToolCallCount", Integer.class);
        int nextCount = signature.equals(lastSignature) ? (sameCount == null ? 1 : sameCount + 1) : 1;

        context.set("lastToolCallSignature", signature);
        context.set("sameToolCallCount", nextCount);

        return nextCount >= MAX_SAME_TOOL_CALL_ROUNDS;
    }

    private void trimMessageHistory(AgentContext context) {
        List<Message> history = context.getMessageHistory();
        if (history.size() <= MAX_HISTORY_MESSAGES) {
            return;
        }
        int beforeSize = history.size();
        int removeCount = beforeSize - MAX_HISTORY_MESSAGES;
        history.subList(0, removeCount).clear();
        log.debug("Trim history: {} -> {}", beforeSize, history.size());
    }

    private List<Message> sanitizeMessageHistory(List<Message> history) {
        List<Message> sanitized = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            Message current = history.get(i);

            if (current instanceof AssistantMessage) {
                AssistantMessage assistant = (AssistantMessage) current;
                List<AssistantMessage.ToolCall> toolCalls = assistant.getToolCalls();
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    if (i + 1 < history.size() && history.get(i + 1) instanceof ToolResponseMessage) {
                        ToolResponseMessage toolResponse = (ToolResponseMessage) history.get(i + 1);
                        if (isToolResponseComplete(toolCalls, toolResponse)) {
                            sanitized.add(current);
                            sanitized.add(toolResponse);
                        } else {
                            log.warn("Drop mismatched assistant/tool response pair at index {}", i);
                        }
                        i++;
                    } else {
                        log.warn("Drop unresolved assistant tool_calls at index {}", i);
                    }
                    continue;
                }
            }

            if (current instanceof ToolResponseMessage) {
                log.warn("Drop orphan tool response at index {}", i);
                continue;
            }

            sanitized.add(current);
        }

        return sanitized;
    }

    private boolean hasUnresolvedToolCalls(List<Message> messageHistory) {
        for (int i = 0; i < messageHistory.size(); i++) {
            Message current = messageHistory.get(i);

            if (current instanceof AssistantMessage) {
                AssistantMessage assistant = (AssistantMessage) current;
                List<AssistantMessage.ToolCall> toolCalls = assistant.getToolCalls();
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    if (i + 1 >= messageHistory.size()) {
                        return true;
                    }
                    Message next = messageHistory.get(i + 1);
                    if (!(next instanceof ToolResponseMessage)) {
                        return true;
                    }
                    if (!isToolResponseComplete(toolCalls, (ToolResponseMessage) next)) {
                        return true;
                    }
                    i++;
                }
            } else if (current instanceof ToolResponseMessage) {
                return true;
            }
        }
        return false;
    }

    private boolean isToolResponseComplete(List<AssistantMessage.ToolCall> toolCalls,
                                           ToolResponseMessage toolResponseMessage) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return true;
        }
        if (toolResponseMessage == null || toolResponseMessage.getResponses() == null) {
            return false;
        }

        Set<String> expectedIds = new HashSet<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (toolCall != null && toolCall.id() != null) {
                expectedIds.add(toolCall.id());
            }
        }

        if (expectedIds.isEmpty()) {
            return !toolResponseMessage.getResponses().isEmpty();
        }

        Set<String> actualIds = new HashSet<>();
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            if (response != null && response.id() != null) {
                actualIds.add(response.id());
            }
        }

        return actualIds.containsAll(expectedIds);
    }

    @SuppressWarnings("unchecked")
    private List<AssistantMessage.ToolCall> extractToolCalls(ThinkResult thinkResult) {
        if (thinkResult.getMetadata() == null) {
            return List.of();
        }
        Object value = thinkResult.getMetadata().get("toolCalls");
        if (!(value instanceof List<?>)) {
            return List.of();
        }
        List<?> list = (List<?>) value;

        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof AssistantMessage.ToolCall) {
                result.add((AssistantMessage.ToolCall) item);
            }
        }
        return result;
    }

    protected boolean isTaskComplete(String response, AgentContext context) {
        if (response == null) {
            return false;
        }
        String lower = response.toLowerCase();
        return lower.contains("task complete") ||
                lower.contains("completed") ||
                lower.contains("done");
    }

    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
                .agentId(getAgentId())
                .name(getConfig().getName())
                .description(getConfig().getDescription())
                .skills(List.of("tool-calling", "multi-turn", "react"))
                .domains(List.of("general"))
                .tools(List.of())
                .build();
    }
}
