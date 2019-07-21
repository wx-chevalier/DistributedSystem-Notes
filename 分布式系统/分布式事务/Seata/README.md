# Seata

Seata 原名 Fescar（Fast & Easy Commit And Rollback），是一个用于微服务架构的分布式事务解决方案，它的特点是高性能且易于使用，旨在实现简单并快速的事务提交与回滚。

- TXC(Taobao Transaction Constructor)，阿里巴巴中间件团队自 2014 年起启动该项目，以满足应用程序架构从单一服务变为微服务所导致的分布式事务问题。
- GTS(Global Transaction Service)，2016 年 TXC 作为阿里中间件的产品，更名为 GTS 发布。
- 2019 年开始基于 TXC/GTS 开源 Fescar（Fast，后来更名为 Seata。

# 架构与组件

Seata 就是用于解决上述微服务架构中的事务问题的解决方案。如下图所示，分布式事务是一个全局事务（Global Transaction），由一批分支事务（Branch Transation）组成，通常分支事务只是本地事务。

![](https://i.postimg.cc/v80hf94D/image.png)

Seata 中有三大基本组件：

- Transaction Coordinator(TC)：维护全局和分支事务的状态，驱动全局事务提交与回滚。
- Transaction Manager(TM)：定义全局事务的范围：开始、提交或回滚全局事务。
- Resource Manager(RM)：管理分支事务处理的资源，与 TC 通信以注册分支事务并报告分支事务的状态，并驱动分支事务提交或回滚。

Seata 管理分布式事务的典型生命周期：

- TM 要求 TC 开始新的全局事务，TC 生成表示全局事务的 XID。
- XID 通过微服务的调用链传播。
- RM 在 TC 中将本地事务注册为 XID 的相应全局事务的分支。
- TM 要求 TC 提交或回滚 XID 的相应全局事务。
- TC 驱动 XID 的相应全局事务下的所有分支事务，完成分支提交或回滚。
