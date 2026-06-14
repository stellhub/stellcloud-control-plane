package io.github.stellcloud.api.nula.vo;

import java.util.List;

/** 通用配置页面视图对象。 */
public record SharedConfigPageVO(
        String title,
        String subtitle,
        List<MetricVO> metrics,
        String tableTitle,
        TableVO table,
        List<String> actions) {}
