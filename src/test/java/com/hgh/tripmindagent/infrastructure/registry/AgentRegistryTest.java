package com.hgh.tripmindagent.infrastructure.registry;

import com.hgh.tripmindagent.agent.BaseAgent;
import com.hgh.tripmindagent.agent.base.AgentCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("AgentRegistry tests")
class AgentRegistryTest {

    @Autowired
    private AgentRegistry agentRegistry;

    @Test
    @DisplayName("getAllAgents should return non-empty collection")
    void testGetAllAgents_ReturnsNonEmptyList() {
        Collection<BaseAgent> agents = agentRegistry.getAllAgents();

        assertThat(agents).isNotNull();
        assertThat(agents).isNotEmpty();
        assertThat(agents.size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("getAgent should return orchestrator by id")
    void testGetAgentById_Success() {
        BaseAgent agent = agentRegistry.getAgent("orchestrator");

        assertThat(agent).isNotNull();
        assertThat(agent.getCapability().getAgentId()).isEqualTo("orchestrator");
    }

    @Test
    @DisplayName("getAgent should throw when id does not exist")
    void testGetAgentById_NotFound() {
        assertThatThrownBy(() -> agentRegistry.getAgent("non-existent-agent"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findByCapability should match research agent")
    void testFindByCapability_Research() {
        assertFindByCapabilityHitsAgent("research");
    }

    @Test
    @DisplayName("findByCapability should match budget agent")
    void testFindByCapability_Budget() {
        assertFindByCapabilityHitsAgent("budget");
    }

    @Test
    @DisplayName("findByCapability should match weather agent")
    void testFindByCapability_Weather() {
        assertFindByCapabilityHitsAgent("weather");
    }

    @Test
    @DisplayName("findByCapability should match itinerary agent")
    void testFindByCapability_Itinerary() {
        assertFindByCapabilityHitsAgent("itinerary");
    }

    @Test
    @DisplayName("findByCapability should return empty for unmatched query")
    void testFindByCapability_NoMatch() {
        List<BaseAgent> agents = agentRegistry.findByCapability("query-that-does-not-match-any-capability");

        assertThat(agents).isEmpty();
    }

    @Test
    @DisplayName("all capabilities should be available")
    void testGetAllCapabilities_ReturnsCompleteList() {
        List<AgentCapability> capabilities = agentRegistry.getAllAgents().stream()
                .map(BaseAgent::getCapability)
                .toList();

        assertThat(capabilities).isNotNull();
        assertThat(capabilities).isNotEmpty();
        assertThat(capabilities.size()).isGreaterThanOrEqualTo(5);
        assertThat(capabilities).anyMatch(cap -> cap.getAgentId().equals("orchestrator"));
        assertThat(capabilities).anyMatch(cap -> cap.getAgentId().equals("research"));
        assertThat(capabilities).anyMatch(cap -> cap.getAgentId().equals("budget"));
        assertThat(capabilities).anyMatch(cap -> cap.getAgentId().equals("weather"));
        assertThat(capabilities).anyMatch(cap -> cap.getAgentId().equals("itinerary"));
    }

    @Test
    @DisplayName("agent capability should contain required fields")
    void testAgentCapability_ContainsNecessaryInfo() {
        BaseAgent agent = agentRegistry.getAgent("research");
        AgentCapability capability = agent.getCapability();

        assertThat(capability.getAgentId()).isNotBlank();
        assertThat(capability.getName()).isNotBlank();
        assertThat(capability.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("registry should auto-register core agents")
    void testRegistryInitialization_AutoRegistersAllAgents() {
        Collection<BaseAgent> agents = agentRegistry.getAllAgents();

        List<String> agentIds = agents.stream()
                .map(agent -> agent.getCapability().getAgentId())
                .toList();

        assertThat(agentIds).contains("orchestrator", "research", "budget", "weather", "itinerary");
    }

    private void assertFindByCapabilityHitsAgent(String agentId) {
        BaseAgent target = agentRegistry.getAgent(agentId);
        String query = buildMatchingQuery(target.getCapability());

        List<BaseAgent> matches = agentRegistry.findByCapability(query);

        assertThat(query).isNotBlank();
        assertThat(matches).isNotEmpty();
        assertThat(matches).anyMatch(agent -> agent.getCapability().getAgentId().equals(agentId));
    }

    private static String buildMatchingQuery(AgentCapability capability) {
        if (capability.getDomains() != null && !capability.getDomains().isEmpty()) {
            return capability.getDomains().getFirst();
        }
        if (capability.getSkills() != null && !capability.getSkills().isEmpty()) {
            return capability.getSkills().getFirst();
        }
        return capability.getDescription() == null ? "" : capability.getDescription();
    }
}