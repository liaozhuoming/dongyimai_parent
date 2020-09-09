package com.offcn.pay.service;

import java.util.Map;

/**
 * @Auther: lhq
 * @Date: 2020/9/2 14:29
 * @Description: 支付宝支付接口
 */
public interface AliPayService {

    /**
     * 预下单，生成二维码连接
     * @param out_trade_no   订单编号
     * @param total_fee     应付金额  （单位：分）
     * @return
     */
    public Map createNative(String out_trade_no, String total_fee);

    /**
     * 查询支付状态
     * @param out_trade_no   订单编号
     * @return
     */
    public Map queryPayStatus(String out_trade_no);
}
