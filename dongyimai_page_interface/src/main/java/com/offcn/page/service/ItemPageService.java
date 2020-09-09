package com.offcn.page.service;

/**
 * @Auther: lhq
 * @Date: 2020/8/26 11:04
 * @Description:  网页静态化接口
 */
public interface ItemPageService {

    /**
     * 生成商品详情页
     * @param goodsId
     * @return
     */
    public boolean genItemHtml(Long goodsId);

    /**
     * 批量删除商品详情页
     * @param ids
     * @return
     */
    public boolean deleteItemHtml(Long[] ids);
}
