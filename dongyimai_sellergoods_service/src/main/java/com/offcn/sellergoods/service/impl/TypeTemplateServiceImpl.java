package com.offcn.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbSpecificationOptionMapper;
import com.offcn.mapper.TbTypeTemplateMapper;
import com.offcn.pojo.TbSpecificationOption;
import com.offcn.pojo.TbSpecificationOptionExample;
import com.offcn.pojo.TbTypeTemplate;
import com.offcn.pojo.TbTypeTemplateExample;
import com.offcn.pojo.TbTypeTemplateExample.Criteria;
import com.offcn.sellergoods.service.TypeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {

    @Autowired
    private TbTypeTemplateMapper typeTemplateMapper;

    @Autowired
    private TbSpecificationOptionMapper specificationOptionMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询全部
     */
    @Override
    public List<TbTypeTemplate> findAll() {
        return typeTemplateMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbTypeTemplate> page = (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbTypeTemplate typeTemplate) {
        typeTemplateMapper.insert(typeTemplate);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbTypeTemplate typeTemplate) {
        typeTemplateMapper.updateByPrimaryKey(typeTemplate);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbTypeTemplate findOne(Long id) {
        return typeTemplateMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            typeTemplateMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbTypeTemplateExample example = new TbTypeTemplateExample();
        Criteria criteria = example.createCriteria();

        if (typeTemplate != null) {
            if (typeTemplate.getName() != null && typeTemplate.getName().length() > 0) {
                criteria.andNameLike("%" + typeTemplate.getName() + "%");
            }
            if (typeTemplate.getSpecIds() != null && typeTemplate.getSpecIds().length() > 0) {
                criteria.andSpecIdsLike("%" + typeTemplate.getSpecIds() + "%");
            }
            if (typeTemplate.getBrandIds() != null && typeTemplate.getBrandIds().length() > 0) {
                criteria.andBrandIdsLike("%" + typeTemplate.getBrandIds() + "%");
            }
            if (typeTemplate.getCustomAttributeItems() != null && typeTemplate.getCustomAttributeItems().length() > 0) {
                criteria.andCustomAttributeItemsLike("%" + typeTemplate.getCustomAttributeItems() + "%");
            }
        }

        Page<TbTypeTemplate> page = (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(example);
        //调用放入缓存，该方法一定要放在分页查询之后执行
        this.saveToRedis();
        System.out.println("品牌和规格已经存入缓存成功");




        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据模板ID查询规格及规格选项列表
     *
     * @param typeId
     * @return
     */
    public List<Map> findSpecList(Long typeId) {
        //1.根据模板ID查询模板对象，得到规格的集合
        TbTypeTemplate tbTypeTemplate = typeTemplateMapper.selectByPrimaryKey(typeId);
        //2.将规格集合做数据类型转换   {"id":27,"text":"网络"}
        List<Map> specList = JSON.parseArray(tbTypeTemplate.getSpecIds(), Map.class);
        //3.遍历规格集合，通过规格ID，查询规格选项列表
        if (!CollectionUtils.isEmpty(specList)) {
            for (Map map : specList) {
                //Map得到的数组整型时 默认是int类型
                Long specId = new Long((Integer) map.get("id"));
                TbSpecificationOptionExample specificationOptionExample = new TbSpecificationOptionExample();
                TbSpecificationOptionExample.Criteria criteria = specificationOptionExample.createCriteria();
                //设置查询条件，规格ID
                criteria.andSpecIdEqualTo(specId);
                List<TbSpecificationOption> optionList = specificationOptionMapper.selectByExample(specificationOptionExample);
                //4.重新设置规格选项列表到规格集合的数据结构中    {'id':27,'text':'网络','options':[{},{}]}
                map.put("options", optionList);
            }
        }
        return specList;
    }
    private void saveToRedis() {
        //1.先查询模板ID
        List<TbTypeTemplate> templateList = this.findAll();
        for (TbTypeTemplate tbTypeTemplate : templateList) {
            //2.根据模板ID取得品牌信息   {"id":8,"text":"魅族"}
            List<Map> brandList = JSON.parseArray(tbTypeTemplate.getBrandIds(), Map.class);
            //存入到缓存
            redisTemplate.boundHashOps("brandList").put(tbTypeTemplate.getId(), brandList);
            //3.根据模板ID取得规格信息   {"id":8,"text":"模板名称","options":[]}
            List<Map> specList = this.findSpecList(tbTypeTemplate.getId());
            //存入到缓存
            redisTemplate.boundHashOps("specList").put(tbTypeTemplate.getId(), specList);
        }

    }

}
