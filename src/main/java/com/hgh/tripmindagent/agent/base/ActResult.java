package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 行动结果
 * 
 * @author hgh
 */
@Data
@Builder
public class ActResult {
    
    /**
     * 行动描述
     */
    private String action;
    
    /**
     * 观察结果
     */
    private String observation;
    
    /**
     * 是否终止
     */
    private boolean terminated;
    
    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
