package pers.kerry.seata.demo.accountservice.service.impl;

import org.springframework.stereotype.Service;
import pers.kerry.seata.demo.accountservice.service.AccountService;
import pers.kerry.seata.demo.accountservice.tcc.AccountTccAction;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/11 9:47 上午
 * @author: kerry
 */
@Service
public class AccountServiceImpl implements AccountService {
    private final AccountTccAction accountTccAction;

    public AccountServiceImpl(AccountTccAction accountTccAction) {
        this.accountTccAction = accountTccAction;
    }

    @Override
    public void decreaseMoney(Long userId, BigDecimal money) {
        accountTccAction.prepareDecreaseMoney(null, userId, money);
    }

}
