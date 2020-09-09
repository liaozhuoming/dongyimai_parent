package com.offcn.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.offcn.search.service.ItemSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Auther: lhq
 * @Date: 2020/8/21 15:47
 * @Description:
 */
@RestController
@RequestMapping("/itemsearch")
public class ItemSearchController {

    @Reference
    private ItemSearchService itemSearchService;

    /**
     * 搜索商品
     * @param searchMap
     * @return
     */
    @RequestMapping("/search")
    public Map search(@RequestBody Map searchMap){
       return itemSearchService.search(searchMap);
    }
}
