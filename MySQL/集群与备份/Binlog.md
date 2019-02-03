# Binlog 格式

Mysql binlog 日志有三种格式，分别为 Statement、ROW、MiXED。

## Statement:每一条会修改数据的 sql 都会记录在 binlog 中

**优点：**不需要记录每一行的变化，减少了 binlog 日志量，节约了 IO，提高性能。(相比 row 能节约多少性能与日志量，这个取决于应用的 SQL 情况，正常同一条记录修改或者插入 row 格式所产生的日志量还小于 Statement 产生的日志量，但是考虑到如果带条件的 update 操作，以及整表删除，alter 表等操作，ROW 格式会产生大量日志，因此在考虑是否使用 ROW 格式日志时应该跟据应用的实际情况，其所产生的日志量会增加多少，以及带来的 IO 性能问题。)

**缺点：**由于记录的只是执行语句，为了这些语句能在 slave 上正确运行，因此还必须记录每条语句在执行的时候的一些相关信息，以保证所有语句能在 slave 得到和在 master 端执行时候相同 的结果。另外 mysql 的复制,像一些特定函数功能，slave 可与 master 上要保持一致会有很多相关问题(如 sleep()函数， last_insert_id()，以及 user-defined functions(udf)会出现问题).

使用以下函数的语句也无法被复制：

\* LOAD_FILE()

\* UUID()

\* USER()

\* FOUND_ROWS()

\* SYSDATE() (除非启动时启用了 --sysdate-is-now 选项)

同时在 INSERT ...SELECT 会产生比 RBR 更多的行级锁

## Row:不记录 sql 语句上下文相关信息，仅保存哪条记录被修改

**优点：** binlog 中可以不记录执行的 sql 语句的上下文相关的信息，仅需要记录那一条记录被修改成什么了。所以 rowlevel 的日志内容会非常清楚的记录下每一行数据修改的细节。而且不会出现某些特定情况下的存储过程，或 function，以及 trigger 的调用和触发无法被正确复制的问题

**缺点:**所有的执行的语句当记录到日志中的时候，都将以每行记录的修改来记录，这样可能会产生大量的日志内容,比如一条 update 语句，修改多条记录，则 binlog 中每一条修改都会有记录，这样造成 binlog 日志量会很大，特别是当执行 alter table 之类的语句的时候，由于表结构修改，每条记录都发生改变，那么该表每一条记录都会记录到日志中。

## MixedLevel

是以上两种 level 的混合使用，一般的语句修改使用 statment 格式保存 binlog，如一些函数，statement 无法完成主从复制的操作，则 采用 row 格式保存 binlog,MySQL 会根据执行的每一条具体的 sql 语句来区分对待记录的日志形式，也就是在 Statement 和 Row 之间选择 一种.新版本的 MySQL 中队 row level 模式也被做了优化，并不是所有的修改都会以 row level 来记录，像遇到表结构变更的时候就会以 statement 模式来记录。至于 update 或者 delete 等修改数据的语句，还是会记录所有行的 变更。

# Binlog 配置

1.基本配制

Mysql BInlog 日志格式可以通过 mysql 的 my.cnf 文件的属性 binlog_format 指定。如以下：

binlog_format = MIXED //binlog 日志格式

log_bin =目录/mysql-bin.log //binlog 日志名

expire_logs_days = 7 //binlog 过期清理时间

max_binlog_size 100m //binlog 每个日志文件大小

2.Binlog 日志格式选择

Mysql 默认是使用 Statement 日志格式，推荐使用 MIXED.

由于一些特殊使用，可以考虑使用 ROWED，如自己通过 binlog 日志来同步数据的修改，这样会节省很多相关操作。对于 binlog 数据处理会变得非常轻松,相对 mixed，解析也会很轻松(当然前提是增加的日志量所带来的 IO 开销在容忍的范围内即可)。

3.mysqlbinlog 格式选择

mysql 对于日志格式的选定原则:如果是采用 INSERT，UPDATE，DELETE 等直接操作表的情况，则日志格式根据 binlog_format 的设定而记录,如果是采用 GRANT，REVOKE，SET PASSWORD 等管理语句来做的话，那么无论如何 都采用 SBR 模式记录

通过 MysqlBinlog 指令查看具体的 mysql 日志，如下:

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

SET TIMESTAMP=1350355892/_!_/;

BEGIN

/_!_/;

# at 1643330

#121016 10:51:32 server id 1 end_log_pos 1643885 Query thread_id=272571 exec_time=0 error_code=0

SET TIMESTAMP=1350355892/_!_/;

Insert into T_test….)

/_!_/;

# at 1643885

#121016 10:51:32 server id 1 end_log_pos 1643912 Xid = 0

COMMIT/_!_/;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

1.开始事物的时间:

SET TIMESTAMP=1350355892/_!_/;

BEGIN

2.sqlevent 起点

#at 1643330 :为事件的起点，是以 1643330 字节开始。

3.sqlevent 发生的时间点

#121016 10:51:32:是事件发生的时间，

4.serverId

server id 1 :为 master 的 serverId

5.sqlevent 终点及花费时间，错误码

end_log_pos 1643885:为事件的终点，是以 1643885 字节结束。

execTime 0: 花费的时间

error_code=0:错误码

Xid:事件指示提交的 XA 事务

Mixed 日志说明：

在 slave 日志同步过程中，对于使用 now 这样的时间函数，MIXED 日志格式，会在日志中产生对应的 unix_timestamp()\*1000 的时间字符串，slave 在完成同步时，取用的是 sqlEvent 发生的时间来保证数据的准确性。另外对于一些功能性函数 slave 能完成相应的数据同步，而对于上面指定的一些类似于 UDF 函数，导致 Slave 无法知晓的情况，则会采用 ROW 格式存储这些 Binlog，以保证产生的 Binlog 可以供 Slave 完成数据同步。
