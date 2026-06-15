package io.github.stellcloud.api.nula.vo;

import jakarta.validation.constraints.NotBlank;

/** 公共配置编辑页面请求视图对象。 */
public record CommonConfigRequestVO(
        String id,
        String ownerId,
        @NotBlank String name,
        String description,
        @NotBlank String environment,
        @NotBlank String cluster,
        String group,
        @NotBlank String format,
        @NotBlank String content,
        String updatedBy) {}
