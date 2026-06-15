package io.github.stellcloud.api.atlas.vo;

/** CMDB 应用删除结果视图对象。 */
public record ApplicationDeleteVO(String appId, boolean success, String message) {}
