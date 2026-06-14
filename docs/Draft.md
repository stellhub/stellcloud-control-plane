# StellCloud Control Plane Interface Draft

本文档记录当前统一控制面接入四个下游服务时发现的页面化接口现状与缺口。已经存在的 HTTP 接口已在当前项目中通过 `stellflux-http-client` 进行封装调用；缺少的接口先沉淀在本文档中，后续需要回到对应服务补齐。

## 已封装的下游接口

### 服务治理：stellorbit-service

当前服务已存在面向控制面的 Spring MVC HTTP 接口，控制面统一代理前缀为：

```text
/api/stellcloud/control-plane/v1/orbit/**
```

路径映射规则：

```text
/api/stellcloud/control-plane/v1/orbit/applications
  -> /api/stellorbit/applications

/api/stellcloud/control-plane/v1/orbit/rules/routes
  -> /api/stellorbit/rules/routes

/api/stellcloud/control-plane/v1/orbit/rules/rate-limits
  -> /api/stellorbit/rules/rate-limits

/api/stellcloud/control-plane/v1/orbit/rules/breakers
  -> /api/stellorbit/rules/breakers

/api/stellcloud/control-plane/v1/orbit/rules/auth
  -> /api/stellorbit/rules/auth

/api/stellcloud/control-plane/v1/orbit/rule-releases
  -> /api/stellorbit/rule-releases
```

### 配置中心：stellnula-service

当前服务已存在配置、灰度规则、治理规则和数据面节点接口，控制面统一代理前缀为：

```text
/api/stellcloud/control-plane/v1/nula/**
```

路径映射规则：

```text
/api/stellcloud/control-plane/v1/nula/configs/{configId}
  -> /api/v1/configs/{configId}

/api/stellcloud/control-plane/v1/nula/configs/{configId}/gray-rules/{grayName}
  -> /api/v1/configs/{configId}/gray-rules/{grayName}

/api/stellcloud/control-plane/v1/nula/data-plane/nodes
  -> /api/v1/data-plane/nodes

/api/stellcloud/control-plane/v1/nula/governance/rules
  -> /api/v1/governance/rules
```

### 注册中心：stellmap-service

当前服务已存在注册数据面与 admin 查询接口，控制面统一代理前缀为：

```text
/api/stellcloud/control-plane/v1/map/**
```

路径映射规则：

```text
/api/stellcloud/control-plane/v1/map/registry/instances
  -> /api/v1/registry/instances

/api/stellcloud/control-plane/v1/map/registry/register
  -> /api/v1/registry/register

/api/stellcloud/control-plane/v1/map/registry/deregister
  -> /api/v1/registry/deregister

/api/stellcloud/control-plane/v1/map/registry/heartbeat
  -> /api/v1/registry/heartbeat

/api/stellcloud/control-plane/v1/map/admin/v1/status
  -> /admin/v1/status

/api/stellcloud/control-plane/v1/map/admin/v1/replication/status
  -> /admin/v1/replication/status
```

## 缺少的页面化接口

### 消息队列：stellflow-service

`stellflow-service` 当前主要暴露 broker 二进制协议和 controller/broker gRPC 控制链路，已能处理 `CREATE_TOPIC`、`DELETE_TOPIC`、`ALTER_PARTITION`、`DESCRIBE_CLUSTER` 等内部协议请求，但缺少可被 `stellcloud-web` 直接消费的 REST 管理接口。

建议在 `stellflow-service` 中新增以下 HTTP API：

```text
GET    /api/stellflow/admin/cluster
GET    /api/stellflow/admin/brokers
GET    /api/stellflow/admin/topics
POST   /api/stellflow/admin/topics
GET    /api/stellflow/admin/topics/{topicName}
DELETE /api/stellflow/admin/topics/{topicName}
GET    /api/stellflow/admin/topics/{topicName}/partitions
PATCH  /api/stellflow/admin/topics/{topicName}/partitions/{partitionId}
GET    /api/stellflow/admin/consumer-groups
GET    /api/stellflow/admin/consumer-groups/{groupId}
GET    /api/stellflow/admin/consumer-groups/{groupId}/offsets
```

控制面预期映射：

```text
/api/stellcloud/control-plane/v1/flow/**
  -> /api/stellflow/admin/**
```

### 配置中心：stellnula-service

当前 `ConfigManagementController` 支持按 `configId` 查询、更新、删除和复制发布，但缺少页面列表和命名空间管理接口。

建议补充：

```text
GET    /api/v1/configs
GET    /api/v1/namespaces
POST   /api/v1/namespaces
PUT    /api/v1/namespaces/{namespace}
DELETE /api/v1/namespaces/{namespace}
GET    /api/v1/configs/{configId}/revisions
GET    /api/v1/configs/{configId}/releases
POST   /api/v1/configs/{configId}/rollback
```

### 注册中心：stellmap-service

当前 `stellmap-service` 更偏实例注册、实例查询和 watch，缺少面向页面的服务目录 CRUD 与聚合查询。

建议补充：

```text
GET    /api/v1/registry/services
GET    /api/v1/registry/services/{serviceName}
PUT    /api/v1/registry/services/{serviceName}/metadata
DELETE /api/v1/registry/services/{serviceName}
GET    /api/v1/registry/services/{serviceName}/instances
GET    /api/v1/registry/namespaces
GET    /api/v1/registry/applications
```

### 服务治理：stellorbit-service

`stellorbit-service` 的基础治理 CRUD 已比较完整。后续为提升页面体验，可以补充跨资源聚合接口：

```text
GET /api/stellorbit/overview
GET /api/stellorbit/applications/{applicationId}/governance-summary
GET /api/stellorbit/runtime/nodes/summary
```
