package pers.kerry.seata.demo.accountservice.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/13 10:25 下午
 * @author: kerry
 */
@Data
public class AccountDO {
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 剩余可用额度
     */
    private BigDecimal residue;
    /**
     * TCC事务锁定的金额
     */
    private BigDecimal frozen;
}
