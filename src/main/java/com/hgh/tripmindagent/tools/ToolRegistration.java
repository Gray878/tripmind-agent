package com.hgh.tripmindagent.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 * 
 * 注意：工具类使用 @Tool 注解标记方法，Spring AI 会自动将其转换为 ToolCallback
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 文件操作工具
     */
    @Bean
    public FileOperationTool fileOperationTool() {
        return new FileOperationTool();
    }

    /**
     * 网络搜索工具
     */
    @Bean
    public WebSearchTool webSearchTool() {
        return new WebSearchTool(searchApiKey);
    }

    /**
     * 网页抓取工具
     */
    @Bean
    public WebScrapingTool webScrapingTool() {
        return new WebScrapingTool();
    }

    /**
     * 资源下载工具
     */
    @Bean
    public ResourceDownloadTool resourceDownloadTool() {
        return new ResourceDownloadTool();
    }

    /**
     * 终端操作工具
     */
    @Bean
    public TerminalOperationTool terminalOperationTool() {
        return new TerminalOperationTool();
    }

    /**
     * PDF 生成工具
     */
    @Bean
    public PDFGenerationTool pdfGenerationTool() {
        return new PDFGenerationTool();
    }

    /**
     * 终止工具
     */
    @Bean
    public TerminateTool terminateTool() {
        return new TerminateTool();
    }
    
    /**
     * 所有工具的数组（避免集合注入时出现 List 嵌套）
     */
    @Bean
    public Object[] allAgentTools(
            FileOperationTool fileOperationTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            ResourceDownloadTool resourceDownloadTool,
            TerminalOperationTool terminalOperationTool,
            PDFGenerationTool pdfGenerationTool,
            TerminateTool terminateTool) {
        return new Object[] {
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        };
    }
}
