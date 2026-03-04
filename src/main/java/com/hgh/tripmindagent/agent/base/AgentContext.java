package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体执行上下文
 * 
 * @author hgh
 */
@Data
@Builder
public class AgentContext {
    
    /**
     * 智能体ID
     */
    private String agentId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户输入
     */
    private String userPrompt;
    
    /**
     * 请求参数
     */
    private Map<String, Object> params;
    
    /**
     * 开始时间
     */
    private long startTime;
    
    /**
     * 当前步骤
     */
    @Builder.Default
    private int currentStep = 0;
    
    /**
     * 消息历史
     */
    @Builder.Default
    private List<Message> messageHistory = new ArrayList<>();
    
    /**
     * 上下文变量（线程安全）
     */
    @Builder.Default
    private Map<String, Object> variables = new ConcurrentHashMap<>();
    
    /**
     * 流式输出
     */
    private SseEmitter emitter;
    
    /**
     * 设置变量
     */
    public void set(String key, Object value) {
        variables.put(key, value);
    }
    
    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) variables.get(key);
    }
    
    /**
     * 添加消息
     */
    public void addMessage(Message message) {
        messageHistory.add(message);
    }
    
    /**
     * 添加历史记录
     */
    public void addHistory(ThinkResult thinkResult, ActResult actResult) {
        set("history.step." + currentStep + ".think", thinkResult);
        set("history.step." + currentStep + ".act", actResult);
        currentStep++;
    }
    
    /**
     * 发送事件（SSE）
     */
    public void sendEvent(String eventName, String data) {
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                // 忽略发送失败
            }
        }
    }
}
