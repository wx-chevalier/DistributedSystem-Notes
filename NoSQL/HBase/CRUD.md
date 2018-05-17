# Filter

基础 API 中的查询操作在面对大量数据的时候是非常苍白的，这里 Hbase 提供了高级的查询方法：Filter 。 Filter 可以根据簇、列、版本等更多的条件来对数据进行过滤，基于 Hbase 本身提供的三维有序(主键有序、列有序、版本有序)，这些 Filter 可以高效的完成查询过滤的任务。带有 Filter 条件的 RPC 查询请求会把 Filter 分发到各个 RegionServer，是一个服务器端(Server-side )的过滤器，这样也可以降低网络传输的压力。 要完成一个过滤的操作，至少需要两个参数。一个是抽象的操作符，Hbase 提供了枚举类型的变量来表示这些抽象的操作符：LESS/LESS_OR_EQUAL/EQUAL/NOT_EUQAL 等；另外一个就是具体的比较器(Comparator )，代表具体的比较逻辑，如果可以提高字节级的比较、字符串级的比较等。有了这两个参数，我们就可以清晰的定义筛选的条件，过滤数据。 CompareFilter ( CompareOp compareOp， WritableByteArrayComparable valueComparator )

     CompareFilter是高层的抽象类，下面我们将看到它的实现类和实现类代表的各种过滤条件。这里实现类实际上代表的是参数中的过滤器过滤的内容，可以使主键、簇名、列值等，这就是由CompareFilter决定了。
    行过滤器(RowFilter)
    行过滤器的比较对象是行主键

Scan scan = new Scan(); Filter filter1 = new RowFilter(CompareFIlter.CompareOp.LESS_OR_EUQAL, new BinaryComparator(Bytes.toBytes("hello"))); scan.setFilter(filter1); scan.close();

    例中的Filter会将所有的小于等于“Hello”的主键过滤出来。
    簇过滤器(FamilyFilter)
    簇过滤器过滤的是簇的名字。
    列过滤器(QualifierFilter)
    列过滤器过滤的是列的名字。
    值过滤器(ValueFilter)
    值过滤器过滤的是扫描对象的值。
    单值过滤器(SingleColumnValueFilter)
    单值过滤器是以特定列的值为过滤内容，与值过滤器不同的是，这里是特定的列，而值过滤器比较的是行内的所有列。所有在使用单值过滤器的时候要指定比较的列的坐标。

SingleColumnValueFilter(byte[] family, byte[] qualifier, CompareOp compareOp, WritableByteArrayComparable comparator)

    对于找不到该列的行，可以有特殊的处理

void setFilterIfMissing(boolean filterIfMissing)

    默认缺省行将被包含进过滤的结果集中。
    前缀过滤器(PrefixFilter)
    前缀过滤器将会过滤掉不匹配的记录，过滤的对象是主键的值。

PrefixFilter(byte[] prefix)

     页过滤器(PageFilter)
    页过滤器可以根据主键有序返回固定数量的记录，这需要客户端在遍历的时候记住页开始的地方，配合scan的startkey一起使用。

PageFilter(int size)

     键过滤器(KeyOnlyFilter)
    键过滤器可以简单的设置过滤的结果集中只包含键而忽略值，这里有一个选项可以把结果集的值保存为值的长度。
    FirstKeyOnlyFilter
    在键过滤器的基础上，根据列有序，只包含第一个满足的键。
    ColumnPrefixFilter
    这里过滤的对象是列的值。
    TimestampsFilter

TimestampsFilter(List<Long> times)

    这里参数是一个集合，只有包含在集合中的版本才会包含在结果集中。
    包装类过滤器，此类过滤器要通过包装其他的过滤器才有意义，是其他过滤器的一种加强。
    SkipFilter

SkipFilter(Filter filter)

    过滤器集合(FilterList)
    Hbase的过滤器设计遵照于设计模式中的组合模式，以上的所有过滤器都可以叠加起来共同作用于一次查询。

## 计数器

Hbase 提供一个计数器工具可以方便快速的进行计数的操作，而免去了加锁等保证原子性的操作。但是实质上，计数器还是列，有自己的簇和列名。值得注意的是，维护计数器的值最好是用 Hbase 提供的 API，直接操作更新很容易引起数据的混乱。 计数器的增量可以是正数负数，正数代表加，负数代表减。

long icrementColumnValue(byte[] row, byte[] famuly, byte[] qualifier, long amount) Result increment(Increment increment)

## 协处理器(Coprocessor )

协处理器的思想是把处理的复杂代码分发到各个 RegionServer，使大部分的计算可以在服务器端，或者扫描的时候完成，提高处理的效率。形式上比较类似 RDBMS 中的存储过程，不同的是，存储过程的原理是在服务器端进行预处理等优化，而协处理器仅仅只是服务器处理，这里又有点类似于 Map-Reduce 中的 Map 阶段。 协处理器 (Coprocesssor) 有两种，一种是观察者(Obsever )另外一种是 Endpoint(LZ 跪了，实在不知道翻译成啥)。 每个协处理器都有一个优先级，优先级分为 USER/SYSTEM，优先级决定处理器的执行顺序，SYSTEM 级别的处理器永远先于 USER。 每个处理器都有自己的执行环境 (CoprocessorEnvironment)，这个环境包含当前集群和请求的状态等信息，是处理中重要的一部分，以构造函数参数的形式被传入到处理器。 另外就是 CoprocessorHost，这是 Hbase 管理协处理器的类，用来维护所有的处理器和其环境。

    协处理器的加载有两种方式，一种是通过配置文件，在配置文件中指定加载路径、类名等，通过这种方式加载的处理器都是SYSTEM级别的，会作用于所有的请求，所有的表；另一种方式是通过在创建表的时候在表中指定，这种方式既可以创建全局的SYSTEM级别的处理器，也可以创建USER级别的处理器，USER级别的处理器是针对表的。

Path path = new Paht("test.jar"); HTableDescriptor htd = new HTableDescriptor("test"); htd.addFamily(new HColumnDescriptor("family1")); htd.setValue("Coprocessor$1", path.toString + "|" + className + "|" + Coprocessor.Priority.USER); HBaseAdmin admin = new HBaseAdmin(conf); admin.createTable(htd);

    这里setValue方法有两个参数，第一个参数是协处理器的名字，$后面跟的是影响执行顺序的序号；第二个参数是<path>|<classname>|<priority>。
    Observer
    这是第一种处理器，观察者，观察者有三种，分别用来监听RegionServerObserver、MasterServerObserver、WALObserver。
    RegionServer监听的是Region Server上的操作，如在Region Server上的Get、Put等。操作被赋予生命周期：Pending open--open--Pending close
    监听器是可以监听生命周期中的各个阶段，并对其做出处理。
    每一个监听的方法都有一个上下文参数(Context)，通过Context参数可以直接的操作请求的声明周期。

void bypass(); void complete(); MasterObserver 监听的是 Master Server 上的操作，有点类似 RDBMS 中的 DDL 的操作如表操作、列操作等。 具体的操作和 RegionServer 比较类似。 Endpoint 这是第二种处理器，Endpoint 相当于被分发到各个 RegionServer 上的存储过程，可以在客户端远程调用的方法。Endpoint 的存在使我们可以进行一些服务器端的计算，如服务器聚集、求和等运算，弥补了查询 API 的不足。服务器端计算的优势是显而易见的，它可以降低网络传输的数据量，合理利用服务器资源。 从功能上可以看出 Endpoint 是一个基于 RPC 调用的模块，所以在实现自己的 Endpoint 时候需要定义我们自己的通信协议。在 Hbase 中，通信协议被抽象为 CoprocessorProtocol 接口，要实现我们的协议，我们要创建协议接口继承自 CoprocessorProtocol 接口，然后再实现我们的协议类。 \
public interface MyProtocol extends CoprocessorProtocol { public int work(); } 协议类本身也是处理器，所以还要继承 BaseEndpointCoprocessor 类。

public class MyEndpoint extends BaseEndpointCoprocessor implements MyProtocol { public int work() { Sytem.out.println("hello"); } } 在抽象的父类 BaseEndpointCoprocessor 中还提供了一些有用的方法，如我们可以拿到对应的环境类。 \
RegionCoprocessorEnvironment getEnvironment()

    配置好Endpoint重启集群环境以后，我们的实现类会被分发到各个RegionServer，通过HTable实例的方法我们可以调用到Endpoint。

<T extends CoprocessorProtocol, R> Map<byte[], R> coprocessorExec(Class<T> protocol, byte[] startKey, byte[] endKey, Batch.Call<T, R> callable);

    startKey和endKey用于确定哪些RegionServer将执行Endpoint， Batch中的内部类将决定协议中方法的调用。

四、 HTablePool 连接池 在 Hbase 中，创建一个代表表的 HTable 实例是一个耗时且很占资源的操作，类似操作数据库，我们也需要建立我们自己的连接池，于是有了代表连接池的抽象类：HTable 。

HTablePool(Configuaration conf, int maxSize) HTablePool(Configuaration conf, int maxSize, HTableInterfaceFactory factory)

     创建HTable需要配置文件的实例，连接池的最大连接数也在构造方法中设置。另外，如果想要自己控制HTable被创建的过程，则需要实现自己的工厂方法。在连接池中，最大连接数(maxSize)的含义是，连接池管理的最大的连接数，当所需要的连接数超过最大值时，会临时的创建连接来满足需求，但是这些连接在使用完毕之后会被直接释放且丢弃而不会进入连接池被管理，所以最大连接数代表的是连接池中最大被管理的连接数，而不是使用连接池最大可使用的连接数。

HTableInterface getTable(String tableName) HTableInterface getTable(byte[] tableName) void putTable(HTableInterface table)

     需要注意的是，使用完连接以后需要手动的调用putTable方法将连接放回池中。
