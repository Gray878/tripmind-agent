package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体配置类
 * 
 * @author hgh
 */
@Data
@Builder
public class AgentConfig {
    
    /**
     * 智能体名称
     */
    private String name;
    
    /**
     * 智能体描述
     */
    private String description;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 下一步提示词
     */
    private String nextStepPrompt;
    
    /**
     * 最大执行步数
     */
    @Builder.Default
    private int maxSteps = 10;
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 120000;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private boolean streamEnabled = true;
    
    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 配置验证
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("智能体名称不能为空");
        }
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("系统提示词不能为空");
        }
        if (maxSteps <= 0 || timeout <= 0) {
            throw new IllegalArgumentException("最大步数和超时时间必须大于0");
        }
    }
}
