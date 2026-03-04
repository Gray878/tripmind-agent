package com.hgh.tripmindagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体配置属性
 *
 * @author hgh
 */
@Data
@Component
@ConfigurationProperties(prefix = "tripmind.agents")
public class AgentConfigProperties {
    
    /**
     * 智能体配置映射
     * key: 智能体 ID
     * value: 智能体配置项
     */
    private Map<String, AgentConfigItem> configs = new HashMap<>();
    
    /**
     * 智能体配置项
     */
    @Data
    public static class AgentConfigItem {
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
         * 最大步数
         */
        private Integer maxSteps = 10;
        
        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 60000L;
        
        /**
         * 下一步提示词（可选）
         */
        private String nextStepPrompt;
    }
    
    /**
     * 获取智能体配置
     * 
     * @param agentId
     * @return
     */
    public AgentConfigItem getConfig(String agentId) {
        return configs.getOrDefault(agentId, createDefaultConfig(agentId));
    }
    
    /**
     * 创建默认配置
     */
    private AgentConfigItem createDefaultConfig(String agentId) {
        AgentConfigItem config = new AgentConfigItem();
        config.setName(agentId);
        config.setDescription("智能体: " + agentId);
        config.setSystemPrompt("你是一个智能助手");
        config.setMaxSteps(10);
        config.setTimeout(60000L);
        return config;
    }
}
