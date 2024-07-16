drop database if exists `seata_order`;

CREATE DATABASE `seata_order` charset utf8;

use `seata_order`;


CREATE TABLE `order` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `order_no` VARCHAR(128) NOT NULL COMMENT '订单号',
  `user_id` bigint(11) DEFAULT NULL COMMENT '用户id',
  `product_id` bigint(11) DEFAULT NULL COMMENT '产品id',
  `amount` int(11) DEFAULT NULL COMMENT '数量',
  `money` decimal(11,2) DEFAULT NULL COMMENT '金额',
  `status` int(1) DEFAULT NULL COMMENT '订单状态：0：创建中；1：已完结',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;


-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(100) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';

CREATE TABLE IF NOT EXISTS segment
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    VERSION       BIGINT      DEFAULT 0  NOT NULL COMMENT '版本号',
    business_type VARCHAR(63) DEFAULT '' NOT NULL COMMENT '业务类型，唯一',
    max_id        BIGINT      DEFAULT 0  NOT NULL COMMENT '当前最大id',
    step          INT         DEFAULT 0  NULL COMMENT '步长',
    increment     INT         DEFAULT 1  NOT NULL COMMENT '每次id增量',
    remainder     INT         DEFAULT 0  NOT NULL COMMENT '余数',
    created_at    BIGINT UNSIGNED        NOT NULL COMMENT '创建时间',
    updated_at    BIGINT UNSIGNED        NOT NULL COMMENT '更新时间',
    CONSTRAINT uniq_business_type UNIQUE (business_type)
) CHARSET = utf8mb4
  ENGINE INNODB COMMENT '号段表';


INSERT INTO segment
(VERSION, business_type, max_id, step, increment, remainder, created_at, updated_at)
VALUES (1, 'order_business', 1000, 1000, 1, 0, NOW(), NOW());