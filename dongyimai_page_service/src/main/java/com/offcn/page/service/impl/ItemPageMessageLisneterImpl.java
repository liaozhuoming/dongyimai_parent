package com.offcn.page.service.impl;

import com.offcn.page.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @Auther: lhq
 * @Date: 2020/8/27 14:43
 * @Description:
 */
@Component
public class ItemPageMessageLisneterImpl implements MessageListener {

    @Autowired
    private ItemPageService itemPageService;

    public void onMessage(Message message) {
        if(message instanceof TextMessage){
            TextMessage textMessage = (TextMessage)message;
            try {
                Long goodsId = Long.parseLong(textMessage.getText());
                itemPageService.genItemHtml(goodsId);
                System.out.println("从消息队列中得到消息："+goodsId+"，生成页面成功");


            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
