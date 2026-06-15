package io.github.stellcloud.api.nula.vo;

import java.util.List;
import java.util.Map;

/** 公共配置范围页面视图对象。 */
public record CommonConfigScopeVO(
        List<String> environments,
        Map<String, List<String>> clustersByEnvironment,
        List<String> groups) {}
