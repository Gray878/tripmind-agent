package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体能力描述
 * 
 * @author hgh
 */
@Data
@Builder
public class AgentCapability {
    
    /**
     * 智能体ID
     */
    private String agentId;
    
    /**
     * 智能体名称
     */
    private String name;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 技能列表
     */
    @Builder.Default
    private List<String> skills = new ArrayList<>();
    
    /**
     * 领域列表
     */
    @Builder.Default
    private List<String> domains = new ArrayList<>();
    
    /**
     * 工具列表
     */
    @Builder.Default
    private List<String> tools = new ArrayList<>();
    
    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 计算匹配度（0-1之间）
     */
    public double matchScore(AgentRequest request) {
        String query = request.getUserPrompt().toLowerCase();
        double score = 0.0;
        
        // 技能匹配
        for (String skill : skills) {
            if (query.contains(skill.toLowerCase())) {
                score += 0.3;
            }
        }
        
        // 领域匹配
        for (String domain : domains) {
            if (query.contains(domain.toLowerCase())) {
                score += 0.4;
            }
        }
        
        // 描述匹配
        if (description != null && query.contains(description.toLowerCase())) {
            score += 0.3;
        }
        
        return Math.min(score, 1.0);
    }
}
