package pers.kerry.seata.demo.orderservice.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:
 * @date: 2021/2/13 11:26 下午
 * @author: kerry
 */
@Data
public class OrderDO {
    /**
     * 数据库主键
     */
    private Long id;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 产品id
     */
    private Long productId;
    /**
     * 数量
     */
    private Integer amount;
    /**
     * 金额
     */
    private BigDecimal money;
    /**
     * 订单状态：0：创建中；1：已完结
     */
    private Integer status;

    public OrderDO(){}

    public OrderDO(String orderNo, Long userId, Long productId, Integer amount, BigDecimal money, Integer status) {
        this.orderNo=orderNo;
        this.userId = userId;
        this.productId = productId;
        this.amount = amount;
        this.money = money;
        this.status = status;
    }
}
