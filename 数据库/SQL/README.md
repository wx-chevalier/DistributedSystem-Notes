![default](https://user-images.githubusercontent.com/5803001/45228854-de88b400-b2f6-11e8-9ab0-d393ed19f21f.png)

# SQL

SQL 是一种专用编程语言，用于管理关系数据库管理系统（RDMS）中的数据，是一个综合的、通用的关系数据库语言，同时又是一种高度非过程化的语言，只要求用户指出做什么而不需要指出怎么做。SQL 语言的功能包括查询、操纵、定义和控制，通常会将 SQL 语句分为以下几类：

- 数据定义语言（DDL）：DDL 包括用于创建表，删除表或创建和删除数据库的其他方面的命令。

- 数据操作语言（DML）：DML 包括用于查询和修改数据库的命令。 它包括用于查询数据库的 select 语句，以及用于修改数据库的 insert，update 和 delete 语句。

```sql
SELECT
    [ALL | DISTINCT | DISTINCTROW ]
      [HIGH_PRIORITY]
      [STRAIGHT_JOIN]
      [SQL_SMALL_RESULT] [SQL_BIG_RESULT] [SQL_BUFFER_RESULT]
      [SQL_CACHE | SQL_NO_CACHE] [SQL_CALC_FOUND_ROWS]
    select_expr [, select_expr ...]
    [FROM table_references
      [PARTITION partition_list]
    [WHERE where_condition]
    [GROUP BY {col_name | expr | position}
      [ASC | DESC], ... [WITH ROLLUP]]
    [HAVING where_condition]
    [WINDOW window_name AS (window_spec)
        [, window_name AS (window_spec)] ...]
    [ORDER BY {col_name | expr | position}
      [ASC | DESC], ...]
    [LIMIT {[offset,] row_count | row_count OFFSET offset}]
    [INTO OUTFILE 'file_name'
        [CHARACTER SET charset_name]
        export_options
      | INTO DUMPFILE 'file_name'
      | INTO var_name [, var_name]]
    [FOR {UPDATE | SHARE} [OF tbl_name [, tbl_name] ...] [NOWAIT | SKIP LOCKED]
      | LOCK IN SHARE MODE]]
```

# 关系代数

关系代数是一种抽象的查询语言，它用对关系的运算来表达查询。任何一种运算都是将一定的运算符作用于一定的运算对象上，得到预期的结果。所以运算对象、运算符、运算结果是运算的三大要素。按运算符的不同分为传统的集合运算和专门的关系运算两类：

- 传统的集合运算包括：并（∪）、差（−）、交（∩）、笛卡尔积（×）。
- 专门的关系运算包括：选择（σ）、投影（π）、连接（⋈）、除运算（÷）。

[![image.png](https://i.postimg.cc/dVjs2b9F/image.png)](https://postimg.cc/0zr1xH3X)
