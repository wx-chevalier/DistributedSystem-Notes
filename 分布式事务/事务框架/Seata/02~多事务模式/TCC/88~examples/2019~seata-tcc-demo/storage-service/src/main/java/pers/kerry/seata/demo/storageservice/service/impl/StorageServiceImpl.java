package pers.kerry.seata.demo.storageservice.service.impl;

import org.springframework.stereotype.Service;
import pers.kerry.seata.demo.storageservice.service.StorageService;
import pers.kerry.seata.demo.storageservice.tcc.StorageTccAction;

/**
 * @description:
 * @date: 2021/2/14 11:14 上午
 * @author: kerry
 */
@Service
public class StorageServiceImpl implements StorageService {
    private final StorageTccAction storageTccAction;
    public StorageServiceImpl(StorageTccAction storageTccAction){
        this.storageTccAction=storageTccAction;
    }

    /**
     * 扣库存
     *
     * @param productId
     * @param count
     * @return
     */
    @Override
    public void decreaseStorage(Long productId, Integer count) {
        storageTccAction.prepareDecreaseStorage(null, productId, count);
    }
}
