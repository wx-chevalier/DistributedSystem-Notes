package pers.kerry.seata.demo.storageservice.service;


/**
 * @description:
 * @date: 2021/2/14 11:14 上午
 * @author: kerry
 */
public interface StorageService {
    /**
     * 扣库存
     * @param productId
     * @param count
     * @return
     */
    void decreaseStorage(Long productId, Integer count);
}
