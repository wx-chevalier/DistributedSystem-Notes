# 第15章 组合多个查询

重点：

* 简介用于组合查询的操作符
* 何时对查询进行组合
* GROUP BY子句与组合命令
* ORDER BY与组合命令
* 如何获取准确的数据

利用UNION、UNION ALL、INTERSECT、 EXCEPT把多个SQL查询组合为一个

## 15.1 单查询与组合查询

单查询是一个SELECT语句，组合查询具有多个SELECT语句，由负责结合两个查询的操作符组成。组合操作符用于组合和限制两个SELECT语句的结果，他们可以返回或清除重复的记录，可以获取不同字段里的类似数据。



## 15.2 组合查询操作符

ANSI标准包括UNION、UNION ALL、EXCEPT、INTERSECT

### 15.2.1 UNION

组合两个或多个SELECT语句的结果，不包含重复的记录

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
UNION
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

注：使用UNION时，每个SELECT语句里必须选择同样数量的字段、同样数量的字段表达式、同样的数据类型、同样的次序—但长度不必一样。另外，使用UNION结合两个返回不同字段的SELECT语句，返回结果的列标题有第一个查询的字段名称决定，相同的列序号会被合并成一列

### 15.2.2 UNION ALL

组合两个或多个SELECT的结果，且包含重复的结果，其他与UNION一样

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
UNION ALL
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

### 15.2.3 INTERSECT

组合两个SELECT语句，但只返回两个SELECT语句中相同的记录。好像MySQL还不支持

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
INTERSECT
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

### 15.2.4 EXCEPT

组合两个SELECT语句，返回第一个SELECT语句里有第二个SELECT语句里没有的记录。MySQL不支持

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
EXCEPT
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

可以用MINUS代替(MySQL也没有)

## 15.2 组合查询里使用ORDER BY

ORDER BY子句只能用于对全部查询结果的排序，因此组合查询只能有一个ORDER BY子句，且只能以别名或数字来引用字段

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
OPERATOR{UNION|EXCEPT|INTERSECT|UNION ALL}
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
[ORDER BY]
```

注：如果排序的字段在全部查询语句里都有相同的名称，它的名称可以用于ORDER BY子句里

```
SELECT PROD_DESC FROM PRODUCTS_TBL
UNION
SELECT PROD_DESC FROM PRODUCTS_TBL
ORDER BY PROD_DESC
```

## 15.4 组合查询里使用GROUP BY

与ORDER BY不同，GROUP BY可用于组合查询中的每一个SELECT语句，也可用于全部查询的结果。另外，HAVING子句也可用于组合查询里的每个SELECT语句。

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
[GROUP BY]
[HAVING]
OPERATOR {UNION|EXPECT|INTERSECT|UNION ALL}
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
[GROUP BY]
[HAVING]
[ORDER BY]
```

## 15.5 获取准确的数据

使用组合查询时要小心，在使用INTERSECT操作符时，如果第一个查询的SELECT语句有问题，就可能会得到不正确或不完整的数据。



# 第16章 利用索引改善性能

重点：

* 索引如何工作
* 创建索引
* 不同类型的索引
* 何时使用索引
* 何时不适用索引



## 16.1 什么是索引

索引是一个指针，指向数据在表里的准确物理位置。

索引通常与相应的表是分开保存的，主要目的是提高数据检索的性能。

索引的创建与删除不会影响到数据本身，但会影响数据检索的速度。

索引会占据物理存储空间，且可能比表本身还大。

## 16.2 索引是如何工作的

索引创建之后，用于记录与被索引字段相关联的位置值。当表里添加新数据时，索引里也会添加新项。执行查询，且WHERE条件里指定的字段设置了索引时，数据库会首先在索引里搜索WHERE子句里指定的值。如果在索引中找到了这个值，索引就返回给搜索数据在表里的实际位置。如果没哟索引，查询时，数据库会进行全表扫描。

索引通常以一种树形结构保存信息。

## 16.3 CREATE INDEX命令

创建索引()不同实现，创建方式不同)

```
CREATE INDEX INDEX_NAME ON TABLE_NAME
```

## 16.4 索引的类型

### 16.4.1 单字段索引

如果某个字段经常在WHERE子句作为单独的查询条件，它的单子段索引最有效。

单子段索引是基于一个字段创建的：

```
CREATE INDEX INDEX_NAME
ON TABLE_NAME(COLUMN_NAME)
```

### 16.4.2 唯一索引

唯一索引用于改善性能和保证数据完整性。只能用于表里没有重复值的字段，其他与普通索引一样

```
CREATE UNIQE INDEX INDEX_NAME
ON TABLE_NAME(COLUMN_NAME)
```

注：定义表的主键时，会创建一个默认的索引。允许NULL值的字段上也不能创建唯一索引。

### 16.4.3 组合索引

组合索引是基于一个表里两个或多个字段的索引。

```
CREATE INDEX INDEX_NAME
ON TABLE_NAME(COLUMN1,COLUMN2)
```



注：字段在索引里的次序对数据检索速度有很大的影响，一般来说，最具有限制的值应该排在前面。

### 16.4.4 隐含索引

隐含索引是数据库服务程序在创建对象时自动创建的，如为主键约束和唯一性约束自动创建索引。

## 16.5 何时考虑使用索引

一般来说，大多数用于表结合的字段都应该设置索引。

经常在ORDER BY和GROUP BY里引用的字段应该考虑设置索引。

具有大量唯一值的字段，或是在WHERE子句里会返回很小部分记录的字段，可以考虑设置索引。

## 16.6 何时应该避免使用索引

使用索引的方针：

* 索引不应该用于小规模的表
* 当字段用于WHERE子句作为过滤器会返回表里的大部分记录时，该字段不适合设置索引
* 经常会被批量更新的表可以具有索引，但批量操作的性能会由于索引而降低，因为索引也会被更新(一般在批量操作前去除索引，完成后再创建索引)
* 不应该对包含大量NULL值的字段设置索引。
* 经常被操作的字段不应该设置索引，因为对索引的维护会变得很繁重

## 16.7 修改索引

```
ALTER INDEX INDEX_NAME
```

## 16.8 删除索引

```
DROP INDEX INDEX_NAME ON TABLE_NAME
```



# 第17章 改善数据库性能

重点：

* 什么是SQL语句调整
* 数据库调整与SQL语句调整
* 格式化SQL语句
* 适当地结合表
* 最严格的条件
* 全表扫描
* 使用索引
* 避免使用OR和HAVING
* 避免大规模排序操作

## 17.1 什么是SQL语句调整

SQL语句调整是优化生成SQL语句的过程，从而以最有效和最高效的方式获得结果。

SQL语句调整主要涉及调整语句的FROM和WHERE子句，因为数据库服务程序主要根据这两个子句执行查询。

## 17.2 数据库调整和SQL语句调整

数据库调整是调整实际数据库的过程，调整的目标是确保数据库的设计能够最好地满足用户对数据库操作的需要

SQL调整是调整访问数据库的SQL语句，包括数据库查询和事务操作。目标是利用数据库和系统资源、索引，针对数据库的当前状态进行最有效的访问，从而减少对数据库执行查询所需的开销。

注：两种调整缺一不可，相互配合

## 17.3 格式化SQL语句

### 17.3.1 为提高可读性格式化SQL语句

语句的整洁程度并不会影响实际的性能，但方便修改和调整

良好可读性的基本规则：

* 每个子句都以新行开始
* 子句里的参数超过一行长度需要换行时，利用TAB或空格来缩进
* 以一致的方式使用TAB和空格
* 当语句里使用多个表时，使用表的别名
* 有节制地使用注释(如果允许的话)
* 如果SELECT语句里要使用多个字段，就让每个字段都从新行开始
* FROM子句里要使用多个表，让每个表名都从新行开始
* 让WHERE子句里每个条件都以新行开始，看清所有条件和次序

### 17.3.2 FROM子句里的表

FROM子句里表的次序对性能有很大影响，取决于优化器如何读取SQL语句。通常，较小的表列在前面，较大的表列在后面，有更好的性能

### 17.3.3 结合条件的次序

有基表时，基表的字段一般放到结合操作的右侧，要被结合的表通常按照从小到大的次序排列，就像FROM子句里表的排列顺序一样

如果没有基表，表应该从小到大排列，让最大的表位于WHERE子句里结合操作的右侧，结合条件应该位于WHERE子句的最前面，其后才是过滤条件

```
FROM TABLE1,                    Smallest table
     TABLE2,                    to
     TABLE3                     Largest(base) table
WHERE TABLE1.COLUMN=TABLE3.COLUMN  Join Condition
  AND TABLE2.COLUMN=TABLE3.COLUMN  Join Condition
 [AND CONDITION1]                  Filter Condition
 [AND CONDITION2]                  Filter Condition
```

注：由于结合操作通常会从表里返回大部分数据，因此结合条件应该在更严格的条件之后再生效。

### 17.3.4 最严格条件

最严格条件通常是SQL查询达到最优性能的关键因素。

最严格条件是WHERE子句里返回最少记录的条件，对返回的数据进行了最大限度的过滤

应该让SQL优化器首先计算最严格条件，减少查询开销。最严格条件的位置取决于优化器的工作方式。

```
# 假设优化器从WHERE子句的底部开始读取
FROM TABLE1,
     TABLE2,
     TABLE3
WHERE TABLE1.COLUMN=TABLE3.COLUMN
  AND TABLE2.COLUMN=TABLE3.COLUMN 
 [AND CONDITION1]               least restrictive
 [AND CONDITION2]               Most restrictive
```

注：最好使用具有索引的字段作为查询里的最严格条件，从而改善性能

## 17.4 全表扫描

没有使用索引时，就会发生全表扫描。读取大规模的表时，应该避免进行全表扫描。

避免全表扫描最简单的方法：在WHERE子句中设置条件来过滤返回的数据

应该被索引的数据：

* 主键
* 外键
* 在结合表里经常使用的字段
* 经常在查询里作为条件的字段
* 大部分值是唯一值的字段。

注：对小型表，或是返回表里的大部分记录的查询，应该执行全表扫描

## 17.5 其他性能考虑

* 使用LIKE操作符和通配符：在查询里使用通配符能够消除很多可能返回的记录，对于搜索类似数据(不等于特定值的数据)的查询，通配符非常灵活。
* 避免使用OR操作符：在SQL语句里使用IN代替OR能够提高数据检索速度。
* 避免使用HAVING子句：HAVING子句可以减少GROUP BY子句返回的数据，但会让SQL优化器进行额外的工作，花费额外的时间
* 避免大规模排序操作：最好把大规模排序在批处理过程里，在数据库使用的非繁忙期运行
* 使用存储过程：为经常运行的SQL语句创建存储过程(经过编译的、以可执行格式永久保存在数据库里的SQL语句)
* 在批加载时关闭索引：当用户向数据库提交一个事务时，表和相关联的索引都会有数据变化。在批量加载时，索引可能会严重地降低性能(删除索引——批处理——创建索引：这样还可以减少索引里的碎片)

## 17.6 基于成本的优化

基于成本的优化根据资源消耗量对SQL语句进行排序。根据查询的衡量方法(执行时间、读库次数等)以及给定时间段内的执行次数，可以确定资源消耗量：
$$
总计资源消耗=衡量方法 \times 执行次数
$$
## 17.7 性能工具

待补充...



# 第18章 管理数据库用户

# 第19章 管理数据库安全

# 第20章 创建和使用视图及异名

## 20.1 什么是视图

视图是一个虚拟表，对于用户来说，视图在外观和行为上类似表，但它不需要实际的物理存储，但也被看做是一个数据库对象，它从表里引用数据。在数据库里，视图的使用方式与表一样，可以向操作表一样从视图里获取数据。视图只保存在内存里，且只保存其定义本身。

视图实际上由预定义查询形式的表所组成。视图可以包含表的全部或部分记录，可以由一个表或多个表创建。

创建视图时，实际上是在数据库里执行了一个SELECT语句，它定义了这个视图。



## 20.1.1 使用视图来简化数据访问

数据在表里的格式可能不适合终端用户查询，使用视图简化查询。

### 20.1.2 使用视图作为一种安全形式

视图可以限制用户只访问表里的特定字段或满足一定条件的记录。

### 20.1.3 使用视图维护摘要数据

如果摘要数据报告所基于的表经常更新、或是报告经常被创建，可以使用视图来包含摘要数据。

## 20.2 创建视图

可以从一个、多个表或另一个视图来创建视图。创建视图，用户必须拥有适当的系统权限。

```
CREATE [RECURSIVE]VIEW VIEW_NAME
[COLUMN_NAME[,COLUMN_NAME]]
[OF UDT NAME [UNDER TABLE NAME]
[REF IS COLUMN NAME SYSTEM GENERATED|USER GERNERATED|DERIVED]
[COLUMN NAME WITH OPTIONS SCOPE TABLE_NAME]]
AS
{SELECT STATEMENT}
[WITH [CASCADED|LOCAL] CHECK OPTION]
```

### 20.2.1 从一个表创建视图

```
CREATE VIEW VIEW_NAME AS 
SELECT * | COLUMN1[,COLUMN2]
FROM TABLE_NAME
[WHERE EXPRESSION1[,EXPRESSION2]]
[WITH CHECK OPTION]
[GROUP BY]

#1
CREATE VIEW NAMES AS
SELECT CONCAT(LAST_NAME,FIRST_NAME,MIDDLE_NAME) NAME 
FROM EMPLOYEE_TBL;

SELECT * FROM NAMES;

#2
CREATE VIEW CITY_PAY AS
SELECT E.CITY,AVG(P.PAY_RATE) AVG_PAY
FROM EMPLOYEE_TBL E,
     EMPLOYEE_PAY_TBL P
WHERE E.EMP_ID=P.EMP_ID
GROUP BY E.CITY;

SELET * FROM CITY_PAY;
```

### 20.2.2 从多个表创建视图

通过在SELECT语句里使用JOIN，可以从多个表创建视图

```
CREATE VIEW VIEW_NAME AS
SELECT *|COLUMN1[,COLUMN2]
FROM TABLE1,TABLE2[,TABLE3]
WHERE TABLE1.COLUMN=TABLE3.COLUMN
 [AND TABLE2.COLUMN=TABLE3.COLUMN]
 [EXPRESSION1[,EXPRESSION2]]
[WITH CHECK OPTION]
[GROUP BY]
```

在从多个表创建视图时，这些表必须在WHERE子句里通过共同字段实现结合。



### 20.2.3 从视图创建视图

```
CREATE VIEW2 AS
SELECT * FROM VIEW1
```

视图创建视图可以具有多个层次(视图的视图，以此类推)，允许的层次取决于具体实现。

基于视图创建视图的唯一问题在于它们的可管理性，例如，基于VIEW1创建了VIEW2，基于VIEW2创建了VIEW3，如果VIEW1被删除，VIEW2和VIEW3也就不可用了。

注：如果从基表和从另一个视图创建视图具有一样的难度和效率，首选从基表创建视图。

## 20.3 WITH CHECK OPTION

WITH CHECK OPTION确保全部的UPDATE和INSERT语句满足视图定义里的条件。如果不满足条件，UPDATE或INSERT语句就会返回错误。实际上是通过查看视图定义是否被破坏来确保引用完整性。

```
CREATE VIEW EMPLOYEE_PAGERS AS
SELECT LAST_NAME,FIRST_NAME,PAGER
FROM EMPLOYEE_TBL
WHERE PAGER IS NOT NULL
WITH CHECK OPTION

INSERT INTO EMPLOYEE_PAGERS
VALUES('SMITH','JOHN',NULL)  //此时会报错，因为视图定义里PAGER不能为NULL
```

在基于视图创建另一个视图时，WITH CHECK OPTION有两个选项：CASCADED和LOCAL，CASCADED是默认选项。

在对基表进行更新时，CASCADED选项会检查所有底层视图、所有完整性约束，以及新视图的定义条件。LOCAL选项只检查两个视图的完整性约束和新视图的定义条件。因此CASCADED创建视图是更安全的做法。

## 20.4 从视图创建表

```
CREATE TABLE TABLE_NAME AS
SELECT *|COLUMN1[,COLUMN2]
FROM VIEW_NAME
[WHERE CONDITION1[,CONDITION2]]
[ORDER BY]
```

注：表包含实际的数据、占据物理存储空间；视图不包含数据，只需保存视图定义

## 20.5 视图与ORDER BY子句

CREATE VIEW语句里不能包含ORDER BY子句，但可以包含GROUP BY子句，起到类似ORDER BY子句的作用。

```
CREATE VIEW NAMES2 AS
SELECT CONCAT(LAST_NAME,FIRST_NAME,MIDDLE_NAME) NAME
FROM EMPLOYEE_TBL
GROUP BY CONCAT(LAST_NAME,FIRST_NAME,MIDDLE_NAME)
```

## 20.6 通过视图更新数据

在一定条件下，视图的底层数据可以进行更新：

* 视图不包含结合
* 视图不包含GROUP BY子句
* 视图不包含UNION语句
* 视图不包含对伪字段ROWNUM的任何引用
* 视图不包含任何组函数
* 不能使用DISTINCT子句
* WHERE子句包含的嵌套的表达式不能与FROM子句引用同一个表
* 视图可以执行INSERT、UPDATE和DELETE等语句。

## 20.7 删除视图

DROP VIEW命令用于从数据库里删除视图，有两个选项：

* RESTRICT：使用RESTRICT进行删除操作，而其他视图在约束里有所引用，删除操作会出错
* CASCADE：其他视图或约束被引用，DROP VIEW也会成功

## 20.8 嵌套视图对性能的影响

在查询中使用视图与使用表有着相同的性能。

嵌套的层数越多，搜索引擎为了获得一个执行计划而需要进行的分析工作就越多。尽量减少嵌套层数。

## 20.9 异名

异名就是表或视图的另一个名称，方便在访问其他用户的表或视图时不必使用完整限制名。

异名可以创建为：

* PUBLIC：可以被数据库里的其他用户使用，一般只有DBA或被授权用户可以创建
* PRIVATE：只能被所有者和拥有权限的用户使用，全部用户都以创建

注：因为MySQL不支持异名，下面就不细讲。

### 20.9.1 创建异名

### 20.9.2 删除异名



# 第21章 使用系统目录

## 21.1 什么是系统目录

系统目录是一些表和视图的集合，包含关于数据库的信息。每个数据库都有系统目录，其中定义了数据库的结构，还有数据库所包含数据的信息。

系统目录基本上是一组对象，包含了定义数据库里其他对象的信息、数据库本身的结构以及其他各种重要信息。

方便用户在无需知道数据库结构或进程的前提下查看数据库权限、对象

## 21.2 如何创建系统目录

系统目录由数据库创建时自动创建或DBA在数据创建之后立即创建。

## 21.3 系统目录包含什么内容

包含的内容有：

* 用户账户和默认设置
* 权限和其他安全信息
* 性能统计
* 对象大小估计
* 对象变化
* 表结构和存储
* 索引结构和存储
* 数据库其他对象的信息，比如视图、异名、触发器和存储过程
* 表约束和引用完整性信息
* 用户会话
* 审计信息
* 内部数据库设置
* 数据库文件的位置

系统目录由数据库服务程序维护。

### 21.3.1 用户数据

关于个人用户的全部信息

### 21.3.2 安全信息

用户标识、加密的密码、各种权限和权限组等

### 21.3.3 数据库设计信息

数据库的信息：创建日期、名称、对象大小估计、数据文件的大小和位置等

### 21.3.4 性能统计

SQL语句性能的信息：优化器执行SQL语句的时间和方法等

内存分配和使用、数据库里剩余空间、控制表哥和索引碎片的信息

## 21.4 不同实现里的系统目录表格

MySQL：

| 表格名称         | 内容             |
| ------------ | -------------- |
| COLUMNS_PRIV | 字段权限           |
| DB           | 数据库权限          |
| FUNC         | 自定义函数的管理       |
| HOST         | 与MySQL相关联的主机名称 |
| TABLES_PRIV  | 表权限            |
| USER         | 表关系            |

## 21.5 查询系统目录

用户通常只能查询与用户相关的表，不能访问系统表，后者通常只能由被授权的用户访问。

暂略

## 21.6 更新系统目录对象

系统目录的更新是由数据库服务程序自动完成的。



# 第22章 高级SQL主题

## 22.1 光标

数据库操作通常是以数据集为基础的操作，即大部分ANSI SQL命令是作用于一组数据的。

光标则是以记录为单位的操作，获得数据库中数据的子集。程序可以依次对光标里的每一行进行求值，光标一般用于过程化程序里嵌入的SQL语句。

不同实现对光标用法的定义不同。只介绍MySQL

声明光标：

```
DECLARE CURSOR_NAME CURSOR
FOR SELECT_STATEMENT
```

创建光标之后，可以使用如下操作对其进行访问：

* OPEN：打开定义的光标
* FETCH：从光标获取记录，赋予程序变量
* CLOSE：对光标的操作完成之后，关闭光标

### 22.1.1 打开光标

使用光标前，必须先打开光标。光标被打开时，指定光标的SELECT语句被执行，查询结果被保存在内存的特定区域。

```
OPEN CURSOR_NAME
```

### 22.1.2 从光标获取数据

光标打开之后，使用FETCH获取光标的内容：

```
FETCH CURSOR_NAME INTO VARIABLE_NAME[,VARIABLE_NAME]...
```

注：从光标获得数据时，需要注意可能会到达光标末尾。

MySQL中的处理方法：

```
BEGIN
     DECLARE done INT DEFAULT 0;
     DECLARE custname VARCHAR(30);
     DECLARE namecursor CURSOR FOR SELECT CUST_NAME FROM TBL_CUSTOMER;
      OPEN namecursor;
      read_loop:LOOP
           FETCH namecursor INTO custname;
           IF done THEN
                 LEAVE read_loop;
           END IF;
           -- Do something with the variable
      END LOOP;
      CLOSE namecursor;
END;
```

### 22.1.3 关闭光标

关闭光标后，程序不能再使用:

```
CLOSE CURSOR_NAME
```

注：关闭光比不一定意味着释放它所占据的内存空间。有些实现需要主动释放，有些实现会自动释放。

## 22.2 存储过程和函数

存储过程是一组相关联的SQL语句，通常称为函数或子程序。存储过程与一系列单个SQL语句相比更容易执行。存储过程可以调用其他存储过程。

存储过程是保存在数据库里的一组SQL语句或函数，被预编译，随时可以被数据库用户使用。存储函数与存储过程一样，但函数可以返回一个值。函数由过程调用，被调用时可以传递参数，返回一个值给调用它的过程。

创建存储过程：

```
CREATE [OR REPLACE] PROCEDURE PROCEDURE_NAME
[(ARGUMENT)[{IN|OUT|IN OUT}]TYPE,
 ARGUMENT [{IN|OUT|IN OUT}]TYPE] {AS}
 PROCEUDRE_BODY

# EXAMPLE
CREATE PROCEDURE NEW_PRODUCT
(PROD_ID IN VARCHAR2,PROD_DESC IN VARCHAR2,COST IN NUMBER)
AS
BEGIN
   INSERT INTO PRODUCTS_TBL
   VALUES(PROD_ID,PROD_DESC,COST);
   COMMIT;
END;
```

执行存储过程：

```
CALL PROCEDURE_NAME([PARAMETER[,...]])

# 
CALL NEW_PRODUCT('9999','INDIAN CORN',1.99);
```

与单个SQL语句相比，存储过程的优点：

* 存储过程的语句已经保存在数据库里
* 存储过程的语句已经被解析过，以可执行格式存在
* 支持模块化编程
* 可以调用其他存储过程和函数
* 可以被其他类型的程序调用
* 通常具有更好的响应时间
* 提高了整体易用性

## 22.3 触发器

触发器是数据库编译了的SQL过程，基于数据库里发生的其他行为来执行操作。是存储过程的一种，会在特定DML行为作用于表格时被执行。可以再INSERT、DELETE、UPDATE语句之前或之后执行，可以在这些语句之前检查数据完整性，可以回退事务，可以修改一个表里的数据，可以从另一个数据库的表里读取数据

触发器会导致更多的I/O开销，尽量不使用。

### 22.3.1 CREATE TRIGGER语句

创建触发器

```
CREATE [DEFINER={USER|CURRENT_USER]}]
TRIGGER TRIGGER_NAME
{BEFORE|AFTER}
{INSERT|UPDATE|DELETE[,...]}
ON TABLE_NAME
AS
SQL_STATEMENT
```

注：触发器的内容不能修改，想要修改触发器，只能替换或重新创建它。有些实现允许使用create trigger语句替换已经存在的同名触发器

### 22.3.2 DROP TRIGGER语句

删除触发器

```
DROP TRIGGER TRIGGER_NAME
```

### 22.3.2 FOR EACH ROW语句

MySQL里的触发器可以调整触发条件。FOR EACH ROW语句可以让过程在SQL语句影响每条记录时都触发，或是一条语句只触发一次。

```
CREATE TRIGGER TRIGGER_NAME
ON TABLE_NAME FOR EACH ROW SQL_STATEMENT
```

有和没有FOR EACH ROW的区别在于触发器执行次数。普通触发器一般一次，加了FOR EACH ROW之后，影响几条记录，触发几次。

## 22.4 动态SQL

动态SQL允许程序员或终端用户在运行时创建SQL语句的具体代码，并把语句传递给数据库，数据库然后把数据返回到绑定的程序变量里。

静态SQL是事先编好的，不准备进行改变的。灵活性不如动态SQL

使用调用级接口可以创建动态SQL

## 22.5 调用级接口

调用级接口(CLI)用于把SQL代码嵌入到主机程序。

。。。

## 22.6 使用SQL生成SQL

使用SQL生成SQL可以节省SQL语句的编写时间。

例：数据库有100个用户，创建了一个新角色ENABLE，要授予这100个用户

```
SELECT 'GRANT ENABLE TO' || USERNAME ||';' FROM SYS.DBA_USERS
```

单引号''，表示它所包围的内容(包括空格在内)要直义使用。||用于连接字段(oracle的用法)



## 22.7 直接SQL与嵌入SQL

直接SQL是指从某种形式的交互终端上执行的SQL语句，执行结果会直接返回到终端。直接SQL也称为交互调用或直接调用

嵌入SQL是在其他程序里使用SQL代码，SQL代码是通过调用级接口嵌入到主机编程语言里的。



## 22.8 窗口表格函数

窗口表格函数可以对表格的一个窗口进行操作，并且基于这个窗口返回一个值。可以计算连续总和、分级和移动平均值等。

```
ARGUMENT OVER ([PARTITION CLAUSE][ORDER CLAUSE][FRAME CLAUSE])
```

几乎所有汇总函数都可以作为窗口表格函数，还有5个新的窗口表格函数：

* RANK() OVER
* DENSE_RANK() OVER
* PERCENT_RANK() OVER
* CUME_DIST() OVER
* ROW_NUMBER() OVER

```
SELECT EMP_ID,SALARY,RANK() OVER(PARTITION BY YEAR(DATE_HIRE)
ORDER BY SALARY DESC) AS RANK_IN_DEPT
FROM EMPLOYEE_PAY_TBL
```

有的实现不支持窗口表格函数



## 22.9 使用XML

以XML格式输出查询结果(MySQL貌似不能直接输出到XML)。

XML功能集的另一个重要特性是能够从XML文档或片段里获取信息，MySQL通过EXTRACTVALUE函数提供这个功能

```
ExtractValue([XML Fragment],[locator string])
```

第一个参数是XML片段，第二个参数是定位器，用于返回与字符串匹配标记的第一个值。









### 

## 