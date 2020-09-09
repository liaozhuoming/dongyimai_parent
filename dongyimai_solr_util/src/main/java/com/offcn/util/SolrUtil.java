package com.offcn.util;

import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: lhq
 * @Date: 2020/8/21 14:20
 * @Description:
 */
@Component
public class SolrUtil {

    @Autowired
    private TbItemMapper itemMapper;


    @Autowired
    private SolrTemplate solrTemplate;

    public void importItemData(){
        TbItemExample tbItemExample = new TbItemExample();
        TbItemExample.Criteria criteria = tbItemExample.createCriteria();
        //查询 审核通过 状态
        criteria.andStatusEqualTo("1");
        List<TbItem> itemList = itemMapper.selectByExample(tbItemExample);

        for(TbItem item:itemList){
            System.out.println(item.getTitle()+"-----"+item.getPrice());
            //从数据库中查询spec数据，并进行数据类型转换
            Map<String,String> specMap = JSON.parseObject(item.getSpec(), Map.class);

            Map<String,String> pinyinMap = new HashMap<String, String>();
            //遍历KEY值
            for(String key:specMap.keySet()){
                //将KEY值进行拼音转换，并重新将数据保存的到新的集合
                pinyinMap.put(Pinyin.toPinyin(key,"").toLowerCase(),specMap.get(key));
            }
            //重新设置到动态域
            item.setSpecMap(pinyinMap);

        }

        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
        System.out.println("导入成功");



    }

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext-*.xml");

        SolrUtil solrUtil =(SolrUtil) context.getBean("solrUtil");

        solrUtil.importItemData();

    }





}
