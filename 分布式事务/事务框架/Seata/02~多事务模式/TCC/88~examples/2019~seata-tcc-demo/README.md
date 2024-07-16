
## 0. 非正文

目录结构说明:

1. eureka-server：eureka注册中心
2. account-service：用户账户服务
3. storage-service：商品库存服务
4. order-service：订单服务
5. resources/database-sql：mysql数据库表结构的初始化脚本
6. resources/seata-server：seata server服务的配置文件 file.conf、registry.conf

启动顺序:

1. resources/database-sql：初始化数据库
2. eureka-server：运行注册中心
3. resources/seata-server：下载、安装、配置、启动 seata server服务
4. account-service：运行用户账户服务
5. storage-service：运行商品库存服务
6. order-service：运行订单服务
7. 测试：通过postman等工具，调用 order-server 的下订单接口

## 1. 前言
本文先通过分布式事务中tcc方案，衍生出seata的tcc模式，主要还是会通过代码示例来做介绍。[github代码地址](https://github.com/Kerry2019/seata-tcc-demo)可提前下载，该项目中包括数据库、seata配置，以及所有分布式服务的全部代码。大家如果想练练手，可以先拉取该项目代码，再结合本文学习。核心配置环境如下：

| 环境类型 | 版本号 |
| --- | --- |
| jdk | 1.8.0_251 |
| mysql | 8.0.22 |
| seata server | 1.4.1 |
### 1.1. tcc
我们前面有几篇文章都有介绍过分布式事务的方案，目前常见的分布式事务方案有：2pc、tcc和异步确保型。之前讲过用jta atomikos实现多数据源的 `2pc`，用 `异步确保型` 方案实现支付业务的事务等等，就是没专门讲过 `tcc` 的应用。

因为tcc方案的操作难度还是比较大的。不能单打独斗，最好需要依托一个成熟的框架来实现。常见的tcc开源框架有tcc-transaction、Hmily和ByteTCC等，不过他们不像seata背靠大厂，无法提供持续的维护，因此我更推荐seata的tcc方案。

### 1.2. seata

先说说seata吧，分布式事务的解决方案肯定不局限于上面说的三种，实际上五花八门。因为它的确很让人头疼，各位大神都想研发出最好用的框架。本文的主角 - `seata` ，就是阿里的一个开源项目。

seata提供了AT、TCC、SAGA 和 XA，一共4种事务模式。像AT模式就很受欢迎，我们在实现多数据源的事务一致性时，通常会选用 `2PC`的方案，等待所有数据源的事务执行成功，最后再一起提交事务。这个等待所有数据源事务执行的过程就比较耗时，即影响性能，也不安全。

而seata AT模式的做法就很灵活，它学习数据库的 undo log，每个事务执行时立即提交事务，但会把 undo 的回退sql记录下来。如果所有事务执行成功，清除记录 undo sql的行记录，如果某个事务失败，则执行对应 undo sql 回滚数据。在保证事务的同时，并发量也大了起来。

但我们今天要讲的是 seata TCC 模式，如果你对 Seata的其他模式感兴趣，可以上官网了解。


## 2. 业务
先讲一下示例的业务吧，我们还是拿比较经典的电商支付场景举例。假设支付成功后，涉及到三个系统事务：
1. 订单系统（order）：创建支付订单。
2. 库存系统（storage）：对应商品扣除库存。
3. 账户系统（account）：用户账户扣除响应金额。

### 2.1. tcc业务
按照tcc（try-confirm-cancel）的思路，这三个事务可以分别分解成下面的过程。
> **订单系统 order**
1. **try：** 创建订单，但是订单状态设置一个临时状态（如：status=0）。
2. **confirm：** try成功，提交事务，将订单状态更新为完全状态（如：status=1）。
3. **cancel：** 回滚事务，删除该订单记录。

> **库存系统 storage**
1. **try：** 将需要减少的库存量冻结起来。
2. **confirm：** try成功，提交事务，使用冻结的库存扣除，完成业务数据处理。
3. **cancel：** 回滚事务，冻结的库存解冻，恢复以前的库存量。

> **账户系统 account**
1. **try：** 将需要扣除的钱冻结起来。
2. **confirm：** try成功，提交事务，使用冻结的钱扣除，完成业务数据处理。
3. **cancel：** 回滚事务，冻结的钱解冻，恢复以前的账户余额。

### 2.2. 数据库
为了模拟分布式事务，上述的不同系统业务，我们通过在不同数据库中创建表结构来模拟。当然tcc的分布式事务不局限于数据库层面，还包括http接口调用和rpc调用等，但是异曲同工，可以作为示例参考。

下面先列出三张业务表的表结构，具体的sql可见最后附件。
> **表：order**

| 列名 | 类型 | 备注 |
| --- | --- | --- |
| id | int | 主键 |
| order_no | varchar | 订单号 |
| user_id | int | 用户id |
| product_id | int | 产品id |
| amount | int | 数量 |
| money | decimal | 金额 |
| status | int | 订单状态：0：创建中；1：已完结 |

> **表：storage**

| 列名 | 类型 | 备注 |
| --- | --- | --- |
| id | int | 主键 |
| product_id | int | 产品id |
| residue | int | 剩余库存 |
| frozen | int | TCC事务锁定的库存 |

> **表：account**

| 列名 | 类型 | 备注 |
| --- | --- | --- |
| id | int | 主键 |
| user_id | int | 用户id |
| residue | int | 剩余可用额度 |
| frozen | int | TCC事务锁定的金额 |


## 3. seata server
### 3.1. 下载

seata server 的安装包可直接从[官方github](https://github.com/seata/seata/releases)下载，下载压缩包后，解压到本地或服务器上。

### 3.2. 配置

Seata Server 的配置文件有两个：

* seata/conf/registry.conf
* seata/conf/file.conf

> **registry.conf**

Seata Server 要向注册中心进行注册，这样，其他服务就可以通过注册中心去发现 Seata Server，与 Seata Server 进行通信。Seata 支持多款注册中心服务：nacos 、eureka、redis、zk、consul、etcd3、sofa。我们项目中要使用 eureka 注册中心，eureka服务的连接地址、注册的服务名，这需要在 registry.conf 文件中对 `registry` 进行配置。

Seata 需要存储全局事务信息、分支事务信息、全局锁信息，这些数据存储到什么位置？针对存储位置的配置，支持放在配置中心，或者也可以放在本地文件。Seata Server 支持的配置中心服务有：nacos 、apollo、zk、consul、etcd3。这里我们选择最简单的，使用本地文件，这需要在 registry.conf 文件中对 `config` 进行配置。

> **file.conf**

file.conf 中对事务信息的存储位置进行配置，存储位置支持：file、db、redis。
这里我们选择数据库作为存储位置，这需要在 file.conf 中进行配置。

### 3.3. 启动

执行 seata/bin/seata-server.sh（windows 是 seata-server.bat） 脚本即可启动seata server。还可以配置下列参数：
``` 
-h：注册到注册中心的ip
-p：server rpc 监听端口，默认 8091
-m：全局事务会话信息存储模式，file、db，优先读取启动参数
-n：server node，多个server时，需要区分各自节点，用于生成不同区间的transctionId，以免冲突
-e：多环境配置
```

### 3.4. 常见问题
> **mysql 8**

默认启动后会报`mysql-connector-java-x.jar`驱动的错误，是因为seata server 默认不支持mysql 8。
可以在seata server的 lib 文件夹下替换 mysql 的驱动 jar 包。lib 文件夹下，已经有一个 jdbc 文件夹，把里面驱动版本为 8 的 mysql-connector-java-x.jar 包拷贝到外面 lib 文件夹下即可。

## 4. 代码

github示例项目中包括3个业务服务、1个注册中心，以及resources下的数据库脚本和seata server配置文件。按照服务的启动顺序，如下分类：

1. **resources/database-sql**：初始化数据库
2. **eureka-server**：运行 注册中心
3. **resources/seata-server**：下载、安装、配置、启动 seata server服务
4. **account-service**：运行 用户账户服务
5. **storage-service**：运行 商品库存服务
6. **order-service**：运行 订单服务
7. 测试：通过postman等工具，调用 order-server 的下订单接口


3个业务服务中，`order订单服务` 可以被称为“主事务”，当订单创建成功后，再在订单服务中调用 `account账号服务`和 `storage库存服务`两个“副事务”。因此从 seata tcc代码层面上，可以分成下面两类。
下文中不会列举业务代码，完整代码可以从github上查看，只会列出 seata 的相关代码和配置。
### 4.1. 主事务（order）
#### 4.1.1. application 配置文件

配置文件中需要配置 `tx-service-group`，需要注意的是，3个业务服务中都需要配置同样的值。

>application.yml
```yaml
spring:
  cloud:
    alibaba:
      seata:
        tx-service-group: order_tx_group
```
#### 4.1.2. seata配置文件
在application.yml同级目录，即 resources 目录下，创建两个seata 的配置文件。还记得在seata server 启动的时候也有这两个文件，但内容不一样，不要混淆了。

>file.conf
```
transport {
  type = "TCP"
  server = "NIO"
  heartbeat = true
  enableClientBatchSendRequest = true

  threadFactory {
    bossThreadPrefix = "NettyBoss"
    workerThreadPrefix = "NettyServerNIOWorker"
    serverExecutorThread-prefix = "NettyServerBizHandler"
    shareBossWorker = false
    clientSelectorThreadPrefix = "NettyClientSelector"
    clientSelectorThreadSize = 1
    clientWorkerThreadPrefix = "NettyClientWorkerThread"
    bossThreadSize = 1
    workerThreadSize = "default"
  }
  shutdown {
    wait = 3
  }
  serialization = "seata"
  compressor = "none"
}
service {
  vgroupMapping.order_tx_group = "seata-server"
  order_tx_group.grouplist = "127.0.0.1:8091"
  enableDegrade = false
  disableGlobalTransaction = false
}

client {
  rm {
    asyncCommitBufferLimit = 10000
    lock {
      retryInterval = 10
      retryTimes = 30
      retryPolicyBranchRollbackOnConflict = true
    }
    reportRetryCount = 5
    tableMetaCheckEnable = false
    reportSuccessEnable = false
  }
  tm {
    commitRetryCount = 5
    rollbackRetryCount = 5
  }
  undo {
    dataValidation = true
    logSerialization = "jackson"
    logTable = "undo_log"
  }
  log {
    exceptionRate = 100
  }
}
```
>registry.conf
```
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "eureka"

  eureka {
    serviceUrl = "http://localhost:8761/eureka"
  }
}

config {
  # file、nacos 、apollo、zk、consul、etcd3、springCloudConfig
  type = "file"
  file {
    name = "file.conf"
  }
}
```
#### 4.1.3. @LocalTCC tcc服务

这是配置 TCC 子服务的核心代码，

* **@LocalTCC：**
该注解需要添加到上面描述的接口上，表示实现该接口的类被 seata 来管理，seata 根据事务的状态，自动调用我们定义的方法，如果没问题则调用 Commit 方法，否则调用 Rollback 方法。
* **@TwoPhaseBusinessAction：**
该注解用在接口的 Try 方法上。
* **@BusinessActionContextParameter：**
该注解用来修饰 Try 方法的入参，被修饰的入参可以在 Commit 方法和 Rollback 方法中通过 BusinessActionContext 获取。
* **BusinessActionContext：**
在接口方法的实现代码中，可以通过 BusinessActionContext 来获取参数, BusinessActionContext 就是 seata tcc 的事务上下文，用于存放 tcc 事务的一些关键数据。BusinessActionContext 对象可以直接作为 commit 方法和 rollbakc 方法的参数，Seata 会自动注入参数。

OrderTccAction.java
```java
@LocalTCC
public interface OrderTccAction {

    /**
     * try 尝试
     *
     * BusinessActionContext 上下文对象，用来在两个阶段之间传递数据
     * BusinessActionContextParameter 注解的参数数据会被存入 BusinessActionContext
     * TwoPhaseBusinessAction 注解中commitMethod、rollbackMethod 属性有默认值，可以不写
     *
     * @param businessActionContext
     * @param orderNo
     * @param userId
     * @param productId
     * @param amount
     * @param money
     * @return
     */
    @TwoPhaseBusinessAction(name = "orderTccAction")
    boolean prepareCreateOrder(BusinessActionContext businessActionContext,
                               @BusinessActionContextParameter(paramName = "orderNo") String orderNo,
                               @BusinessActionContextParameter(paramName = "userId") Long userId,
                               @BusinessActionContextParameter(paramName = "productId") Long productId,
                               @BusinessActionContextParameter(paramName = "amount") Integer amount,
                               @BusinessActionContextParameter(paramName = "money") BigDecimal money);

    /**
     * commit 提交
     * @param businessActionContext
     * @return
     */
    boolean commit(BusinessActionContext businessActionContext);

    /**
     * cancel 撤销
     * @param businessActionContext
     * @return
     */
    boolean rollback(BusinessActionContext businessActionContext);
}
```

OrderTccActionImpl.java
```java
@Slf4j
@Component
public class OrderTccActionImpl implements OrderTccAction {
    private final OrderMapper orderMapper;
    public OrderTccActionImpl(OrderMapper orderMapper){
        this.orderMapper=orderMapper;
    }
    
    /**
     * try 尝试
     *
     * BusinessActionContext 上下文对象，用来在两个阶段之间传递数据
     * BusinessActionContextParameter 注解的参数数据会被存入 BusinessActionContext
     * TwoPhaseBusinessAction 注解中commitMethod、rollbackMethod 属性有默认值，可以不写
     *
     * @param businessActionContext
     * @param orderNo
     * @param userId
     * @param productId
     * @param amount
     * @param money
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean prepareCreateOrder(BusinessActionContext businessActionContext,
                                      String orderNo,
                                      Long userId,
                                      Long productId,
                                      Integer amount,
                                      BigDecimal money) {
        orderMapper.save(new OrderDO(orderNo,userId, productId, amount, money, 0));
        ResultHolder.setResult(OrderTccAction.class, businessActionContext.getXid(), "p");
        return true;
    }

    /**
     * commit 提交
     *
     * @param businessActionContext
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean commit(BusinessActionContext businessActionContext) {
        //检查标记是否存在，如果标记不存在不重复提交
        String p = ResultHolder.getResult(OrderTccAction.class, businessActionContext.getXid());
        if (p == null){
            return true;
        }

        /**
         * 上下文对象从第一阶段向第二阶段传递时，先转成了json数据，然后还原成上下文对象
         * 其中的整数比较小的会转成Integer类型，所以如果需要Long类型，需要先转换成字符串在用Long.valueOf()解析。
         */
        String orderNo = businessActionContext.getActionContext("orderNo").toString();
        orderMapper.updateStatusByOrderNo(orderNo, 1);
        //提交完成后，删除标记
        ResultHolder.removeResult(OrderTccAction.class, businessActionContext.getXid());
        return true;
    }

    /**
     * cancel 撤销
     *
     * 第一阶段没有完成的情况下，不必执行回滚。因为第一阶段有本地事务，事务失败时已经进行了回滚。
     * 如果这里第一阶段成功，而其他全局事务参与者失败，这里会执行回滚
     * 幂等性控制：如果重复执行回滚则直接返回
     *
     * @param businessActionContext
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean rollback(BusinessActionContext businessActionContext) {
        //检查标记是否存在，如果标记不存在不重复提交
        String p = ResultHolder.getResult(OrderTccAction.class, businessActionContext.getXid());
        if (p == null){
            return true;
        }
        String orderNo = businessActionContext.getActionContext("orderNo").toString();
        orderMapper.deleteByOrderNo(orderNo);
        //提交完成后，删除标记
        ResultHolder.removeResult(OrderTccAction.class, businessActionContext.getXid());
        return true;
    }

}
```

#### 4.1.4. @GlobalTransactional 全局服务

`@GlobalTransactional` 注解是唯一作用到“主事务”的方法。该注解加在“主事务”调用“副事务”的方法上。

OrderServiceImpl.java
```java
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderTccAction orderTccAction;
    private final AccountFeign accountFeign;
    private final StorageFeign storageFeign;

    public OrderServiceImpl(OrderTccAction orderTccAction, AccountFeign accountFeign, StorageFeign storageFeign){
        this.orderTccAction=orderTccAction;
        this.accountFeign=accountFeign;
        this.storageFeign=storageFeign;
    }

    /**
     * 创建订单
     * @param orderDO
     */
    @GlobalTransactional
    @Override
    public void createOrder(OrderDO orderDO) {
        String orderNo=this.generateOrderNo();
        //创建订单
        orderTccAction.prepareCreateOrder(null,
                orderNo,
                orderDO.getUserId(),
                orderDO.getProductId(),
                orderDO.getAmount(),
                orderDO.getMoney());
        //扣余额
        accountFeign.decreaseMoney(orderDO.getUserId(),orderDO.getMoney());
        //扣库存
        storageFeign.decreaseStorage(orderDO.getProductId(),orderDO.getAmount());
    }

    private String generateOrderNo(){
        return LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")
                );
    }
}
```

### 4.2. 副事务（account、storage）

account 和 storage 两个服务相比较于 order，只少了 “4.1.4. @GlobalTransactional 全局服务”，其他的配置完全一样。因此，这里就不再赘言了。

## 5. 总结

> **测试**

通过调用“主事务” order-service 的创建订单接口，来模拟分布式事务。我们可以通过在3个业务服务的不同代码处故意抛出错误，看是否能够实现事务的一致回滚。

> **seata框架表结构**

在 /resources/database-sql 的数据库脚本中，各自还有一些 seata 框架本身的表结构，用于存储分布式事务各自的中间状态。因为这个中间状态很短，一旦事务一致性达成，表数据就会自动删除，因此平时我们无法查看数据库。

因为seata tcc模式，会一直阻塞到所有的 try执行完毕，再执行后续的。从而我们可以通过在部分业务服务try的代码中加上`Thread.sleep(10000)`，强制让事务过程变慢，从而就可以看到这些 seata 表数据。

> **幂等性**

tcc模式中，`Commit` 和 `Cancel` 都是有自动重试功能的，处于事务一致性考虑，重试功能很有必要。但我们就一定要慎重考虑方法的 `幂等性`，示例代码中的ResultHolder类并不是个好方案，还是要在Commit、Cancel业务方法本身做幂等性要求。 
