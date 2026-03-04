package com.hgh.tripmindagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * TripMind 配置类
 * 
 * @author TripMind Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tripmind")
public class TripMindConfig {
    
    /**
     * 消息总线配置
     */
    private MessageBusConfig messageBus = new MessageBusConfig();
    
    /**
     * 智能体配置
     */
    private Map<String, AgentConfigProperties> agents = new HashMap<>();
    
    /**
     * 消息总线配置
     */
    @Data
    public static class MessageBusConfig {
        /**
         * 消息总线类型：memory（默认）/ redis
         */
        private String type = "memory";
    }
    
    /**
     * 智能体配置属性
     */
    @Data
    public static class AgentConfigProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 最大步数
         */
        private int maxSteps = 10;
        
        /**
         * 超时时间（毫秒）
         */
        private long timeout = 120000;
    }
}
