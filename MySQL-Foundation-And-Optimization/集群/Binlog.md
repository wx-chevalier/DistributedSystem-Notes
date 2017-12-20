# Binlog格式

Mysql binlog日志有三种格式，分别为Statement、ROW、MiXED。

## Statement:每一条会修改数据的sql都会记录在binlog中
**优点：**不需要记录每一行的变化，减少了binlog日志量，节约了IO，提高性能。(相比row能节约多少性能与日志量，这个取决于应用的SQL情况，正常同一条记录修改或者插入row格式所产生的日志量还小于Statement产生的日志量，但是考虑到如果带条件的update操作，以及整表删除，alter表等操作，ROW格式会产生大量日志，因此在考虑是否使用ROW格式日志时应该跟据应用的实际情况，其所产生的日志量会增加多少，以及带来的IO性能问题。)

**缺点：**由于记录的只是执行语句，为了这些语句能在slave上正确运行，因此还必须记录每条语句在执行的时候的一些相关信息，以保证所有语句能在slave得到和在master端执行时候相同 的结果。另外mysql 的复制,像一些特定函数功能，slave可与master上要保持一致会有很多相关问题(如sleep()函数， last_insert_id()，以及user-defined functions(udf)会出现问题).

使用以下函数的语句也无法被复制：

\* LOAD_FILE()

\* UUID()

\* USER()

\* FOUND_ROWS()

\* SYSDATE() (除非启动时启用了 --sysdate-is-now 选项)

同时在INSERT ...SELECT 会产生比 RBR 更多的行级锁

## Row:不记录sql语句上下文相关信息，仅保存哪条记录被修改
**优点：** binlog中可以不记录执行的sql语句的上下文相关的信息，仅需要记录那一条记录被修改成什么了。所以rowlevel的日志内容会非常清楚的记录下每一行数据修改的细节。而且不会出现某些特定情况下的存储过程，或function，以及trigger的调用和触发无法被正确复制的问题

**缺点:**所有的执行的语句当记录到日志中的时候，都将以每行记录的修改来记录，这样可能会产生大量的日志内容,比如一条update语句，修改多条记录，则binlog中每一条修改都会有记录，这样造成binlog日志量会很大，特别是当执行alter table之类的语句的时候，由于表结构修改，每条记录都发生改变，那么该表每一条记录都会记录到日志中。

## MixedLevel
是以上两种level的混合使用，一般的语句修改使用statment格式保存binlog，如一些函数，statement无法完成主从复制的操作，则 采用row格式保存binlog,MySQL会根据执行的每一条具体的sql语句来区分对待记录的日志形式，也就是在Statement和Row之间选择 一种.新版本的MySQL中队row level模式也被做了优化，并不是所有的修改都会以row level来记录，像遇到表结构变更的时候就会以statement模式来记录。至于update或者delete等修改数据的语句，还是会记录所有行的 变更。








# Binlog配置
1.基本配制

Mysql BInlog日志格式可以通过mysql的my.cnf文件的属性binlog_format指定。如以下：

binlog_format           = MIXED                 //binlog日志格式

log_bin                  =目录/mysql-bin.log    //binlog日志名

expire_logs_days    = 7                //binlog过期清理时间

max_binlog_size    100m                    //binlog每个日志文件大小

2.Binlog日志格式选择

Mysql默认是使用Statement日志格式，推荐使用MIXED.

由于一些特殊使用，可以考虑使用ROWED，如自己通过binlog日志来同步数据的修改，这样会节省很多相关操作。对于binlog数据处理会变得非常轻松,相对mixed，解析也会很轻松(当然前提是增加的日志量所带来的IO开销在容忍的范围内即可)。

3.mysqlbinlog格式选择

mysql对于日志格式的选定原则:如果是采用 INSERT，UPDATE，DELETE 等直接操作表的情况，则日志格式根据 binlog_format 的设定而记录,如果是采用 GRANT，REVOKE，SET PASSWORD 等管理语句来做的话，那么无论如何 都采用 SBR 模式记录

通过MysqlBinlog指令查看具体的mysql日志，如下:

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

SET TIMESTAMP=1350355892/*!*/;

BEGIN

/*!*/;

# at 1643330

#121016 10:51:32 server id 1  end_log_pos 1643885        Query     thread_id=272571   exec_time=0   error_code=0

SET TIMESTAMP=1350355892/*!*/;

Insert into T_test….)

/*!*/;

# at 1643885

#121016 10:51:32 server id 1  end_log_pos 1643912        Xid = 0

COMMIT/*!*/;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

1.开始事物的时间:

SET TIMESTAMP=1350355892/*!*/;

BEGIN

2.sqlevent起点

#at 1643330 :为事件的起点，是以1643330字节开始。

3.sqlevent 发生的时间点

#121016 10:51:32:是事件发生的时间，

4.serverId

server id 1 :为master 的serverId

5.sqlevent终点及花费时间，错误码

end_log_pos 1643885:为事件的终点，是以1643885 字节结束。

execTime 0: 花费的时间

error_code=0:错误码

Xid:事件指示提交的XA事务

Mixed日志说明：

在slave日志同步过程中，对于使用now这样的时间函数，MIXED日志格式，会在日志中产生对应的unix_timestamp()*1000的时间字符串，slave在完成同步时，取用的是sqlEvent发生的时间来保证数据的准确性。另外对于一些功能性函数slave能完成相应的数据同步，而对于上面指定的一些类似于UDF函数，导致Slave无法知晓的情况，则会采用ROW格式存储这些Binlog，以保证产生的Binlog可以供Slave完成数据同步。