package com.hgh.tripmindagent.agent.orchestrator;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 子任务模型
 * 
 * @author hgh
 */
@Data
@Builder
public class SubTask {
    
    /**
     * 智能体ID
     */
    private String agentId;
    
    /**
     * 查询内容
     */
    private String query;
    
    /**
     * 优先级（数字越小优先级越高）
     */
    private int priority;
    
    /**
     * 参数
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
}
