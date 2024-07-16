package pers.kerry.seata.demo.storageservice.controller;

import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pers.kerry.seata.demo.storageservice.service.StorageService;

/**
 * @description:
 * @date: 2021/2/14 11:13 上午
 * @author: kerry
 */
@RestController
@RequestMapping("storage")
public class StorageController {
    private final StorageService storageService;
    public StorageController(StorageService storageService){
        this.storageService=storageService;
    }

    @GetMapping("/decrease-storage")
    public String decreaseStorage(@RequestParam("productId") Long productId,@RequestParam("count") Integer count)  {
        storageService.decreaseStorage(productId,count);
        return "减少商品库存成功！";
    }
}
