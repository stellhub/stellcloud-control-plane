package io.github.stellcloud.api.atlas.vo;

/** CMDB 应用标准命名视图对象。 */
public record ApplicationNamingVO(
        String organization,
        String businessDomain,
        String capabilityDomain,
        String application,
        String role) {}
