# 第2章 定义数据结构

## 2.1 数据是什么

**数据**是一个信息集合，以某种数据类型保存在数据库里。

**数据类型**用于指定特定列所包含数据的规则，决定了数据保存在列里的方式。

## 2.2 基本数据类型

数据类型是数据的基本特征，其特性被设置到表里的字段。

隐士转换

显示转换:CAST/CONVERT

``` 
SELECT CAST('12/21/2015' AS DATETIME) AS MYDATE
```

最基本的数据类型：

* 字符串类型
* 数值类型
* 日期和时间类型

### 2.2.1 定长字符串

具有相同的长度，使用定长数据类型保存。`CHARACTER(n)`n:字段里能保存的最多字符数量

通常使用空格来填充数量不足的字符。

### 2.2.2 变长字符串

长度不固定的字符串。`CHARACTER VARYING(n)`n:字段里能保存的最多字符数量。

常见的变长字符类型：VARCHAR,VARBINARY,VARCHAR2(后两个：oracle)。

不使用空格来填充字段里的空白。

### 2.2.3 大对象类型

需要保存更长的数据(一般超过VARCHAR字段所保留的长度)：BLOB(二进制字节串)和TEXT(长字符串)

### 2.2.4 数值类型

保存在定义为某种数值类型的字段里：NUMBER,INTEGER,REAL,

DECIMAL等

SQL数值标准：

* BIT(n)
* BIT VARYING(n)
* DECIMAL(p,s)
* INTEGER
* SMALLINT
* BIGINT
* FLOAT(p,s)
* DOUBLE PRECISION(p,s)
* REAL(s)

p:字段最大长度

s:小数点后面的位数。

通用的数值类型：NUMERIC，可以为0、正值、负值、定点数、浮点数

``` 
NUMERIC(5) //接受最大值限制为99999
```

### 2.2.5 小数类型

包含小数点的数值。SQL小数标准如下：

``` 
DECIMAL(p,s) 

p:有效位数，即数值总长度
s:标度，即小数点后面的位数，如超过，四舍五入。
注：小数点本身不算做一个字符
```

### 2.2.6 整数

不包含小数点的数值，包括正负。

### 2.2.7 浮点数

有效位数和标度都可变并且没有限制的小数数值。

* REAL：单精度浮点数值(有效位数：1-21，含)
* DOUBLE PRECISION：双精度浮点数值(有效位数：22-53，含)

### 2.2.8 日期和时间类型

用于保存日期和时间信息。SQL支持DATETIME数据类型，包含：

* DATE
* TIME
* DATETIME
* TIMESTAMP

DATETIME数据类型的元素包括：

* YEAR
* MONTH
* DAY
* HOUR
* MINUTE
* SECOND

日期数据一般不指定长度。

### 2.2.9 直义字符串

就是一系列字符



### 2.2.10 NULL数据类型

NULL表示没有值。

引用NULL值的方法:`NULL(关键字NULL本身)`

`'null'`并不代表NULL值，只是一个包含字符N-U-L-L的直义字符串

如果某个字段必须包含数据，设置为NOT NULL。

### 2.2.11 布尔值

取值范围：TRUE、FALSE、NULL

### 2.2.12 自定义类型

用户定义的类型，允许用户根据已有的数据类型来定制自己的数据类型。

语句`CREATE TYPE`用于创建自定义类型

``` 
CREATE TYPE PERSON AS OBJECT
(NAME     VARCHAR(30),
 SSN      VARCHAR(9));

 //引用自定义类型
 CREATE TABLE EMP_PAY
 (EMPLOYEE     PERSON,
  SALARY       DECIMAL(10,2),
  HIRE_DATE    DATE);
```

### 2.2.13 域

域是能够被使用的有效数据类型的集合。域与数据相关联，从而只接受特定的数据。

创建域之后，可以向域添加约束，约束与数据类型共同发挥作用，从而进一步限制字段能够接受的数据。

``` 
//创建域
CREATE DOMAIN MONEY_D AS NUMBER(8,2);
//添加约束
ALTER DOMAIN MONEY_D ADD CONSTRAINT MONEY_CON1 CHECK(VALUE>5);
//引用域
CREATE TABLE EMP_PAY
(EMP_ID     NUMBER(9),
 EMP_NAME   VARCHAR2(30),
 PAY_RATE    MONEY_D);

```



# 第3章 管理数据库对象

## 3.1 什么是数据库对象

数据库里定义的、用于存储或引用数据的对象，如：表、视图、簇、序列、索引、异名。

## 3.2 什么是规划

与数据库某个用户名相关联的*数据库对象集合*。相应的用户名称为规划所有人，或关联对象组的所有人。数据库里可以有一个或多个规划，用户只与同名规划相关联。

USER1创建EMPLOYEE_TBL表，规划名为USER1，两种方式引用这张表：

``` 
EMPLOYEE_TBL
USER1.EMPLOYEE_TBL
//对USER1，两种方式均可；对其他用户，要指定规划名，即USER1.EMPLOYEE_TBL(使用异名可以不指定规划名)
```

不同规划中可以具有相同名称的数据库对象。不指名规划名操作对象时，数据库服务程序会默认选择用户所拥有的对象。

## 3.3 表：数据的主要存储方式

表在数据库占据实际的物理空间，可以是永久的也可以是临时的

### 3.3.1 列

字段或者列，被设置为特定的数据类型，用于存放特定类型的数据。

列名应该连续，使用下划线做分隔符，长度在不用SQL中有规定。

列可以指定为NULL或NOT NULL。NULL不是空白，而是类似于一个空的字符串，在数据库中占据一个特殊的位置。

### 3.3.2 行

数据库里的一条记录。

### 3.3.3 CREATE TABLE语句

用于建表。

``` 
CREATE TABLE table_name
( field1 date_type [not null],
  field2 date_type [not null],
  field3 data_type [not null],
  field4 date_type [not null],
  ...);
```

注意需要分号";"，括号**可选**

### 3.3.4 命名规范

。。。

### 3.3.5 ALTER TABLE命令

用于修改表，可以添加列、删除列、修改列定义、添加和去除约束。有些实现还可以修改表STORAGE值

``` 
alter table table_name [modify] [column column_name] [datatype|null not null] [restrict|cascade] [drop] [constraint constraint_name] [add] [column] column definition
```

#### 一、修改表的元素

修改列的属性：

* 数据类型
* 长度、有效位数或标度
* 值能否为空

``` 
ALTER TABLE EMPLOYEE_TBL MODIFY EMP_ID VARCHAR(10);
//改变长度
```

#### 二、添加列

如果表已经包含数据，新添加的列不能定义为NOT NULL。

强行向表添加一列的方法：

1. 添加一列，定义为NOT NULL；
2. 给该列在每条记录里都插入数据；
3. 列定义修改为NOT NULL。

#### 三、添加自动增加的列

MySQL提供了SERIAL方法为表生成真正的唯一值：

``` 
CREATE TABLE TEST_INCRMENT
(ID   SERIAL,
 TEST_NAME  VARCAHR(20));
```

列的默认属性是NULL，不需要明确设置，但NOT NULL需要。

#### 四、修改列

修改列的一些通用规则：

* 列的长度可以增加到某类型的最大长度
* 若要缩短长度，则要求该列中的所有数据长度都小于等于新长度。
* 数值数据的位数可以增加
* 若要缩短数值数据的位数，则要求已有记录数值的位数小于等于新位数
* 数值里的小数位数可以增加或减少
* 列的数据类型一般是可以改变的

### 3.3.6 从现有表新建另一个表

利用CREATE TABLE和SELECT的组合可以复制现有的表。可以选择部分或全部列。

``` 
create table new_table_name as 
select [*|column1,column2,...] from table_name [where]
```

默认使用相同的STORAGE属性

### 3.3.7 删除表

如果使用了RESTRICT选项，并且表被视图或约束所引用，DROP语句会返回一个错误。

使用CASCADE选项时，删除操作会成功，全部引用视图和约束都会被删除。

``` 
drop table table_name [restrict|cascade]
```

## 3.4 完整性约束

用于确定关系型数据库里数据的准确性和一致性。数据完整性是通过引用完整性的概念实现的。

### 3.4.1 主键约束

主键是表里一个或多个用于实现记录唯一性的字段。主键可以有一个或多个字段构成。主键在创建表时指定。

``` 
CREATE TABLE EMPLOYEE_TBL
(EMP_ID CHAR(9) NOT NULL PRIMARY KEY,
 EMP_NAME VARCHAR(40) NOT NULL,
 ...
);
```

这时主键是个隐含约束。还可以在建表时明确指定主键作为一个约束

``` 
CREATE TABLE EMPLOYEE_TBL
(EMP_ID CHAR(9) NOT NULL,
 EMP_NAME VARCHAR(40) NOT NULL,
 ...
PRIMARY KEY (EMP_ID));
```

包含多个字段的主键有两种定义方式：

``` 
CREATE TABLE PRODUCT_TST
(PROD_ID   VARCHAR(10)  NOT NULL,
 VEND_ID   VARCHAR(10)  NOT NULL,
 ...
PRIMARY KEY(PROD_ID,VEND_ID));
```

或者：

``` 
ALTER TABLE PRODUCT_TST
ADD CONSTRAINT PRODUCTS_PK PRIMARY KEY(PROD_ID,VEND_ID);
```

### 3.4.2 唯一性约束

要求表里某个字段的值在每条记录里都是唯一的，与主键类似。可以对非主键字段设置唯一性约束

``` 
CREATE TABLE EMPLOYEE_TBL
(EMP_ID CHAR(9) NOT NULL PRIMARY KEY,
 EMP_NAME VARCHAR(40) NOT NULL,
 ...
 EMP_PHONE INTEGER(10) NULL UNIQUE,
);
```

### 3.4.3 外键约束

外键是子表里的字段，引用父表里的主键。外键约束确保表与表之间引用完整性的主要机制。

``` 
CREATE TABLE EMPLOYY_PAY_TST
(EMP_ID     CHAR(9)    NOT NULL,
 ...
 CONSTRAINT EMP_ID_FK FOREIGN KEY (EMP_ID) REFERENCES EMPLOYEE_TBL);
```

利用ALTER TABLE可以向表里添加外键：

``` 
alter table employee_pay_tbl
add constraint id_fk foreign key(emp_id) refrences employee_tbl (emp_id);
```

### 3.4.4 NOT NULL约束

NOT NULL也是一个可用于字段的约束。

### 3.4.5 检查约束

检查(CHK)约束用于检查输入到特定字段的数据的有效性。

``` 
CREATE TABLE EMPLOYEE_CHECK_TST
(EMP_ID   CHAR(9)   NOT NULL,
 ...
 EMP_ZIP  NUMBER(5) NOT NULL,
 ..
 PRIMARY KEY(EMP_ID),
 CONSTRINT CHK_EMP_ZIP CHECK (EMP_ZIP='1234'));
```

这里，EMP_ZIP字段设置了检查约束，确保输入到这个表里的全部雇员的ZIP代码均为"1234"。

如果想利用检查约束确保ZIP代码属于某个值列表：

``` 
CONSTRINT CHK_EMP_ZIP CHECK (EMP_ZIP IN ('1234','2234','3234'));
```

检查约束可以使用几乎任何条件

### 3.4.6 去除约束

利用ALTER TABLE命令的DROP CONSTRAINT选项可以去除已经定义的约束

``` 
ALTER TABLE EMPLOYEES DROP CONSTRAINT EMPLOYEES_PK;
```



# 第4章 规格化过程

规格化：原始数据分解为表的过程。

## 4.1 规格化数据库

规格化是去除数据库里冗余数据的过程(设计和重新设计数据库时使用)。

### 4.1.1 原始数据库

...

### 4.1.2 数据库逻辑设计

#### 一、什么是终端用户的需求

考虑与用户相关的因素：

* 应保存什么数据
* 用户如何访问数据库
* 用户权限
* 数据如何分组
* 哪些数据最经常被访问
* 全部数据与数据库如何关联
* 采取什么措施保证数据的正确性
* 采取什么措施减少数据冗余
* 采取什么措施让负责维护数据的用户易于使用数据库

#### 二、数据冗余

没有冗余，意味着重复的数据应该保持到最少。

### 4.1.3 规格形式

规格形式是衡量数据库被规格化级别的一种方式。

常见的三种规格形式：

* 第一规格形式
* 第二规格形式
* 第三规格形式

这三种主要的规格形式中，每一种都依赖于前一种形式所采用的规格化步骤。(即，要想以第二规格形式对数据库进行规格化，数据库必须处于第一种规格形式)

#### 一、第一规格形式

目标：把原始数据分解到表中。在所有表都设计完成之后，给大多数表或全部表设置一个主键。

#### 二、第二规格形式

目标：提取对主键仅有部分依赖的数据，保存到另一个表里。

#### 三、第三规格形式

目标：删除表里不依赖于主键的数据。

### 4.1.4 命名规范

。。。

### 4.1.5 规格化的优点

主要有：

* 更好的数据库整体组织性
* 减少冗余数据
* 数据库内部的数据一致性
* 更灵活的数据库设计
* 更好地处理数据库安全
* 加强引用整体性的概念

引用完整性表示一个表里某列的值依赖于另一个表里某列的值。一般通过主键和外键来控制。

### 4.1.6 规格化的缺点

降低数据库的性能。

## 4.2 去规格化数据库

去规格化是修改规格化数据库的表的构成，在可控制的数据冗余范围内提高数据库性能。去规格化是在数据库规格化基础上进行一些调整。

去规格化会增加数据冗余，程序代码会更复杂，引用完整性更加琐碎。



# 第5章 操作数据

本章重点：

* 数据操作语言概述(DML)
* 如何操作表里的数据
* 数据填充
* 从表里删除数据
* 从表里修改数据

## 5.1 数据操作概述

3个基本的DML命令：

* INSERT
* UPDATE
* DELETE

## 5.2 用新数据填充表

### 5.2.1 把数据插入列表

INSERT语句：

``` 
INSERT INTO TABLE_NAME VALUES ('value1','value2','value3')；
```

SQL语句不区分大小写(但最好统一)，数据区分大小写。

必须在VALUES列表里包含表里的每个列。列表里，每个值以逗号","隔开，字符、日期和时间类型的值必须以单引号包围，数值和NULL值不需要(单引号对数值型数据是可选的)。注意值的顺序。

### 5.2.2 给表里指定列插入数据

``` 
INSERT INTO TABLE_NAME ('COLUMN1','COLUMN2') VALUES ('VALUE1','VALUE2');
```

注：INSERT语句里的字段列表次序并不一定与表定义中的字段次序相同，但插入值的次序要与字段列表的次序相同。

### 5.2.3 从另一个表插入数据

``` 
insert into table _name [('column1','column2')] 
select [*|('column1','column2')] from table_name [where condition(s)];
```

主要字段顺序，以及数据类型的兼容性

### 5.2.4 插入NULL值

``` 
insert into schema.table_name values('column1',NULL,'column3');
```

NULL要位于正确的位置上。也可以不指定字段以及NULL值，插入时会被自动赋予NULL(该字段要允许为NULL，且没有默认值)

## 5.3 更新现有数据

UPDATE命令，一般每次之更新数据库里的一个表，但可以同时更新一张表中的多个字段。

### 5.3.1 更新一列的数据

``` 
update table_name set column_name='value' [where condition];
```

可能更新一条或多条记录。

注：大多数情况下，DML命令都需要使用WHERE子句。

### 5.3.2 更新一条或多条记录里的多个字段

``` 
update table_name
set column1='value',
   [column2='value',
    column3='value']
[where condition]
```

注：set只使用一次。

## 5.4 从表里删除数据

DELETE命令，不能删除某一列数据，而是删除行里全部字段(即一条或多条记录)

``` 
delete from table_name [where condition];
```





# 第6章 管理数据库事务

重点：

* 事务的定义
* 用于控制事务的命令
* 事务命令的语法和范例
* 何时使用事务命令
* 低劣事务控制的后果

## 6.1 什么事务

事务是对数据库执行的一个操作单位。一个事务可以是一个或多个DML语句。

事务的本质特征：

* 所有的事务都有开始和结束
* 事务可以被保存或撤销
* 如果事务中途失败，事务中的任何部分都不会记录到数据库。

## 6.2 控制事务

对可能发生的各种事务的管理能力。

一个事务成功执行时，目标表并不是立即被修改，而是通过事务控制将结果保存到目标表或者撤销执行。

控制事务的3个命令：

* COMMIT
* ROLLBACK
* SAVEPOINT

注：事务控制命令只与INSERT、UPDATE和DELETE配合使用。

### 6.2.1 COMMIT命令

用于把事务所做的修改保存到数据库，它把上一个COMMIT或ROLLBACK命令之后的全部事务都保存到数据库。

``` 
commit [work]
```



COMMIT：其后是用于终止语句的字符或命令

WORK：是个选项，让命令对用户更加友好

注：全部修改都会先被送到临时回退区域，如果这个临时回退区域没有空间，数据库很可能会被挂起，禁止进一步的事务操作。

### 6.2.2 ROLLBACK命令

用于撤销还没有保存到数据库的命令，只能用于撤销一个COMMIT或ROLLBACK命令之后的事务

``` 
rollback [work];
```

### 6.2.3 SAVEPOINT命令

保存点是事务过程的一个逻辑点，可以把事务回退到这个点，而不必回退整个事务。

``` 
savepoint savepoint_name
//在事务语句之间创建一个保存点
```

保存点可以将大量事务操作划分为较小的、更易于管理的组

### 6.2.4 ROLLBACK TO SAVEPOINT命令

回退到保存点：

``` 
ROLLBACK TO SAVEPOINT_NAME
```

注：在相应的事务操作里，保存点的名字要唯一。

### 6.2.5 RELEASE SAVEPOINT命令

用于删除创建的保存点。某个保存点被释放后，不能利用ROLLBACK命令来撤销这个保存点之后的事务操作。

``` 
RELEASE SAVEPOINT savepoint_name
```

### 6.2.6 SET TRANSACTION 命令

用于初始化数据库事务，可以指定事务的特性。

``` 
SET TRANSACTION READ WRITE
SET TRANSACTION READ ONLY;
//设置事务只读或读写
```

## 6.3 事务控制与数据库性能

COMMIT和ROLLBACK命令会将临时存储区域里的回退信息被清除。若一直没出现COMMIT或ROLLBACK命令，临时存储区域里的回退信息会不断增长，直到没有剩余空间，导致数据库停止全部进程。



# 第7章 数据库查询

## 7.1 什么是查询

...

## 7.2 SELECT语句

SELECT语句里有4个关键字(子句):

* SELECT
* FROM
* WHERE
* ORDER BY

### 7.2.1 SELECT语句

SELECT语句与FROM子句联合使用，以一种有组织的、可读的方式从数据库提取数据

``` 
SELECT [*|ALL|DISTINCT COLUMN1,COLUMN2] FROM TABLE1[,TABLE2];
```

ALL：用于显示一列的全部值，包括重复值。是默认操作

DISTINCT：禁止在输出结果里面包含重复的行

注：用逗号分隔参数

``` 
SELECT DISTINCT PROD_DESC FROM CANDY_TBL
SELECT DISTINCT(PRO_DESC) FROM CANDY_TBL
//ALL也可以这样，不过ALL是默认操作，可以不指明
```

### 7.2.2 FROM子句

指明查询的数据源，可以指定一个或多个表，任何查询的必要条件

``` 
from table1 [,table2]
```

### 7.2.3 WHERE子句

指定了要返回满足什么标准的信息。条件的值是TRUE或FALSE。

WHERE子句里可以有多个条件，它们之间以操作符AND或OR连接

``` 
select [*|all|distinct column1,column2]
from table1 [,table2]
where [condition1 | expression 1] [and|or condition2|expression2]
```

### 7.2.4 ORDER BY子句

以用户指定的列表格式对查询结果进行排列。ORDER BY子句的默认次序是升序(对于字符，从A到Z的次序，对于数字值，0到9，降序则相反)

ORDER BY子句的语法：

``` 
select [*|all|distinct column1,column2]
from table1 [,table2]
where [condition1 | expression1] [and|or condition2|expression2]
order by column1|integer [ASC|DESC]
```

注：SQL排序是基于字符的ASCII排序，数字在字母前面，且数字值在排序时被当做字符处理，例如`1、12、2、255、3`

ORDER BY子句里的字段可以缩写为一个整数，该整数取代了实际的字段名称(即作为一个别名)，表示字段在关键字SELECT之后列表里的位置。

一个查询里可以对多个字段进行排序，使用字段名或对应的整数。可以指定不同字段升序或者降序(即允许一个升，一个降...)

ORDER BY子句里的字段次序不一定要与关键字SELECT之后的字段次序一致。但字段次序决定了排序过程的完成方式

### 7.2.5 大小写敏感

SQL命令和关键字不区分大小写。

排序规则决定了RDBMS如何解释数据，包括排序方式和大小写敏感性等内容。

数据的大小写敏感性直接决定了WHERE子句如何匹配记录。MySQL默认大小写不敏感。

## 7.3 简单查询的范例

### 统计表里的记录数量

``` 
select count(*) from table_name...
select count(column_name) from table_name...
select count(distinct column_name) from table_name...
```

### 使用字段别名

``` 
select column_name alias_name from table_name...
```



# 第8章 使用操作符对数据进行分类

重点：

* 什么是操作符
* SQL里操作符的概述
* 操作符如何单独使用
* 操作符如何联合使用

## 8.1 什么是SQL里的操作符

操作符是一个保留字或字符，主要用于SQL语句的WHERE子句来执行操作，可以指定条件，还可以联接多个条件。

操作符有：

* 比较操作符
* 逻辑操作符
* 求反操作符
* 算术操作符

## 8.2 比较操作符

用于在SQL语句里对单个值进行测试，主要有：=、<>、<、>

### 8.2.1 相等

进行相等比较时，被比较的值必须完全匹配，否则不返回数据(根据比较结果true或false来决定)。

### 8.2.2 不等于

MySQL中<>和!=均表示不等于

### 8.2.3 小于和大于

。。。



### 8.2.4 比较操作符的组合

等号可以与小于和大于联合使用：`<=、>=`

## 8.3 逻辑操作符

逻辑操作符包括：

* IS NULL
* BETWEEN
* IN
* LIKE
* EXISTS
* UNIQUE
* ALL和ANY

### 8.3.1 IS NULL

用于与NULL值进行比较

``` 
where salary is null
where salary='null'
//这两个不一样
```

### 8.3.2 BETWEEN

用于寻找位于一个给定最大值和最小值之间的值，且包含边界值

``` 
where salary between '20000' and '30000'
```

### 8.3.3 IN

操作符IN用于把一个值与一个指定列表进行比较，当被比较的值至少与列表中的一个值相匹配时，返回TRUE

操作符IN可以得到与操作符OR一样的结果，但速度更快

### 8.3.4 LIKE

利用通配符把一个值与类似的值进行比较，通配符通常有两个：

* %：0或多个字符
* _：一个数字或字符

可以复合使用：

``` 
where salary like '2_%_%'
where salary like '_2%3'
where salary like '2___3'
```

### 8.3.5 EXISTS

用于搜索指定表里是否存在满足特定条件的记录

``` 
select cost from products_tbl
where exists (select cost from products_tbl where cost>100);
```

### 8.3.6 ALL、SOME和ANY操作符

**ALL**用于把一个值与另一个集合里的全部值进行比较

``` 
select * from products_tbl 
where cost > ALL (select cost from products_tbl where cost<10); 
```

**ANY**用于把一个与另一个列表里任意值进行比较。SOME是ANY的别名，可以互换使用。

``` 
select * from products_tbl 
where cost > ANY (select cost from products_tbl where cost<10); 
```

ANY与IN的不同：

IN可以使用下面的表达式列表，而ANY不行：

``` 
In (<Item#1>,<Item#2>,<Item#3>)
```

另外，与IN相反的是NOT IN，相当于<>ALL，而不是<>ANY

## 8.4 连接操作符

两个连接操作符：

* AND
* OR

使用连接操作符可以用多个不同的操作符进行多种比较

### 8.4.1 AND

所有由AND连接的条件都必须为TRUE，SQL语句才会实际执行

### 8.4.2 OR

只要OR连接的条件里有至少一个TRUE，SQL语句就会执行

操作符是从左向右进行解析的



# 8.5 求反操作

NOT可以颠倒逻辑操作符的含义，可以与其他操作符构成以下几种形式：

* <>,!=(NOT EQUAL)
* NOT BETWEEN
* NOT IN
* NOT LIKE
* IS NOT NULL
* NOT EXISTS
* NOT UNIQUE

### 8.5.1 不相等

。。。

### 8.5.2 NOT BETWEEN

不包含边界值



### 8.5.3 NOT IN

返回值不在列表中的记录

### 8.5.4 NOT LIKE

返回不相似的值

### 8.5.5 IS NOT NULL

...

### 8.5.6 NOT EXISTS

...

### 8.6 算术操作符

四个操作符：

* +
* -
* *
* /

### 8.6.1 加法

``` 
select salary+bonus from employee_pay_tbl
select salary from employee_pay_tbl where salary+bonus>'40000'
```



### 8.6.2 减法

### 8.6.3 乘法

### 8.6.4 除法



### 8.6.5 算术操作符的组合

算术操作符可以彼此组合，并遵循基本算术运算中的优先级。可以使用圆括号控制算术运算次序(也是唯一的方式)



# 第9章 汇总查询得到的数据

重点：

* 什么是函数
* 如何使用函数
* 何时使用函数
* 使用汇总函数
* 使用汇总函数对数据进行合计
* 函数得到的结果

## 9.1 什么是汇总函数

函数是SQL里的关键字，用于对字段里的数据进行操作。函数是一个命令，通常与字段或表达式联合使用，处理输入的数据并产生结果。

基本汇总函数包括：

* COUNT
* SUM
* MAX
* MIN
* AVG

### 9.1.1 COUNT函数

用于统计不包含NULL值的记录或字段值，返回一个数值，可以与DISTINCT命令一起使用，默认使用ALL。

注：COUNT(*)会统计表里的全部记录数量，包括重复的，不管是否包含NULL值，因此不能与DISTINCT一起使用。

``` 
COUNT [(*)|(DISTINCT|ALL)] (COLUMN NAME)
```

注：COUNT统计的是行数，不涉及数据类型，行里可以包含任意类型的数据

### 9.1.2 SUM函数

返回一组记录中某一字段值的总和。可以DISTINCT一起使用

``` 
SUM ([DISTINCT] COLUMN NAME)
```

注：SUM函数只能处理数值类型字段。但CHAR数据可以隐式转换为数值类型。如果数据不能隐式转换为数值类型，返回结果为0

### 9.1.3 AVG函数

计算一组指定记录的平均值，也可以与DISTINCT一起使用。

``` 
AVG ([DISTINCT] COLUMN NAME)
```

注：AVG函数只能处理数值型字段。查询结果可能会被舍到相应数据类型的精度

### 9.1.4 MAX函数

返回一组记录中某个字段的最大值，NULL值不在计算范围之内。可以和DISTINCT一起使用，但全部记录和不同记录的最大值一样，所有没有意义

``` 
MAX ([DISTINCT] COLUMN NAME)
```

注：可以对字符数据使用汇总函数，如MAX，MIN，对于这种类型，使用排序规则

### 9.1.5 MIN函数

返回一组记录里某个字段的最小值，NULL不在计算范围之内。同MAX，DISTINCT没有意义

``` 
MIN ([DISTINCT] COLUMN NAME)
```

注：与MAX类似，MIN也可以根据数据词典，返回字符数据的最小值



# 第10章 数据排序与分组

重点：

* 为何想对数据进行分组
* GROUP BY子句
* 分组估值函数
* 分组函数的使用方法
* 根据字段进行分组
* GROUP BY与ORDER BY
* HAVING子句

## 10.1 为什么要对数据进行分组

数据分组是按照逻辑次序把具有重复值的字段进行合并。

数据分组是通过在SELECT语句里使用GROUP BY子句实现的。

## 10.2 GROUP BY子句

GROUP BY子句配合SELECT使用，把相同的数据划分为组。SELECT语句中，GROUP BY子句在WHERE子句之后，在ORDER BY子句之前

``` 
SELECT COLUMN1,COLUMN2
FROM TABLE1,TABLE2
WHERE CONDITIONS
GROUP BY COLUMN1,COLUMN2
ORDER BY COLUMN1,COLUMN2
```

注：GROUP BY子句对CPU的运行效率有很大影响，所以需要使用WHERE子句来缩小数据范围。

### 10.2.1 分组函数

。。。

### 10.2.2 对选中的数据进行分组

被选中的字段(SELECT之后的字段列表)才能在GROUP BY子句里引用，并且达到要求的字段名称必须出现在GROUP BY子句里(字段名称也可以用相应的数字代替，分组字段的次序不一定要与SELECT子句里的字段次序相同)。

## 10.2.3 创建分组和使用汇总函数

``` 
#1:
SELECT EMP_ID,SUM(SALARY) FROM EMPLOYEE_PAY_TBL 
GROUP BY SALARY,EMP_ID;
#2:
SELECT CITY,COUNT(*) FROM EMPLOYEE_TBL GROUP BY CITY;
#3:
SELECT CITY,AVG(PAY_RATE),AVG(SALARY)
FROM EMP_PAY_TMP
WHERE CITY IN('INDIANAPOLIS','WHITELAND')
GROUP BY CITY
ORDER BY 2,3
```



### 10.2.4 以整数代表字段名称

。。。

### 10.3 GROUP BY与ORDER BY

两者的相同之处在于他们都是对数据进行排序，ORDER BY专门用于对查询得到的数据进行排序，GROUP BY也把查询得到的数据排序为适当分组的数据，因此，GROUP BY子句也可以像ORDER BY子句那样用于数据排序。

GROUP BY实现排序曹组的区别与缺点：

* 所有被选中、非汇总函数的字段必须列在GROUP BY子句里
* 除非需要使用汇总函数，否则使用GROUP BY子句进行排序通常是没必要的。

``` 
SELECT LAST_NAME,FIRST_NAME,CITY FROM EMPLOYEE_TBL
GROUP BY LAST_NAME,FIRST_NAME,CITY
```

注：SElECT语句里列出的全部字段，除了汇总字段之外，全部都要出现在GROUP BY子句里。

GROUP BY子句可以用于在CREATE VIEW语句里进行排序，而ORDER BY不行。

## 10.4 CUBE与ROLLUP语句

ROLLUP语句可以用来进行小计，即在全部分组数据的基础上，对其中一部分进行汇总：

``` 
GROUP BY ROLLUP(ordered column list of grouping sets)
```

在完成了基本的分组数据汇总以后，按照从右向左的顺序，每次去掉字段列表中的最后一个字段，再对剩余的字段进行分组统计，并将获得的小计结果插入返回表中，被去掉的字段位置使用NULL填充，最后，再对全表进行一次统计，所有字段位置均使用NULL填充。

MySQL的语法：

``` 
GROUP BY order column list of grouping sets WITH ROLLUP
```

CUBE语句对分组列表中的所有字段进行排列组合，并根据每一种组合结果，分别进行统计汇总。最后，CUBE语句对全表进行统计。

``` 
GROUP BY CUBE(column list of grouping sets)
```



## 10.5 HAVING子句

在SELECT语句里与GROUP BY子句联合使用，用于告诉GROUP BY子句在输出里包含哪些分组，即设置GROUP BY子句形成分组的条件。

HAVING子句必须跟着GROUP BY子句之后，ORDER BY子句之前

``` 
SELECT COLUMN1,COLUMN2
FROM TABLE1,TABLE2
WHERE CONDITIONS
GROUP BY COLUMN1,COLUMN2
HAVING CONDITIONS
ORDER BY COLUMN1,COLUMN2
```

``` 
SELECT CITY,AVG(PAY_RATE),AVG(SALARY)
FROM EMP_PAY_TMP
WHERE CITY <> 'GREENWOOD'
GROUP BY CITY
HAVING AVG(SALARY)>20000
ORDER BY 3;
```



# 第11章 调整数据的外观

重点：

* 字符函数简介
* 如何及何时使用字符函数
* ANSI SQL函数范例
* 常见实现的特定函数范例
* 转换函数概述
* 如何及何时使用转换函数


## 11.1 ANSI字符函数

字符函数用于在SQL里以不同于存储方式的格式来表示字符串。

最常用的ANSI字符函数主要用于进行串接、子串和TRANSLATE等操作。

## 11.2 常用字符函数

### 11.2.1 串接函数

(串接及其他一些函数在不同实现里略有不同，MySQL、Oracle(||)、SQL Server(+)都不太一样，只介绍MySQL)

```
CONCAT(COLUMN_NAME,['',] COLUMN_NAME [COLUMN_NAME])
```

MySQL的串接函数可以连接多个字符串。

注：串接函数用于连接字符，若要连接数字，需要将数字首先转换为字符串

### 11.2.2 TRANSLATE函数

搜索字符串里的字符并查找特定的字符，标记找到的位置，然后用替代字符串里对应的字符替换它。

```
TRANSLATE(CHARACTER SET,VALUE2,VALUE2)
```

```
SELECT CITY,TRANSLATE(CITY,'IND','ABC') FROM EMPLOYEE_TBL 
//把字符串里每个I替换为A，每个N替换为B，每个D替换为C
```

### 11.2.3 REPLACE

用于把某个字符或字符串替换为指定的一个字符或多个字符，使用方法类似于TRANSLATE函数

```
REPLACE('VALUE','VALUE',[NULL]'VALUE')

SELECT REPLACE(FIRST_NAME,'T','B') FROM EMPLOYEE_TBL
```

注：translate(char,from,to)中from和to是逐字对应的，若to的长度小于from，返回值中from多出来的部分字符会被删除。而replace(char,search_string,replacement_string)，要在char中找到匹配search_string的字符串，然后完全替换为replacement_string，并不像translate那样逐字对应。

### 11.2.4 UPPER

把字符串里的小写字母转换为大写

```
UPPER(character string)

SELECT UPPER(CITY) FROM EMPLOYEE_TBL
```

### 11.2.5 LOWER

与UPPER相反，略

### 11.2.6 SUBSTR

获取字符串子串

```
SUBSTR(COLUMN_NAME,STARTING POSITION,LENGTH)
```

注：navicat中SUBSTRING也可以用



### 11.2.7 INSTR

用于在字符串里寻找指定的字符集，返回其所在的位置，若不存在，返回0。

```
INSTR (COLUMN_NAME,'SET',[STARTING POSITION [,OCCURRENCE]]);

SELECT INSTR(STATE,'I',1,1) FROM EMPLOYEE_TBL
//返回每个州里字母I第一次出现的位置
```

### 11.2.8 LTRIM

另一种截取部分字符串的方式(SUBSTRING)，用于从左剪除字符串里的字符

```
LTRIM(CHARACTER STRING [,'set'])

SELECT POSITION,LTRIM(POSITION,'SALES') FROM EMPLOYEE_PAY_TBL
```

注：LTRIM会剪除被搜索的字符串在目标字符串里最后一次出现位置之左的全部字符(被搜索的字符必须以相同次序出现在目标字符串里，而且必须位于目标字符串的最左侧)。例如，被搜索字符串为'SALES'，目标字符串为'SHIPPER'，剪除后为'HIPPER'



### 11.2.9 RTRIM

类似于LTRIM，用于剪除字符串的右侧字符。

```
RTRIM (CHARACTER STRING [,'set'])
```

### 11.2.10 DECODE

DECODE函数不是ANSI标准里的，主要用于Oracle和PostgreSQL。

它可以再字符串里搜索一个值或字符串，如果找到了，就在结果里显示另一个字符串

```
DECODE(COLUMN_NAME,'SEARCH1','RETURN1',['SEARCH2','RETURN2','DEFAULT VALUE'])
```

找到匹配的，就用对应值替换，否则显示默认值'DEFAULT VALUE'

## 11.3 其他字符函数

### 11.3.1 LENGTH

用于得到字符串、数字、日期或表达式的长度，单位是字节。

```
LENGTH(CHARACTER STRING)
```



### 11.3.2 IFNULL(检查NULL值)

用于在一个表达式是NULL时从另一个表达式获得值。它可以用于大多数数据类型，但值与替代值必须是同一数据类型。

```
IFNULL('VALUE','SUBSTITUTION')

SELECT PAGER,IF(PAGER,999999) FROM EMPLOYEE_TBL
```



### 11.3.3 COALESCE

用指定值替代NULL值，这点与IFNULL一样，不同点在于，COALESCE可以接受一个数据集，依次检查其中每一个值，直到发现一个非NULL值，如果没有找到非NULL值，会返回一个NULL值。

```
SELECT EMP_ID,COALESCE(BONUS,SALARY,PAY_RATE) FROM EMPLOYEE_PAY_TBL
//返回BONUS、SALARY、PAY_RATE字段里第一个非NULL值
```

### 11.3.4 LPAD

左填充，用于在字符串左侧添加字符或空格

```
LPAD(CHARACTER SET)

//在PROD_DESC左侧添加'.'，使其长度达到30
SELECT LPAD(PROD_DESC,30,'.') PRODUCT FROM PRODUCTS_TBL
```

### 11.3.5 RPAD

右填充，用于在字符串右侧添加字符或空格

```
RPAD(CHARACTER SET)
```



### 11.3.6 ASCII

返回字符串最左侧字符的ASCII码

```
ASCII(CHARACTER SET)
```



## 11.4 算术函数

算术函数可以对数据库里的值根据算术规则进行运行

常见的算术函数有：

* ABS
* ROUND(舍入)
* SQRT
* SIGN(符号)
* POWER
* CEIL、FLOOR(上限、下限)
* EXP
* SIN、COS、TAN

一般语法：

```
FUNCTION(EXPRESSION)
```

## 11.5 转换函数

把数据类型从一种转换为另一种。

常见的数据转换：

* 字符到数字
* 数字到字符
* 字符到日期
* 日期到字符

### 11.5.1 字符串转换为数字

数值数据类型和字符串数据类型的两个主要区别：

* 算术表达式和算术函数可以用于数值
* 在输出结果里，数值是右对齐，字符串是左对齐

注：对于要转换为数值的字符串来说，其中的字符必须是0-9

### 11.5.2 数字转换为字符串

。。。



## 11.6 字符函数的组合使用

大多数函数可以在SQL语句里组合使用。

```
SELECT CONCAT(LAST_NAME,',',FIRST_NAME) NAME,
       CONCAT(SUBSTR(EMP_ID,1,3),'-',
       SUBSTR(EMP_ID,4,2),'2',
       SUBSTR(EMP_ID,6,4)) AS ID
FROM EMPLOYEE_TBL
```



# 第12章 日期和时间

重点：

* 理解日期和时间
* 日期和时间是如何存储的
* 典型的日期和时间格式
* 如何使用日期函数
* 如何使用日期转换

## 12.1 日期是如何存储的

### 12.1.1 日期和时间的标准数据类型

日期和时间(DATETIME)存储的标准SQL数据类型有3种：

* DATE：直接存储日期，YYYY-MM-DD
* TIME：直接存储时间，HH:MM:SS.nn...
* TIMESTAMP：存储日期和时间，YYYY-MM-DD HH:MM:SS.nn...

### 12.1.2 DATETIME元素

DATETIME元素是属于日期和时间的元素，包含在DATETIME定义里。

| DATETIME元素 | 有效范围             |
| ---------- | ---------------- |
| YEAR       | 0001-9999        |
| MONTH      | 01-12            |
| DAY        | 01-31            |
| HOUR       | 00-23            |
| MINUTE     | 00-59            |
| SECOND     | 00.00..-61.999.. |

### 12.1.3 不同是实现的日期类型

只列出MySQL的：

| 数据类型      | 用途        |
| --------- | --------- |
| DATETIME  | 存数日期和时间信息 |
| TIMESTAMP | 存储日期和时间信息 |
| DATE      | 存储日期值     |
| TIME      | 存储时间值     |
| YEAR      | 单字节，表示年   |

## 12.2 日期函数

日期函数用于调整日期和时间数据的外观，以适当的方式显示日期和时间数据、进行比较、计算日期之间的间隔等

### 12.2.1 当前日期

MySQL通过NOW()函数获取当前日期和时间

### 12.2.2 时区

。。。 暂略

### 12.2.3 时间与日期相加

DATETIME值可以增加时间间隔。

MySQL:

```
DATE_ADD(date,INTERVAL expr unit)
```

### 12.2.4 其他日期函数

MySQL：

* DAYNAME(date)
* DAYOFMONTH(date)
* DAYOFWEEK(date)
* DAYOFYEAR(date)

### 12.3 日期转换

CAST操作符可以把一种数据类型转换为另一种:

```
CAST(EXPRESSION AS NEW_DATE_TYPE)
```

### 12.3.1 日期描述

日期描述由格式元素组成，用于从数据库以期望的格式提取日期和时间信息

(P147  一张表   略)

### 12.3.2 日期转换为字符串

### 12.3.3 字符串转换为日期

```
STR_TO_DATE(str,format)
```





# 第13章 在查询里结合表

重点：

* 表的结合
* 不同类型的结合
* 如何、何时使用结合
* 表结合的范例
* 不恰当表结合的影响
* 在查询中利用别名对表进行重命名

## 13.1 从多个表获取数据

通过主键和外键形成相互关联的表，并通过这些字段结合在一起



## 13.2 结合的类型

结合是把两个或多个表组合在一起来获取数据。

常用的结合方式：

* 等值结合与内部结合
* 非等值结合
* 外部结合
* 自结合

### 13.2.1 结合条件的位置

结合表时，WHERE子句是必要的。要结合的表列在FROM子句里，结合在WHERE子句里完成。

### 13.2.2 等值结合(内部结合)

利用通用字段结合两个表，通常是每个表里的主键。

```
SELECT TABLE1.COLUMN1,TABLE2.COLUMN2...
FROM TABLE1,TABLE2[,TABLE3] 
WHERE TABLE1.COLUMN_NAME=TABLE2.COLUMN_NAME [AND TABLE1.COLUMN=TABLE3.COLUMN_NAME]
```

SELECT子句里每个字段名称都以表名作为前缀，这称为限定字段。

还可以利用INNER JOIN关键字

```
SELECT TABLE1.COLUMN1,TABLE2.COLUMN2...
FROM TABLE1
INNER JOIN TABLE2 ON TABLE1.COLUMN_NAME=TABLE2.COLUMN_NAME
```

### 13.2.3 使用表的别名

在SQL语句里对表进行重命名，是一种临时性的改变。表具有别名是完成自结合的必要条件。表的别名跟在表名后面。

### 13.2.4 不等值结合

根据同一个字段在两个表里值不相等来实现结合。

```
FROM TABLE1,TABLE2 [,TABLE3]
WHERE TABLE1.COLUMN_NAME !=TABLE2.COLUMN_NAME
[AND TABLE1.COLUMN_NAME !=TABLE3.COLUMN_NAME]
```

注：使用不等值结合可能会得到很多无用的数据

### 13.2.5 外部结合

外部结合会返回一个表里的全部记录，即使对应的记录在第二个表里不存在。"+"用于在查询里表示外部结合，放在WHERE子句里表名的后面。具有加号的表是没有匹配记录的表。

外部结合：左外部结合、右外部结合、全外部结合

```
FROM TABLE1 
{RIGHT | LEFT | FULL} [OUTER] JOIN TABLE2
ON TABLE1.COLUMN=TABLE2.COLUMN
```

可以在JOIN条件里对同一个表里的多个字段进行外部结合。

### 13.2.6 自结合

自结合利用表别名在SQL语句对表进行重命名，像处理两个表一样把表结合到自身。

```
SELECT A.COLUMN_NAME,B.COLUMN_NAME [,C.COLUMN_NAME]
FROM TABLE A,TABLE B [,TABLE C]
WHERE A.COLUMN_NAME=B.COLUMN_NAME
[AND A.COLUMN_NAME=C.COLUMN_NAME]

#1
SELECT A.LAST_NAME,B.LAST_NAME,A.FIRST_NAME
FROM EMPLOYEE_TBL A,EMPLOYEE_TBL B
WHERE A.LAST_NAME=B.LAST_NAME;
#2 
SELECT A.LAST_NAME,B.LAST_NAME,A.FIRST_NAME
FROM EMPLOYEE_TABLE A
INNER JOIN EMPLOYEE_TBL B ON A.LAST_NAME=B.LAST_NAME
```



### 13.2.7 结合多个主键

主键由多个字段组成，或者外键由多个字段组成，分别引用多个主键

### 13.3 需要考虑的事项

查询里的结合越多，数据库需要完成的工作就越多，花费的时间也越长。

### 13.3.1 使用基表

基表用于结合具有公用字段的一个或多个表，或是结合没有公用字段的多个表。

```
SELECT C.CUST_NAME,P.PROD_DESC 
FROM CUSTOMER_TBL C,PRODUCTS_TBL P,ORDERS_TBL O
WHERE C.CUST_ID=O.CUST_ID AND P.PROD_ID=O.PROD_ID;
```

### 13.3.2 笛卡尔积

笛卡尔积是笛卡尔结合或"无结合"的结果。如果从两个或多个没有结合的表里获取数据，输出结果就是所有被选表里的全部记录。

笛卡尔积也被称为交叉结合

```
FROM TABLE1,TABLE2 [,TABLE3]
WHERE TABLE1,TABLE2 [,TABLE3]
```

笛卡尔积的结果数量是两个或多个表记录数量的乘积，一般结合where子句使用

# 第14章 使用子查询定义未确定数据

重点：

* 子查询
* 子查询与数据操作命令
* 嵌入式子查询

## 14.1 什么是子查询

子查询也被称为嵌套查询，是位于另一个查询的WHERE或HAVAING子句里的查询，它返回的数据通常在主查询里作为一个条件，从而进一步限制数据库返回的数据。可用于SELECT、INSERT、UPDATE、DELETE语句

子查询必须遵循的规则：

* 子查询必须位于圆括号内
* 除非主查询里有多个字段让子查询进行比较，否则子查询的SELECT子句里只能有一个字段
* 子查询不能使用ORDER BY子句，可以使用GROUP BY子句实现ORDER BY功能
* 返回多条记录的子查询只能与多值操作符配合使用
* SELECT列表里不能引用任何BLOB、ARRAY、CLOB或NCLOB类型的值
* 子查询不能直接被包围在函数里
* 操作符BETWEEN不能用于子查询，但子查询内部可以使用它。

基本语法：

```
SELECT COLUMN_NAME [,COLUMN_NAME] 
FROM TABLE2 [,TABLE2]
WHERE COMLUMN_NAME OPERATOR
(SELECT COLUMN_NAME[,COLUMN_NAME] 
 FROM TABLE1 [,TABLE2] 
 [WHERE CONDITIONS])
```

正确使用BETWEEN：

```
SELECT COLUMN_NAME
FROM TABLE_A
WHERE COLUMN_NAME OPERATOR (SELECT COLUMN_NAME
                            FROM TABLE_B
                            WHERE VALUE BETWEEN VALUE)
```

错误使用BETWEEN：

```
SELECT COLUMN_NAME
FROM TABLE_A
WHERE COLUMN_NAME BETWEEN VALUE AND (SELECT COLUMN_NAME      
                                     FROM TABLE_B)
```



### 14.1.1 子查询与SELECT语句

子查询也可用于数据操作语句，但主要还是用于SELECT语句里。

使用子查询来查找不确定的值：

```
SELECT E.EMP_ID,E.LAST_NAME,E.FIRST_NAME,EP.PAY_RATE
FROM EMPLOYEE_TBL E,EMPLOYEE_PAY_TBL TP
WHERE E.EMP_ID=EP.EMP_ID
AND EP.PAY_RATE < (SELECT PAY_RATE FROM EMPLOYEE_PAY_TBL WHERE EMP_ID='34555')
```

### 14.1.2 子查询与INSERT语句

子查询可以与数据操作语言(DML)配合使用

INSERT：将子查询返回的结果插入到另一个表

```
INSERT INTO TABLE_NAME [(COLUMN1 [,COLUMN2])]
SELECT [*|COLUMN1 [,COLUMN2]]
FROM TABLE1 [,TABLE2]
[WHERE VALUE OPERATOR [SELECT ....]]
```

### 14.1.3 子查询与UPDATE

子查询可以与UPDATE语句配合使用来更新一个表里的一个或多个字段：

```
UPDATE TABLE 
SET COLUMN_NAME[,COLUMN_NAME]=
    (SELECT COLUMN_NAME[,COLUMN_NAME]
     FROM TABLE [WHERE])
```

注：这语句有问题啊

### 14.1.4 子查询与DELETE语句

```
DELETE FROM TABLE_NAME
[WHERE COLUMUN_NAME OPERATOR 
                (SELECT COLUMN_NAME 
                 FROM TABLE_NAME 
                 [WHERE])]
```



## 14.2 嵌套的子查询

子查询可以嵌入到另一个子查询里。

在有子查询时，子查询先于主查询执行。最内层子查询最先被执行，再依次执行外层的子查询，直到主查询

注：多个子查询可能会延长响应时间，还可能降低结果的准确性



## 14.3 关联子查询

关联子查询是依赖主查询里的信息的子查询。这意味着子查询里的表可以与主查询里的表相关联。要在子查询里使用某个表，必须首先在主查询里引用这个表。

```
SELECT C.CUST_NAME
FROM CUSTOMER_TBL C
WHERE 10<(SELECT SUM(O.OTY)
          FROM ORDERS_TBL O
          WHERE O.CUST_ID=C.CUST_ID); 
```

## 14.4 子查询的效率

子查询会对执行效率产生影响，子查询先于主查询执行，子查询所花费的时间会直接影响整个查询所需要的时间。

# 第 15 章 组合多个查询

重点：

- 简介用于组合查询的操作符
- 何时对查询进行组合
- GROUP BY 子句与组合命令
- ORDER BY 与组合命令
- 如何获取准确的数据

利用 UNION、UNION ALL、INTERSECT、 EXCEPT 把多个 SQL 查询组合为一个

## 15.1 单查询与组合查询

单查询是一个 SELECT 语句，组合查询具有多个 SELECT 语句，由负责结合两个查询的操作符组成。组合操作符用于组合和限制两个 SELECT 语句的结果，他们可以返回或清除重复的记录，可以获取不同字段里的类似数据。

## 15.2 组合查询操作符

ANSI 标准包括 UNION、UNION ALL、EXCEPT、INTERSECT

### 15.2.1 UNION

组合两个或多个 SELECT 语句的结果，不包含重复的记录

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
UNION
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

注：使用 UNION 时，每个 SELECT 语句里必须选择同样数量的字段、同样数量的字段表达式、同样的数据类型、同样的次序—但长度不必一样。另外，使用 UNION 结合两个返回不同字段的 SELECT 语句，返回结果的列标题有第一个查询的字段名称决定，相同的列序号会被合并成一列

### 15.2.2 UNION ALL

组合两个或多个 SELECT 的结果，且包含重复的结果，其他与 UNION 一样

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

组合两个 SELECT 语句，但只返回两个 SELECT 语句中相同的记录。好像 MySQL 还不支持

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

组合两个 SELECT 语句，返回第一个 SELECT 语句里有第二个 SELECT 语句里没有的记录。MySQL 不支持

```
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
EXCEPT
SELECT COLUMN1[,COLUMN2]
FROM TABLE1[,TABLE2]
[WHERE]
```

可以用 MINUS 代替(MySQL 也没有)

## 15.2 组合查询里使用 ORDER BY

ORDER BY 子句只能用于对全部查询结果的排序，因此组合查询只能有一个 ORDER BY 子句，且只能以别名或数字来引用字段

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

注：如果排序的字段在全部查询语句里都有相同的名称，它的名称可以用于 ORDER BY 子句里

```
SELECT PROD_DESC FROM PRODUCTS_TBL
UNION
SELECT PROD_DESC FROM PRODUCTS_TBL
ORDER BY PROD_DESC
```

## 15.4 组合查询里使用 GROUP BY

与 ORDER BY 不同，GROUP BY 可用于组合查询中的每一个 SELECT 语句，也可用于全部查询的结果。另外，HAVING 子句也可用于组合查询里的每个 SELECT 语句。

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

使用组合查询时要小心，在使用 INTERSECT 操作符时，如果第一个查询的 SELECT 语句有问题，就可能会得到不正确或不完整的数据。

# 第 16 章 利用索引改善性能

重点：

- 索引如何工作
- 创建索引
- 不同类型的索引
- 何时使用索引
- 何时不适用索引

## 16.1 什么是索引

索引是一个指针，指向数据在表里的准确物理位置。

索引通常与相应的表是分开保存的，主要目的是提高数据检索的性能。

索引的创建与删除不会影响到数据本身，但会影响数据检索的速度。

索引会占据物理存储空间，且可能比表本身还大。

## 16.2 索引是如何工作的

索引创建之后，用于记录与被索引字段相关联的位置值。当表里添加新数据时，索引里也会添加新项。执行查询，且 WHERE 条件里指定的字段设置了索引时，数据库会首先在索引里搜索 WHERE 子句里指定的值。如果在索引中找到了这个值，索引就返回给搜索数据在表里的实际位置。如果没哟索引，查询时，数据库会进行全表扫描。

索引通常以一种树形结构保存信息。

## 16.3 CREATE INDEX 命令

创建索引()不同实现，创建方式不同)

```
CREATE INDEX INDEX_NAME ON TABLE_NAME
```

## 16.4 索引的类型

### 16.4.1 单字段索引

如果某个字段经常在 WHERE 子句作为单独的查询条件，它的单子段索引最有效。

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

注：定义表的主键时，会创建一个默认的索引。允许 NULL 值的字段上也不能创建唯一索引。

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

经常在 ORDER BY 和 GROUP BY 里引用的字段应该考虑设置索引。

具有大量唯一值的字段，或是在 WHERE 子句里会返回很小部分记录的字段，可以考虑设置索引。

## 16.6 何时应该避免使用索引

使用索引的方针：

- 索引不应该用于小规模的表
- 当字段用于 WHERE 子句作为过滤器会返回表里的大部分记录时，该字段不适合设置索引
- 经常会被批量更新的表可以具有索引，但批量操作的性能会由于索引而降低，因为索引也会被更新(一般在批量操作前去除索引，完成后再创建索引)
- 不应该对包含大量 NULL 值的字段设置索引。
- 经常被操作的字段不应该设置索引，因为对索引的维护会变得很繁重

## 16.7 修改索引

```
ALTER INDEX INDEX_NAME
```

## 16.8 删除索引

```
DROP INDEX INDEX_NAME ON TABLE_NAME
```

# 第 17 章 改善数据库性能

重点：

- 什么是 SQL 语句调整
- 数据库调整与 SQL 语句调整
- 格式化 SQL 语句
- 适当地结合表
- 最严格的条件
- 全表扫描
- 使用索引
- 避免使用 OR 和 HAVING
- 避免大规模排序操作

## 17.1 什么是 SQL 语句调整

SQL 语句调整是优化生成 SQL 语句的过程，从而以最有效和最高效的方式获得结果。

SQL 语句调整主要涉及调整语句的 FROM 和 WHERE 子句，因为数据库服务程序主要根据这两个子句执行查询。

## 17.2 数据库调整和 SQL 语句调整

数据库调整是调整实际数据库的过程，调整的目标是确保数据库的设计能够最好地满足用户对数据库操作的需要

SQL 调整是调整访问数据库的 SQL 语句，包括数据库查询和事务操作。目标是利用数据库和系统资源、索引，针对数据库的当前状态进行最有效的访问，从而减少对数据库执行查询所需的开销。

注：两种调整缺一不可，相互配合

## 17.3 格式化 SQL 语句

### 17.3.1 为提高可读性格式化 SQL 语句

语句的整洁程度并不会影响实际的性能，但方便修改和调整

良好可读性的基本规则：

- 每个子句都以新行开始
- 子句里的参数超过一行长度需要换行时，利用 TAB 或空格来缩进
- 以一致的方式使用 TAB 和空格
- 当语句里使用多个表时，使用表的别名
- 有节制地使用注释(如果允许的话)
- 如果 SELECT 语句里要使用多个字段，就让每个字段都从新行开始
- FROM 子句里要使用多个表，让每个表名都从新行开始
- 让 WHERE 子句里每个条件都以新行开始，看清所有条件和次序

### 17.3.2 FROM 子句里的表

FROM 子句里表的次序对性能有很大影响，取决于优化器如何读取 SQL 语句。通常，较小的表列在前面，较大的表列在后面，有更好的性能

### 17.3.3 结合条件的次序

有基表时，基表的字段一般放到结合操作的右侧，要被结合的表通常按照从小到大的次序排列，就像 FROM 子句里表的排列顺序一样

如果没有基表，表应该从小到大排列，让最大的表位于 WHERE 子句里结合操作的右侧，结合条件应该位于 WHERE 子句的最前面，其后才是过滤条件

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

最严格条件通常是 SQL 查询达到最优性能的关键因素。

最严格条件是 WHERE 子句里返回最少记录的条件，对返回的数据进行了最大限度的过滤

应该让 SQL 优化器首先计算最严格条件，减少查询开销。最严格条件的位置取决于优化器的工作方式。

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

避免全表扫描最简单的方法：在 WHERE 子句中设置条件来过滤返回的数据

应该被索引的数据：

- 主键
- 外键
- 在结合表里经常使用的字段
- 经常在查询里作为条件的字段
- 大部分值是唯一值的字段。

注：对小型表，或是返回表里的大部分记录的查询，应该执行全表扫描

## 17.5 其他性能考虑

- 使用 LIKE 操作符和通配符：在查询里使用通配符能够消除很多可能返回的记录，对于搜索类似数据(不等于特定值的数据)的查询，通配符非常灵活。
- 避免使用 OR 操作符：在 SQL 语句里使用 IN 代替 OR 能够提高数据检索速度。
- 避免使用 HAVING 子句：HAVING 子句可以减少 GROUP BY 子句返回的数据，但会让 SQL 优化器进行额外的工作，花费额外的时间
- 避免大规模排序操作：最好把大规模排序在批处理过程里，在数据库使用的非繁忙期运行
- 使用存储过程：为经常运行的 SQL 语句创建存储过程(经过编译的、以可执行格式永久保存在数据库里的 SQL 语句)
- 在批加载时关闭索引：当用户向数据库提交一个事务时，表和相关联的索引都会有数据变化。在批量加载时，索引可能会严重地降低性能(删除索引——批处理——创建索引：这样还可以减少索引里的碎片)

## 17.6 基于成本的优化

基于成本的优化根据资源消耗量对 SQL 语句进行排序。根据查询的衡量方法(执行时间、读库次数等)以及给定时间段内的执行次数，可以确定资源消耗量：

$$
总计资源消耗=衡量方法 \times 执行次数
$$

## 17.7 性能工具

待补充...

# 第 18 章 管理数据库用户

# 第 19 章 管理数据库安全

# 第 20 章 创建和使用视图及异名

## 20.1 什么是视图

视图是一个虚拟表，对于用户来说，视图在外观和行为上类似表，但它不需要实际的物理存储，但也被看做是一个数据库对象，它从表里引用数据。在数据库里，视图的使用方式与表一样，可以向操作表一样从视图里获取数据。视图只保存在内存里，且只保存其定义本身。

视图实际上由预定义查询形式的表所组成。视图可以包含表的全部或部分记录，可以由一个表或多个表创建。

创建视图时，实际上是在数据库里执行了一个 SELECT 语句，它定义了这个视图。

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

通过在 SELECT 语句里使用 JOIN，可以从多个表创建视图

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

在从多个表创建视图时，这些表必须在 WHERE 子句里通过共同字段实现结合。

### 20.2.3 从视图创建视图

```
CREATE VIEW2 AS
SELECT * FROM VIEW1
```

视图创建视图可以具有多个层次(视图的视图，以此类推)，允许的层次取决于具体实现。

基于视图创建视图的唯一问题在于它们的可管理性，例如，基于 VIEW1 创建了 VIEW2，基于 VIEW2 创建了 VIEW3，如果 VIEW1 被删除，VIEW2 和 VIEW3 也就不可用了。

注：如果从基表和从另一个视图创建视图具有一样的难度和效率，首选从基表创建视图。

## 20.3 WITH CHECK OPTION

WITH CHECK OPTION 确保全部的 UPDATE 和 INSERT 语句满足视图定义里的条件。如果不满足条件，UPDATE 或 INSERT 语句就会返回错误。实际上是通过查看视图定义是否被破坏来确保引用完整性。

```
CREATE VIEW EMPLOYEE_PAGERS AS
SELECT LAST_NAME,FIRST_NAME,PAGER
FROM EMPLOYEE_TBL
WHERE PAGER IS NOT NULL
WITH CHECK OPTION

INSERT INTO EMPLOYEE_PAGERS
VALUES('SMITH','JOHN',NULL)  //此时会报错，因为视图定义里PAGER不能为NULL
```

在基于视图创建另一个视图时，WITH CHECK OPTION 有两个选项：CASCADED 和 LOCAL，CASCADED 是默认选项。

在对基表进行更新时，CASCADED 选项会检查所有底层视图、所有完整性约束，以及新视图的定义条件。LOCAL 选项只检查两个视图的完整性约束和新视图的定义条件。因此 CASCADED 创建视图是更安全的做法。

## 20.4 从视图创建表

```
CREATE TABLE TABLE_NAME AS
SELECT *|COLUMN1[,COLUMN2]
FROM VIEW_NAME
[WHERE CONDITION1[,CONDITION2]]
[ORDER BY]
```

注：表包含实际的数据、占据物理存储空间；视图不包含数据，只需保存视图定义

## 20.5 视图与 ORDER BY 子句

CREATE VIEW 语句里不能包含 ORDER BY 子句，但可以包含 GROUP BY 子句，起到类似 ORDER BY 子句的作用。

```
CREATE VIEW NAMES2 AS
SELECT CONCAT(LAST_NAME,FIRST_NAME,MIDDLE_NAME) NAME
FROM EMPLOYEE_TBL
GROUP BY CONCAT(LAST_NAME,FIRST_NAME,MIDDLE_NAME)
```

## 20.6 通过视图更新数据

在一定条件下，视图的底层数据可以进行更新：

- 视图不包含结合
- 视图不包含 GROUP BY 子句
- 视图不包含 UNION 语句
- 视图不包含对伪字段 ROWNUM 的任何引用
- 视图不包含任何组函数
- 不能使用 DISTINCT 子句
- WHERE 子句包含的嵌套的表达式不能与 FROM 子句引用同一个表
- 视图可以执行 INSERT、UPDATE 和 DELETE 等语句。

## 20.7 删除视图

DROP VIEW 命令用于从数据库里删除视图，有两个选项：

- RESTRICT：使用 RESTRICT 进行删除操作，而其他视图在约束里有所引用，删除操作会出错
- CASCADE：其他视图或约束被引用，DROP VIEW 也会成功

## 20.8 嵌套视图对性能的影响

在查询中使用视图与使用表有着相同的性能。

嵌套的层数越多，搜索引擎为了获得一个执行计划而需要进行的分析工作就越多。尽量减少嵌套层数。

## 20.9 异名

异名就是表或视图的另一个名称，方便在访问其他用户的表或视图时不必使用完整限制名。

异名可以创建为：

- PUBLIC：可以被数据库里的其他用户使用，一般只有 DBA 或被授权用户可以创建
- PRIVATE：只能被所有者和拥有权限的用户使用，全部用户都以创建

注：因为 MySQL 不支持异名，下面就不细讲。

### 20.9.1 创建异名

### 20.9.2 删除异名

# 第 21 章 使用系统目录

## 21.1 什么是系统目录

系统目录是一些表和视图的集合，包含关于数据库的信息。每个数据库都有系统目录，其中定义了数据库的结构，还有数据库所包含数据的信息。

系统目录基本上是一组对象，包含了定义数据库里其他对象的信息、数据库本身的结构以及其他各种重要信息。

方便用户在无需知道数据库结构或进程的前提下查看数据库权限、对象

## 21.2 如何创建系统目录

系统目录由数据库创建时自动创建或 DBA 在数据创建之后立即创建。

## 21.3 系统目录包含什么内容

包含的内容有：

- 用户账户和默认设置
- 权限和其他安全信息
- 性能统计
- 对象大小估计
- 对象变化
- 表结构和存储
- 索引结构和存储
- 数据库其他对象的信息，比如视图、异名、触发器和存储过程
- 表约束和引用完整性信息
- 用户会话
- 审计信息
- 内部数据库设置
- 数据库文件的位置

系统目录由数据库服务程序维护。

### 21.3.1 用户数据

关于个人用户的全部信息

### 21.3.2 安全信息

用户标识、加密的密码、各种权限和权限组等

### 21.3.3 数据库设计信息

数据库的信息：创建日期、名称、对象大小估计、数据文件的大小和位置等

### 21.3.4 性能统计

SQL 语句性能的信息：优化器执行 SQL 语句的时间和方法等

内存分配和使用、数据库里剩余空间、控制表哥和索引碎片的信息

## 21.4 不同实现里的系统目录表格

MySQL：

| 表格名称     | 内容                      |
| ------------ | ------------------------- |
| COLUMNS_PRIV | 字段权限                  |
| DB           | 数据库权限                |
| FUNC         | 自定义函数的管理          |
| HOST         | 与 MySQL 相关联的主机名称 |
| TABLES_PRIV  | 表权限                    |
| USER         | 表关系                    |

## 21.5 查询系统目录

用户通常只能查询与用户相关的表，不能访问系统表，后者通常只能由被授权的用户访问。

暂略

## 21.6 更新系统目录对象

系统目录的更新是由数据库服务程序自动完成的。

# 第 22 章 高级 SQL 主题

## 22.1 光标

数据库操作通常是以数据集为基础的操作，即大部分 ANSI SQL 命令是作用于一组数据的。

光标则是以记录为单位的操作，获得数据库中数据的子集。程序可以依次对光标里的每一行进行求值，光标一般用于过程化程序里嵌入的 SQL 语句。

不同实现对光标用法的定义不同。只介绍 MySQL

声明光标：

```
DECLARE CURSOR_NAME CURSOR
FOR SELECT_STATEMENT
```

创建光标之后，可以使用如下操作对其进行访问：

- OPEN：打开定义的光标
- FETCH：从光标获取记录，赋予程序变量
- CLOSE：对光标的操作完成之后，关闭光标

### 22.1.1 打开光标

使用光标前，必须先打开光标。光标被打开时，指定光标的 SELECT 语句被执行，查询结果被保存在内存的特定区域。

```
OPEN CURSOR_NAME
```

### 22.1.2 从光标获取数据

光标打开之后，使用 FETCH 获取光标的内容：

```
FETCH CURSOR_NAME INTO VARIABLE_NAME[,VARIABLE_NAME]...
```

注：从光标获得数据时，需要注意可能会到达光标末尾。

MySQL 中的处理方法：

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

存储过程是一组相关联的 SQL 语句，通常称为函数或子程序。存储过程与一系列单个 SQL 语句相比更容易执行。存储过程可以调用其他存储过程。

存储过程是保存在数据库里的一组 SQL 语句或函数，被预编译，随时可以被数据库用户使用。存储函数与存储过程一样，但函数可以返回一个值。函数由过程调用，被调用时可以传递参数，返回一个值给调用它的过程。

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

与单个 SQL 语句相比，存储过程的优点：

- 存储过程的语句已经保存在数据库里
- 存储过程的语句已经被解析过，以可执行格式存在
- 支持模块化编程
- 可以调用其他存储过程和函数
- 可以被其他类型的程序调用
- 通常具有更好的响应时间
- 提高了整体易用性

## 22.3 触发器

触发器是数据库编译了的 SQL 过程，基于数据库里发生的其他行为来执行操作。是存储过程的一种，会在特定 DML 行为作用于表格时被执行。可以再 INSERT、DELETE、UPDATE 语句之前或之后执行，可以在这些语句之前检查数据完整性，可以回退事务，可以修改一个表里的数据，可以从另一个数据库的表里读取数据

触发器会导致更多的 I/O 开销，尽量不使用。

### 22.3.1 CREATE TRIGGER 语句

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

注：触发器的内容不能修改，想要修改触发器，只能替换或重新创建它。有些实现允许使用 create trigger 语句替换已经存在的同名触发器

### 22.3.2 DROP TRIGGER 语句

删除触发器

```
DROP TRIGGER TRIGGER_NAME
```

### 22.3.2 FOR EACH ROW 语句

MySQL 里的触发器可以调整触发条件。FOR EACH ROW 语句可以让过程在 SQL 语句影响每条记录时都触发，或是一条语句只触发一次。

```
CREATE TRIGGER TRIGGER_NAME
ON TABLE_NAME FOR EACH ROW SQL_STATEMENT
```

有和没有 FOR EACH ROW 的区别在于触发器执行次数。普通触发器一般一次，加了 FOR EACH ROW 之后，影响几条记录，触发几次。

## 22.4 动态 SQL

动态 SQL 允许程序员或终端用户在运行时创建 SQL 语句的具体代码，并把语句传递给数据库，数据库然后把数据返回到绑定的程序变量里。

静态 SQL 是事先编好的，不准备进行改变的。灵活性不如动态 SQL

使用调用级接口可以创建动态 SQL

## 22.5 调用级接口

调用级接口(CLI)用于把 SQL 代码嵌入到主机程序。

。。。

## 22.6 使用 SQL 生成 SQL

使用 SQL 生成 SQL 可以节省 SQL 语句的编写时间。

例：数据库有 100 个用户，创建了一个新角色 ENABLE，要授予这 100 个用户

```
SELECT 'GRANT ENABLE TO' || USERNAME ||';' FROM SYS.DBA_USERS
```

单引号''，表示它所包围的内容(包括空格在内)要直义使用。||用于连接字段(oracle 的用法)

## 22.7 直接 SQL 与嵌入 SQL

直接 SQL 是指从某种形式的交互终端上执行的 SQL 语句，执行结果会直接返回到终端。直接 SQL 也称为交互调用或直接调用

嵌入 SQL 是在其他程序里使用 SQL 代码，SQL 代码是通过调用级接口嵌入到主机编程语言里的。

## 22.8 窗口表格函数

窗口表格函数可以对表格的一个窗口进行操作，并且基于这个窗口返回一个值。可以计算连续总和、分级和移动平均值等。

```
ARGUMENT OVER ([PARTITION CLAUSE][ORDER CLAUSE][FRAME CLAUSE])
```

几乎所有汇总函数都可以作为窗口表格函数，还有 5 个新的窗口表格函数：

- RANK() OVER
- DENSE_RANK() OVER
- PERCENT_RANK() OVER
- CUME_DIST() OVER
- ROW_NUMBER() OVER

```
SELECT EMP_ID,SALARY,RANK() OVER(PARTITION BY YEAR(DATE_HIRE)
ORDER BY SALARY DESC) AS RANK_IN_DEPT
FROM EMPLOYEE_PAY_TBL
```

有的实现不支持窗口表格函数

## 22.9 使用 XML

以 XML 格式输出查询结果(MySQL 貌似不能直接输出到 XML)。

XML 功能集的另一个重要特性是能够从 XML 文档或片段里获取信息，MySQL 通过 EXTRACTVALUE 函数提供这个功能

```
ExtractValue([XML Fragment],[locator string])
```

第一个参数是 XML 片段，第二个参数是定位器，用于返回与字符串匹配标记的第一个值。

###

##
