package com.offcn.sellergoods.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.offcn.entity.PageResult;
import com.offcn.entity.Result;
import com.offcn.group.Goods;
//import com.offcn.page.service.ItemPageService;
import com.offcn.pojo.TbGoods;
import com.offcn.pojo.TbItem;
import com.offcn.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.List;

//import com.offcn.search.service.ItemSearchService;

/**
 * controller
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Reference
    private GoodsService goodsService;

    //@Reference
    //private ItemSearchService itemSearchService;

    //@Reference(timeout = 30000)
    //private ItemPageService itemPageService;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Destination queueSolrDestination;

    @Autowired
    private Destination queueSolrDelDestination;

    @Autowired
    private Destination topicPageDestination;

    @Autowired
    private Destination topicPageDelDestination;

    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findAll")
    public List<TbGoods> findAll() {
        return goodsService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return goodsService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param goods
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody Goods goods) {
        try {
            goodsService.add(goods);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param goods
     * @return
     */
    @RequestMapping("/update")
    public Result update(@RequestBody Goods goods) {
        try {
            goodsService.update(goods);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public Goods findOne(Long id) {
        return goodsService.findOne(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @RequestMapping("/delete")
    public Result delete(final Long[] ids) {
        try {
            goodsService.delete(ids);
            //执行删除solr中的商品
            // itemSearchService.deleteByGoodsIds(Arrays.asList(ids));
            //通过消息队列通知搜索服务完成删除商品
            jmsTemplate.send(queueSolrDelDestination, new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return session.createObjectMessage(ids);
                }
            });

            //通过消息队列通知页面服务完成删除商品详情页
            jmsTemplate.send(topicPageDelDestination, new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return session.createObjectMessage(ids);
                }
            });



            return new Result(true, "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param brand
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbGoods goods, int page, int rows) {
        return goodsService.findPage(goods, page, rows);
    }

    /**
     * 商品审核
     *
     * @param ids
     * @param status
     * @return
     */
    @RequestMapping("/updateStatus")
    public Result updateStatus(Long[] ids, String status) {
        try {
            goodsService.updateStatus(ids, status);

            if (status.equals("1")) {   //判断审核操作为审核通过
                //1.查询出审核通过的增量商品
                List<TbItem> itemList = goodsService.findItemListByGoodsIdsAndStatus(ids, status);
                if (!CollectionUtils.isEmpty(itemList)) {
                    //2.将增量商品集合同步到solr中
                    //itemSearchService.importItem(itemList);

                    //通过消息队列发送已审核通过的SKU信息
                    final String itemListStr = JSON.toJSONString(itemList);

                    jmsTemplate.send(queueSolrDestination, new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            return session.createTextMessage(itemListStr);
                        }
                    });
                } else {
                    System.out.println("没有数据");
                }

                //生成商品详情页 ,通过消息队列完成通知
                for (final Long goodsId : ids) {
                    //itemPageService.genItemHtml(goodsId);
                    jmsTemplate.send(topicPageDestination, new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            return session.createTextMessage(goodsId+"");
                        }
                    });

                }

            }


            return new Result(true, "审核成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "审核失败");
        }
    }


   /* @RequestMapping("/genHtml")
    public String genHtml(Long goodsId) {
        boolean flag = itemPageService.genItemHtml(goodsId);
        return "create:" + flag;
    }
*/
}
