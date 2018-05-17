SQL Server 2016有望提供JSON操作原生支持。这一支持的首次迭代将作为SQL Server 2016 CTP 2的一部分发布。CTP又名社区技术预览版，等同于微软的公开Alpha测试，期间，开发者可以提出技术上的修改建议。

**JSON导出**

“格式化和导出”JSON是CTP 2的主要功能。这可以通过在SELECT语句的末尾增加FOR JSON子句实现。该功能基于FOR XML子句，并且与它类似，允许对结果JSON字符串进行自动和半手工格式化。

微软希望这一语法可以提供与PostgreSQL的row_to_json和json_object函数相同的功能。

**JSON转换**

大部分功能将在CTP 3中发布。这些功能中的第一项是FROM OPENJSON子句，这是一个表值函数(TFV)，它接受一个JSON字符串作为输入。它还需要一个数组或对象在被解析的JSON数据中的路径。

默认情况下，OPENJSON返回一个键值对集合，但开发者可以使用WITH子句提供一个模式。由于JSON不支持日期或整数(它们分别表示为字符串和double类型)，所以WITH子句可以减少稍后需要的类型转换数量。

下面是[Jovan Popovic博文中关于JSON支持](http://blogs.msdn.com/b/jocapc/archive/2015/05/16/json-support-in-sql-server-2016.aspx)的例子。

```
DECLARE @JSalestOrderDetails nVarCar(2000) = N '{"OrdersArray": [
{"Number":1, "Date": "8/10/2012", "Customer": "Adventure works", "Quantity": 1200},
{"Number":4, "Date": "5/11/2012", "Customer": "Adventure works", "Quantity": 100},
{"Number":6, "Date": "1/3/2012", "Customer": "Adventure works", "Quantity": 250},
{"Number":8, "Date": "12/7/2012", "Customer": "Adventure works", "Quantity": 2200}
]}';

SELECT Number, Customer, Date, Quantity
FROM OPENJSON (@JSalestOrderDetails, '$.OrdersArray')
WITH (
    Number varchar(200),
    Date datetime,
    Customer varchar(200),
    Quantity int
) AS OrdersArray
```

微软宣称，在PostgrSQL中实现同样的功能需要综合使用json_each、json_object_keys、json_populate_record和json_populate_recordset函数。

**JSON存储**

正如所见，JSON数据存储在NVARCHAR变量中。在表的列中存储JSON数据也是这样。关于为什么这么做，微软有如下理由：

> - 迁移——我们发现，人们早已将JSON存储为文本，因此，如果我们引入一种单独的JSON类型，那么为了使用我们的新特性，他们将需要修改数据库模式，并重新加载数据。而采用现在这种实现方式，开发者不需要对数据库做任何修改就可以使用JSON功能。
> - 跨功能的兼容性——所有SQL Server组件均支持NVARCHAR，因此，JSON也将在所有的组件中得到支持。开发者可以将JSON存储在Hekaton、时态表或列存储表中，运用包括行级安全在内的标准安全策略，使用标准B树索引和FTS索引，使用JSON作为参数或返回过程值，等等。开发者不需要考虑功能X是否支持JSON——如果功能X支持NVARCHAR，那么它也支持JSON。此外，该特性有一些约束——Hekaton及列存储不支持LOB值，所以开发者只能存储小JSON文档。不过，一旦我们在Hekaton及列存储中增加了LOB支持，那么开发者就可以在任何地方存储大JSON文档了。
> - 客户端支持——目前，我们没有为客户端应用程序提供标准JSON对象类型(类似XmlDom对象的东西)。自然地，Web和移动应用程序以及JavaScript客户端将使用JSON文本，并使用本地解析器解析它。在JavaScript中，可以使用object类型表示JSON。我们不太可能实现一些仅在少数RDBMS中存在的JSON类型代理。在C#.Net中，许多开发者使用内置了JObject或JArray类型的JSON.Net解析器；不过，那不是一种标准，也不太可能成为ADO.NET的一部分。即便如此，我们认为，C#应用可以接受来自数据库层的纯字符串，并使用最喜欢的解析器解析它。我们所谈论的内容不只是跟应用程序有关。如果开发者试图在SSIS/SSRS、Tableau和Informatica ETL中使用JSON列，它们会将其视为文本。我们认为，即使我们增加了JSON类型，在SQL Server之外，它仍将被表示成字符串，并根据需要使用某个自定义的解析器解析它。因此，我们并没有找到任何重大的理由将其实现为一种原生JSON类型。

在包含JSON的NVARCHAR列上使用新的ISJSON函数作为检查约束是个不错的主意。如果不这样做，那么有缺陷的客户端应用程序就可能插入不可解析的字符串，使开发者面临数据污染的风险。

**JSON查询**

如果直接对JSON进行标量查询，可以使用JSON_VALUE函数。该函数使用一种类似JavaScript的符号定位JSON对象中的值。它使用$符号表示object的根，点号表示属性，方括号表示数组索引。它与PostgreSQL中的json_extract_path_text函数等效。

**JSON索引**

JSON数据可以直接索引，但开发者可以毫不费力地在标量数据上实现同样的效果。只需要使用JSON_VALUE函数创建一个计算列，然后在这个列上创建索引。
