package com.offcn.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.group.Goods;
import com.offcn.mapper.*;
import com.offcn.pojo.*;
import com.offcn.pojo.TbGoodsExample.Criteria;
import com.offcn.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
@Transactional //注解式事务管理
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private TbGoodsDescMapper goodsDescMapper;

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private TbItemCatMapper itemCatMapper;

    @Autowired
    private TbBrandMapper brandMapper;

    @Autowired
    private TbSellerMapper sellerMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbGoods> findAll() {
        return goodsMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(Goods goods) {
        //1.设置商品的审核状态
        goods.getGoods().setAuditStatus("0");   //未审核
        //2.保存商品表SPU
        goodsMapper.insert(goods.getGoods());
        //3.设置商品ID到 商品扩展
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());

        //int i = 1/0;

        //4.保存商品扩展表
        goodsDescMapper.insert(goods.getGoodsDesc());
        this.saveItemList(goods);

    }

    //设置SKU属性
    private void setItemValue(Goods goods, TbItem item) {
        item.setCategoryid(goods.getGoods().getCategory3Id());   //分类ID  3级分类
        item.setCreateTime(new Date());                            //创建时间
        item.setUpdateTime(new Date());                            //更新时间
        item.setGoodsId(goods.getGoods().getId());                //商品ID
        item.setSellerId(goods.getGoods().getSellerId());        //商家ID
        //根据分类ID查询分类
        TbItemCat tbItemCat = itemCatMapper.selectByPrimaryKey(item.getCategoryid());
        item.setCategory(tbItemCat.getName());                    //分类名称
        //根据品牌ID查询品牌对象
        TbBrand tbBrand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(tbBrand.getName());                        //品牌名称

        //根据商家ID查询商家
        TbSeller tbSeller = sellerMapper.selectByPrimaryKey(item.getSellerId());
        item.setSeller(tbSeller.getNickName());                    //商家名称 店铺名称

        List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
        if (!CollectionUtils.isEmpty(imageList)) {
            item.setImage((String) imageList.get(0).get("url"));//图片路径
        }
    }

    //保存SKU信息
    private void saveItemList(Goods goods) {
        //判断规格是否启用
        if ("1".equals(goods.getGoods().getIsEnableSpec())) {
            //5.保存商品详细信息SKU
            for (TbItem item : goods.getItemList()) {
                //拼接SKU的名称  规格选项+title
                String title = goods.getGoods().getGoodsName();
                Map<String, String> spec = JSON.parseObject(item.getSpec(), Map.class);
                for (String key : spec.keySet()) {
                    title += " " + spec.get(key);
                }
                item.setTitle(title);                                       //SKU名称
                this.setItemValue(goods, item);
                itemMapper.insert(item);
            }
        } else {
            TbItem item = new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());   //商品名称
            item.setPrice(goods.getGoods().getPrice());         //商品价格
            item.setStatus("1");
            item.setIsDefault("1");
            item.setNum(9999);
            item.setSpec("{}");             //设置一个空规格对象
            this.setItemValue(goods, item);
            //执行保存SKU信息
            itemMapper.insert(item);
        }
    }


    /**
     * 修改
     */
    @Override
    public void update(Goods goods) {
        //1.重置审核状态 未审核  修改商品SPU的信息
        goods.getGoods().setAuditStatus("0");
        goodsMapper.updateByPrimaryKey(goods.getGoods());
        //2.修改商品扩展信息
        goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
        //3.根据商品ID删除SKU信息
        TbItemExample tbItemExample = new TbItemExample();
        TbItemExample.Criteria criteria = tbItemExample.createCriteria();
        criteria.andGoodsIdEqualTo(goods.getGoods().getId());
        itemMapper.deleteByExample(tbItemExample);
        //4.重新添加SKU信息
        this.saveItemList(goods);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public Goods findOne(Long id) {
        //1.根据ID查询商品SPU的信息
        TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);

        //2.根据ID查询商品扩展信息
        TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);

        //3.查询SKU信息
        TbItemExample tbItemExample = new TbItemExample();
        TbItemExample.Criteria criteria = tbItemExample.createCriteria();
        //设置查询条件为商品ID
        criteria.andGoodsIdEqualTo(id);
        List<TbItem> itemList = itemMapper.selectByExample(tbItemExample);


        //4.设置复合实体
        Goods goods = new Goods();
        goods.setGoods(tbGoods);
        goods.setGoodsDesc(tbGoodsDesc);
        goods.setItemList(itemList);

        return goods;
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            //goodsMapper.deleteByPrimaryKey(id);    物理删除
            //逻辑删除
            TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
            //修改删除状态
            tbGoods.setIsDelete("1");
            //执行修改
            goodsMapper.updateByPrimaryKey(tbGoods);

        }
        //查询出已经审核通过的将要删除的SKU列表
        List<TbItem> itemList = this.findItemListByGoodsIdsAndStatus(ids, "1");
        for (TbItem item : itemList) {
            item.setStatus("0");   //重置SKU的状态为 禁用
            itemMapper.updateByPrimaryKey(item);
        }
    }


    @Override
    public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbGoodsExample example = new TbGoodsExample();
        Criteria criteria = example.createCriteria();

        if (goods != null) {
            if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
                //criteria.andSellerIdLike("%" + goods.getSellerId() + "%");
                criteria.andSellerIdEqualTo(goods.getSellerId());
            }
            if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
                criteria.andGoodsNameLike("%" + goods.getGoodsName() + "%");
            }
            if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
                criteria.andAuditStatusLike("%" + goods.getAuditStatus() + "%");
            }
            if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
                criteria.andIsMarketableLike("%" + goods.getIsMarketable() + "%");
            }
            if (goods.getCaption() != null && goods.getCaption().length() > 0) {
                criteria.andCaptionLike("%" + goods.getCaption() + "%");
            }
            if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
                criteria.andSmallPicLike("%" + goods.getSmallPic() + "%");
            }
            if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
                criteria.andIsEnableSpecLike("%" + goods.getIsEnableSpec() + "%");
            }
            // if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
            // criteria.andIsDeleteLike("%" + goods.getIsDelete() + "%");
            //}
            //查询未删除的商品
            criteria.andIsDeleteIsNull();
        }

        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量审核商品
     *
     * @param ids
     * @param status
     */
    public void updateStatus(Long[] ids, String status) {
        //1.遍历ID数组
        for (Long id : ids) {
            //2.通过ID先把商品对象查询出来
            TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
            //3.设置状态
            tbGoods.setAuditStatus(status);
            //4.执行修改商品操作
            goodsMapper.updateByPrimaryKey(tbGoods);
            //5.修改SKU的状态
            TbItemExample itemExample = new TbItemExample();
            TbItemExample.Criteria criteria = itemExample.createCriteria();
            criteria.andGoodsIdEqualTo(id);
            List<TbItem> itemList = itemMapper.selectByExample(itemExample);
            if (!CollectionUtils.isEmpty(itemList)) {
                for (TbItem item : itemList) {
                    //设置SKU的状态
                    item.setStatus(status);
                    //执行修改
                    itemMapper.updateByPrimaryKey(item);
                }
            }
        }
    }

    /**
     * 通过商品ID和状态查询SKU集合
     *
     * @param ids
     * @param status
     * @return
     */
    public List<TbItem> findItemListByGoodsIdsAndStatus(Long[] ids, String status) {

        TbItemExample itemExample = new TbItemExample();
        TbItemExample.Criteria criteria = itemExample.createCriteria();
        criteria.andStatusEqualTo(status);
        criteria.andGoodsIdIn(Arrays.asList(ids));

        List<TbItem> itemList = itemMapper.selectByExample(itemExample);

        return itemList;
    }

}
