# Sharding-Sphere

Sharding-Sphere 是一套开源的分布式数据库中间件解决方案组成的生态圈，它由 Sharding-JDBC、Sharding-Proxy 和 Sharding-Sidecar 这 3 款相互独立的产品组成。它们均提供标准化的数据分片、读写分离、柔性事务和数据治理功能，可适用于如 Java 同构、异构语言、容器、云原生等各种多样化的应用场景。

Sharding-JDBC 定位为轻量级 Java 框架，在 Java 的 JDBC 层提供的额外服务。 它使用客户端直连数据库，以 jar 包形式提供服务，无需额外部署和依赖，可理解为增强版的 JDBC 驱动，完全兼容 JDBC 和各种 ORM 框架。

![](https://i.postimg.cc/6q2kHQBR/image.png)

Sharding-Proxy 定位为透明化的数据库代理端，提供封装了数据库二进制协议的服务端版本，用于完成对异构语言的支持。 目前先提供 MySQL 版本，它可以使用任何兼容 MySQL 协议的访问客户端(如：MySQL Command Client, MySQL Workbench 等)操作数据，对 DBA 更加友好。

![](https://i.postimg.cc/SRBwHpKP/image.png)

Sharding-Sidecar 定位为 Kubernetes 或 Mesos 的云原生数据库代理，以 DaemonSet 的形式代理所有对数据库的访问。 通过无中心、零侵入的方案提供与数据库交互的的啮合层，即 Database Mesh，又可称数据网格。

![](https://i.postimg.cc/SxhKzBGV/image.png)
