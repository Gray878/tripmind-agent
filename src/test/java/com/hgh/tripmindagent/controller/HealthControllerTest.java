package com.hgh.tripmindagent.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HealthController 单元测试
 * 
 * <p>测试健康检查接口</p>
 * 
 * @author hgh
 */
@WebMvcTest(HealthController.class)
@DisplayName("健康检查控制器测试")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("简单健康检查 - 返回ok")
    void testHealthCheck_ReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @DisplayName("详细健康检查 - 返回详细信息")
    void testHealthDetail_ReturnsDetailedInfo() throws Exception {
        mockMvc.perform(get("/health/detail"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.system.javaVersion").exists())
                .andExpect(jsonPath("$.system.osName").exists())
                .andExpect(jsonPath("$.system.processors").exists())
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.components.database").value("UP"))
                .andExpect(jsonPath("$.components.vectorStore").value("UP"))
                .andExpect(jsonPath("$.components.aiModel").value("UP"));
    }
}
