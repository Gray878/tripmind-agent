package com.hgh.tripmindagent.agent.budget;

import com.hgh.tripmindagent.agent.ToolCallAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预算智能体
 * 
 * 职责：
 * 1. 根据用户预算和行程，分配各项费用
 * 2. 计算交通、住宿、餐饮、门票等详细费用
 * 3. 预留 10% 的弹性预算
 * 4. 提供费用优化建议
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class BudgetAgent extends ToolCallAgent {
    
    private static final String SYSTEM_PROMPT = """
        你是旅游预算规划专家，负责精确计算旅游费用。
        
        你的职责：
        1. 根据用户预算和行程，分配各项费用
        2. 计算交通、住宿、餐饮、门票等详细费用
        3. 预留 10% 的弹性预算
        4. 提供费用优化建议
        
        输出格式（Markdown 表格）：
        ### 💰 预算明细
        
        | 项目 | 单价 | 数量 | 小计 | 备注 |
        |------|------|------|------|------|
        | 机票 | ¥1500 | 2 | ¥3000 | 往返 |
        | 住宿 | ¥500 | 3晚 | ¥1500 | 三星酒店 |
        | 餐饮 | ¥150 | 9餐 | ¥1350 | 人均 |
        | 门票 | ¥100 | 5个 | ¥500 | 景点门票 |
        | 交通 | ¥50 | 6次 | ¥300 | 市内交通 |
        | 购物 | - | - | ¥500 | 预留 |
        | 弹性预算 | - | - | ¥715 | 10% |
        | **总计** | - | - | **¥7865** | - |
        
        ### 💡 省钱建议
        1. 提前预订机票可节省 20%
        2. 选择民宿代替酒店可节省 ¥300
        3. 使用公交卡可节省 ¥100
        
        总预算必须控制在用户指定范围内。
        """;
    
    public BudgetAgent(ChatModel chatModel) {
        super("budget",
              AgentConfig.builder()
                  .name("BudgetAgent")
                  .description("预算规划专家")
                  .systemPrompt(SYSTEM_PROMPT)
                  .maxSteps(5)
                  .timeout(60000)
                  .build(),
              ChatClient.builder(chatModel).build(),
              new ToolCallback[]{});  // 预算计算主要靠 LLM 推理
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
            .agentId("budget")
            .name("预算规划专家")
            .description("精确计算旅游费用，提供预算分配方案")
            .skills(List.of("费用计算", "预算分配", "成本优化"))
            .domains(List.of("预算规划", "费用管理"))
            .build();
    }
}
