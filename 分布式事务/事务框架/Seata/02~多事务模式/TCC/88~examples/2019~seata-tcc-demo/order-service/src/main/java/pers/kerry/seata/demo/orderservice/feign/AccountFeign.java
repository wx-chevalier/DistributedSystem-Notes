package pers.kerry.seata.demo.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/14 10:49 上午
 * @author: kerry
 */
@FeignClient(name = "account")
public interface AccountFeign {

    /**
     * 扣余额
     * @param userId 用户id
     * @param money 金额
     * @return
     */
    @GetMapping("/account/decrease-money")
    String decreaseMoney(@RequestParam("userId") Long userId, @RequestParam("money") BigDecimal money);
}

