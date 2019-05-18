# Purge

InnoDB 由于要支持多版本协议，因此无论是更新，删除，都只是设置记录上的 deleted bit 标记位，而不是真正的删除记录。后续这些记录的真正删除，是通过 Purge 后台进程实现的。Purge 进程定期扫描 InnoDB 的 undo，按照先读老 undo，再读新 undo 的顺序，读取每条 undo record。对于每一条 undo record，判断其对应的记录是否可以被 purge(purge 进程有自己的 read view，等同于进程开始时最老的活动事务之前的 view，保证 purge 的数据，一定是不可见数据，对任何人来说)，如果可以 purge，则构造完整记录(row_purge_parse_undo_rec)。然后按照先 purge 二级索引，最后 purge 聚簇索引的顺序，purge 一个操作生成的旧版本完整记录。

```sh
row_purge_step->row_purge->trx_purge_fetch_next_rec->row_purge_parse_undo_rec

                     ->row_purge_del_mark->row_purge_remove_sec_if_poss

                                      ->row_purge_remove_clust_if_poss
```
