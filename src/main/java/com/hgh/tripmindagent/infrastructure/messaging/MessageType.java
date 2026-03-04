package com.hgh.tripmindagent.infrastructure.messaging;

/**
 * 消息类型枚举
 * 
 * @author TripMind Team
 */
public enum MessageType {
    
    /**
     * 任务请求
     */
    TASK_REQUEST,
    
    /**
     * 任务响应
     */
    TASK_RESPONSE,
    
    /**
     * 状态变更
     */
    STATE_CHANGE,
    
    /**
     * 工具调用
     */
    TOOL_CALL,
    
    /**
     * 错误通知
     */
    ERROR,
    
    /**
     * 广播消息
     */
    BROADCAST
}
