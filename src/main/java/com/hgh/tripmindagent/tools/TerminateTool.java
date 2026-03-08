package com.hgh.tripmindagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * Explicit termination tool for tool-calling loops.
 */
public class TerminateTool {

    @Tool(description = "Terminate when the request is complete or the assistant cannot continue.")
    public String doTerminate() {
        return "任务结束";
    }
}
