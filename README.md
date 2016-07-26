<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [infrastructure-handbook](#infrastructure-handbook)
- [Distributed:分布式系统](#distributed%E5%88%86%E5%B8%83%E5%BC%8F%E7%B3%BB%E7%BB%9F)
  - [Model:分布式模型](#model%E5%88%86%E5%B8%83%E5%BC%8F%E6%A8%A1%E5%9E%8B)
    - [Synchronization:同步](#synchronization%E5%90%8C%E6%AD%A5)
      - [ConsensusProtocol:一致性协议](#consensusprotocol%E4%B8%80%E8%87%B4%E6%80%A7%E5%8D%8F%E8%AE%AE)
        - [Paxos](#paxos)
- [MiddleWare:中间件](#middleware%E4%B8%AD%E9%97%B4%E4%BB%B6)
- [Network](#network)
  - [Protocol](#protocol)
    - [HTTP](#http)
      - [HTTP/2](#http2)
    - [HTTPS](#https)
- [Storage](#storage)
  - [DataBase](#database)
    - [KeyValue](#keyvalue)
      - [Redis](#redis)
        - [DataType:数据类型](#datatype%E6%95%B0%E6%8D%AE%E7%B1%BB%E5%9E%8B)
        - [Management:管理](#management%E7%AE%A1%E7%90%86)
        - [Optimization:优化](#optimization%E4%BC%98%E5%8C%96)
        - [Script:脚本](#script%E8%84%9A%E6%9C%AC)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# infrastructure-handbook
My Handbook For Infrastructure

# Distributed:分布式系统

## Model:分布式模型

### Synchronization:同步

#### ConsensusProtocol:一致性协议

##### Paxos

- [Paxos原理与实践](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Distributed/Model/Synchronization/ConsensusProtocol/Paxos/paxos.md)

# MiddleWare:中间件

# Network

## Protocol

### HTTP

- [HTTP 基础](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http.md)
- [HTTP Request](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-request.md)
- [HTTP Response](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-response.md)
- [HTTP 缓存](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-cache.md)

#### HTTP/2

### HTTPS

- [HTTPS原理与实践](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTPS/HTTPS.md)


# Storage
## DataBase
### KeyValue
#### Redis
- [Redis基础与配置](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/redis.md)
- [Redis命令进阶](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/redis-advancedcommands.md)

##### DataType:数据类型
- [Redis基础数据类型](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/DataType/redis-datatypes.md)
- [Redis中的集合类型](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/DataType/redis-datatypes-collection.md)
- [Redis中的数据队列:订阅/优先权队列](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/DataType/redis-messagequeue.md)

##### Management:管理
- [Redis数据持久化](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/Management/redis-persistence.md)
- [Redis数据迁移](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/Management/redis-migration.md)

##### Optimization:优化
- [Redis性能优化](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/Optimization/redis-optimization.md)

##### Script:脚本
- [Redis脚本](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Storage/DataBase/KeyValue/Redis/Script/redis-script.md)
