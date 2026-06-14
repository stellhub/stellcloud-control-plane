package io.github.stellcloud.api.nula.vo;

import java.util.List;
import java.util.Map;

/** 应用配置范围页面视图对象。 */
public record AppConfigScopeVO(List<String> environments, Map<String, List<String>> clustersByEnvironment) {}
