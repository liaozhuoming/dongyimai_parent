package com.offcn.search.service.impl;

import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.Arrays;


@Component
public class ItemSearchDeleteMessageListenerImpl implements MessageListener {

    @Autowired
    private ItemSearchService itemSearchService;

    public void onMessage(Message message) {
        //1.接收消息进行类型转换
        if(message instanceof ObjectMessage){
        ObjectMessage objectMessage = (ObjectMessage)message;
            try {
                Long[] ids = (Long[]) objectMessage.getObject();
                //2.调用搜索服务，完成删除
                itemSearchService.deleteByGoodsIds(Arrays.asList(ids));
                System.out.println("消息队列接收到删除消息，完成删除操作");
            } catch (JMSException e) {
                e.printStackTrace();
            }

            }
    }
}
