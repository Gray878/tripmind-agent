package com.hgh.tripmindagent.infrastructure.messaging;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能体消息
 * 
 * @author TripMind Team
 */
@Data
@Builder
public class AgentMessage {
    
    /**
     * 消息ID
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();
    
    /**
     * 发送者智能体ID
     */
    private String fromAgentId;
    
    /**
     * 接收者智能体ID（null表示广播）
     */
    private String toAgentId;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private Object payload;
    
    /**
     * 响应主题
     */
    private String replyTo;
    
    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}
