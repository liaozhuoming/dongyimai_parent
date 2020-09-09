package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.offcn.cart.service.CartService;
import com.offcn.entity.Result;
import com.offcn.group.Cart;
import com.offcn.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/9/1 11:14
 * @Description: 购物车控制层
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference
    private CartService cartService;



    //从Cookie中取得购物车列表
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(HttpServletRequest request, HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();   //获得当前登录人的名称
        System.out.println("username:"+username);
        String cookieStr = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
        if (StringUtils.isEmpty(cookieStr)) {
            //如果列表为空 ，则需要new
            cookieStr = "[]";
        } //如果不为空，则直接返回
        List<Cart> cartList_cookie = JSON.parseArray(cookieStr, Cart.class);
        //判断用户是否登录
        if(username.equals("anonymousUser")){   //未登录
            //从Cookie中取得购物车列表
            return cartList_cookie;
        }else{ //已登录
            List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
            //判断cookie的购物车列表是否为空
            if(cartList_cookie.size()>0){
                //执行合并
              cartList_redis =  cartService.margeCartList(cartList_redis,cartList_cookie);
            }
            //清空Cookie
            CookieUtil.deleteCookie(request,response,"cartList");
            //重新将购物车存入到Redis
            cartService.saveCartListToRedis(username,cartList_redis);
            return cartList_redis;
        }
    }

    @RequestMapping("/addGoodsToCartList")
    @CrossOrigin(origins="http://localhost:9105",allowCredentials="true")
    public Result addGoodsToCartList(HttpServletRequest request, HttpServletResponse response,Long itemId,Integer num){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();   //获得当前登录人的名称
        //开启允许跨域访问
        //response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");
        //允许跨域访问带参数
        //response.setHeader("Access-Control-Allow-Credentials", "true");


        try {
            //1.取得购物车列表
            List<Cart> cartList = this.findCartList(request,response);
            //2.向原购物车中添加商品
            cartList =  cartService.addGoodsToCartList(cartList,itemId,num);

            if(username.equals("anonymousUser")){  //未登录
                //.重新放回到Cookie
                CookieUtil.setCookie(request,response,"cartList",JSON.toJSONString(cartList),3600*24,"UTF-8");
            }else{//已登录
                cartService.saveCartListToRedis(username,cartList);
            }

            return new Result(true,"添加购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"添加购物车失败");
        }

    }


}
