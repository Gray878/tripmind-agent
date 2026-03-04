package com.hgh.tripmindagent.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryMessageBus 单元测试
 * 
 * @author hgh
 */
@DisplayName("内存消息总线测试")
class InMemoryMessageBusTest {

    private InMemoryMessageBus messageBus;

    @BeforeEach
    void setUp() {
        messageBus = new InMemoryMessageBus();
    }

    @Test
    @DisplayName("发布订阅 - 基本功能")
    void testPublishSubscribe_BasicFunctionality() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger receivedCount = new AtomicInteger(0);

        MessageHandler handler = message -> {
            receivedCount.incrementAndGet();
            latch.countDown();
        };

        String topic = "test.topic";
        messageBus.subscribe(topic, handler);

        AgentMessage message = AgentMessage.builder()
                .type(MessageType.TASK_REQUEST)
                .fromAgentId("test-agent")
                .payload("测试消息")
                .build();

        // When
        messageBus.publish(topic, message);

        // Then
        boolean received = latch.await(1, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("多个订阅者 - 都能收到消息")
    void testMultipleSubscribers_AllReceiveMessage() throws InterruptedException {
        // Given
        int subscriberCount = 3;
        CountDownLatch latch = new CountDownLatch(subscriberCount);
        AtomicInteger totalReceived = new AtomicInteger(0);

        MessageHandler handler = message -> {
            totalReceived.incrementAndGet();
            latch.countDown();
        };

        String topic = "test.topic";
        for (int i = 0; i < subscriberCount; i++) {
            messageBus.subscribe(topic, handler);
        }

        AgentMessage message = AgentMessage.builder()
                .type(MessageType.TASK_RESPONSE)
                .fromAgentId("test-agent")
                .payload("完成消息")
                .build();

        // When
        messageBus.publish(topic, message);

        // Then
        boolean allReceived = latch.await(1, TimeUnit.SECONDS);
        assertThat(allReceived).isTrue();
        assertThat(totalReceived.get()).isEqualTo(subscriberCount);
    }

    @Test
    @DisplayName("取消订阅 - 不再收到消息")
    void testUnsubscribe_NoLongerReceivesMessages() throws InterruptedException {
        // Given
        AtomicInteger receivedCount = new AtomicInteger(0);
        MessageHandler handler = message -> receivedCount.incrementAndGet();

        String topic = "test.topic";
        messageBus.subscribe(topic, handler);

        // 发送第一条消息
        messageBus.publish(topic, AgentMessage.builder()
                .type(MessageType.TASK_REQUEST)
                .fromAgentId("test-agent")
                .build());

        Thread.sleep(100);

        // When - 取消订阅
        messageBus.unsubscribe(topic, handler);

        // 发送第二条消息
        messageBus.publish(topic, AgentMessage.builder()
                .type(MessageType.TASK_REQUEST)
                .fromAgentId("test-agent")
                .build());

        Thread.sleep(100);

        // Then - 只收到第一条消息
        assertThat(receivedCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("异步处理 - 不阻塞发布者")
    void testAsynchronousProcessing_DoesNotBlockPublisher() {
        // Given
        MessageHandler slowHandler = message -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        String topic = "test.topic";
        messageBus.subscribe(topic, slowHandler);

        // When
        long startTime = System.currentTimeMillis();
        messageBus.publish(topic, AgentMessage.builder()
                .type(MessageType.TASK_REQUEST)
                .fromAgentId("test-agent")
                .build());
        long duration = System.currentTimeMillis() - startTime;

        // Then - 发布应该立即返回
        assertThat(duration).isLessThan(100);
    }

    @Test
    @DisplayName("消息内容 - 正确传递")
    void testMessageContent_CorrectlyTransferred() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        String[] receivedPayload = new String[1];

        String topic = "test.topic";
        messageBus.subscribe(topic, message -> {
            receivedPayload[0] = (String) message.getPayload();
            latch.countDown();
        });

        String expectedPayload = "测试内容";
        AgentMessage message = AgentMessage.builder()
                .type(MessageType.TASK_REQUEST)
                .fromAgentId("test-agent")
                .payload(expectedPayload)
                .build();

        // When
        messageBus.publish(topic, message);

        // Then
        latch.await(1, TimeUnit.SECONDS);
        assertThat(receivedPayload[0]).isEqualTo(expectedPayload);
    }

    @Test
    @DisplayName("并发发布 - 线程安全")
    void testConcurrentPublish_ThreadSafe() throws InterruptedException {
        // Given
        int messageCount = 100;
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicInteger receivedCount = new AtomicInteger(0);

        String topic = "test.topic";
        messageBus.subscribe(topic, message -> {
            receivedCount.incrementAndGet();
            latch.countDown();
        });

        // When - 并发发布消息
        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            new Thread(() -> {
                messageBus.publish(topic, AgentMessage.builder()
                        .type(MessageType.TASK_REQUEST)
                        .fromAgentId("agent-" + index)
                        .build());
            }).start();
        }

        // Then
        boolean allReceived = latch.await(5, TimeUnit.SECONDS);
        assertThat(allReceived).isTrue();
        assertThat(receivedCount.get()).isEqualTo(messageCount);
    }
}
