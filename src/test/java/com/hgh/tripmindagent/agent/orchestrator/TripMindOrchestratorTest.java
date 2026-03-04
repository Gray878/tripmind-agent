package com.hgh.tripmindagent.agent.orchestrator;

import com.hgh.tripmindagent.agent.BaseAgent;
import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.config.AgentConfigProperties;
import com.hgh.tripmindagent.infrastructure.registry.AgentLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripMindOrchestrator unit tests")
class TripMindOrchestratorTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private AgentLocator agentLocator;

    @Mock
    private BaseAgent researchAgent;

    @Mock
    private BaseAgent budgetAgent;

    private TripMindOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        AgentConfigProperties configProperties = new AgentConfigProperties();
        orchestrator = new TripMindOrchestrator(List.of(), chatModel, configProperties);
        orchestrator.setAgentLocator(agentLocator);
    }

    @Test
    @DisplayName("executeParallel should aggregate successful subtask results")
    void executeParallel_AggregatesSuccessResults() {
        when(agentLocator.getAgent("research")).thenReturn(researchAgent);
        when(agentLocator.getAgent("budget")).thenReturn(budgetAgent);
        when(researchAgent.runAsync(any(AgentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(success("research-result")));
        when(budgetAgent.runAsync(any(AgentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(success("budget-result")));

        List<SubTask> tasks = List.of(
                subTask("research", "query-a", Map.of()),
                subTask("budget", "query-b", Map.of())
        );

        AgentResult result = orchestrator.executeParallel(tasks);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStepCount()).isEqualTo(2);
        assertThat(result.getResult()).contains("research-result");
        assertThat(result.getResult()).contains("budget-result");
    }

    @Test
    @DisplayName("executeParallel should include failed subtask error message")
    void executeParallel_ContainsFailureMessageWhenSubtaskFails() {
        when(agentLocator.getAgent("research")).thenReturn(researchAgent);
        when(agentLocator.getAgent("budget")).thenReturn(budgetAgent);
        when(researchAgent.runAsync(any(AgentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(success("research-result")));
        when(budgetAgent.runAsync(any(AgentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AgentResult.failure("budget service unavailable")));

        List<SubTask> tasks = List.of(
                subTask("research", "query-a", Map.of()),
                subTask("budget", "query-b", Map.of())
        );

        AgentResult result = orchestrator.executeParallel(tasks);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).contains("research-result");
        assertThat(result.getResult()).contains("budget service unavailable");
    }

    @Test
    @DisplayName("executeParallel should keep going when an agent cannot be resolved")
    void executeParallel_KeepsGoingWhenAgentMissing() {
        when(agentLocator.getAgent("weather")).thenThrow(new IllegalArgumentException("agent not found: weather"));

        List<SubTask> tasks = List.of(subTask("weather", "query-weather", Map.of()));

        AgentResult result = orchestrator.executeParallel(tasks);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStepCount()).isEqualTo(1);
        assertThat(result.getResult()).contains("weather");
    }

    @Test
    @DisplayName("executeParallel should pass query and params into AgentRequest")
    void executeParallel_BuildsExpectedAgentRequest() {
        when(agentLocator.getAgent("research")).thenReturn(researchAgent);
        when(researchAgent.runAsync(any(AgentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(success("ok")));

        List<SubTask> tasks = List.of(subTask("research", "need museum and food", Map.of("days", 3)));

        orchestrator.executeParallel(tasks);

        ArgumentCaptor<AgentRequest> requestCaptor = ArgumentCaptor.forClass(AgentRequest.class);
        verify(researchAgent).runAsync(requestCaptor.capture());
        AgentRequest captured = requestCaptor.getValue();

        assertThat(captured.getUserPrompt()).isEqualTo("need museum and food");
        assertThat(captured.getParams()).containsEntry("days", 3);
    }

    private static AgentResult success(String result) {
        return AgentResult.builder()
                .success(true)
                .result(result)
                .build();
    }

    private static SubTask subTask(String agentId, String query, Map<String, Object> params) {
        return SubTask.builder()
                .agentId(agentId)
                .query(query)
                .params(params)
                .build();
    }
}
