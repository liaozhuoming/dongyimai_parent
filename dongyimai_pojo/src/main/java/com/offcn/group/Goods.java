package com.offcn.group;

import com.offcn.pojo.TbGoods;
import com.offcn.pojo.TbGoodsDesc;
import com.offcn.pojo.TbItem;

import java.io.Serializable;
import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/8/17 10:48
 * @Description:  商品的复合实体类
 */
public class Goods implements Serializable {

    private TbGoods goods;    //商品表  SPU
    private TbGoodsDesc goodsDesc ; //商品扩展信息
    private List<TbItem> itemList;  //商品详细信息  SKU

    public TbGoods getGoods() {
        return goods;
    }

    public void setGoods(TbGoods goods) {
        this.goods = goods;
    }

    public TbGoodsDesc getGoodsDesc() {
        return goodsDesc;
    }

    public void setGoodsDesc(TbGoodsDesc goodsDesc) {
        this.goodsDesc = goodsDesc;
    }

    public List<TbItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<TbItem> itemList) {
        this.itemList = itemList;
    }
}
