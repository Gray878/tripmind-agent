package com.hgh.tripmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central tool registration.
 *
 * Local @Tool classes are registered as beans and MCP tools are merged in at runtime.
 */
@Slf4j
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Bean
    public FileOperationTool fileOperationTool() {
        return new FileOperationTool();
    }

    @Bean
    public WebSearchTool webSearchTool() {
        return new WebSearchTool(searchApiKey);
    }

    @Bean
    public WebScrapingTool webScrapingTool() {
        return new WebScrapingTool();
    }

    @Bean
    public ResourceDownloadTool resourceDownloadTool() {
        return new ResourceDownloadTool();
    }

    @Bean
    public TerminalOperationTool terminalOperationTool() {
        return new TerminalOperationTool();
    }

    @Bean
    public PDFGenerationTool pdfGenerationTool() {
        return new PDFGenerationTool();
    }

    @Bean
    public TerminateTool terminateTool() {
        return new TerminateTool();
    }

    /**
     * Collect MCP callback tools exposed by Spring AI MCP client auto-configuration.
     */
    @Bean("aggregatedMcpToolCallbacks")
    public ToolCallback[] aggregatedMcpToolCallbacks(ObjectProvider<ToolCallbackProvider> mcpToolCallbackProviders) {
        Map<String, ToolCallback> callbackMap = new LinkedHashMap<>();

        mcpToolCallbackProviders.orderedStream().forEach(provider -> {
            ToolCallback[] callbacks = provider.getToolCallbacks();
            if (callbacks == null) {
                return;
            }
            for (ToolCallback callback : callbacks) {
                if (callback == null || callback.getToolDefinition() == null) {
                    continue;
                }
                callbackMap.putIfAbsent(callback.getToolDefinition().name(), callback);
            }
        });

        if (callbackMap.isEmpty()) {
            log.info("No MCP tool callbacks loaded. Set SPRING_AI_MCP_CLIENT_ENABLED=true to enable MCP tools.");
        } else {
            log.info("Loaded {} MCP tool callbacks: {}", callbackMap.size(), callbackMap.keySet());
        }

        return callbackMap.values().toArray(new ToolCallback[0]);
    }

    /**
     * Tools used by orchestrator.
     */
    @Bean("allAgentTools")
    public Object[] allAgentTools(
            FileOperationTool fileOperationTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            ResourceDownloadTool resourceDownloadTool,
            TerminalOperationTool terminalOperationTool,
            PDFGenerationTool pdfGenerationTool,
            TerminateTool terminateTool,
            @Qualifier("aggregatedMcpToolCallbacks") ToolCallback[] mcpToolCallbacks) {
        ArrayList<Object> localTools = new ArrayList<>();
        localTools.add(fileOperationTool);
        if (hasSearchApiKey()) {
            localTools.add(webSearchTool);
        } else {
            log.warn("SEARCH_API_KEY is empty, skip registering webSearchTool for allAgentTools.");
        }
        localTools.add(webScrapingTool);
        localTools.add(resourceDownloadTool);
        localTools.add(terminalOperationTool);
        localTools.add(pdfGenerationTool);
        localTools.add(terminateTool);

        return mergeTools(localTools.toArray(new Object[0]), mcpToolCallbacks);
    }

    /**
     * Tools used by research agent.
     */
    @Bean("researchAgentTools")
    public Object[] researchAgentTools(
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            @Qualifier("aggregatedMcpToolCallbacks") ToolCallback[] mcpToolCallbacks) {
        ArrayList<Object> localTools = new ArrayList<>();
        if (hasSearchApiKey()) {
            localTools.add(webSearchTool);
        } else {
            log.warn("SEARCH_API_KEY is empty, skip registering webSearchTool for researchAgentTools.");
        }
        localTools.add(webScrapingTool);
        return mergeTools(localTools.toArray(new Object[0]), mcpToolCallbacks);
    }

    /**
     * Tools used by weather agent.
     */
    @Bean("weatherAgentTools")
    public Object[] weatherAgentTools(
            WebSearchTool webSearchTool,
            @Qualifier("aggregatedMcpToolCallbacks") ToolCallback[] mcpToolCallbacks) {
        ArrayList<Object> localTools = new ArrayList<>();
        if (hasSearchApiKey()) {
            localTools.add(webSearchTool);
        } else {
            log.warn("SEARCH_API_KEY is empty, skip registering webSearchTool for weatherAgentTools.");
        }
        return mergeTools(localTools.toArray(new Object[0]), mcpToolCallbacks);
    }

    private Object[] mergeTools(Object[] localTools, ToolCallback[] mcpToolCallbacks) {
        ArrayList<Object> merged = new ArrayList<>(Arrays.asList(localTools));
        if (mcpToolCallbacks != null && mcpToolCallbacks.length > 0) {
            merged.addAll(Arrays.asList(mcpToolCallbacks));
        }
        return merged.toArray(new Object[0]);
    }

    private boolean hasSearchApiKey() {
        return searchApiKey != null && !searchApiKey.isBlank();
    }
}
