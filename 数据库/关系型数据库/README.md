# 关系型数据库工作原理

数据库系统的萌芽出现于 60 年代。当时计算机开始广泛地应用于数据管理，对数据的共享提出了越来越高的要求。传统的文件系统已经不能满足人们的需要。能够统一管理和共享数据的数据库管理系统（DBMS）应运而生。1961 年通用电气公司（General ElectricCo.）的 Charles Bachman 成功地开发出世界上第一个网状 DBMS 也是第一个数据库管理系统—— 集成数据存储（Integrated DataStore IDS），奠定了网状数据库的基础。

1970 年，IBM 的研究员 E.F.Codd 博士在刊物 Communication of the ACM 上发表了一篇名为“A Relational Modelof Data for Large Shared Data Banks”的论文，提出了关系模型的概念，奠定了关系模型的理论基础。1974 年，IBM 的 Ray Boyce 和 DonChamberlin 将 Codd 关系数据库的 12 条准则的数学定义以简单的关键字语法表现出来，里程碑式地提出了 SQL（Structured Query Language）语言。在很长的时间内，关系数据库（如 MySQL 和 Oracle）对于开发任何类型的应用程序都是首选，巨石型架构也是应用程序开发的标准架构。

![mindmap](https://i.postimg.cc/TwNZP70n/image.png)

# 数据库组件

![](https://i.postimg.cc/02hPY8z7/image.png)

典型的数据库组件可能会包含如下部分：

- 过程管理器(The process manager)：数据库都会有一个过程池/线程池需要进行管理。此外，为了使运行时间更短，现代数据库会使用自己的线程来替代操作系统线程。
- 网络管理器(The network manager)：网络的输入输出是个大问题，特别是对于分布式数据库来说。所以部分数据库针对网络管理打造了自己的管理器。
- 文件系统管理器(File system manager)：磁碟 IO 是数据库的第一瓶颈。使用管理器进行磁碟文件进行管理是很重要的。
- 内存管理器(Memory manager)：当你需要处理大量内存数据或大量查询，一个高效的内存管理器是必须的。
- 安全管理器(Security manager)：进行认证和用户认证管理。
- 客户端管理器(Client manager)：进行客户端连接管理。

# 链接

- 持久链接：[关系型数据库理论 https://url.wx-coder.cn/DJNQn ](https://url.wx-coder.cn/DJNQn)
