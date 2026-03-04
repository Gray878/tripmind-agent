package com.hgh.tripmindagent.infrastructure.registry;

import com.hgh.tripmindagent.agent.BaseAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import com.hgh.tripmindagent.agent.base.AgentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能体注册中心
 * 负责管理所有智能体的注册、发现和匹配
 * 
 * @author TripMind Team
 */
@Slf4j
@Component
public class AgentRegistry {
    
    private final Map<String, BaseAgent> agents = new ConcurrentHashMap<>();
    private final Map<String, AgentCapability> capabilities = new ConcurrentHashMap<>();
    
    // 通过构造函数注入所有智能体
    private final List<BaseAgent> allAgents;
    
    public AgentRegistry(List<BaseAgent> allAgents) {
        this.allAgents = allAgents;
    }
    
    /**
     * 自动注册所有智能体
     */
    @PostConstruct
    public void init() {
        log.info("开始注册智能体...");
        allAgents.forEach(this::register);
        log.info("智能体注册完成，共 {} 个", agents.size());
    }
    
    /**
     * 注册智能体
     */
    public void register(BaseAgent agent) {
        String agentId = agent.getAgentId();
        agents.put(agentId, agent);
        capabilities.put(agentId, agent.getCapability());
        log.info("注册智能体: {} ({})", agent.getConfig().getName(), agentId);
    }
    
    /**
     * 注销智能体
     */
    public void unregister(String agentId) {
        agents.remove(agentId);
        capabilities.remove(agentId);
        log.info("注销智能体: {}", agentId);
    }
    
    /**
     * 获取智能体
     */
    public BaseAgent getAgent(String agentId) {
        BaseAgent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("智能体不存在: " + agentId);
        }
        return agent;
    }
    
    /**
     * 根据能力查找最匹配的智能体
     */
    public List<BaseAgent> findByCapability(String query) {
        AgentRequest request = AgentRequest.builder()
                .userPrompt(query)
                .build();
        
        return capabilities.values().stream()
                .map(cap -> new AgentMatch(cap.getAgentId(), cap.matchScore(request)))
                .filter(match -> match.score > 0.3)  // 过滤低分
                .sorted(Comparator.comparingDouble(AgentMatch::score).reversed())
                .map(match -> agents.get(match.agentId))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有智能体
     */
    public Collection<BaseAgent> getAllAgents() {
        return agents.values();
    }
    
    /**
     * 获取所有智能体ID
     */
    public Set<String> getAllAgentIds() {
        return agents.keySet();
    }
    
    /**
     * 获取智能体能力
     */
    public AgentCapability getCapability(String agentId) {
        return capabilities.get(agentId);
    }
    
    /**
     * 智能体匹配结果
     */
    private record AgentMatch(String agentId, double score) {}
}
