package com.example.codegardener.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // API 문서의 제목, 버전 등 기본 정보 설정
        Info info = new Info()
                .title("Code Garden API Document")
                .version("v0.1")
                .description("Code Garden 프로젝트의 API 명세서입니다.");

        // 인증 방식(Security Scheme) 설정
        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // 인증 타입: HTTP
                        .scheme("bearer")               // 스킴: Bearer
                        .bearerFormat("JWT"));          // 포맷: JWT

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}