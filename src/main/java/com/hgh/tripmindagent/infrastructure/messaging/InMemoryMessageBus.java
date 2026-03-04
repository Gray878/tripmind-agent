package com.hgh.tripmindagent.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 内存消息总线实现
 * 适用于开发和测试环境
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tripmind.message-bus.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryMessageBus implements MessageBus {
    
    private final Map<String, List<MessageHandler>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    @Override
    public void publish(String topic, AgentMessage message) {
        List<MessageHandler> handlers = subscribers.get(topic);
        if (handlers == null || handlers.isEmpty()) {
            log.warn("没有订阅者订阅主题: {}", topic);
            return;
        }
        
        log.debug("发布消息到主题 {}: {}", topic, message.getMessageId());
        
        // 异步通知所有订阅者
        handlers.forEach(handler -> 
            executor.submit(() -> {
                try {
                    handler.handle(message);
                } catch (Exception e) {
                    log.error("消息处理失败", e);
                }
            })
        );
    }
    
    @Override
    public void subscribe(String topic, MessageHandler handler) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                  .add(handler);
        log.info("订阅主题: {}", topic);
    }
    
    @Override
    public CompletableFuture<AgentMessage> request(String topic, AgentMessage message, long timeout) {
        CompletableFuture<AgentMessage> future = new CompletableFuture<>();
        
        // 创建临时响应主题
        String responseTopic = topic + ".response." + message.getMessageId();
        
        // 订阅响应
        subscribe(responseTopic, response -> {
            future.complete(response);
            unsubscribe(responseTopic, null);
        });
        
        // 发布请求
        message.setReplyTo(responseTopic);
        publish(topic, message);
        
        // 设置超时
        executor.submit(() -> {
            try {
                Thread.sleep(timeout);
                if (!future.isDone()) {
                    future.completeExceptionally(new TimeoutException("请求超时"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return future;
    }
    
    @Override
    public void unsubscribe(String topic, MessageHandler handler) {
        if (handler == null) {
            subscribers.remove(topic);
        } else {
            List<MessageHandler> handlers = subscribers.get(topic);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }
    }
}
