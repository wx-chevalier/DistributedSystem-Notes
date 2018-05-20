



# Introduction





Structured Query Langugae, or SQL, is a special-purpose programming language used to manage data within a relational database mangement system (RDMS).

You will find that there are multiple ways to write the same query in SQL, and some ways are better than others.



Data Definition Language (DDL): DDL includes commands to create a table, to drop a table, or create and drop other aspects of a database.

Data Manipulation Language (DML): DML includes commands that are used to query and modify a database. It includes the select statement for querying the database, and the insert, update, and delete statements, all for modifying the database.

## Reference

- [W3School SQL教程](https://wizardforcel.gitbooks.io/w3school-sql/content/part1.html)

- [SQL Basics](https://hgducharme.gitbooks.io/sql-basics/content/basic_syntax/README.html)

## Basic Syntax

### Case-When

Case具有两种格式。简单Case函数和Case搜索函数。

```
--简单Case函数
CASE sex
WHEN '1' THEN '男'
WHEN '2' THEN '女'
ELSE '其他' END
--Case搜索函数
CASE WHEN sex = '1' THEN '男'
WHEN sex = '2' THEN '女'
ELSE '其他' END sex_description
```

这两种方式，可以实现相同的功能。简单Case函数的写法相对比较简洁，但是和Case搜索函数相比，功能方面会有些限制，比如写判断式。 
还有一个需要注意的问题，Case函数只返回第一个符合条件的值，剩下的Case部分将会被自动忽略。 

```
--比如说，下面这段SQL，你永远无法得到“第二类”这个结果
CASE WHEN col_1 IN ( 'a', 'b') THEN '第一类'
WHEN col_1 IN ('a')       THEN '第二类'
ELSE'其他' END
```
## Basic Select Statements



# DDL



# Table



## Table Variables & Set Operators



## Join Operators

表联接最常见的即是出现在查询模型中，但是实际的用法绝不会局限在查询模型中。较常见的联接查询包括了以下几种类型：Inner Join  / Outer Join / Full Join / Cross Join 。

### Inner Join


Inner Join是最常用的Join类型，基于一个或多个公共字段把记录匹配到一起。Inner Join只返回进行联结字段上匹配的记录。 如：


``` sql
select * from Products inner join Categories on Products.categoryID=Categories.CategoryID 
```


以上语句，只返回物品表中的种类ID与种类表中的ID相匹配的记录数。这样的语句就相当于: 
``` sql
select * from Products, Categories where Products.CategoryID=Categories.CategoryID
```
Inner Join是在做排除操作，任一行在两个表中不匹配，注定将从结果集中除掉。(我想，相当于两个集合中取其两者的交集，这个交集的条件就是on后面的限定)还要注意的是，不仅能对两个表作联结，可以把一个表与其自身进行联结。

### Outer Join

Outer Join包含了Left Outer Join 与 Right Outer Join. 其实简写可以写成Left Join与Right Join。left join，right join要理解并区分左表和右表的概念，A可以看成左表,B可以看成右表。left join是以左表为准的.,左表(A)的记录将会全部表示出来,而右表(B)只会显示符合搜索条件的记录(例子中为: A.aID = B.bID).B表记录不足的地方均为NULL。 right join和left join的结果刚好相反,这次是以右表(B)为基础的,A表不足的地方用NULL填充。

#### Left Outer Join

#### Right Outer Join

### Full Join


Full Join 相当于把Left和Right联结到一起，告诉SQL Server要全部包含左右两侧所有的行，相当于做集合中的并集操作。


### Cross Join


与其它的JOIN不同在于，它没有ON操作符，它将JOIN一侧的表中每一条记录与另一侧表中的所有记录联结起来，得到的是两侧表中所有记录的笛卡儿积。


# Select

## 

# Aggregation
## Group By
### Case条件分组
有如下数据:(为了看得更清楚，我并没有使用国家代码，而是直接用国家名作为Primary Key)
| 国家(country) | 人口(population) |
| --------------- | ------------------ |
| 中国            | 600                |
| 美国            | 100                |
| 加拿大          | 100                |
| 英国            | 200                |
| 法国            | 300                |
| 日本            | 250                |
| 德国            | 200                |
| 墨西哥          | 50                 |
| 印度            | 250                |

根据这个国家人口数据，统计亚洲和北美洲的人口数量。应该得到下面这个结果。
| 洲     | 人口 |
| ------ | ---- |
| 亚洲   | 1100 |
| 北美洲 | 250  |
| 其他   | 700  |
想要解决这个问题，你会怎么做？生成一个带有洲Code的View，是一个解决方法，但是这样很难动态的改变统计的方式。 
如果使用Case函数，SQL代码如下:
```
SELECT  SUM(population),
CASE country
WHEN '中国'     THEN '亚洲'
WHEN '印度'     THEN '亚洲'
WHEN '日本'     THEN '亚洲'
WHEN '美国'     THEN '北美洲'
WHEN '加拿大'  THEN '北美洲'
WHEN '墨西哥'  THEN '北美洲'
ELSE '其他' END
FROM    Table_A
GROUP BY CASE country
WHEN '中国'     THEN '亚洲'
WHEN '印度'     THEN '亚洲'
WHEN '日本'     THEN '亚洲'
WHEN '美国'     THEN '北美洲'
WHEN '加拿大'  THEN '北美洲'
WHEN '墨西哥'  THEN '北美洲'
ELSE '其他' END;
```
同样的，我们也可以用这个方法来判断工资的等级，并统计每一等级的人数。SQL代码如下； 
```
SELECT
CASE WHEN salary <= 500 THEN '1'
WHEN salary > 500 AND salary <= 600  THEN '2'
WHEN salary > 600 AND salary <= 800  THEN '3'
WHEN salary > 800 AND salary <= 1000 THEN '4'
ELSE NULL END salary_class,
COUNT(*)
FROM    Table_A
GROUP BY
CASE WHEN salary <= 500 THEN '1'
WHEN salary > 500 AND salary <= 600  THEN '2'
WHEN salary > 600 AND salary <= 800  THEN '3'
WHEN salary > 800 AND salary <= 1000 THEN '4'
ELSE NULL END;
```
还可以用一个SQL语句完成不同条件的分组合并：
有如下数据 

| 国家(country) | 性别(sex) | 人口(population) |
| --------------- | ----------- | ------------------ |
| 中国            | 1           | 340                |
| 中国            | 2           | 260                |
| 美国            | 1           | 45                 |
| 美国            | 2           | 55                 |
| 加拿大          | 1           | 51                 |
| 加拿大          | 2           | 49                 |
| 英国            | 1           | 40                 |
| 英国            | 2           | 60                 |

按照国家和性别进行分组，得出结果如下 

| 国家   | 男  | 女  |
| ------ | --- | --- |
| 中国   | 340 | 260 |
| 美国   | 45  | 55  |
| 加拿大 | 51  | 49  |
| 英国   | 40  | 60  |

普通情况下，用UNION也可以实现用一条语句进行查询。但是那样增加消耗(两个Select部分)，而且SQL语句会比较长。 

下面是一个是用Case函数来完成这个功能的例子 

```
SELECT country,
SUM( CASE WHEN sex = '1' THEN
population ELSE 0 END),  --男性人口
SUM( CASE WHEN sex = '2' THEN
population ELSE 0 END)   --女性人口
FROM  Table_A
GROUP BY country;
```

这样我们使用Select，完成对二维表的输出形式，充分显示了Case函数的强大。 







