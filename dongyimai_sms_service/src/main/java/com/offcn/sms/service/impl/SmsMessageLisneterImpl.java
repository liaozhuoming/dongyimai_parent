package com.offcn.sms.service.impl;

import com.offcn.utils.SmsUtil;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * @Auther: lhq
 * @Date: 2020/8/28 10:37
 * @Description:
 */
@Component
public class SmsMessageLisneterImpl implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;

    public void onMessage(Message message) {
        if(message instanceof MapMessage){
            MapMessage mapMessage = (MapMessage)message;
            try {
                String mobile = mapMessage.getString("mobile");
                String param = mapMessage.getString("param");
               HttpResponse httpResponse =  smsUtil.sendSms(mobile,param);
                System.out.println("发送短信成功："+httpResponse.getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
