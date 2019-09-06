# 图数据库

> A graph database is a database that uses graph structures for semantic queries with nodes, edges and properties to represent and store data. A key concept of the system is the graph (or edge or relationship), which directly relates data items in the store a collection of nodes of data and edges representing the relationships between the nodes. The relationships allow data in the store to be linked together directly, and in many cases retrieved with one operation. Graph databases hold the relationships between data as a priority. Querying relationships within a graph database is fast because they are perpetually stored within the database itself. Relationships can be intuitively visualized using graph databases, making it useful for heavily inter-connected data.

典型的图结构由点，边和点边对应属性构成。图数据库则是针对图这种结构的独有特点⽽而专⻔门设计的⼀一种数据库，⾮非常适合⾼高度互连数据集的存储和查询。示例例中描述的是⼀一个极度简化的社交关系图，圆圈代表顶点(Vertex/Node), 有⽅方向的线条代表边(Edge)，顶点或者边都可以有属性(Property)。

![](https://i.postimg.cc/Y9kvBQ57/image.png)

# 应用场景

## 社交网络

图数据库可以轻松应对海量高度互连社交数据的实时存储和高效查询,帮助您快速构建复杂的社交网络系统。例如,在一个典型的社交网络中,常常会存在谁认识谁,谁上过什么学校,谁常住什么地方,谁喜欢什么餐馆之类的查询”,传统关系型数据库对于超过 3 度的查询往往会很低效甚至无法支持,但图数据库从基因层面提供了解决方案,轻松应对社交网络的各种复杂存储和查询场景。

## 欺诈检测

在金融领域,图数据库经常用于欺诈检测场景。例如,通过贷款、分期消费者的联系人(或者联系人的联系人)信用信息,对用户进行信用评分,如果评分较低,则拒绝贷款或者提升利率;通过申请人的个人信息(包括电话号码、家庭住址),判断申请人信息是否属实。通常,欺诈者是通过“黑市”购买的用户信息然后拼凑出的“个人信息”,并且这些信息会被反复使用,使用图数据库,可以快速的发现申请人提供的个人信息与现有的用户信息的关系。

## 推荐引擎

图数据库非常适合实时推荐场景。您可以将用户的购买行为,位置,好友关系,收藏等数据实时的存储在图数据库中,然后利用图数据库能对高度互连数据提供高效查询的特点,通过各种维度的快速查询实时进行多维度个性化推荐。例如,在某 APP 中,通过用户位置及以前的购买行为信息,当某用户 A 到达某商场 B,APP 可以向用户实时推荐附近的门店及商品等信息。

## 知识图谱

图数据库可以帮助您快速的构建知识图谱。您可以将图谱数据存储在图数据库中,既可以通过外部输入实时更新,也可以对图数据库内部图谱信息进行分析来不断发现并完善图谱数据。比如,基于图数据库,你可以快速实现像针对足球明星这样的知识图谱应用,帮助用户浏览,发现感兴趣的信息。

## 网络/IT 运营

图数据库非常适合网络/T 运营相关场景。比如,您可以将路由器,交换机,防火墙,服务器等各种网络设备和终端及其拓扑信息存储在图数据库中,当某服务器或终端遭受恶意攻击或者受到感染时,你可以通过图数据库快速分析并找到传播路径,然后进行相关追踪及处理。

# 链接

- https://my.oschina.net/u/4169309/blog/3096755
