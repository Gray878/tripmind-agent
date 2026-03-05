package com.hgh.tripmindagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置
 * 
 * @author hgh
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI tripMindOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TripMind 旅游规划 API")
                        .description("基于多智能体架构的智能旅游规划系统")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("TripMind Team")
                                .email("support@tripmind.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
