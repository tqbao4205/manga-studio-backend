package com.mangaflow.studio.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MangaFlow API")
                        .version("1.0")
                        .contact(new Contact()
                                .email("team@mangaflow.com")
                                .name("MangaFlow Team")))
                .tags(Arrays.asList(
                        new Tag()
                                .name("1. Series Creation")
                                .description("Mangaka tạo series mới — thông tin cơ bản, nhân vật, cốt truyện, chapter, page, sắp xếp & lưu nháp"),
                        new Tag()
                                .name("2. Series Review")
                                .description("Tantou duyệt series — mời tantou, submit, đánh giá, comment, approve/reject"),
                        new Tag()
                                .name("3. Board Review & Publication")
                                .description("Editorial Board duyệt series — meeting, vote, decision, lịch xuất bản"),
                        new Tag()
                                .name("4. Task Collaboration")
                                .description("Mangaka tạo & giao task, Assistant nhận, nộp bài, Mangaka duyệt"),
                        new Tag()
                                .name("5. Chapter Review")
                                .description("Mangaka submit chapter, Tantou duyệt — feedback, approve, request revision"),
                        new Tag()
                                .name("6. Publication & Ranking")
                                .description("Xuất bản chapter, xếp hạng tuần/tháng, series at-risk, thông báo")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
