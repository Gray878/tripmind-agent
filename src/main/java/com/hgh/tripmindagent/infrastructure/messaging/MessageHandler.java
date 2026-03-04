package com.hgh.tripmindagent.infrastructure.messaging;

/**
 * 消息处理器接口
 * 
 * @author hgh
 */
@FunctionalInterface
public interface MessageHandler {
    
    /**
     * 处理消息
     * 
     * @param message 消息
     */
    void handle(AgentMessage message);
}
