package io.github.stellcloud.api.nula.vo;

import java.util.List;

/** 标准模块表格页面视图对象。 */
public record TableVO(List<String> columns, List<List<String>> rows) {}
