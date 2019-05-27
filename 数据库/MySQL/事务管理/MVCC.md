![default](https://user-images.githubusercontent.com/5803001/45228854-de88b400-b2f6-11e8-9ab0-d393ed19f21f.png)

# 锁与事务

InnoDB 采用的是两阶段锁定协议，事务执行过程中，根据需要不断的加锁，最后 COMMIT 或 ROLLBACK 的时候一次性释放所有锁，实质上是一种悲观并发控制（悲观锁），而悲观锁会降低系统的并发性能。为了提高并发性能，InnoDB 同时还实现了多版本并发控制（MVCC）。

# MVCC

MySQL 的 Innodb 引擎支持多种事务隔离级别，而其中的 RR 级别（Repeatable-Read）就是依靠 MVCC 来实现的，普通的查询操作都是非锁定读，如果存在事务冲突，会利用 Undo Log 获取新事务操作之前的镜像返回，在读已提交的隔离级别下，会获取新事务修改前的最新的一份已经提交的数据，而在可重复读的隔离级别下，会读取该事务开始时的数据版本。当有多个事务并发操作同一行记录时，该记录会同时存在多个 Undo Log ，每个 Undo Log 就是一个版本。

所有的锁定读都是当前读，也就是读取当前记录的最新版本；对于锁定读而言，不会利用 Undo Log 读取镜像。另外所有的 insert、update、delete 操作也是当前读，update、delete 会在更新之前进行一次当前读，然后加锁，而 insert 因为会触发唯一索引检测，也会包含一个当前读。

MySQL 中 MVCC 的版本指的是事务 ID(Transaction ID)，首先来看一下 MySQL Innodb 中行记录的存储格式，除了最基本的行信息外，还会有一些额外的字段，譬如和 MVCC 有关的字段：`DATA_TRX_ID` 和 `DATA_ROLL_PTR`，如下是一张表的初始信息：

| Primary Key | Time      | Name | DATA_TRX_ID | DATA_ROLL_PTR |
| ----------- | --------- | ---- | ----------- | ------------- |
| 1           | 2018-4-28 | Huan | 1           | NULL          |

- `DATA_TRX_ID`：最近更新这条记录的 Transaction ID，数据库每开启一个事务，事务 ID 都会增加，每个事务拿到的 ID 都不一样
- `DATA_ROLL_PTR`：用来存储指向 Undo Log 中旧版本数据指针，支持了事务的回滚。

对于 SELECT 操作，满足以下两个条件 Innodb 会返回该行数据：

- 该行的创建版本号小于等于当前版本号，用于保证在 SELECT 操作之前所有的操作已经执行落地。
- 该行的删除版本号大于当前版本或者为空。删除版本号大于当前版本意味着有一个并发事务将该行删除了。

对于数据操作类型语句，INSERT 会将新插入的行的创建版本号设置为当前系统的版本号，DELETE 会将要删除的行的删除版本号设置为当前系统的版本号。UPDATE 则是会转换成 INSERT 与 DELETE 的组合，将旧行的删除版本号设置为当前版本号，并将新行 INSERT 同时设置创建版本号为当前版本号。其中，写操作(INSERT、DELETE 和 UPDATE)执行时，需要将系统版本号递增。由于旧数据并不真正的删除，所以必须对这些数据进行清理，Innodb 会开启一个后台线程执行清理工作，具体的规则是将删除版本号小于当前系统版本的行删除，这个过程叫做 Purge。通过 MVCC 很好的实现了事务的隔离性，可以达到 Repeated Read 级别，要实现 Serializable 还必须加锁。

# 并发读写

假如 MVCC 是按照时间来判定数据的版本，在 Time=1 的时刻，数据库的状态如下：

```
| Time | Record A               | Record B               |
| ---- | ---------------------- | ---------------------- |
| 0    | “Record A When time=0” | “Record B when time=0” |
| 1    | “Record A When time=1” |                        |
```

这个时候系统中实际存储了三条记录，`Record A` 在时间 0 和 1 的各一条记录，`Record B` 的一条记录，如果一个事务在 Time=0 的时刻开启，那么读到的数据是：

```
| Record A               | Record B               |
| ---------------------- | ---------------------- |
| “Record A When time=0” | “Record B when time=0” |
```

如果这个事务在 Time=1 的时候开启，那么读到的数据是：

```
| Record A               | Record B               |
| ---------------------- | ---------------------- |
| “Record A When time=1” | “Record B when time=0” |
```

上面的 Case 可以看到，对于读来讲，事务只能读到某一个版本及这个版本之前的最新一条数据，假如在 Time=2 的时候，事务 `Transaction X` 要插入 `Record C`，并更新 `Record B`，但事务还未提交，那么数据库的状态如下：

```
| Time             | Record A               | Record B               | Record C               |
| ---------------- | ---------------------- | ---------------------- | ---------------------- |
| 0                | “Record A When time=0” | “Record B when time=0” |                        |
| 1                | “Record A When time=1” |                        |                        |
| 2(Not Committed) |                        | “Record B when time=2” | “Record C When time=2” |
```

在这个情况下，其它读事务所能看到系统的最新版本是系统处于 Time=1 的时候，所以依然不会读到 `Transaction X` 所改写的数据，此时读到的数据依然为：

```
| Record A               | Record B               |
| ---------------------- | ---------------------- |
| “Record A When time=1” | “Record B when time=0” |
```

基于这种版本机制，就不会出现另一个事务读取时，出现读到 `Record C` 而 `Record B` 还未被 `Transaction X` 更新的中间结果，因为其它事务所看到的系统依然处于 Time=1 的状态。至于说，每个事务应该看到具体什么版本的数据，这个是由不同系统的 MVCC 实现来决定的，下文我会介绍 MySQL 的 MVCC 实现。除了读到的数据必须小于等于当前系统已提交的版本外，写事务在提交时必须大于当前的版本，而这里如果想想还会有一个问题，如果 Time=2 的时刻，开启了多个写或更新事务，当它们同时尝试提交时，必然会有一个事务发现数据库已经处于 Time=2 的状态了，则会按照事务的发起时间来判断是否应该回滚。
