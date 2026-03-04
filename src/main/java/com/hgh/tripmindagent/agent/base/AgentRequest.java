package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能体请求对象
 * 
 * @author hgh
 */
@Data
@Builder
public class AgentRequest {
    
    /**
     * 请求ID
     */
    @Builder.Default
    private String requestId = UUID.randomUUID().toString();
    
    /**
     * 用户输入
     */
    private String userPrompt;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 请求参数
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
    
    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private boolean streamEnabled = false;
    
    /**
     * 验证请求
     */
    public void validate() {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("用户输入不能为空");
        }
    }
}
