package pers.kerry.seata.demo.orderservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.kerry.seata.demo.orderservice.pojo.OrderDO;
import pers.kerry.seata.demo.orderservice.service.OrderService;

/**
 * @description:
 * @date: 2021/2/13 11:22 下午
 * @author: kerry
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     *
     * @param orderDO
     * @return
     */
    @PostMapping("/create")
    public String createOrder(@RequestBody OrderDO orderDO) {
        try {
            orderService.createOrder(orderDO);
        } catch (Exception e) {
            e.printStackTrace();
            return "fail：创建订单失败！";
        }
        return "success：创建订单成功！";
    }
}
