package com.hgh.tripmindagent.agent.base;

import com.hgh.tripmindagent.agent.BaseAgent;
import com.hgh.tripmindagent.agent.model.AgentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("BaseAgent unit tests")
class BaseAgentTest {

    private TestAgent testAgent;

    @BeforeEach
    void setUp() {
        ChatClient mockChatClient = mock(ChatClient.class);
        AgentConfig config = AgentConfig.builder()
                .name("test-agent")
                .description("test agent")
                .systemPrompt("test system prompt")
                .maxSteps(5)
                .timeout(30000L)
                .build();
        testAgent = new TestAgent("test-agent", config, mockChatClient);
    }

    @Test
    @DisplayName("run should return success result")
    void testBasicExecution_Success() {
        AgentRequest request = AgentRequest.builder()
                .userPrompt("test request")
                .userId("test_user")
                .build();

        AgentResult result = testAgent.run(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo("test execution success");
    }

    @Test
    @DisplayName("concurrent run should allow only one lock winner")
    void testConcurrentExecution_MutualExclusion() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        AgentRequest request = AgentRequest.builder()
                .userPrompt("concurrency test")
                .userId("test_user")
                .build();

        List<Throwable> errors = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    AgentResult result = testAgent.run(request);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    synchronized (errors) {
                        errors.add(t);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("agent state should return to IDLE after run")
    void testStateManagement_CorrectTransitions() {
        AgentRequest request = AgentRequest.builder()
                .userPrompt("state test")
                .userId("test_user")
                .build();

        AgentResult result = testAgent.run(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(testAgent.getCurrentState()).isEqualTo(AgentState.IDLE);
    }

    @Test
    @DisplayName("interceptor hooks should be invoked")
    void testInterceptorExecution_CorrectInvocation() {
        TestInterceptor interceptor = new TestInterceptor();
        testAgent.addInterceptor(interceptor);

        AgentRequest request = AgentRequest.builder()
                .userPrompt("interceptor test")
                .userId("test_user")
                .build();

        testAgent.run(request);

        assertThat(interceptor.beforeRunCalled).isTrue();
        assertThat(interceptor.afterRunCalled).isTrue();
    }

    private static class TestAgent extends BaseAgent {

        TestAgent(String agentId, AgentConfig config, ChatClient chatClient) {
            super(agentId, config, chatClient);
        }

        @Override
        protected AgentResult doRun(AgentContext context) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return AgentResult.builder()
                    .success(true)
                    .result("test execution success")
                    .build();
        }

        @Override
        public AgentCapability getCapability() {
            return AgentCapability.builder()
                    .agentId("test-agent")
                    .name("test")
                    .description("test capability")
                    .build();
        }

        AgentState getCurrentState() {
            return this.getState().get();
        }
    }

    private static class TestInterceptor implements AgentInterceptor {
        boolean beforeRunCalled;
        boolean afterRunCalled;

        @Override
        public void beforeRun(AgentContext context) {
            beforeRunCalled = true;
        }

        @Override
        public void afterRun(AgentContext context, AgentResult result) {
            afterRunCalled = true;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
