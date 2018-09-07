![default](https://user-images.githubusercontent.com/5803001/45228847-daf52d00-b2f6-11e8-9367-a48c196da018.png)

# 深入浅出分布式基础架构

深入浅出分布式基础架构是笔者归档自己，在学习与实践软件分布式架构过程中的，笔记与代码的仓库；主要包含分布式计算、分布式系统、数据存储、虚拟化、网络、操作系统等几个部分。本部分详细的基础架构请参考笔者在 [2016: 我的技术体系结构图](https://github.com/wxyyxc1992/Awesome-Coder/blob/master/Knowledge-Graph/2016-Knowledge-Graph.md)一文中的描述；本仓库目前包含的主要内容分为开源项目与各个技术领域的文章。

---

✨ 技术文章

- [Linux 篇](./Linux)

* [分布式计算篇](./DistributedComputing)

- [虚拟化与容器调度篇](./Virtualization)

* [分布式存储篇](./DistributedStorage)

* [MySQL 篇](./MySQL)

* [Redis 篇](./NoSQL)

---

✨ 关联项目

- [LocalMQ #Project#](https://github.com/wxyyxc1992/InfraS-Lab/tree/master/LocalMQ):LocalMQ 是笔者模仿 RocketMQ 的设计理念实现的简化版本地高性能消息队列，可以用来了解如何从零开始构建消息队列。

- [ServicesD #Project#](https://github.com/wxyyxc1992/T-Arsenal/tree/master/ServicesD): ServicesD 是笔者在日常工作中使用到的一系列 Docker 镜像集锦，涉及到应用部署、集群架构、站点质量保障、微服务治理等多个方面。

* [Focker #Project#](https://github.com/wxyyxc1992/InfraS-Lab/tree/master/Focker): 从零开始构建类 Docker 容器。

# 前言

按照 NIST(National Institute of Standards and Technology，美国国家标准与技术研究院)的定义的云的标准，云计算并不是在讨论某种特定的云的技术，或者某个厂商提供的产品服务。云的定义更接近于把计算、服务等各种资源变成按需提供的服务，同时提供广泛的网络访问，以及可共享、可度量的后台资源。云更多的是一种服务，虚拟化和容器化只是云底层的技术设施，而本身并没有把它变成一种服务来提供。

- 硬件系统资源的自动化调度(CloudOS)；

- 应用的并行化和分布化实现业务的自动扩展；

- 基于初始配置的自动化业务开通和业务部署；

- 基于前述能力，实现基于 SLA、QoS 等策略的自动化质量保障、故障隔离和故障自愈；

- 基于大数据智能驱动的系统自动化和自动调优。

# 关于

![default](https://user-images.githubusercontent.com/5803001/45228854-de88b400-b2f6-11e8-9ab0-d393ed19f21f.png)

## 规划

## 致谢

由于笔者平日忙于工作，几乎所有线上的文档都是我夫人帮忙整理，在此特别致谢；同时也感谢我家的布丁安静的趴在脚边，不再那么粪发涂墙。

## 版权

![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)
![](https://parg.co/bDm)

笔者所有文章遵循 [知识共享 署名-非商业性使用-禁止演绎 4.0 国际许可协议](https://creativecommons.org/licenses/by-nc-nd/4.0/deed.zh)，欢迎转载，尊重版权。如果觉得本系列对你有所帮助，欢迎给我家布丁买点狗粮(支付宝扫码)~

![](https://github.com/wxyyxc1992/OSS/blob/master/2017/8/1/Buding.jpg?raw=true)
