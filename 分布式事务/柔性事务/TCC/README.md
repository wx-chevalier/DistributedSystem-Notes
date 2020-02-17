# TCC

TCC（Try-Confirm-Cancel）的概念，最早是由 Pat Helland 于 2007 年发表的一篇名为《Life beyond Distributed Transactions:an Apostate’s Opinion》的论文提出。

TCC 是服务化的二阶段编程模型，其 Try、Confirm、Cancel 3 个方法均由业务编码实现：

- Try 操作作为一阶段，负责资源的检查和预留。
- Confirm 操作作为二阶段提交操作，执行真正的业务。
- Cancel 是预留资源的取消。

TCC 事务的 Try、Confirm、Cancel 可以理解为 SQL 事务中的 Lock、Commit、Rollback。

# 链接

- https://mp.weixin.qq.com/s/Qr6yGSSp_R5RUJXQkEeDlg
