# Undo 与 Redo

# Purge

```sh
# 查看初始共享表空间文件，ibdata1 的大小为 76M。
mysql> system ls -lh /usr/local/var/mysql/ibdata1

# 执行大量更新操作
mysql> set autocommit=0;

mysql> update mytest set salary=0;

mysql> system ls -lh /usr/local/var/mysql/ibdata1;

# 最后执行回滚
mysql> rollback;
```

我们执行会产生大量 Undo 操作的语句 update mytest set salary=0，完成后我们再观察共享表空间，会发现 ibdata1 已经增长到了 114MB，这就说明了共享表空间中还包含有 Undo 信息。rollback 这个事务，ibdata1 这个表空间不会立刻减少，MySQL 会自动判断这些 Undo 信息是否需要，如果不需要，则会将这些空间标记为可用空间，供下次 Undo 使用。master thread 每 10 秒会执行一次 full purge 操作。因此很有可能的一种情况是，你再次执行上述的 UPDATE 语句后，会发现 ibdata1 不会再增大了，那就是这个原因了。
