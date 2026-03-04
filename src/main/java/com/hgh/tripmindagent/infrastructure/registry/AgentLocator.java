package com.hgh.tripmindagent.infrastructure.registry;

import com.hgh.tripmindagent.agent.BaseAgent;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 智能体定位器接口
 *
 * @author hgh
 */
public interface AgentLocator {
    
    /**
     * 根据 ID 获取智能体
     * 
     * @param agentId
     * @return
     */
    BaseAgent getAgent(String agentId);
    
    /**
     * 根据能力查找最匹配的智能体
     * 
     * @param query
     * @return
     */
    List<BaseAgent> findByCapability(String query);
    
    /**
     * 获取所有智能体
     * 
     * @return
     */
    Collection<BaseAgent> getAllAgents();
    
    /**
     * 获取所有智能体 ID
     * 
     * @return
     */
    Set<String> getAllAgentIds();
}
