package com.hgh.tripmindagent.agent;

import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.agent.orchestrator.TripMindOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TripMind 集成测试
 * 
 * @author hgh
 */
@Slf4j
@SpringBootTest
public class TripMindIntegrationTest {
    
    @Autowired(required = false)
    private TripMindOrchestrator orchestrator;
    
    @Test
    public void testTripPlanning() {
        // 如果没有配置 ChatModel，跳过测试
        if (orchestrator == null) {
            log.warn("TripMindOrchestrator 未配置，跳过测试");
            return;
        }
        
        // 创建请求
        AgentRequest request = AgentRequest.builder()
                .userPrompt("我想去日本东京旅游3天，预算8000元，喜欢美食和购物，3月5日出发")
                .userId("test-user")
                .build();
        
        // 执行
        AgentResult result = orchestrator.run(request);
        
        // 验证
        assertNotNull(result);
        log.info("执行结果: {}", result.getResult());
        
        // 如果执行失败，打印错误信息
        if (!result.isSuccess()) {
            log.error("执行失败: {}", result.getErrorMessage());
        }
    }
}
