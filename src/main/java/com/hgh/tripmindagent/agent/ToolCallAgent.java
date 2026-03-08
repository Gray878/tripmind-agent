package com.hgh.tripmindagent.agent;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent with explicit tool-call lifecycle control.
 */
@Slf4j
@Getter
public class ToolCallAgent extends ReActAgent {

    private static final int MAX_SAME_TOOL_CALL_ROUNDS = 3;
    private static final int MAX_HISTORY_MESSAGES = 12;
    private static final int MAX_OBSERVATION_PREVIEW_LENGTH = 1200;
    private static final int MAX_PLAYWRIGHT_SUMMARY_LENGTH = 6000;
    private static final int MAX_PLAYWRIGHT_LINKS = 20;
    private static final String DEFAULT_NEXT_STEP_PROMPT =
            "Please provide the final answer based on tool results. Call tools again only if key info is still missing.";

    private final Object[] localTools;
    private final ToolCallback[] callbackTools;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;

    public ToolCallAgent(String agentId, AgentConfig config, ChatClient chatClient, Object[] tools) {
        super(agentId, config, chatClient);
        Object[] normalizedTools = normalizeTools(tools);
        this.localTools = extractLocalTools(normalizedTools);
        this.callbackTools = extractCallbackTools(normalizedTools);
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

    private Object[] extractLocalTools(Object[] tools) {
        List<Object> locals = new ArrayList<>();
        if (tools == null) {
            return new Object[0];
        }
        for (Object tool : tools) {
            if (tool == null || tool instanceof ToolCallback || tool instanceof ToolCallbackProvider) {
                continue;
            }
            locals.add(tool);
        }
        return locals.toArray(new Object[0]);
    }

    private ToolCallback[] extractCallbackTools(Object[] tools) {
        List<ToolCallback> callbacks = new ArrayList<>();
        if (tools == null) {
            return new ToolCallback[0];
        }
        for (Object tool : tools) {
            if (tool == null) {
                continue;
            }
            if (tool instanceof ToolCallback) {
                callbacks.add((ToolCallback) tool);
                continue;
            }
            if (tool instanceof ToolCallbackProvider) {
                ToolCallback[] providerCallbacks = ((ToolCallbackProvider) tool).getToolCallbacks();
                if (providerCallbacks != null && providerCallbacks.length > 0) {
                    callbacks.addAll(List.of(providerCallbacks));
                }
            }
        }
        return callbacks.toArray(new ToolCallback[0]);
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
            ChatClient.ChatClientRequestSpec requestSpec = getChatClient().prompt(prompt);
            if (localTools.length > 0) {
                requestSpec = requestSpec.tools(localTools);
            }
            if (callbackTools.length > 0) {
                requestSpec = requestSpec.toolCallbacks(callbackTools);
            }
            ChatResponse response = requestSpec.call().chatResponse();

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
                    .finished(false)
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
                        .append(formatObservationForDisplay(response.name(), response.responseData())).append("\n");
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

    private String formatObservationForDisplay(String toolName, String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "(empty)";
        }

        String normalized = rawResponse.trim();
        if (isPlaywrightTool(toolName)) {
            return summarizePlaywrightResponse(normalized);
        }

        return truncateForDisplay(normalized);
    }

    private boolean isPlaywrightTool(String toolName) {
        return toolName != null && toolName.toLowerCase().contains("playwright");
    }

    private String summarizePlaywrightResponse(String rawResponse) {
        String normalizedResponse = normalizePlaywrightResponse(rawResponse);
        String pageUrl = firstNonBlank(
                extractSingleLineValue(normalizedResponse, "- Page URL:"),
                extractSingleLineValue(normalizedResponse, "Page URL:")
        );
        String pageTitle = firstNonBlank(
                extractSingleLineValue(normalizedResponse, "- Page Title:"),
                extractSingleLineValue(normalizedResponse, "Page Title:")
        );
        List<LinkPreview> links = extractFirstLinks(normalizedResponse, pageUrl, MAX_PLAYWRIGHT_LINKS);

        StringBuilder summary = new StringBuilder("Playwright page snapshot captured.");
        if (pageUrl != null) {
            summary.append("\nURL: ").append(pageUrl);
        }
        if (pageTitle != null) {
            summary.append("\nTitle: ").append(pageTitle);
        }
        if (!links.isEmpty()) {
            summary.append("\nFound links: ").append(links.size());
            summary.append("\nLinks:");
            for (LinkPreview link : links) {
                summary.append("\n- ").append(link.title()).append(" | ").append(link.url());
            }
        }

        summary.append("\n[raw page snapshot omitted]");
        return truncateForDisplay(summary.toString(), MAX_PLAYWRIGHT_SUMMARY_LENGTH);
    }

    private String normalizePlaywrightResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String candidate = extractPlaywrightTextField(rawResponse);
        if (candidate == null || candidate.isBlank()) {
            candidate = rawResponse;
        }

        return candidate
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private String extractPlaywrightTextField(String rawResponse) {
        try {
            Object parsed = JSONUtil.parse(rawResponse);
            if (parsed instanceof JSONArray jsonArray && !jsonArray.isEmpty()) {
                Object first = jsonArray.get(0);
                if (first instanceof JSONObject jsonObject) {
                    String text = jsonObject.getStr("text");
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            }
            if (parsed instanceof JSONObject jsonObject) {
                String text = jsonObject.getStr("text");
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // Raw response may be plain text.
        }
        return rawResponse;
    }

    private String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String extractSingleLineValue(String content, String prefix) {
        if (content == null || prefix == null) {
            return null;
        }

        int index = content.indexOf(prefix);
        if (index < 0) {
            return null;
        }

        int start = index + prefix.length();
        int end = content.indexOf('\n', start);
        String value = end < 0 ? content.substring(start) : content.substring(start, end);
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private List<LinkPreview> extractFirstLinks(String content, String basePageUrl, int maxCount) {
        List<LinkPreview> links = new ArrayList<>();
        if (content == null || maxCount <= 0) {
            return links;
        }

        Pattern pattern = Pattern.compile("- link \"([^\"]+)\"[^\\n]*\\R\\s*- /url:\\s*(\\S+)");
        Matcher matcher = pattern.matcher(content);
        Map<String, String> uniqueLinks = new LinkedHashMap<>();

        while (matcher.find() && uniqueLinks.size() < maxCount) {
            String linkText = matcher.group(1);
            String linkUrl = matcher.group(2);
            if (linkText == null || linkText.isBlank() || linkUrl == null || linkUrl.isBlank()) {
                continue;
            }

            String normalizedUrl = resolveNormalizedUrl(linkUrl, basePageUrl);
            if (normalizedUrl == null) {
                continue;
            }

            uniqueLinks.putIfAbsent(normalizedUrl, linkText.trim());
            if (uniqueLinks.size() >= maxCount) {
                break;
            }
        }

        for (Map.Entry<String, String> entry : uniqueLinks.entrySet()) {
            links.add(new LinkPreview(entry.getValue(), entry.getKey()));
        }

        return links;
    }

    private String resolveNormalizedUrl(String rawUrl, String basePageUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String candidate = rawUrl.trim();
        if (candidate.startsWith("#") || candidate.toLowerCase().startsWith("javascript:")) {
            return null;
        }

        try {
            URI uri = new URI(candidate);
            URI resolved = uri;
            if (!uri.isAbsolute()) {
                if (basePageUrl == null || basePageUrl.isBlank()) {
                    return null;
                }
                resolved = new URI(basePageUrl).resolve(uri);
            }

            String scheme = resolved.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
            return resolved.normalize().toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncateForDisplay(String value) {
        return truncateForDisplay(value, MAX_OBSERVATION_PREVIEW_LENGTH);
    }

    private String truncateForDisplay(String value, int maxLength) {
        if (value == null) {
            return "(null)";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        int omitted = value.length() - maxLength;
        return value.substring(0, maxLength)
                + "\n... [truncated " + omitted + " chars]";
    }

    private record LinkPreview(String title, String url) {
    }
}
