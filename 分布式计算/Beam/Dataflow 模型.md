# 流处理模型

Dataflow 模型（或者说 Beam 模型）旨在建立一套准确可靠的关于流处理的解决方案。在 Dataflow 模型提出以前，流处理常被认为是一种不可靠但低延迟的处理方式，需要配合类似于 MapReduce 的准确但高延迟的批处理框架才能得到一个可靠的结果，这就是著名的 Lambda 架构。这种架构给应用带来了很多的麻烦，例如引入多套组件导致系统的复杂性、可维护性难度提高。因此 Lambda 架构遭到很多开发者的炮轰，并试图设计一套统一批流的架构减少这种复杂性。Spark 1.X 的 Mirco-Batch 模型就尝试从批处理的角度处理流数据，将不间断的流数据切分为一个个微小的批处理块，从而可以使用批处理的 transform 操作处理数据。还有 Jay 提出的 Kappa 架构，使用类似于 Kafka 的日志型消息存储作为中间件，从流处理的角度处理批处理。在工程师的不断努力和尝试下，Dataflow 模型孕育而生。

起初，Dataflow 模型是为了解决 Google 的广告变现问题而设计的。因为广告主需要实时的知道自己投放的广告播放、观看情况等指标从而更好的进行决策，但是批处理框架 Mapreduce、Spark 等无法满足时延的要求（因为它们需要等待所有的数据成为一个批次后才会开始处理），（当时）新生的流处理框架 Aurora、Niagara 等还没经受大规模生产环境的考验，饱经考验的流处理框架 Storm、Samza 却没有“恰好一次”的准确性保障（在广告投放时，如果播放量算多一次，意味广告主的亏损，导致对平台的不信任，而少算一次则是平台的亏损，平台方很难接受），DStreaming（Spark1.X）无法处理事件时间，只有基于记录数或基于数据处理时间的窗口，Lambda 架构过于复杂且可维护性低，最契合的 Flink 在当时并未成熟。最后 Google 只能基于 MillWheel 重新审视流的概念设计出 Dataflow 模型和 Google Cloud Dataflow 框架，并最终影响了 Spark 2.x 和 Flink 的发展，也促使了 Apache Beam 项目的开源。

# 核心概念

Dataflow 模型从流处理的角度重新审视数据处理过程，将批和流处理的数据抽象成数据集的概念，并将数据集划分为无界数据集和有界数据集，认为流处理是批处理的超集。模型定义了时间域（time domain）的概念，将时间明确的区分为事件时间（event-time）和处理时间（process-time），给出构建一个正确、稳定、低时延的流处理系统所会面临的四个问题及其解决办法：

- 计算的结果是什么（What results are calculated）？通过 transformations 操作

- 在事件时间中的哪个位置计算结果（Where in event time are results calculated）？使用窗口（windowing）的概念

- 在处理时间中的哪个时刻触发计算结果（When in processing time are results materialized）？使用 triggers + watermarks 进行触发计算

- 如何修正结果（How do refinements of results relate）？通过 accumulation 的类型修正结果数据

基于这些考虑，模型的核心概念包括了：

- 事件时间（Event time）和处理时间（processing time）流处理中最重要的问题是事件发生的时间（事件时间）和处理系统观测到的时间（处理时间）存在延迟。

- 窗口（Windowing）为了合理地计算无界数据集地结果，所以需要沿时间边界切分数据集（也就是窗口）。

- 触发器（Triggers）触发器是一种表示处理过程中遇上某种特殊情况时，此刻的输出结果可以是精确的，有意义的机制。

- 水印（Watermarks）水印是针对事件时间的概念，提供了一种事件时间相对于处理时间是乱序的系统中合理推测无界数据集里数据完整性的工具。

累计类型（Accumulation）累计类型是处理单个窗口的输出数据是如何随着流处理的进程而发生变化的。

# 链接

- https://www.zhihu.com/question/30151872/answer/640568211
