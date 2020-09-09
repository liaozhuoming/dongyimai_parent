package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 搜索商品信息
     *
     * @param searchMap
     * @return
     */
    public Map search(Map searchMap) {
        Map resultMap = new HashMap();
        /*//1.创建查询对象
        Query query = new SimpleQuery();
        //2.创建查询条件选择器，并设置查询条件
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        //3.将查询条件选择器，设置回查询对象
        query.addCriteria(criteria);
        //4.执行查询
        ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);

        //返回查询结果集合
        resultMap.put("rows", page.getContent());*/
        //多关键字处理 判断是否含有空格
        if (!StringUtils.isEmpty(searchMap.get("keywords")) && ((String) (searchMap.get("keywords"))).indexOf(" ") > -1) {
            String keywords = ((String) (searchMap.get("keywords"))).replace(" ", "");
            searchMap.put("keywords", keywords);
        }


        //1.查询高亮显示集合
        resultMap.putAll(this.searchList(searchMap));
        //2.查询分类集合
        List categoryList = this.findCategoryList(searchMap);
        resultMap.put("categoryList", categoryList);
        //3.查询品牌和规格集合
        String category = (String) searchMap.get("category");
        //如果点击分类查询条件，则根据分类的内容查询
        if (!StringUtils.isEmpty(category)) {
            resultMap.putAll(this.searchBrandAndSpecList(category));
        } else {
            //如果分类有多个，则默认根据第一个分类进行查询
            if (categoryList.size() > 0) {
                resultMap.putAll(this.searchBrandAndSpecList((String) categoryList.get(0)));
            }
        }

        return resultMap;
    }

    //导入SKU
    public void importItem(List<TbItem> itemList) {

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
        System.out.println("新增商品导入成功");


    }

    public void deleteByGoodsIds(List ids) {
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(ids);
        query.addCriteria(criteria);
        //执行删除
        solrTemplate.delete(query);
        solrTemplate.commit();
        System.out.println("从搜索引擎中删除商品成功"+ids);
    }

    private Map searchList(Map searchMap) {
        Map resultMap = new HashMap();
        //1.创建高亮查询对象
        HighlightQuery query = new SimpleHighlightQuery();
        //2.设置高亮查询属性
        HighlightOptions options = new HighlightOptions();
        options.addField("item_title");
        options.setSimplePrefix("<em style='color:red'>");   //前缀
        options.setSimplePostfix("</em>");  //后缀
        query.setHighlightOptions(options);
        //3.将查询条件及高亮查询属性设置回查询对象
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //根据分类筛选结果
        if (!"".equals(searchMap.get("category"))) {
            //创建查询条件选择器，并设置查询条件
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            //创建过滤查询对象,并设置查询条件选择器
            FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            //将过滤查询对象设置回高亮查询对象
            query.addFilterQuery(filterQuery);
        }
        //根据品牌筛选结果
        if (!"".equals(searchMap.get("brand"))) {
            //创建查询条件选择器，并设置查询条件
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            //创建过滤查询对象,并设置查询条件选择器
            FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            //将过滤查询对象设置回高亮查询对象
            query.addFilterQuery(filterQuery);
        }
        //根据规格筛选结果
        if (searchMap.get("spec") != null) {
            //取得spec的Map的数据结构
            Map<String, Object> specMap = (Map<String, Object>) searchMap.get("spec");
            for (String key : specMap.keySet()) {
                //创建查询条件选择器，并设置查询条件
                Criteria filterCriteria = new Criteria("item_spec_" + Pinyin.toPinyin(key, "").toLowerCase()).is(specMap.get(key));
                //创建过滤查询对象,并设置查询条件选择器
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                //将过滤查询对象设置回高亮查询对象
                query.addFilterQuery(filterQuery);
            }
        }
        //根据价格筛选结果
        if (!"".equals(searchMap.get("price"))) {
            //接收价格区间，并根据 - 做字符串拆分   500-1000    price[0]=500 price[1]=1000
            String[] price = ((String) searchMap.get("price")).split("-");
            if (!price[0].equals("0")) {   //左区间不为0的时候 判断大于等于
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if (!price[1].equals("*")) {  //右区间不为*的时候  判断小于
                Criteria filterCriteria = new Criteria("item_price").lessThan(price[1]);
                FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }

        //分页查询
        Integer pageNo = (Integer) searchMap.get("pageNo");  //当前页码

        if (null == pageNo) {
            pageNo = 1;
        }

        Integer pageSize = (Integer) searchMap.get("pageSize"); //每页显示的记录数
        if (null == pageSize) {
            pageSize = 20;
        }

        query.setOffset((pageNo - 1) * pageSize);//每页起始记录数
        query.setRows(pageSize);//每页显示的条数

        //排序
       String sortValue = (String) searchMap.get("sort");   //排序规则
       String sortFiled = (String) searchMap.get("sortFiled");  //排序字段
        if(!StringUtils.isEmpty(sortValue)){
            //升序
            if("ASC".equals(sortValue)) {
                Sort sort = new Sort(Sort.Direction.ASC,"item_"+sortFiled);
                query.addSort(sort);
            }
            //降序
            if("DESC".equals(sortValue)){
                Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortFiled);
                query.addSort(sort);
            }
        }




        //4.执行高亮查询
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //5.获得高亮查询结果集入口
        List<HighlightEntry<TbItem>> highlighted = page.getHighlighted();
        //6.处理高亮结果
        //遍历结果集
        for (HighlightEntry<TbItem> highlightEntry : highlighted) {
            //获取基本数据对象
            TbItem item = highlightEntry.getEntity();
            //注意：对高亮结果要做判空处理
            if (highlightEntry.getHighlights().size() > 0 && highlightEntry.getHighlights().get(0).getSnipplets().size() > 0) {
                List<HighlightEntry.Highlight> highlightList = highlightEntry.getHighlights();
                List<String> snippletsList = highlightList.get(0).getSnipplets();
                //从高亮集合中取得第一个高亮片段，设置回结果对象
                item.setTitle(snippletsList.get(0));
            }
        }

        //返回处理后的高亮结果集合
        resultMap.put("rows", page.getContent());
        resultMap.put("total", page.getTotalElements());  //总记录数
        resultMap.put("totalPages", page.getTotalPages());   //总页数


        return resultMap;

    }

    /**
     * 分组查询分类列表
     *
     * @param searchMap
     * @return
     */
    private List findCategoryList(Map searchMap) {
        List list = new ArrayList();
        //1.初始化查询对象
        Query query = new SimpleQuery();
        //2.设置分组字段
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        //3.将分组设置回查询对象
        query.setGroupOptions(groupOptions);

        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //4.执行分组查询
        GroupPage<TbItem> groupPage = solrTemplate.queryForGroupPage(query, TbItem.class);
        //5.获得分组查询结果集入口
        GroupResult<TbItem> groupResult = groupPage.getGroupResult("item_category");
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //6.处理分组结果集
        //得到分组的入口集合
        List<GroupEntry<TbItem>> groupEntryList = groupEntries.getContent();
        for (GroupEntry groupEntry : groupEntryList) {
            list.add(groupEntry.getGroupValue());
        }
        return list;
    }


    //在缓存中查询品牌和规格集合
    private Map<String, Object> searchBrandAndSpecList(String category) {
        Map<String, Object> resultMap = new HashMap();
        //1.根据分类名称在缓存中查询模板ID
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (null != typeId) {
            //2.根据模板ID查询品牌列表对象
            List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
            resultMap.put("brandList", brandList);
            //3.根据模板ID查询规格列表对象
            List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(typeId);
            resultMap.put("specList", specList);
        }

        return resultMap;
    }


}
