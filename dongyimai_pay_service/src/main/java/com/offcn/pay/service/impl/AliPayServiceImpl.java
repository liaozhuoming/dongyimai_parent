package com.offcn.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.offcn.pay.service.AliPayService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: lhq
 * @Date: 2020/9/2 14:32
 * @Description:
 */
@Service
public class AliPayServiceImpl implements AliPayService {

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 预下单，生成二维码连接
     *
     * @param out_trade_no 订单编号
     * @param total_fee    应付金额  （单位：分）
     * @return
     */
    public Map createNative(String out_trade_no, String total_fee) {
        Map resultMap = new HashMap();
        //金额转换 分转元
        long total_fee_long = Long.parseLong(total_fee);
        BigDecimal total_fee_big = new BigDecimal(total_fee_long);
        BigDecimal cs = new BigDecimal(100L);
        BigDecimal money = total_fee_big.divide(cs);

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest(); //创建API对应的request类
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + out_trade_no + "\"," + //商户订单号
                "    \"total_amount\":\"" + money.doubleValue() + "\"," +
                "    \"subject\":\"商品01\"," +
                "    \"store_id\":\"NJ_001\"," +
                "    \"timeout_express\":\"90m\"}"); //订单允许的最晚付款时间
        AlipayTradePrecreateResponse response = null;
        try {
            response = alipayClient.execute(request);
            String code = response.getCode();//状态码
            System.out.println("状态码：" + code);
            System.out.print(response.getBody());
            //根据response中的结果继续业务逻辑处理
            if (code != null && code.equals("10000")) {
                resultMap.put("qrCode", response.getQrCode());
                resultMap.put("outTradeNo", response.getOutTradeNo());   //订单交易编号
                resultMap.put("totalFee", total_fee);
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 查询支付状态
     *
     * @param out_trade_no 订单编号
     * @return
     */
    public Map queryPayStatus(String out_trade_no) {
        Map resultMap = new HashMap();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest(); //创建API对应的request类
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + out_trade_no + "\"," +
                "    \"trade_no\":\"\"}");  //设置业务参数
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request); //通过alipayClient调用API，获得对应的response类
            System.out.println(response.getBody());
            String code = response.getCode();
            if (code != null & code.equals("10000")) {
                //根据response中的结果继续业务逻辑处理
                resultMap.put("trade_no", response.getTradeNo());//支付宝的流水号
                resultMap.put("out_trade_no", response.getOutTradeNo());//订单编号
                resultMap.put("trade_status", response.getTradeStatus());//订单状态
            }


        } catch (AlipayApiException e) {
            e.printStackTrace();
        }


        return resultMap;
    }
}
