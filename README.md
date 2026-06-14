# StellCloud Control Plane

StellCloud Control Plane 是 StellCloud 平台的统一后端控制面。项目基于
[stellflux](https://github.com/stellhub/stellflux) 框架开发，向上对接
`stellcloud-web` 前端控制台，向下整合 `stellorbit-service`、`stellnula-service`、
`stellflow-service`、`stellmap-service`，为多个 Stell 中间件产品提供统一的
OpenAPI、CRUD、审计、鉴权和运维治理入口。

## 1. Problem Analysis

StellCloud 需要把多个独立后端能力整合成一个平台级控制面：

- `stellcloud-web` 需要稳定、统一、可文档化的 REST API，而不是直接感知每个后端服务的内部接口差异。
- `stellorbit-service`、`stellnula-service`、`stellflow-service`、`stellmap-service`
  分别承载治理、配置、消息/流程、注册发现等能力，需要统一资源模型和统一错误语义。
- 前端页面首先需要基础 CRUD 能力，因此控制面应先完成列表、详情、创建、更新、删除、启停、发布、回滚等通用操作，再逐步扩展复杂治理流程。
- 控制面必须保留后端服务边界，避免把业务规则复制到前端，也避免把所有下游接口简单透传成无约束网关。

## 2. Design

### Platform Position

```text
stellcloud-web
      |
      v
stellcloud-control-plane
      |
      +-- stellorbit-service
      +-- stellnula-service
      +-- stellflow-service
      +-- stellmap-service
```

### Architecture

```text
┌─────────────────────────────┐
│        stellcloud-web        │
│  Console / CRUD / OpenAPI    │
└──────────────┬──────────────┘
               │
               v
┌─────────────────────────────┐
│ stellcloud-control-plane     │
│                             │
│  api        REST Controllers │
│  app        Use cases        │
│  domain     Resource models  │
│  adapter    Downstream SDKs  │
│  config     Stellflux config │
└──────┬────────┬────────┬─────┘
       │        │        │
       v        v        v
stellorbit  stellnula  stellflow
       │
       v
   stellmap
```

### Framework Plan

控制面优先沿用 stellflux 已有 Spring Boot starter 和自动装配模式：

- `stellflux-spring-boot-starter-http`：提供 HTTP Server + HTTP Client 基础能力。
- `stellflux-spring-boot-starter-stellnula`：对接配置中心能力。
- `stellflux-spring-boot-starter-stellflow`：对接消息、事件流或流程编排能力。
- `stellflux-spring-boot-starter-stellmap`：对接注册中心、服务发现和实例治理能力。
- `stellflux-spring-boot-starter-scheduler-stellmap`：需要平台任务调度归属判断时接入。
- `stellflux-spring-boot-starter-log`、`stellflux-spring-boot-starter-metrics`、
  `stellflux-spring-boot-starter-traces`：提供控制面自身可观测性。

### Downstream Boundaries

| Downstream service | Control-plane domain | Initial capability |
| --- | --- | --- |
| `stellorbit-service` | Service governance | 应用、路由、流量策略、限流、熔断、灰度发布 |
| `stellnula-service` | Configuration center | 命名空间、配置项、配置版本、发布、回滚 |
| `stellflow-service` | Event and flow platform | Topic、生产者、消费者、消费组、工作流、订阅 |
| `stellmap-service` | Registry and discovery | 服务、实例、健康状态、租约、调度任务归属 |

### API Principles

- API 前缀统一使用 `/api/stellcloud/control-plane/v1`。
- 面向前端返回统一响应结构、分页结构、错误码和 trace id。
- 每个资源提供基础 CRUD，复杂动作使用明确的 action endpoint。
- 下游服务异常在控制面统一映射，避免把底层 SDK 异常直接暴露给前端。
- 所有写操作预留操作者、租户、环境、应用上下文和审计字段。

### Initial REST Resource Plan

```text
GET    /api/stellcloud/control-plane/v1/overview
GET    /api/stellcloud/control-plane/v1/backends
GET    /api/stellcloud/control-plane/v1/backends/{backend}/health

GET    /api/stellcloud/control-plane/v1/orbit/applications
POST   /api/stellcloud/control-plane/v1/orbit/applications
GET    /api/stellcloud/control-plane/v1/orbit/applications/{applicationId}
PUT    /api/stellcloud/control-plane/v1/orbit/applications/{applicationId}
DELETE /api/stellcloud/control-plane/v1/orbit/applications/{applicationId}

GET    /api/stellcloud/control-plane/v1/nula/namespaces
POST   /api/stellcloud/control-plane/v1/nula/namespaces
GET    /api/stellcloud/control-plane/v1/nula/configs
POST   /api/stellcloud/control-plane/v1/nula/configs
PUT    /api/stellcloud/control-plane/v1/nula/configs/{configId}
DELETE /api/stellcloud/control-plane/v1/nula/configs/{configId}
POST   /api/stellcloud/control-plane/v1/nula/configs/{configId}/publish
POST   /api/stellcloud/control-plane/v1/nula/configs/{configId}/rollback

GET    /api/stellcloud/control-plane/v1/flow/topics
POST   /api/stellcloud/control-plane/v1/flow/topics
GET    /api/stellcloud/control-plane/v1/flow/topics/{topicName}
PUT    /api/stellcloud/control-plane/v1/flow/topics/{topicName}
DELETE /api/stellcloud/control-plane/v1/flow/topics/{topicName}
GET    /api/stellcloud/control-plane/v1/flow/consumer-groups

GET    /api/stellcloud/control-plane/v1/map/services
POST   /api/stellcloud/control-plane/v1/map/services
GET    /api/stellcloud/control-plane/v1/map/services/{serviceName}
PUT    /api/stellcloud/control-plane/v1/map/services/{serviceName}
DELETE /api/stellcloud/control-plane/v1/map/services/{serviceName}
GET    /api/stellcloud/control-plane/v1/map/services/{serviceName}/instances
GET    /api/stellcloud/control-plane/v1/map/scheduler/tasks/{taskName}
```

### Frontend Contract

`stellcloud-web` 只依赖控制面的平台 API：

- `overview` 页面读取平台总览、后端健康状态和资源统计。
- `orbit` 页面管理治理资源。
- `nula` 页面管理配置资源。
- `flow` 页面管理消息、订阅和工作流资源。
- `map` 页面管理服务注册、实例状态和调度任务归属。
- 所有页面通过 `application`、`environment`、`tenant`、`role` 等上下文字段过滤资源。

## 3. Implementation

### Suggested Package Layout

```text
src/main/java/io/github/stellhub/stellcloud/controlplane
├── StellCloudControlPlaneApplication.java
├── api
│   ├── common
│   ├── overview
│   ├── orbit
│   ├── nula
│   ├── flow
│   └── map
├── application
│   ├── overview
│   ├── orbit
│   ├── nula
│   ├── flow
│   └── map
├── domain
│   ├── common
│   ├── orbit
│   ├── nula
│   ├── flow
│   └── map
├── infrastructure
│   ├── client
│   ├── config
│   ├── error
│   ├── security
│   └── audit
└── support
    ├── pagination
    ├── response
    └── validation
```

### Milestones

1. 初始化 Spring Boot + stellflux 项目骨架，补齐 `pom.xml`、启动类、配置文件和健康检查接口。
2. 定义统一响应、分页、错误码、审计上下文和 OpenAPI 分组。
3. 接入 `stellorbit-service`，完成应用、路由、策略等基础 CRUD。
4. 接入 `stellnula-service`，完成命名空间、配置项、版本发布和回滚接口。
5. 接入 `stellflow-service`，完成 Topic、消费组、订阅、工作流基础管理接口。
6. 接入 `stellmap-service`，完成服务、实例、健康状态和调度任务查询接口。
7. 为 `stellcloud-web` 输出稳定 API 文档，并补充联调样例、错误码说明和开发环境配置。

### Runtime Configuration Draft

```yaml
server:
  port: 18080

stellcloud:
  control-plane:
    api-prefix: /api/stellcloud/control-plane/v1
    downstream:
      stellorbit:
        base-url: http://127.0.0.1:18081
      stellnula:
        base-url: http://127.0.0.1:18082
      stellflow:
        base-url: http://127.0.0.1:18083
      stellmap:
        base-url: http://127.0.0.1:18084
```

## 4. Complete Code

当前阶段先完成 README 总体规划。后续代码实现应遵循：

- Spring Boot 风格，优先构造器注入。
- 使用 Lombok 简化 DTO、配置属性和领域对象。
- 方法注释使用中文，复杂代码块注释使用英文。
- 修改现有代码时仅改必要部分，保持原有逻辑不变。
- 每个控制器提供完整可运行实现，不提交伪代码或省略 imports 的片段。

## Repository Role

This repository contains the StellCloud backend control plane that connects
StellCloud Web with Stell middleware services through a stable platform-level
API surface.
