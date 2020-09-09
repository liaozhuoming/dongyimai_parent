package com.offcn.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.group.Cart;
import com.offcn.mapper.TbOrderItemMapper;
import com.offcn.mapper.TbOrderMapper;
import com.offcn.mapper.TbPayLogMapper;
import com.offcn.order.service.OrderService;
import com.offcn.pojo.TbOrder;
import com.offcn.pojo.TbOrderExample;
import com.offcn.pojo.TbOrderExample.Criteria;
import com.offcn.pojo.TbOrderItem;
import com.offcn.pojo.TbPayLog;
import com.offcn.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private TbOrderItemMapper orderItemMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbOrder order) {
        //先通过当前登录用户在缓存中取得购物车列表
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());
        if (!CollectionUtils.isEmpty(cartList)) {
            List orderList = new ArrayList();   //定义订单编号的数组
            double total_money = 0;           //支付日志总金额


            for (Cart cart : cartList) {
                TbOrder tbOrder = new TbOrder();
                tbOrder.setOrderId(idWorker.nextId());                            //订单主键
                System.out.println("订单编号：" + tbOrder.getOrderId());
                tbOrder.setPaymentType(order.getPaymentType());                //支付方式  1.线上支付 2.货到付款
                tbOrder.setStatus("1");                                            //状态：1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭,7、待评价
                tbOrder.setCreateTime(new Date());                                //创建时间
                tbOrder.setUpdateTime(new Date());                                //更新时间
                tbOrder.setUserId(order.getUserId());                            //用户
                tbOrder.setReceiver(order.getReceiver());                        //收货人
                tbOrder.setReceiverAreaName(order.getReceiverAreaName());        //收货地址
                tbOrder.setReceiverMobile(order.getReceiverMobile());            //收货电话
                tbOrder.setSourceType(order.getSourceType());                    //订单来源：1:app端，2：pc端，3：M端，4：微信端，5：手机qq端
                tbOrder.setSellerId(cart.getSellerId());                        //商家ID
                //定义实付金额
                double money = 0;
                //遍历购物车的订单详情
                for (TbOrderItem orderItem : cart.getOrderItemList()) {
                    orderItem.setId(idWorker.nextId());                    //订单详情ID
                    orderItem.setOrderId(tbOrder.getOrderId());            //订单ID
                    orderItem.setSellerId(cart.getSellerId());            //商家ID
                    money += orderItem.getTotalFee().doubleValue();        //累加实付金额
                    //2.保存订单详情
                    orderItemMapper.insert(orderItem);
                }
                tbOrder.setPayment(new BigDecimal(money));                        //实付金额

                //1.保存订单
                orderMapper.insert(tbOrder);


                orderList.add(tbOrder.getOrderId());
                money += tbOrder.getPayment().doubleValue();
            }
            //3.清空缓存中的购物车列表
            redisTemplate.boundHashOps("cartList").delete(order.getUserId());

            if (order.getPaymentType().equals("1")) {//线上支付
                TbPayLog tbPayLog = new TbPayLog();
                tbPayLog.setOutTradeNo(idWorker.nextId() + "");
                tbPayLog.setCreateTime(new Date());
                //将金额转换  元转分
                BigDecimal total_fee_big = new BigDecimal(total_money);
                BigDecimal cs = new BigDecimal(100L);
                BigDecimal total_money_big = total_fee_big.multiply(cs);
                tbPayLog.setTotalFee(total_money_big.toBigInteger().longValue());    //支付日志总金额
                tbPayLog.setPayType("1");       //线上支付
                tbPayLog.setTradeState("0");        //未支付
                tbPayLog.setUserId(order.getUserId());              //支付用户
                String ids = orderList.toString().replace("[", "").replace("]", "").replace(" ", "");
                tbPayLog.setOrderList(ids);             //订单编号的集合     111,2222,3333
                //将支付日志保存到数据库中
                payLogMapper.insert(tbPayLog);
                //将支付日志存储到缓存中
                redisTemplate.boundHashOps("payLog").put(order.getUserId(), tbPayLog);


            }
        }


    }


    /**
     * 修改
     */
    @Override
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder findOne(Long orderId) {
        return orderMapper.selectByPrimaryKey(orderId);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] orderIds) {
        for (Long orderId : orderIds) {
            orderMapper.deleteByPrimaryKey(orderId);
        }
    }


    @Override
    public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbOrderExample example = new TbOrderExample();
        Criteria criteria = example.createCriteria();

        if (order != null) {
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
            }
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andPostFeeLike("%" + order.getPostFee() + "%");
            }
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andStatusLike("%" + order.getStatus() + "%");
            }
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andShippingNameLike("%" + order.getShippingName() + "%");
            }
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
            }
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + order.getUserId() + "%");
            }
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
            }
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
            }
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
            }
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
            }
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
            }
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
            }
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + order.getReceiver() + "%");
            }
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
            }
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
            }
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + order.getSellerId() + "%");
            }
        }

        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 通过用户ID在缓存中读取支付日志
     *
     * @param userId
     * @return
     */
    public TbPayLog findPayLogFromRedis(String userId) {
        return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
    }

    /**
     * 修改订单支付状态
     *
     * @param out_trade_no 订单编号
     * @param trade_no     支付宝的交易流水号
     */
    public void updateOrderStatus(String out_trade_no, String trade_no) {
        //1.修改支付日志的状态
        TbPayLog tbPayLog = payLogMapper.selectByPrimaryKey(out_trade_no);
        tbPayLog.setTradeState("1");                //已支付
        tbPayLog.setPayTime(new Date());            //支付时间
        tbPayLog.setTransactionId(trade_no);        //支付宝交易流水号
        payLogMapper.updateByPrimaryKey(tbPayLog);

        //2.修改关联订单的状态    111,2222,3333
        String ids = tbPayLog.getOrderList();
        String[] orderIds = ids.split(",");
        for (String orderId : orderIds) {
            TbOrder order = orderMapper.selectByPrimaryKey(Long.parseLong(orderId));
            if (order != null) {
                order.setStatus("2");           //已支付
                orderMapper.updateByPrimaryKey(order);
            }
        }
        //3.清除缓存中的支付日志
        redisTemplate.boundHashOps("payLog").delete(tbPayLog.getUserId());
    }

}
