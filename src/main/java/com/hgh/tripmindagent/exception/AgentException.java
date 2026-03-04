package com.hgh.tripmindagent.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体异常基类
 * 
 * @author hgh
 */
@Getter
public class AgentException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final ErrorCode errorCode;
    
    /**
     * 上下文信息
     */
    private final Map<String, Object> context;
    
    public AgentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public AgentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public AgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public AgentException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    /**
     * 添加上下文信息
     */
    public AgentException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
}
