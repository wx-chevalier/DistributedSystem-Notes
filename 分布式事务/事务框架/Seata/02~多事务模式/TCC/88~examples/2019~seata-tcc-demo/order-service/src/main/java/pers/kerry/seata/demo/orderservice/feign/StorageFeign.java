package pers.kerry.seata.demo.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description:
 * @date: 2021/2/14 10:50 上午
 * @author: kerry
 */
@FeignClient(name = "storage")
public interface StorageFeign {

    /**
     * 扣库存
     * @param productId 产品id
     * @param count 产品数量
     * @return
     */
    @GetMapping("/storage/decrease-storage")
    String decreaseStorage(@RequestParam("productId")Long productId, @RequestParam("count")Integer count);
}

