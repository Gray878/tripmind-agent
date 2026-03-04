package com.hgh.tripmindagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.agent.orchestrator.TripMindOrchestrator;
import com.hgh.tripmindagent.controller.model.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TripPlanController 单元测试
 * 
 * <p>测试旅游规划控制器的各个接口功能</p>
 * 
 * @author hgh
 */
@WebMvcTest(TripPlanController.class)
@DisplayName("旅游规划控制器测试")
class TripPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripMindOrchestrator orchestrator;

    private TripRequest validRequest;
    private AgentResult successResult;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        validRequest = new TripRequest();
        validRequest.setUserPrompt("我想去日本东京旅游3天，预算8000元");
        validRequest.setUserId("test_user");

        successResult = AgentResult.builder()
                .success(true)
                .result("# 东京3日游规划\n\n## 第一天\n...")
                .stepCount(5)
                .tokenUsage(1500)
                .duration(10000)
                .build();
    }

    @Test
    @DisplayName("同步创建旅游规划 - 成功")
    void testCreatePlan_Success() throws Exception {
        // Given
        when(orchestrator.run(any(AgentRequest.class))).thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.stepCount").value(5))
                .andExpect(jsonPath("$.tokenUsage").value(1500))
                .andExpect(jsonPath("$.duration").value(10000));
    }

    @Test
    @DisplayName("同步创建旅游规划 - 参数为空")
    void testCreatePlan_EmptyPrompt() throws Exception {
        // Given
        TripRequest emptyRequest = new TripRequest();
        emptyRequest.setUserPrompt("");
        emptyRequest.setUserId("test_user");

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("同步创建旅游规划 - 参数为null")
    void testCreatePlan_NullPrompt() throws Exception {
        // Given
        TripRequest nullRequest = new TripRequest();
        nullRequest.setUserPrompt(null);
        nullRequest.setUserId("test_user");

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullRequest)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("同步创建旅游规划 - 执行失败")
    void testCreatePlan_ExecutionFailure() throws Exception {
        // Given
        AgentResult failureResult = AgentResult.builder()
                .success(false)
                .errorMessage("智能体执行超时")
                .build();
        when(orchestrator.run(any(AgentRequest.class))).thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("智能体执行超时"));
    }

    @Test
    @DisplayName("同步创建旅游规划 - 无userId")
    void testCreatePlan_WithoutUserId() throws Exception {
        // Given
        TripRequest requestWithoutUserId = new TripRequest();
        requestWithoutUserId.setUserPrompt("我想去北京旅游3天");
        when(orchestrator.run(any(AgentRequest.class))).thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutUserId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("流式创建旅游规划 - 成功")
    void testCreatePlanStream_Success() throws Exception {
        // Given
        SseEmitter emitter = new SseEmitter();
        when(orchestrator.runStream(any(AgentRequest.class))).thenReturn(emitter);

        // When & Then
        mockMvc.perform(post("/api/trip/plan/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("流式创建旅游规划 - 参数为空")
    void testCreatePlanStream_EmptyPrompt() throws Exception {
        // Given
        TripRequest emptyRequest = new TripRequest();
        emptyRequest.setUserPrompt("");

        // When & Then
        mockMvc.perform(post("/api/trip/plan/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("获取旅游规划 - 功能开发中")
    void testGetPlan_NotImplemented() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/trip/plan/plan_123456"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("功能开发中"));
    }

    @Test
    @DisplayName("下载PDF - 功能开发中")
    void testDownloadPdf_NotImplemented() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/trip/plan/plan_123456/pdf"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("功能开发中"));
    }

    @Test
    @DisplayName("同步创建旅游规划 - 复杂需求")
    void testCreatePlan_ComplexRequest() throws Exception {
        // Given
        TripRequest complexRequest = new TripRequest();
        complexRequest.setUserPrompt("我想去日本东京旅游5天，预算12000元，喜欢美食和购物，3月15日出发，需要推荐酒店和交通方式");
        complexRequest.setUserId("test_user");
        complexRequest.getParams().put("language", "zh-CN");
        complexRequest.getParams().put("currency", "CNY");

        when(orchestrator.run(any(AgentRequest.class))).thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complexRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("同步创建旅游规划 - 国内游")
    void testCreatePlan_DomesticTrip() throws Exception {
        // Given
        TripRequest domesticRequest = new TripRequest();
        domesticRequest.setUserPrompt("我想去北京旅游3天，预算5000元，喜欢历史文化");
        domesticRequest.setUserId("test_user");

        when(orchestrator.run(any(AgentRequest.class))).thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(domesticRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
