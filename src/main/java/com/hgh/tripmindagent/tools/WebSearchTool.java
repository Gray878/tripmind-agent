package com.hgh.tripmindagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web search tool backed by SearchAPI.
 */
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final int DEFAULT_MAX_RESULTS = 5;

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(@ToolParam(description = "Search query keyword") String query) {
        if (query == null || query.isBlank()) {
            return "Search query is empty.";
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "SearchAPI key is not configured. Please use MCP search tools or set SEARCH_API_KEY.";
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");

        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            if (organicResults == null || organicResults.isEmpty()) {
                String errorMessage = jsonObject.getStr("error");
                if (errorMessage != null && !errorMessage.isBlank()) {
                    return "SearchAPI error: " + errorMessage;
                }
                return "No search results returned for query: " + query;
            }

            int limit = Math.min(DEFAULT_MAX_RESULTS, organicResults.size());
            List<String> resultItems = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                Object item = organicResults.get(i);
                if (item instanceof JSONObject) {
                    JSONObject resultObject = (JSONObject) item;
                    resultItems.add(resultObject.toString());
                }
            }

            if (resultItems.isEmpty()) {
                return "No valid search results returned for query: " + query;
            }
            return String.join(",", resultItems);
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
