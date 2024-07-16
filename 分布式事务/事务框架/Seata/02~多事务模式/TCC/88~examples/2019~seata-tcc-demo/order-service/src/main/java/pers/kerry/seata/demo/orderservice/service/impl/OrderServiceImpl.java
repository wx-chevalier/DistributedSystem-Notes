package pers.kerry.seata.demo.orderservice.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import pers.kerry.seata.demo.orderservice.feign.AccountFeign;
import pers.kerry.seata.demo.orderservice.feign.StorageFeign;
import pers.kerry.seata.demo.orderservice.pojo.OrderDO;
import pers.kerry.seata.demo.orderservice.service.OrderService;
import pers.kerry.seata.demo.orderservice.tcc.OrderTccAction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @description:
 * @date: 2021/2/13 11:23 下午
 * @author: kerry
 */
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderTccAction orderTccAction;
    private final AccountFeign accountFeign;
    private final StorageFeign storageFeign;

    public OrderServiceImpl(OrderTccAction orderTccAction, AccountFeign accountFeign, StorageFeign storageFeign){
        this.orderTccAction=orderTccAction;
        this.accountFeign=accountFeign;
        this.storageFeign=storageFeign;
    }

    /**
     * 创建订单
     * @param orderDO
     */
    @GlobalTransactional
    @Override
    public void createOrder(OrderDO orderDO) {
        String orderNo=this.generateOrderNo();
        //创建订单
        orderTccAction.prepareCreateOrder(null,
                orderNo,
                orderDO.getUserId(),
                orderDO.getProductId(),
                orderDO.getAmount(),
                orderDO.getMoney());
        //扣余额
        accountFeign.decreaseMoney(orderDO.getUserId(),orderDO.getMoney());
        //扣库存
        storageFeign.decreaseStorage(orderDO.getProductId(),orderDO.getAmount());
    }

    private String generateOrderNo(){
        return LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern("yyMMddHHmmssSSS")
                );
    }
}
