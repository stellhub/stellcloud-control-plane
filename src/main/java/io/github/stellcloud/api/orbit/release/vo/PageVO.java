package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;

/** 分页结果页面视图对象。 */
public record PageVO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
