package com.offcn.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.cart.service.CartService;
import com.offcn.group.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/9/1 10:27
 * @Description:
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 向购物车列表中添加商品
     *
     * @param srcCartList 原购物车列表
     * @param itemId      SKUID
     * @param num         商品数量
     * @return
     */
    public List<Cart> addGoodsToCartList(List<Cart> srcCartList, Long itemId, Integer num) {
        //1.根据SKUID，查询SKU信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        //1.1 判断商品是否为空
        if (item == null) {
            throw new RuntimeException("商品不存在");
        }
        //1.2 商品审核状态 审核通过
        if (!item.getStatus().equals("1")) {
            throw new RuntimeException("商品审核状态为未通过");
        }
        String sellerId = item.getSellerId();  //获得商家ID
        String sellerName = item.getSeller(); //获得商家名称
        //2.判断购物车列表中是否含有该商家的购物车
        Cart cart = this.searchCartBySellerId(srcCartList, sellerId);
        if (cart == null) {   //2.1 商家的购物车为空
            cart = new Cart();
            cart.setSellerId(sellerId);   //商家ID
            cart.setSellerName(sellerName);  //商家名称
            List<TbOrderItem> orderItemList = new ArrayList<TbOrderItem>();  //订单详情集合
            TbOrderItem tbOrderItem = this.createOrderItem(item, num);
            orderItemList.add(tbOrderItem);
            cart.setOrderItemList(orderItemList);   //设置购物车对象中的订单详情列表
            //将新创建的购物车对象放入到原购物车列表
            srcCartList.add(cart);
        } else {   //2.2 购物车不为空
            //3.判断该商品是否在商家的购物车中存在
            TbOrderItem tbOrderItem = this.searchOrderItemByItemId(cart.getOrderItemList(), itemId);
            if (tbOrderItem == null) {      //3.1 订单详情为空
                tbOrderItem = this.createOrderItem(item, num);   //创建订单详情对象
                cart.getOrderItemList().add(tbOrderItem);
            } else { //3.2订单详情不为空
                tbOrderItem.setNum(tbOrderItem.getNum() + num);   //在原有数量基础上+购买数量
                tbOrderItem.setTotalFee(new BigDecimal(tbOrderItem.getPrice().doubleValue() * tbOrderItem.getNum()));  //重新更新购买总价格
                //判断商家的订单详情信息元素是否小于0
                if (tbOrderItem.getNum() < 1) {
                    cart.getOrderItemList().remove(tbOrderItem);    //移除商品详情
                }
                if (cart.getOrderItemList().size() == 0) {
                    srcCartList.remove(cart);         //移除购物车对象
                }
            }
        }
        return srcCartList;
    }

    /**
     * 根据用户账号从缓存中取得购物车列表
     *
     * @param username
     * @return
     */
    public List<Cart> findCartListFromRedis(String username) {
        //从缓存中取得信息
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
        if(CollectionUtils.isEmpty(cartList)){
            cartList = new ArrayList<Cart>();
        }
        return cartList;
    }

    /**
     * 向缓存中保存购物车列表
     *
     * @param username
     * @param cartList
     */
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("向缓存中保存购物车信息成功");
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    /**
     * 合并购物车
     *
     * @param cartList_redis
     * @param cartList_cookie
     * @return
     */
    public List<Cart> margeCartList(List<Cart> cartList_redis, List<Cart> cartList_cookie) {
        //从cookie向redis中合并
        for(Cart cart:cartList_cookie){
            for(TbOrderItem orderItem:cart.getOrderItemList()){
                cartList_redis = this.addGoodsToCartList(cartList_redis,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList_redis;
    }


    //判断该商家中是否含有该商品信息
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
        for (TbOrderItem orderItem : orderItemList) {
            if (orderItem.getItemId().longValue() == itemId.longValue()) {
                return orderItem;
            }
        }
        return null;
    }


    //通过商家ID返回该商家的购物车对象
    private Cart searchCartBySellerId(List<Cart> list, String sellerId) {
        for (Cart cart : list) {
            if (cart.getSellerId().equals(sellerId)) {
                return cart;
            }
        }
        return null;
    }


    //创建订单详情信息
    private TbOrderItem createOrderItem(TbItem item, Integer num) {
        if (num < 1) {
            throw new RuntimeException("数量非法");
        }

        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setItemId(item.getId());                                                //SKUID
        orderItem.setGoodsId(item.getGoodsId());                                          //SPU 商品ID
        orderItem.setPicPath(item.getImage());                                           //图片路径
        orderItem.setSellerId(item.getSellerId());                                       //商家ID
        orderItem.setTitle(item.getTitle());                                              //SKU 名称
        orderItem.setPrice(item.getPrice());                                              //商品单价
        orderItem.setNum(num);                                                            //购买数量
        orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue() * num));    //购买总价格
        return orderItem;
    }
}
