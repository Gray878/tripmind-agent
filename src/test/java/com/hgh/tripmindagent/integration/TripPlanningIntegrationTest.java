package com.hgh.tripmindagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.controller.model.TripRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 旅游规划集成测试
 * 
 * <p>端到端测试完整的旅游规划流程</p>
 * 
 * @author hgh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("旅游规划集成测试")
class TripPlanningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("端到端测试 - 完整旅游规划流程")
    void testEndToEnd_CompleteTripPlanning() throws Exception {
        // Given
        TripRequest request = new TripRequest();
        request.setUserPrompt("我想去北京旅游3天，预算5000元，喜欢历史文化");
        request.setUserId("integration_test_user");

        // When
        MvcResult result = mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        AgentResult agentResult = objectMapper.readValue(responseBody, AgentResult.class);

        assertThat(agentResult).isNotNull();
        assertThat(agentResult.isSuccess()).isTrue();
        assertThat(agentResult.getResult()).isNotEmpty();
        assertThat(agentResult.getResult()).contains("北京");
        assertThat(agentResult.getStepCount()).isGreaterThan(0);
        assertThat(agentResult.getDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("健康检查 - 系统正常运行")
    void testHealthCheck_SystemRunning() throws Exception {
        mockMvc.perform(get("/health"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("详细健康检查 - 返回系统信息")
    void testHealthDetail_ReturnsSystemInfo() throws Exception {
        mockMvc.perform(get("/health/detail"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.components").exists());
    }

    @Test
    @DisplayName("国内游规划 - 多个城市")
    void testDomesticTrip_MultipleCities() throws Exception {
        // 测试北京
        testTripPlan("我想去北京旅游3天", "北京");

        // 测试上海
        testTripPlan("我想去上海旅游4天", "上海");

        // 测试成都
        testTripPlan("我想去成都旅游5天", "成都");
    }

    @Test
    @DisplayName("不同预算 - 正确处理")
    void testDifferentBudgets_CorrectHandling() throws Exception {
        // 低预算
        testTripPlan("我想去杭州旅游3天，预算3000元", "杭州");

        // 中预算
        testTripPlan("我想去西安旅游4天，预算6000元", "西安");

        // 高预算
        testTripPlan("我想去三亚旅游5天，预算15000元", "三亚");
    }

    @Test
    @DisplayName("不同偏好 - 个性化推荐")
    void testDifferentPreferences_PersonalizedRecommendations() throws Exception {
        // 历史文化
        testTripPlan("我想去西安旅游3天，喜欢历史文化", "西安");

        // 美食购物
        testTripPlan("我想去成都旅游3天，喜欢美食和购物", "成都");

        // 自然风光
        testTripPlan("我想去桂林旅游3天，喜欢自然风光", "桂林");
    }

    /**
     * 辅助方法：测试旅游规划
     */
    private void testTripPlan(String userPrompt, String expectedDestination) throws Exception {
        TripRequest request = new TripRequest();
        request.setUserPrompt(userPrompt);
        request.setUserId("test_user");

        MvcResult result = mockMvc.perform(post("/api/trip/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AgentResult agentResult = objectMapper.readValue(responseBody, AgentResult.class);

        assertThat(agentResult.isSuccess()).isTrue();
        assertThat(agentResult.getResult()).contains(expectedDestination);
    }
}
