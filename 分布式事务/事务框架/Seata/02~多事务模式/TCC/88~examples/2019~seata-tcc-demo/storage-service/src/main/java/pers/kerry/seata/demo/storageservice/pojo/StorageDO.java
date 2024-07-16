package pers.kerry.seata.demo.storageservice.pojo;

import lombok.Data;

/**
 * @description:
 * @date: 2021/2/14 11:17 上午
 * @author: kerry
 */
@Data
public class StorageDO {
    private Long id;
    /**
     * 产品id
     */
    private Long productId;
    /**
     * 剩余库存
     */
    private Integer residue;
    /**
     * TCC事务锁定的库存
     */
    private Integer frozen;
}
