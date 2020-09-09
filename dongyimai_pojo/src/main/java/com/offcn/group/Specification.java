package com.offcn.group;

import com.offcn.pojo.TbSpecification;
import com.offcn.pojo.TbSpecificationOption;

import java.io.Serializable;
import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/8/13 11:17
 * @Description: 规格复合实体类
 */
public class Specification implements Serializable {


    private TbSpecification specification;  //规格名称
    private List<TbSpecificationOption> specificationOptionList; //规格选项集合

    public TbSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(TbSpecification specification) {
        this.specification = specification;
    }

    public List<TbSpecificationOption> getSpecificationOptionList() {
        return specificationOptionList;
    }

    public void setSpecificationOptionList(List<TbSpecificationOption> specificationOptionList) {
        this.specificationOptionList = specificationOptionList;
    }
}
