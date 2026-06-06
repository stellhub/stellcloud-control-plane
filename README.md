# StellCloud Control Plane

StellCloud Control Plane is the backend control-plane layer for the StellCloud platform.

It integrates Stellnula Service and Stellorbit Service into a unified control surface for frontend consoles, platform APIs, and cross-product governance workflows.

## Position in the Platform

```text
stellcloud-web
      |
stellcloud-control-plane
      |
      +-- stellnula-service
      +-- stellorbit-service
```

## Responsibilities

- Provide unified OpenAPI-facing control-plane APIs for StellCloud frontend workflows.
- Integrate Stellnula configuration-center capabilities with Stellorbit service-governance capabilities.
- Coordinate configuration, routing, load, retry, traffic shifting, and lifecycle-governance workflows.
- Normalize authentication, authorization, audit, and policy-management boundaries across product services.
- Serve as the platform integration layer for future Stell middleware control-plane capabilities.

## Repository Role

This repository contains the StellCloud backend control plane that connects configuration management and service governance into one platform-level API surface.
