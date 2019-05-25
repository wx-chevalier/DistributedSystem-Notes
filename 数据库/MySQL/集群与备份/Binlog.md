# Binlog

MySQL 通过 BINLOG 录执行成功的 INSERT、UPDATE、DELETE 等更新数据的 SQL 语句，并由此实现 MySQL 数据库的恢复和主从复制。

MySQL 的恢复是 SQL 语句级的，也就是重新执行 BINLOG 中的 SQL 语句。这与 Oracle 数据库不同，Oracle 是基于数据库文件块的。MySQL 的 Binlog 是按照事务提交的先后顺序记录的，恢复也是按这个顺序进行的。这点也与 Oralce 不同，Oracle 是按照系统更新号（System Change Number，SCN）来恢复数据的，每个事务开始时，Oracle 都会分配一个全局唯一的 SCN，SCN 的顺序与事务开始的时间顺序是一致的。

从上面两点可知，MySQL 的恢复机制要求：在一个事务未提交前，其他并发事务不能插入满足其锁定条件的任何记录，也就是不允许出现幻读，这已经超过了 ISO/ANSI SQL92“可重复读”隔离级别的要求，实际上是要求事务要串行化。这也是许多情况下，InnoDB 要用到间隙锁的原因，比如在用范围条件更新记录时，无论在 Read Commited 或是 Repeatable Read 隔离级别下，InnoDB 都要使用间隙锁，但这并不是隔离级别要求的。

## Binlog 与 Redo Log

Binlog 和 Redo Log 的区别是，他是在存储引擎上层 Server 层写入的，他记录的是逻辑操作，也就是对应的 sql ,而 Redo Log 记录的底层某个数据页的物理操作，redolog 是循环写的，而 Binlog 是追加写的，不会覆盖以前写的数据。而 Binlog 也需要在事务提交前写入文件。binlog 的写入页需要通过 fsync 来保证落盘，为了提高 tps ，MySQL 　可以通过参数　　 sync_binlog 来控制是否需要同步刷盘，该策略会影响当主库宕机后备库数据可能并没有完全同步到主库数据。由于事务的原子性，需要保证事务提交的时候 Redo Log 和 Binlog 都写入成功，所以 MySQL 执行层采用了两阶段提交来保证 Redo Log 和 Binlog 都写入成功后才 commit，如果一方失败则会进行回滚。

# 日志格式

MySQL Binlog 日志有三种格式，分别为 Statement、ROW、MiXED。

## Statement

每一条会修改数据的 SQL 都会记录在 Binlog 中，不需要记录每一行的变化，减少了 Binlog 日志量，节约了 IO，提高性能。正常同一条记录修改或者插入 ROW 格式所产生的日志量还小于 Statement 产生的日志量，但是考虑到如果带条件的 update 操作，以及整表删除，alter 表等操作，ROW 格式会产生大量日志，因此在考虑是否使用 ROW 格式日志时应该跟据应用的实际情况，其所产生的日志量会增加多少，以及带来的 IO 性能问题。

由于记录的只是执行语句，为了这些语句能在 Slave 上正确运行，因此还必须记录每条语句在执行的时候的一些相关信息，以保证所有语句能在 Slave 得到和在 master 端执行时候相同 的结果。另外 MySQL 的复制,像一些特定函数功能，slave 可与 master 上要保持一致会有很多相关问题(如 sleep()函数， last_insert_id()，以及 user-defined functions(udf)会出现问题)。同时在 INSERT ...SELECT 会产生比 RBR 更多的行级锁。

## Row

Row 不记录 SQL 语句上下文相关信息，仅保存哪条记录被修改。Binlog 中可以不记录执行的 sql 语句的上下文相关的信息，仅需要记录那一条记录被修改成什么了。所以 rowlevel 的日志内容会非常清楚的记录下每一行数据修改的细节。而且不会出现某些特定情况下的存储过程，或 function，以及 trigger 的调用和触发无法被正确复制的问题。

所有的执行的语句当记录到日志中的时候，都将以每行记录的修改来记录，这样可能会产生大量的日志内容,比如一条 update 语句，修改多条记录，则 Binlog 中每一条修改都会有记录，这样造成 Binlog 日志量会很大，特别是当执行 `alter table` 之类的语句的时候，由于表结构修改，每条记录都发生改变，那么该表每一条记录都会记录到日志中。

## MixedLevel

是以上两种 level 的混合使用，一般的语句修改使用 statment 格式保存 binlog，如一些函数，statement 无法完成主从复制的操作，则采用 Row 格式保存 binlog，MySQL 会根据执行的每一条具体的 sql 语句来区分对待记录的日志形式，也就是在 Statement 和 Row 之间选择 一种.新版本的 MySQL 中队 row level 模式也被做了优化，并不是所有的修改都会以 row level 来记录，像遇到表结构变更的时候就会以 statement 模式来记录。至于 update 或者 delete 等修改数据的语句，还是会记录所有行的变更。

在 Slave 日志同步过程中，对于使用 now 这样的时间函数，MIXED 日志格式，会在日志中产生对应的 `unix_timestamp()*1000` 的时间字符串，slave 在完成同步时，取用的是 sqlEvent 发生的时间来保证数据的准确性。另外对于一些功能性函数 Slave 能完成相应的数据同步，而对于上面指定的一些类似于 UDF 函数，导致 Slave 无法知晓的情况，则会采用 ROW 格式存储这些 Binlog，以保证产生的 Binlog 可以供 Slave 完成数据同步。

# Binlog 配置

Mysql Binlog 日志格式可以通过 mysql 的 my.cnf 文件的属性 binlog_format 指定。如以下：

```c
binlog_format = MIXED // Binlog 日志格式

log_bin =目录/mysql-bin.log // Binlog 日志名

expire_logs_days = 7 // Binlog 过期清理时间

max_binlog_size 100m // Binlog 每个日志文件大小
```

Mysql 默认是使用 Statement 日志格式，推荐使用 MIXED.由于一些特殊使用，可以考虑使用 ROWED，如自己通过 Binlog 日志来同步数据的修改，这样会节省很多相关操作。对于 Binlog 数据处理会变得非常轻松,相对 mixed，解析也会很轻松(当然前提是增加的日志量所带来的 IO 开销在容忍的范围内即可)。

# 链接

- https://mp.weixin.qq.com/s/5J84JzO1uqln5EWqvEpqLw
