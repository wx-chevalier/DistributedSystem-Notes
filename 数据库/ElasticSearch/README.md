# ElasticSearch

ElasticSearch 是一个基于 [Apache Lucene(TM)](https://lucene.apache.org/core/) 的开源搜索引擎。无论在开源还是专有领域，Lucene 可以被认为是迄今为止最先进、性能最好的、功能最全的搜索引擎库。但是，Lucene 只是一个库。想要使用它，你必须使用 Java 来作为开发语言并将其直接集成到你的应用中，更糟糕的是，Lucene 非常复杂，你需要深入了解检索的相关知识来理解它是如何工作的。ElasticSearch 也使用 Java 开发并使用 Lucene 作为其核心来实现所有索引和搜索的功能，但是它的目的是通过简单的`RESTful API` 来隐藏 Lucene 的复杂性，从而让全文搜索变得简单。不过，Elasticsearch 不仅仅是 Lucene 和全文搜索，我们还能这样去描述它：

- 分布式的实时文件存储，每个字段都被索引并可被搜索
- 分布式的实时分析搜索引擎
- 可以扩展到上百台服务器，处理 PB 级结构化或非结构化数据

# 目录结构

```sh
home---这是Elasticsearch解压的目录
　　bin---这里面是ES启动的脚本

　　conf---elasticsearch.yml为ES的配置文件

　　data---这里是ES得当前节点的分片的数据，可以直接拷贝到其他的节点进行使用

　　logs---日志文件

　　plugins---这里存放一些常用的插件，如果有一切额外的插件，可以放在这里使用。
```

# 链接

- 提取其中知识脑图 https://mp.weixin.qq.com/s/pWU-c0hfTi51SPtFT4iFKA

- https://mp.weixin.qq.com/s/Zgcx45R2HPDV6az37g8HzA?from=groupmessage&isappinstalled=0