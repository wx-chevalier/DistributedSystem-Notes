# Blink

Blink 是 Flink 的一个分支，最初在阿里巴巴内部创建的，针对内部用例对 Flink 进行改进。Blink 添加了一系列改进和集成（https://github.com/apache/flink/blob/blink/README.md），其中有很多与有界数据 / 批处理和 SQL 有关。实际上，在上面的功能列表中，除了第 4 项外，Blink 在其他方面都迈出了重要的一步：

- 统一的流式操作符：Blink 扩展了 Flink 的流式运行时操作符模型，支持选择性读取不同的输入源，同时保持推送模型的低延迟特性。这种对输入源的选择性读取可以更好地支持一些算法（例如相同操作符的混合散列连接）和线程模型（通过 RocksDB 的连续对称连接）。这些操作符为“侧边输入”（https://cwiki.apache.org/confluence/display/FLINK/FLIP-17+Side+Inputs+for+DataStream+API）等新功能打下了基础。

- Table API 和 SQL 查询处理器：与最新的 Flink 主分支相比，SQL 查询处理器是演变得最多的一个组件：

  - Flink 目前将查询转换为 DataSet 或 DataStream 程序（取决于输入的特性），而 Blink 会将查询转换为上述流式操作符的数据流。
    Blink 为常见的 SQL 操作添加了更多的运行时操作符，如半连接（semi-join）、反连接（anti-join）等。

  - 查询规划器（优化器）仍然是基于 Apache Calcite，但提供了更多的优化规则（包括连接重排序），并且使用了适当的成本模型。
    更加积极的流式操作符链接。

  - 扩展通用数据结构（分类器、哈希表）和序列化器，在操作二进制数据上更进一步，并减小了序列化开销。代码生成被用于行序列化器。

- 改进的调度和故障恢复：最后，Blink 实现了对任务调度和容错的若干改进。调度策略通过利用操作符处理输入数据的方式来更好地使用资源。故障转移策略沿着持久 shuffle 的边界进行更细粒度的恢复。不需重新启动正在运行的应用程序就可以替换发生故障的 JobManager。
