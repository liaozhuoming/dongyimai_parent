package com.offcn.cart.service;

import com.offcn.group.Cart;

import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/9/1 10:21
 * @Description: 购物车接口
 */
public interface CartService {

    /**
     * 向购物车列表中添加商品
     * @param srcCartList   原购物车列表
     * @param itemId        SKUID
     * @param num           商品数量
     * @return
     */
    public List<Cart>  addGoodsToCartList(List<Cart> srcCartList, Long itemId,Integer num);

    /**
     * 根据用户账号从缓存中取得购物车列表
     * @param username
     * @return
     */
    public List<Cart> findCartListFromRedis(String username);

    /**
     * 向缓存中保存购物车列表
     * @param username
     * @param cartList
     */
    public void  saveCartListToRedis(String username,List<Cart> cartList);

    /**
     * 合并购物车
     * @param cartList_redis
     * @param cartList_cookie
     * @return
     */
    public List<Cart> margeCartList(List<Cart> cartList_redis,List<Cart> cartList_cookie);
}
