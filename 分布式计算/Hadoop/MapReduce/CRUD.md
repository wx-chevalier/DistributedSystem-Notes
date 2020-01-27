# Join

在传统数据库(如：MYSQL)中，JOIN 操作是非常常见且非常耗时的。而在 HADOOP 中进行 JOIN 操作，同样常见且耗时，由于 Hadoop 的独特设计思想，当进行 JOIN 操作时，有一些特殊的技巧。

## Reduce Side Join

reduce side join 是一种最简单的 join 方式，其主要思想如下：
在 map 阶段，map 函数同时读取两个文件 File1 和 File2，为了区分两种来源的 key/value 数据对，对每条数据打一个标签(tag),比如：tag=0 表示来自文件 File1，tag=2 表示来自文件 File2。即：map 阶段的主要任务是对不同文件中的数据打标签。
在 reduce 阶段，reduce 函数获取 key 相同的来自 File1 和 File2 文件的 value list，然后对于同一个 key，对 File1 和 File2 中的数据进行 join(笛卡尔乘积)。即：reduce 阶段进行实际的连接操作。
REF：hadoop join 之 reduce side join
http://blog.csdn.net/huashetianzu/article/details/7819244

### BloomFilter

在某些情况下，SemiJoin 抽取出来的小表的 key 集合在内存中仍然存放不下，这时候可以使用 BloomFiler 以节省空间。
BloomFilter 最常见的作用是：判断某个元素是否在一个集合里面。它最重要的两个方法是：add() 和 contains()。最大的特点是不会存在 false negative，即：如果 contains()返回 false，则该元素一定不在集合中，但会存在一定的 false positive，即：如果 contains()返回 true，则该元素一定可能在集合中。
因而可将小表中的 key 保存到 BloomFilter 中，在 map 阶段过滤大表，可能有一些不在小表中的记录没有过滤掉(但是在小表中的记录一定不会过滤掉)，这没关系，只不过增加了少量的网络 IO 而已。
更多关于 BloomFilter 的介绍，可参考：http://blog.csdn.net/jiaomeng/article/details/1495500

### Map Side Join

之所以存在 reduce side join，是因为在 map 阶段不能获取所有需要的 join 字段，即：同一个 key 对应的字段可能位于不同 map 中。Reduce side join 是非常低效的，因为 shuffle 阶段要进行大量的数据传输。
Map side join 是针对以下场景进行的优化：两个待连接表中，有一个表非常大，而另一个表非常小，以至于小表可以直接存放到内存中。这样，我们可以将小表复制多份，让每个 map task 内存中存在一份(比如存放到 hash table 中)，然后只扫描大表：对于大表中的每一条记录 key/value，在 hash table 中查找是否有相同的 key 的记录，如果有，则连接后输出即可。
为了支持文件的复制，Hadoop 提供了一个类 DistributedCache，使用该类的方法如下:  
(1)用户使用静态方法 DistributedCache.addCacheFile()指定要复制的文件，它的参数是文件的 URI(如果是 HDFS 上的文件，可以这样：hdfs://namenode:9000/home/XXX/file，其中 9000 是自己配置的 NameNode 端口号)。JobTracker 在作业启动之前会获取这个 URI 列表，并将相应的文件拷贝到各个 TaskTracker 的本地磁盘上。(2)用户使用 DistributedCache.getLocalCacheFiles()方法获取文件目录，并使用标准的文件读写 API 读取相应的文件。
REF：hadoop join 之 map side join
http://blog.csdn.net/huashetianzu/article/details/7821674

### Semi Join

Semi Join，也叫半连接，是从分布式数据库中借鉴过来的方法。它的产生动机是：对于 reduce side join，跨机器的数据传输量非常大，这成了 join 操作的一个瓶颈，如果能够在 map 端过滤掉不会参加 join 操作的数据，则可以大大节省网络 IO。
实现方法很简单：选取一个小表，假设是 File1，将其参与 join 的 key 抽取出来，保存到文件 File3 中，File3 文件一般很小，可以放到内存中。在 map 阶段，使用 DistributedCache 将 File3 复制到各个 TaskTracker 上，然后将 File2 中不在 File3 中的 key 对应的记录过滤掉，剩下的 reduce 阶段的工作与 reduce side join 相同。
更多关于半连接的介绍，可参考：半连接介绍：http://wenku.baidu.com/view/ae7442db7f1922791688e877.html
REF：hadoop join 之 semi join
http://blog.csdn.net/huashetianzu/article/details/7823326

# 二次排序

在 Hadoop 中，默认情况下是按照 key 进行排序，如果要按照 value 进行排序怎么办？即：对于同一个 key，reduce 函数接收到的 value list 是按照 value 排序的。这种应用需求在 join 操作中很常见，比如，希望相同的 key 中，小表对应的 value 排在前面。
有两种方法进行二次排序，分别为：buffer and in memory sort 和 value-to-key conversion。
对于 buffer and in memory sort，主要思想是：在 reduce()函数中，将某个 key 对应的所有 value 保存下来，然后进行排序。这种方法最大的缺点是：可能会造成 out of memory。
对于 value-to-key conversion，主要思想是：将 key 和部分 value 拼接成一个组合 key(实现 WritableComparable 接口或者调用 setSortComparatorClass 函数)，这样 reduce 获取的结果便是先按 key 排序，后按 value 排序的结果，需要注意的是，用户需要自己实现 Paritioner，以便只按照 key 进行数据划分。Hadoop 显式的支持二次排序，在 Configuration 类中有个 setGroupingComparatorClass()方法，可用于设置排序 group 的 key 值，具体参考：http://www.cnblogs.com/xuxm2007/archive/2011/09/03/2165805.html
