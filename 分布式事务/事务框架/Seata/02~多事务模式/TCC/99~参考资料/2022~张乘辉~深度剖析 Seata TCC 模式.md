> [原文地址](https://seata.apache.org/zh-cn/blog/seata-tcc/)，Seata 是一个开源的分布式事务解决方案，它支持多种分布式事务处理模式，包括 AT、XA、TCC 和 SAGA 模式。TCC 模式，即 Try-Confirm-Cancel 模式，是一种二阶段提交协议，它要求业务系统实现 Try、Confirm 和 > Cancel 三个阶段的操作。
>
> 在 TCC 模式中：
>
> - **Try 阶段**：对业务资源进行检查并预留。
> - **Confirm 阶段**：对业务处理进行提交，即 commit 操作。如果 Try 成功，Confirm 阶段应该也会成功。
> - **Cancel 阶段**：对业务处理进行取消，即回滚操作，释放 Try 阶段预留的资源。
>
> Seata 的 TCC 模式遵循 TC（事务协调者）、TM（事务管理器）和 RM（资源管理器）三种角色模型。Seata 在启动时会扫描并解析 TCC 接口，如果是发布方，则会向 TC 注册 TCC Resource；如果是调用方，Seata 会代理调> 用方，拦截 TCC 接口的调用，并在 Try 方法执行前向 TC 注册分支事务。
>
> 当全局事务需要提交或回滚时，TC 会通过资源 ID 回调到对应的参与者服务中，执行相应的 Confirm 或 Cancel 方法。
>
> 文章还详细介绍了 Seata 如何实现 TCC 模式，包括资源解析、资源管理和事务处理等关键步骤。此外，还讨论了如何处理 TCC 模式中可能出现的空回滚、幂等和悬挂等异常情况。
