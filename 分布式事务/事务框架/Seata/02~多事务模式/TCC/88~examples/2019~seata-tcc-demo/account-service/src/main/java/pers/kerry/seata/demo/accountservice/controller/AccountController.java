package pers.kerry.seata.demo.accountservice.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.kerry.seata.demo.accountservice.service.AccountService;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/11 9:39 上午
 * @author: kerry
 */
@RestController
@RequestMapping("account")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 账户扣钱
     * @param userId
     * @param money
     * @return
     */
    @GetMapping("decrease-money")
    public String decreaseMoney(Long userId, BigDecimal money){
         accountService.decreaseMoney(userId,money);
         return "Account 扣减金额成功！";
    }
}
