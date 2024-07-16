package pers.kerry.seata.demo.accountservice.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/11 9:43 上午
 * @author: kerry
 */
public interface AccountService {
    /**
     * 账户扣钱
     * @param userId 用户id
     * @param money 扣钱金额
     * @return
     */
    void decreaseMoney(Long userId, BigDecimal money);
}
