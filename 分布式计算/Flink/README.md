# Flink

从早期开始，Flink 就有意采用统一的批处理和流式处理方法。其核心构建块是“持续处理无界的数据流”：如果可以做到这一点，还可以离线处理有界数据集（批处理），因为有界数据集就是在某个时刻结束的数据流。

Flink 包含了一个网络栈，支持低延迟 / 高吞吐的流式数据交换和高吞吐的批次 shuffle。它还提供了很多流式运行时操作符，也为有界输入提供了专门的操作符，如果你选择了 DataSet API 或 Table API，就可以使用这些操作符。

![](https://ww1.sinaimg.cn/large/007rAy9hgy1g25vrpwkhzj30v00gkabw.jpg)

# 链接

- https://read.douban.com/reader/ebook/114289022/?from=book
