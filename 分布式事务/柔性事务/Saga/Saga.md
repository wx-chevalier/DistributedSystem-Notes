# Saga

Saga 起源于 1987 年 Hector & Kenneth 发表的论文 [Sagas](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf)，把一个分布式事务拆分为多个本地事务，每个本地事务都有相应的执行模块和补偿模块（TCC 中的 Confirm 和 Cancel）。当 Saga 事务中任意一个本地事务出错时，可以通过调用相关的补偿方法恢复之前的事务，达到事务最终的一致性。

当每个 Saga 子事务 T1, T2, …, Tn 都有对应的补偿定义 C1, C2, …, Cn-1,那么 Saga 系统可以保证：

- 子事务序列 T1, T2, …, Tn 得以完成 (最佳情况)；

- 或者序列 T1, T2, …, Tj, Cj, …, C2, C1, 0 < j < n, 得以完成。
