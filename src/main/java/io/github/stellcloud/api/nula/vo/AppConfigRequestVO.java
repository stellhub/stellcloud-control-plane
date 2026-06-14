package io.github.stellcloud.api.nula.vo;

import jakarta.validation.constraints.NotBlank;

/** 应用配置编辑页面请求视图对象。 */
public record AppConfigRequestVO(
        String id,
        String appId,
        @NotBlank String name,
        String description,
        @NotBlank String environment,
        @NotBlank String cluster,
        @NotBlank String format,
        @NotBlank String content,
        String updatedBy) {}
