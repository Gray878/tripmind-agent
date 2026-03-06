package com.hgh.tripmindagent.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OpenAI 鍏煎 API 閰嶇疆绫?
 * 
 * 鏀寔鎵€鏈夊吋瀹?OpenAI API 鏍煎紡鐨勬ā鍨嬫彁渚涘晢锛?
 * - OpenAI (瀹樻柟)
 * - DeepSeek
 * - Moonshot (鏈堜箣鏆楅潰)
 * - 鏅鸿氨 AI (GLM)
 * - 闆朵竴涓囩墿 (Yi)
 * - 鍏朵粬鍏煎 OpenAI 鏍煎紡鐨勬ā鍨?
 * 
 * 浣跨敤鏂瑰紡锛?
 * 1. 鍦?application.yml 涓厤缃?spring.ai.openai.api-key 鍜?base-url
 * 2. 閫氳繃 @Qualifier("openaiChatModel") 娉ㄥ叆浣跨敤
 * 
 * @author hgh
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
public class OpenAiCompatibleConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.completions-path:/v1/chat/completions}")
    private String completionsPath;

    @Value("${spring.ai.openai.embeddings-path:/v1/embeddings}")
    private String embeddingsPath;

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * 鍒涘缓 OpenAI 鍏煎鐨?ChatModel Bean
     * 
     * 閫氳繃閰嶇疆涓嶅悓鐨?base-url 鍜?model锛屽彲浠ユ帴鍏ヤ换浣曞吋瀹?OpenAI API 鐨勬ā鍨嬶細
     * 
     * DeepSeek:
     *   base-url: https://api.deepseek.com
     *   model: deepseek-chat
     * 
     * Moonshot:
     *   base-url: https://api.moonshot.cn
     *   model: moonshot-v1-8k
     * 
     * 鏅鸿氨 AI:
     *   base-url: https://open.bigmodel.cn/api/paas/v4
     *   model: glm-4
     * 
     * 闆朵竴涓囩墿:
     *   base-url: https://api.lingyiwanwu.com
     *   model: yi-large
     */
    @Bean(name = "openaiChatModel")
    public ChatModel openaiChatModel() {
        // Spring AI 1.0 default completionsPath is /v1/chat/completions.
        // If baseUrl already ends with /v1, remove it to avoid /v1/v1/... 404.
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);

        // Create OpenAI-compatible API client
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .build();

        // Default chat options
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        // Build chat model
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (!StringUtils.hasText(rawBaseUrl)) {
            return "https://api.openai.com";
        }
        String trimmed = rawBaseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }
}

