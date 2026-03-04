package com.hgh.tripmindagent.controller.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 旅游规划请求
 * 
 * @author TripMind Team
 */
@Data
public class TripRequest {
    
    /**
     * 用户输入
     */
    private String userPrompt;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 额外参数
     */
    private Map<String, Object> params = new HashMap<>();
    
    /**
     * 验证请求
     */
    public void validate() {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("用户输入不能为空");
        }
    }
}
