![default](https://i.postimg.cc/2SVpd63d/image.png)

# 深入浅出分布式系统

参考[某熊的技术之路指北 ☯](https://github.com/wx-chevalier/Developer-Zero-To-Mastery)，深入浅出分布式基础架构是笔者归档自己，在学习与实践软件分布式架构过程中的，笔记与代码的仓库；主要包含分布式计算、分布式系统、数据存储、虚拟化、网络、操作系统等几个部分。

所谓的分布式系统，其主要由网络、分布式存储与分布式计算等部分构成，分布式存储侧重于数据的读写存取及一致性等方面，而分布式计算则侧重于资源、任务的编排调度。

# Preface | 前言

过去数十年间，信息技术的浪潮深刻地改变了这个社会的通信、交流与协作模式，我们熟知的互联网也经历了基于流量点击赢利的单方面信息发布的 Web 1.0 业务模式，转变为由用户主导而生成内容的 Web 2.0 业务模式；在可见的将来随着 3D 相关技术的落地，互联网应用系统所需处理的访问量和数据量必然会再次爆发性增长。

## 分布式系统

从思想演进看，凯文．凯利 2016 年在《失控》一书中指出，分布式系统具有四个突出特点，即没有强制性的中心控制、次级单位具有自治的特质、次级单位之间彼此高度链接、点对点之间的影响通过网络形成了非线性因果关系。凯文．凯利进一步指出，与其说一个分布式、去中心化的网络是一个物体，还不如说它是一个过程。

## 系统的扩展

并发的增加对我们的后端架构提出了巨大的挑战，要求我们的系统弹性可扩展。

![](https://i.postimg.cc/3Rqf3CBz/image.png)

上图从三个维度概括了一个系统的扩展过程：

- X 轴即水平复制，即在负载均衡服务器后增加多个 Web 服务器。
- Z 轴是对数据库的扩展，即分库分表，分库是将关系紧密的表放在一台数据库服务器上，分表是因为一张表的数据太多，需要将一张表的数据通过 hash 放在不同的数据库服务器上。
- Y 轴是业务方向的扩展，才能将巨型应用分解为一组不同的服务，将应用进一步分解为微服务（分库），例如订单管理中心、客户信息管理中心、商品管理中心等等。

总结而言，垂直伸缩只能通过增加服务器的配置有限度地提升系统的处理能力，而水平伸缩能够仅通过增减服务器数量相应地提升和降低系统的吞吐量；这种分布式系统架构，在理论上为吞吐量的提升提供了无限的可能。因此，用于搭建互联网应用的服务器也渐渐放弃了昂贵的小型机，转而采用大量的廉价 PC 服务器。

![](https://i.postimg.cc/YS9Y9xYy/image.png)

在分布式系统的背景下，企业架构也由早期的单体式应用架构渐渐转为更加灵活的分布式应用架构，经历了单体分层架构、SOA 服务化架构、微服务架构、云原生架构等不同架构模式的变迁。后端开发不再局限于单一技术栈，并且为了应对更加庞大的集群规模，单纯的分布式系统已经难于驾驭，因此技术圈开启了一个概念爆发的时代：SOA、DevOps、容器、CI/CD、微服务、Service Mesh 等概念层出不穷，而 Docker、Kubernetes、Mesos、Spring Cloud、gRPC、Istio 等一系列产品的出现，标志着云时代已真正到来。

![服务端开发与架构全景图](https://s2.ax1x.com/2019/09/04/nE8T4x.png)

分布式场景下比较著名的难题就是 CAP 定理。CAP 定理认为，在分布式系统中，系统的一致性（Consistency）、可用性（Availability）、分区容忍性（Partition tolerance），三者不可能同时兼顾。在分布式系统中，由于网络通信的不稳定性，分区容忍性是必须要保证的，因此在设计应用的时候就需要在一致性和可用性之间权衡选择。互联网应用比企业级应用更加偏向保持可用性，因此通常用最终一致性代替传统事务的 ACID 强一致性。

# About

## Copyright

![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg) ![](https://parg.co/bDm)

笔者所有文章遵循 [知识共享 署名-非商业性使用-禁止演绎 4.0 国际许可协议](https://creativecommons.org/licenses/by-nc-nd/4.0/deed.zh)，欢迎转载，尊重版权。如果觉得本系列对你有所帮助，欢迎给我家布丁买点狗粮(支付宝扫码)~

![default](https://i.postimg.cc/y1QXgJ6f/image.png)

## Home & More | 延伸阅读

![技术视野](https://s2.ax1x.com/2019/12/03/QQJLvt.png)

您可以通过以下导航来在 Gitbook 中阅读笔者的系列文章，涵盖了技术资料归纳、编程语言与理论、Web 与大前端、服务端开发与基础架构、云计算与大数据、数据科学与人工智能、产品设计等多个领域：

- 知识体系：《[Awesome Lists | CS 资料集锦](https://ngte-al.gitbook.io/i/)》、《[Awesome CheatSheets | 速学速查手册](https://ngte-ac.gitbook.io/i/)》、《[Awesome Interviews | 求职面试必备](https://github.com/wx-chevalier/Awesome-Interviews)》、《[Awesome RoadMaps | 程序员进阶指南](https://github.com/wx-chevalier/Awesome-RoadMaps)》、《[Awesome MindMaps | 知识脉络思维脑图](https://github.com/wx-chevalier/Awesome-MindMaps)》、《[Awesome-CS-Books | 开源书籍（.pdf）汇总](https://github.com/wx-chevalier/Awesome-CS-Books)》

- 编程语言：《[编程语言理论](https://ngte-pl.gitbook.io/i/)》、《[Java 实战](https://github.com/wx-chevalier/Java-Series)》、《[JavaScript 实战](https://github.com/wx-chevalier/JavaScript-Series)》、《[Go 实战](https://ngte-pl.gitbook.io/i/go/go)》、《[Python 实战](https://ngte-pl.gitbook.io/i/python/python)》、《[Rust 实战](https://ngte-pl.gitbook.io/i/rust/rust)》

- 软件工程、模式与架构：《[编程范式与设计模式](https://ngte-se.gitbook.io/i/)》、《[数据结构与算法](https://ngte-se.gitbook.io/i/)》、《[软件架构设计](https://ngte-se.gitbook.io/i/)》、《[整洁与重构](https://ngte-se.gitbook.io/i/)》、《[研发方式与工具](https://ngte-se.gitbook.io/i/)》

* Web 与大前端：《[现代 Web 全栈开发与工程架构](https://ngte-web.gitbook.io/i/)》、《[数据可视化](https://ngte-fe.gitbook.io/i/)》、《[iOS](https://ngte-fe.gitbook.io/i/)》、《[Android](https://ngte-fe.gitbook.io/i/)》、《[混合开发与跨端应用](https://ngte-fe.gitbook.io/i/)》

* 服务端开发实践与工程架构：《[服务端基础](https://ngte-be.gitbook.io/i/)》、《[微服务与云原生](https://ngte-be.gitbook.io/i/)》、《[测试与高可用保障](https://ngte-be.gitbook.io/i/)》、《[DevOps](https://ngte-be.gitbook.io/i/)》、《[Spring](https://github.com/wx-chevalier/Spring-Series)》、《[信息安全与渗透测试](https://ngte-be.gitbook.io/i/)》

* 分布式基础架构：《[分布式系统](https://ngte-infras.gitbook.io/i/)》、《[分布式计算](https://ngte-infras.gitbook.io/i/)》、《[数据库](https://github.com/wx-chevalier/Database-Series)》、《[网络](https://ngte-infras.gitbook.io/i/)》、《[虚拟化与云计算](https://github.com/wx-chevalier/Cloud-Series)》、《[Linux 与操作系统](https://github.com/wx-chevalier/Linux-Series)》

* 数据科学，人工智能与深度学习：《[数理统计](https://ngte-aidl.gitbook.io/i/)》、《[数据分析](https://ngte-aidl.gitbook.io/i/)》、《[机器学习](https://ngte-aidl.gitbook.io/i/)》、《[深度学习](https://ngte-aidl.gitbook.io/i/)》、《[自然语言处理](https://ngte-aidl.gitbook.io/i/)》、《[工具与工程化](https://ngte-aidl.gitbook.io/i/)》、《[行业应用](https://ngte-aidl.gitbook.io/i/)》

* 产品设计与用户体验：《[产品设计](https://ngte-pd.gitbook.io/i/)》、《[交互体验](https://ngte-pd.gitbook.io/i/)》、《[项目管理](https://ngte-pd.gitbook.io/i/)》

* 行业应用：《[行业迷思](https://github.com/wx-chevalier/Business-Series)》、《[功能域](https://github.com/wx-chevalier/Business-Series)》、《[电子商务](https://github.com/wx-chevalier/Business-Series)》、《[智能制造](https://github.com/wx-chevalier/Business-Series)》

此外，你还可前往 [xCompass](https://wx-chevalier.github.io/home/#/search) 交互式地检索、查找需要的文章/链接/书籍/课程；或者在 [MATRIX 文章与代码索引矩阵](https://github.com/wx-chevalier/Developer-Zero-To-Mastery)中查看文章与项目源代码等更详细的目录导航信息。最后，你也可以关注微信公众号：『**某熊的技术之路**』以获取最新资讯。
