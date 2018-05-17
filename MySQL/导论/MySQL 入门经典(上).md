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










###  