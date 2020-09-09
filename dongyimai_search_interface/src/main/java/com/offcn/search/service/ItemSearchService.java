package com.offcn.search.service;

import com.offcn.pojo.TbItem;

import java.util.List;
import java.util.Map;


public interface ItemSearchService {


    /**
     * 搜索商品信息
     * @param searchMap
     * @return
     */
    public Map search(Map searchMap);

    //导入SKU
    public void importItem(List<TbItem> itemList);
    //删除SKU
    public void deleteByGoodsIds(List ids);
}
