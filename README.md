![default](https://user-images.githubusercontent.com/5803001/45228847-daf52d00-b2f6-11e8-9367-a48c196da018.png)

# 深入浅出分布式基础架构

参考[某熊的技术之路指北 ☯](https://github.com/wx-chevalier/Developer-Zero-To-Mastery)，深入浅出分布式基础架构是笔者归档自己，在学习与实践软件分布式架构过程中的，笔记与代码的仓库；主要包含分布式计算、分布式系统、数据存储、虚拟化、网络、操作系统等几个部分。

# Nav | 导航

您可以通过以下任一方式阅读笔者的系列文章，涵盖了技术资料归纳、编程语言与理论、Web 与大前端、服务端开发与基础架构、云计算与大数据、数据科学与人工智能、产品设计等多个领域：

- 在 Gitbook 中在线浏览，每个系列对应各自的 Gitbook 仓库。

| [Awesome Lists](https://ngte-al.gitbook.io/i/) | [Awesome CheatSheets](https://ngte-ac.gitbook.io/i/) | [Awesome Interviews](https://github.com/wx-chevalier/Developer-Zero-To-Mastery/tree/master/Interview) | [Awesome RoadMaps](https://github.com/wx-chevalier/Developer-Zero-To-Mastery/tree/master/RoadMap) | [Awesome-CS-Books-Warehouse](https://github.com/wx-chevalier/Awesome-CS-Books-Warehouse) |
| ---------------------------------------------- | ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |


| [编程语言理论](https://ngte-pl.gitbook.io/i/) | [Java 实战](https://ngte-pl.gitbook.io/i/go/go) | [JavaScript 实战](https://ngte-pl.gitbook.io/i/javascript/javascript) | [Go 实战](https://ngte-pl.gitbook.io/i/go/go) | [Python 实战](https://ngte-pl.gitbook.io/i/python/python) | [Rust 实战](https://ngte-pl.gitbook.io/i/rust/rust) |
| --------------------------------------------- | ----------------------------------------------- | --------------------------------------------------------------------- | --------------------------------------------- | --------------------------------------------------------- | --------------------------------------------------- |


| [软件工程、数据结构与算法、设计模式、软件架构](https://ngte-se.gitbook.io/i/) | [现代 Web 开发基础与工程实践](https://ngte-web.gitbook.io/i/) | [大前端混合开发与数据可视化](https://ngte-fe.gitbook.io/i/) | [服务端开发实践与工程架构](https://ngte-be.gitbook.io/i/) | [分布式基础架构](https://ngte-infras.gitbook.io/i/) | [数据科学，人工智能与深度学习](https://ngte-aidl.gitbook.io/i/) | [产品设计与用户体验](https://ngte-pd.gitbook.io/i/) |
| ----------------------------------------------------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------------- | --------------------------------------------------------- | --------------------------------------------------- | --------------------------------------------------------------- | --------------------------------------------------- |


- 前往 [xCompass https://wx-chevalier.github.io](https://wx-chevalier.github.io/home/#/search) 交互式地检索、查找需要的文章/链接/书籍/课程，或者关注微信公众号：某熊的技术之路。

![](https://i.postimg.cc/3RVYtbsv/image.png)

- 在下文的 [MATRIX 文章与代码矩阵 https://github.com/wx-chevalier/Developer-Zero-To-Mastery](https://github.com/wx-chevalier/Developer-Zero-To-Mastery) 中查看文章与项目的源代码。
-

| [Linux 与操作系统](./Linux%20与操作系统) | [分布式计算](./分布式计算) | [虚拟化与编排](./虚拟化与编排) | [分布式系统](./分布式系统) | [数据库](./数据库) |
| ---------------------------------------- | -------------------------- | ------------------------------ | -------------------------- | ------------------ |


# Preface | 前言

过去数十年间，信息技术的浪潮深刻地改变了这个社会的通信、交流与协作模式，我们熟知的互联网也经历了基于流量点击赢利的单方面信息发布的 Web 1.0 业务模式，转变为由用户主导而生成内容的 Web 2.0 业务模式。在可见的将来，随着 3D 相关技术的落地，互联网应用系统所需处理的访问量和数据量必然会再次爆发性增长，大型网站的各种高并发访问、海量数据处理等场景越来越多，如何实现网站的高可用、易伸缩、可扩展、安全等目标就显得越来越重要。这就要求我们的系统架构能够应对快速迭代、变化的领域需求变化，并且能够在高并发的场景下依然提供高可用的服务。

在分布式系统的背景下，后端开发不再局限于单一技术栈，企业架构也由早期的单体式应用架构渐渐转为更加灵活的分布式应用架构，历经了单体分层架构、SOA 服务化架构、微服务架构、云原生架构等不同架构模式的变迁。这些架构模式往往都不再是单纯的分布式系统，DevOps、容器、CI/CD、Docker、Kubernetes、Mesos、Spring Cloud、gRPC、Istio、Service Mesh 等概念或者技术赋能我们部署与运维更加庞大的集群规模。

![](https://i.postimg.cc/YS9Y9xYy/image.png)

另一方面，并发的增加对我们的后端架构提出了巨大的挑战，要求我们的系统弹性可扩展。常见的系统扩展包括了垂直伸缩与水平伸缩，前者只能通过增加服务器的配置有限度地提升系统的处理能力，而水平伸缩能够仅通过增减服务器数量相应地提升和降低系统的吞吐量；这种分布式系统架构，在理论上为吞吐量的提升提供了无限的可能。因此，用于搭建互联网应用的服务器也渐渐放弃了昂贵的小型机，转而采用大量的廉价 PC 服务器。

![mindmap](https://tva1.sinaimg.cn/large/007DFXDhgy1g4jlvqqryvj30u014o4qp.jpg)

# 版权

![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg) ![](https://parg.co/bDm)

笔者所有文章遵循 [知识共享 署名-非商业性使用-禁止演绎 4.0 国际许可协议](https://creativecommons.org/licenses/by-nc-nd/4.0/deed.zh)，欢迎转载，尊重版权。如果觉得本系列对你有所帮助，欢迎给我家布丁买点狗粮(支付宝扫码)~

![default](https://i.postimg.cc/y1QXgJ6f/image.png)
