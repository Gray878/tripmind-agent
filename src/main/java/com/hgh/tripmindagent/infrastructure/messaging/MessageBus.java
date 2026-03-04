package com.hgh.tripmindagent.infrastructure.messaging;

import java.util.concurrent.CompletableFuture;

/**
 * 消息总线接口
 * 支持发布-订阅和请求-响应模式
 * 
 * @author hgh
 */
public interface MessageBus {
    
    /**
     * 发布消息（异步）
     * 
     * @param topic 主题
     * @param message 消息
     */
    void publish(String topic, AgentMessage message);
    
    /**
     * 订阅主题
     * 
     * @param topic 主题
     * @param handler 消息处理器
     */
    void subscribe(String topic, MessageHandler handler);
    
    /**
     * 请求-响应模式（同步）
     * 
     * @param topic 主题
     * @param message 消息
     * @param timeout 超时时间（毫秒）
     * @return 响应消息
     */
    CompletableFuture<AgentMessage> request(String topic, AgentMessage message, long timeout);
    
    /**
     * 取消订阅
     * 
     * @param topic 主题
     * @param handler 消息处理器（null表示取消所有订阅）
     */
    void unsubscribe(String topic, MessageHandler handler);
}
