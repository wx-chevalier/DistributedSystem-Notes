package pers.kerry.seata.demo.orderservice.service;

import org.springframework.stereotype.Service;
import pers.kerry.seata.demo.orderservice.pojo.OrderDO;

/**
 * @description:
 * @date: 2021/2/13 11:23 下午
 * @author: kerry
 */

public interface OrderService {
    /**
     * 创建订单
     * @param orderDO
     */
    void createOrder(OrderDO orderDO);
}
