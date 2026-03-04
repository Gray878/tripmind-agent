package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体响应对象
 * 
 * @author TripMind Team
 */
@Data
@Builder
public class AgentResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 数据
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * 执行步骤
     */
    @Builder.Default
    private List<String> steps = new ArrayList<>();
    
    /**
     * 步骤数量
     */
    @Builder.Default
    private int stepCount = 0;
    
    /**
     * Token 使用量
     */
    @Builder.Default
    private long tokenUsage = 0;
    
    /**
     * 执行时长（毫秒）
     */
    @Builder.Default
    private long duration = 0;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 错误堆栈
     */
    private String errorStack;
    
    /**
     * 创建成功结果
     */
    public static AgentResult success(String result) {
        return AgentResult.builder()
                .success(true)
                .result(result)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static AgentResult failure(String error) {
        return AgentResult.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}
