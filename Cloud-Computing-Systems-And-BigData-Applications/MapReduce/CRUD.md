# Join
在传统数据库（如：MYSQL）中，JOIN操作是非常常见且非常耗时的。而在HADOOP中进行JOIN操作，同样常见且耗时，由于Hadoop的独特设计思想，当进行JOIN操作时，有一些特殊的技巧。
## Reduce Side Join
reduce side join是一种最简单的join方式，其主要思想如下：
在map阶段，map函数同时读取两个文件File1和File2，为了区分两种来源的key/value数据对，对每条数据打一个标签（tag）,比如：tag=0表示来自文件File1，tag=2表示来自文件File2。即：map阶段的主要任务是对不同文件中的数据打标签。
在reduce阶段，reduce函数获取key相同的来自File1和File2文件的value list， 然后对于同一个key，对File1和File2中的数据进行join（笛卡尔乘积）。即：reduce阶段进行实际的连接操作。
REF：hadoop join之reduce side join
http://blog.csdn.net/huashetianzu/article/details/7819244

### BloomFilter
在某些情况下，SemiJoin抽取出来的小表的key集合在内存中仍然存放不下，这时候可以使用BloomFiler以节省空间。
BloomFilter最常见的作用是：判断某个元素是否在一个集合里面。它最重要的两个方法是：add() 和contains()。最大的特点是不会存在 false negative，即：如果contains()返回false，则该元素一定不在集合中，但会存在一定的 false positive，即：如果contains()返回true，则该元素一定可能在集合中。
因而可将小表中的key保存到BloomFilter中，在map阶段过滤大表，可能有一些不在小表中的记录没有过滤掉（但是在小表中的记录一定不会过滤掉），这没关系，只不过增加了少量的网络IO而已。
更多关于BloomFilter的介绍，可参考：http://blog.csdn.net/jiaomeng/article/details/1495500

### Map Side Join
之所以存在reduce side join，是因为在map阶段不能获取所有需要的join字段，即：同一个key对应的字段可能位于不同map中。Reduce side join是非常低效的，因为shuffle阶段要进行大量的数据传输。
Map side join是针对以下场景进行的优化：两个待连接表中，有一个表非常大，而另一个表非常小，以至于小表可以直接存放到内存中。这样，我们可以将小表复制多份，让每个map task内存中存在一份（比如存放到hash table中），然后只扫描大表：对于大表中的每一条记录key/value，在hash table中查找是否有相同的key的记录，如果有，则连接后输出即可。
为了支持文件的复制，Hadoop提供了一个类DistributedCache，使用该类的方法如下：    
（1）用户使用静态方法DistributedCache.addCacheFile()指定要复制的文件，它的参数是文件的URI（如果是HDFS上的文件，可以这样：hdfs://namenode:9000/home/XXX/file，其中9000是自己配置的NameNode端口号）。JobTracker在作业启动之前会获取这个URI列表，并将相应的文件拷贝到各个TaskTracker的本地磁盘上。（2）用户使用DistributedCache.getLocalCacheFiles()方法获取文件目录，并使用标准的文件读写API读取相应的文件。
REF：hadoop join之map side join
http://blog.csdn.net/huashetianzu/article/details/7821674

### Semi Join
Semi Join，也叫半连接，是从分布式数据库中借鉴过来的方法。它的产生动机是：对于reduce side join，跨机器的数据传输量非常大，这成了join操作的一个瓶颈，如果能够在map端过滤掉不会参加join操作的数据，则可以大大节省网络IO。
实现方法很简单：选取一个小表，假设是File1，将其参与join的key抽取出来，保存到文件File3中，File3文件一般很小，可以放到内存中。在map阶段，使用DistributedCache将File3复制到各个TaskTracker上，然后将File2中不在File3中的key对应的记录过滤掉，剩下的reduce阶段的工作与reduce side join相同。
更多关于半连接的介绍，可参考：半连接介绍：http://wenku.baidu.com/view/ae7442db7f1922791688e877.html
REF：hadoop join之semi join
http://blog.csdn.net/huashetianzu/article/details/7823326

# 二次排序
在Hadoop中，默认情况下是按照key进行排序，如果要按照value进行排序怎么办？即：对于同一个key，reduce函数接收到的value list是按照value排序的。这种应用需求在join操作中很常见，比如，希望相同的key中，小表对应的value排在前面。
有两种方法进行二次排序，分别为：buffer and in memory sort和 value-to-key conversion。
对于buffer and in memory sort，主要思想是：在reduce()函数中，将某个key对应的所有value保存下来，然后进行排序。 这种方法最大的缺点是：可能会造成out of memory。
对于value-to-key conversion，主要思想是：将key和部分value拼接成一个组合key（实现WritableComparable接口或者调用setSortComparatorClass函数），这样reduce获取的结果便是先按key排序，后按value排序的结果，需要注意的是，用户需要自己实现Paritioner，以便只按照key进行数据划分。Hadoop显式的支持二次排序，在Configuration类中有个setGroupingComparatorClass()方法，可用于设置排序group的key值，具体参考：http://www.cnblogs.com/xuxm2007/archive/2011/09/03/2165805.html