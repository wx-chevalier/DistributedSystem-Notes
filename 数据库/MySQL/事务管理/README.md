# MySQL 中的事务管理

MySQL 中的事务管理概述了数据插入、更新、删除等流程中的引擎相关操作，首先以某个 update 语句 `update person set age = 30 where id = 1;` 的执行流程为例：

- 分配事务 ID ，开启事务，获取锁，没有获取到锁则等待。

- 执行器先通过存储引擎找到 id = 1 的数据页，如果缓冲池有则直接取出，没有则去主键索引上取出对应的数据页放入缓冲池。

- 在数据页内找到 id = 1 这行记录，取出，将 age 改为 30 然后写入内存。

- 生成 redolog Undo Log 到内存，redolog 状态为 prepare。

- 将 redolog Undo Log 写入文件并调用 fsync。

- server 层生成 binlog 并写入文件调用 fsync。

- 事务提交，将 redolog 的状态改为 commited 释放锁。
