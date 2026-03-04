package com.hgh.tripmindagent.controller;

import com.hgh.tripmindagent.agent.base.AgentRequest;
import com.hgh.tripmindagent.agent.base.AgentResult;
import com.hgh.tripmindagent.agent.orchestrator.TripMindOrchestrator;
import com.hgh.tripmindagent.controller.model.TripRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 旅游规划控制器
 * 
 * @author TripMind Team
 */
@Slf4j
@RestController
@RequestMapping("/api/trip")
@Tag(name = "旅游规划", description = "旅游规划相关接口")
public class TripPlanController {
    
    @Autowired
    private TripMindOrchestrator orchestrator;
    
    /**
     * 同步创建旅游规划
     */
    @PostMapping("/plan")
    @Operation(summary = "创建旅游规划", description = "同步执行，返回完整结果")
    public AgentResult createPlan(
            @RequestBody @Parameter(description = "旅游规划请求") TripRequest request) {
        
        log.info("收到旅游规划请求: {}", request.getUserPrompt());
        request.validate();
        
        AgentRequest agentRequest = AgentRequest.builder()
            .userPrompt(request.getUserPrompt())
            .userId(request.getUserId())
            .params(request.getParams())
            .build();
        
        return orchestrator.run(agentRequest);
    }
    
    /**
     * 流式创建旅游规划（SSE）
     */
    @PostMapping("/plan/stream")
    @Operation(summary = "流式创建旅游规划", description = "实时返回执行进度")
    public SseEmitter createPlanStream(
            @RequestBody @Parameter(description = "旅游规划请求") TripRequest request) {
        
        log.info("收到流式旅游规划请求: {}", request.getUserPrompt());
        request.validate();
        
        AgentRequest agentRequest = AgentRequest.builder()
            .userPrompt(request.getUserPrompt())
            .userId(request.getUserId())
            .params(request.getParams())
            .streamEnabled(true)
            .build();
        
        return orchestrator.runStream(agentRequest);
    }
    
    /**
     * 获取旅游规划
     */
    @GetMapping("/plan/{planId}")
    @Operation(summary = "获取旅游规划", description = "根据ID获取已生成的规划")
    public AgentResult getPlan(@PathVariable String planId) {
        // TODO: 从数据库或缓存中获取
        log.info("查询旅游规划: {}", planId);
        return AgentResult.failure("功能开发中");
    }
    
    /**
     * 下载 PDF
     */
    @GetMapping("/plan/{planId}/pdf")
    @Operation(summary = "下载PDF", description = "下载旅游规划PDF文件")
    public AgentResult downloadPdf(@PathVariable String planId) {
        // TODO: 使用 PDFGenerationTool 生成 PDF
        log.info("下载PDF: {}", planId);
        return AgentResult.failure("功能开发中");
    }
}
