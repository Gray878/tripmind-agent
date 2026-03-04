package com.hgh.tripmindagent.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 
 * 错误码规则：
 * - 1xxx: 智能体相关错误
 * - 2xxx: 工具相关错误
 * - 3xxx: 执行相关错误
 * - 4xxx: 配置相关错误
 * - 5xxx: 系统相关错误
 * 
 * @author hgh
 */
@Getter
public enum ErrorCode {
    
    // 1xxx: 智能体相关错误
    AGENT_NOT_FOUND(1001, "智能体不存在"),
    AGENT_ALREADY_RUNNING(1002, "智能体正在运行中"),
    AGENT_STATE_ERROR(1003, "智能体状态错误"),
    AGENT_CAPABILITY_MISMATCH(1004, "智能体能力不匹配"),
    
    // 2xxx: 工具相关错误
    TOOL_NOT_FOUND(2001, "工具不存在"),
    TOOL_EXECUTION_FAILED(2002, "工具执行失败"),
    TOOL_PARAMETER_INVALID(2003, "工具参数无效"),
    
    // 3xxx: 执行相关错误
    EXECUTION_TIMEOUT(3001, "执行超时"),
    EXECUTION_INTERRUPTED(3002, "执行被中断"),
    EXECUTION_FAILED(3003, "执行失败"),
    MAX_STEPS_EXCEEDED(3004, "超过最大步数限制"),
    
    // 4xxx: 配置相关错误
    CONFIG_INVALID(4001, "配置无效"),
    CONFIG_MISSING(4002, "配置缺失"),
    
    // 5xxx: 系统相关错误
    SYSTEM_ERROR(5001, "系统错误"),
    NETWORK_ERROR(5002, "网络错误"),
    DATABASE_ERROR(5003, "数据库错误");
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * 错误消息
     */
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
