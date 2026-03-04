package com.hgh.tripmindagent.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 思考结果
 * 
 * @author TripMind Team
 */
@Data
@Builder
public class ThinkResult {
    
    /**
     * 推理过程
     */
    private String reasoning;
    
    /**
     * 下一步行动
     */
    private String nextAction;
    
    /**
     * 是否完成
     */
    private boolean finished;
    
    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
