# Purge

InnoDB 由于要支持多版本协议，因此无论是更新，删除，都只是设置记录上的 deleted bit 标记位，而不是真正的删除记录。后续这些记录的真正删除，是通过 Purge 后台进程实现的。Purge 进程定期扫描 InnoDB 的 undo，按照先读老 undo，再读新 undo 的顺序，读取每条 undo record。对于每一条 undo record，判断其对应的记录是否可以被 purge(purge 进程有自己的 read view，等同于进程开始时最老的活动事务之前的 view，保证 purge 的数据，一定是不可见数据，对任何人来说)，如果可以 purge，则构造完整记录(row_purge_parse_undo_rec)。然后按照先 purge 二级索引，最后 purge 聚簇索引的顺序，purge 一个操作生成的旧版本完整记录。

```sh
row_purge_step->row_purge->trx_purge_fetch_next_rec->row_purge_parse_undo_rec

                     ->row_purge_del_mark->row_purge_remove_sec_if_poss

                                      ->row_purge_remove_clust_if_poss
```

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

我们执行会产生大量 Undo 操作的语句 `update mytest set salary=0`，完成后我们再观察共享表空间，会发现 ibdata1 已经增长到了 114MB，这就说明了共享表空间中还包含有 Undo 信息。rollback 这个事务，ibdata1 这个表空间不会立刻减少，MySQL 会自动判断这些 Undo 信息是否需要，如果不需要，则会将这些空间标记为可用空间，供下次 Undo 使用。master thread 每 10 秒会执行一次 full purge 操作。因此很有可能的一种情况是，你再次执行上述的 UPDATE 语句后，会发现 ibdata1 不会再增大了，那就是这个原因了。
