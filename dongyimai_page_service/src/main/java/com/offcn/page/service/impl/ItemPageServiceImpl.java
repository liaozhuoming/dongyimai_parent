package com.offcn.page.service.impl;

import com.offcn.mapper.TbGoodsDescMapper;
import com.offcn.mapper.TbGoodsMapper;
import com.offcn.mapper.TbItemCatMapper;
import com.offcn.mapper.TbItemMapper;
import com.offcn.page.service.ItemPageService;
import com.offcn.pojo.TbGoods;
import com.offcn.pojo.TbGoodsDesc;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbItemExample;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: lhq
 * @Date: 2020/8/26 11:15
 * @Description:
 */
@Service
public class ItemPageServiceImpl implements ItemPageService {

    @Value("${pagedir}")
    private String pageDir;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private TbGoodsDescMapper goodsDescMapper;

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private TbItemCatMapper itemCatMapper;


    /**
     * 生成商品详情页
     *
     * @param goodsId
     * @return
     */
    public boolean genItemHtml(Long goodsId) {
        //1.创建模板对象
        Configuration configuration = freeMarkerConfigurer.getConfiguration();
        try {
            Template template = configuration.getTemplate("item.ftl");
            //2.根据商品ID查询SPU信息
            TbGoods tbGoods = goodsMapper.selectByPrimaryKey(goodsId);
            //3.根据商品ID查询商品扩展信息
            TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(goodsId);

            //查询分类对象名称
            String itemCat1 = itemCatMapper.selectByPrimaryKey(tbGoods.getCategory1Id()).getName();
            String itemCat2 = itemCatMapper.selectByPrimaryKey(tbGoods.getCategory2Id()).getName();
            String itemCat3 = itemCatMapper.selectByPrimaryKey(tbGoods.getCategory3Id()).getName();

            //4.根据商品ID查询SKU信息

            TbItemExample itemExample = new TbItemExample();
            TbItemExample.Criteria criteria = itemExample.createCriteria();
            //设置商品ID
            criteria.andGoodsIdEqualTo(goodsId);
            //判断是否审核通过
            criteria.andStatusEqualTo("1");   //审核通过
            //根据默认选中倒序排列
            itemExample.setOrderByClause("is_default desc");
            List<TbItem> itemList = itemMapper.selectByExample(itemExample);


            //5.构建数据对象
            Map dataModel = new HashMap();
            dataModel.put("goods", tbGoods);
            dataModel.put("goodsDesc", tbGoodsDesc);
            dataModel.put("itemcat1", itemCat1);
            dataModel.put("itemcat2", itemCat2);
            dataModel.put("itemcat3", itemCat3);
            dataModel.put("itemList", itemList);

            //6.生成HTML页面
            FileWriter out = new FileWriter(pageDir + goodsId + ".html");

            template.process(dataModel, out);

            out.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    /**
     * 批量删除商品详情页
     *
     * @param ids
     * @return
     */
    public boolean deleteItemHtml(Long[] ids) {
        //删除文件
        try {
            for (Long goodsId : ids) {
                new File(pageDir + goodsId + ".html").delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
