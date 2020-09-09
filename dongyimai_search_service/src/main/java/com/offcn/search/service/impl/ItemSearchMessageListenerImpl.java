package com.offcn.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;


@Component
public class ItemSearchMessageListenerImpl implements MessageListener {


    @Autowired
    private ItemSearchService itemSearchService;

    public void onMessage(Message message) {
        //1.接收消息 ，做类型转换
        if(message instanceof TextMessage){
            TextMessage textMessage = (TextMessage)message;
            try {
                List<TbItem> itemList = JSON.parseArray(textMessage.getText(), TbItem.class);
               /* for(TbItem item:itemList){
                    System.out.println(item.getId()+" "+item.getTitle());
                    Map specMap= JSON.parseObject(item.getSpec());//将spec字段中的json字符串转换为map
                    item.setSpecMap(specMap);//给带注解的字段赋值
                }*/
                //2.调用搜索服务，进行同步solr处理
                itemSearchService.importItem(itemList);
                System.out.println("导入Solr成功");
            } catch (JMSException e) {
                e.printStackTrace();
            }


        }

    }
}
