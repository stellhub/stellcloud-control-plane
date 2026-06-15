package io.github.stellcloud.api.atlas.vo;

import java.util.List;

/** CMDB 应用列表页面视图对象。 */
public record ApplicationListVO(List<ApplicationVO> records, int count, int limit, int offset) {}
