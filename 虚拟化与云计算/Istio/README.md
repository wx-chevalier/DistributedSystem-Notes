# Istio

Istio 是由 Google、IBM、Lyft 等共同开源的 Service Mesh（服务网格）框架，于 2017 年初开始进入大众视野，作为云原生时代下承 Kubernetes、上接 Serverless 架构的重要基础设施层，地位至关重要。

使用 Istio 可以很简单的创建具有负载均衡、服务间认证、监控等功能的服务网络，而不需要对服务的代码进行任何修改。你只需要在部署环境中，例如 Kubernetes 的 pod 里注入一个特别的 sidecar proxy 来增加对 Istio 的支持，用来截获微服务之间的网络流量。

![](https://res.cloudinary.com/jimmysong/image/upload/images/istio-arch.jpg)

# Architecture Overview | 架构概述

Istio 架构分为控制层和数据层。

- 数据层：由一组智能代理（Envoy）作为 sidecar 部署，协调和控制所有 microservices 之间的网络通信。
- 控制层：负责管理和配置代理路由流量，以及在运行时执行的政策。

## Envoy

Istio 使用 Envoy 代理的扩展版本，该代理是以 C++开发的高性能代理，用于调解 service mesh 中所有服务的所有入站和出站流量。 Istio 利用了 Envoy 的许多内置功能，例如动态服务发现，负载平衡，TLS 终止，HTTP/2＆gRPC 代理，断路器，运行状况检查，基于百分比的流量拆分分阶段上线，故障注入和丰富指标。

Envoy 在 kubernetes 中作为 pod 的 sidecar 来部署。 这允许 Istio 将大量关于流量行为的信号作为属性提取出来，这些属性又可以在 Mixer 中用于执行策略决策，并发送给监控系统以提供有关整个 mesh 的行为的信息。 Sidecar 代理模型还允许你将 Istio 功能添加到现有部署中，无需重新构建或重写代码。 更多信息参见设计目标。

## Mixer

Mixer 负责在 service mesh 上执行访问控制和使用策略，并收集 Envoy 代理和其他服务的遥测数据。代理提取请求级属性，发送到 mixer 进行评估。有关此属性提取和策略评估的更多信息，请参见 Mixer 配置。 混音器包括一个灵活的插件模型，使其能够与各种主机环境和基础架构后端进行接口，从这些细节中抽象出 Envoy 代理和 Istio 管理的服务。

## Istio Manager

Istio-Manager 用作用户和 Istio 之间的接口，收集和验证配置，并将其传播到各种 Istio 组件。它从 Mixer 和 Envoy 中抽取环境特定的实现细节，为他们提供独立于底层平台的用户服务的抽象表示。 此外，流量管理规则（即通用 4 层规则和七层 HTTP/gRPC 路由规则）可以在运行时通过 Istio-Manager 进行编程。

## Istio-auth

Istio-Auth 提供强大的服务间和最终用户认证，使用相互 TLS，内置身份和凭据管理。它可用于升级 service mesh 中的未加密流量，并为运营商提供基于服务身份而不是网络控制的策略的能力。 Istio 的未来版本将增加细粒度的访问控制和审计，以使用各种访问控制机制（包括属性和基于角色的访问控制以及授权 hook）来控制和监控访问你服务、API 或资源的人员。
