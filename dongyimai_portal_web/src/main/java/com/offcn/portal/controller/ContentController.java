package com.offcn.portal.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.offcn.content.service.ContentService;
import com.offcn.pojo.TbContent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Auther: lhq
 * @Date: 2020/8/20 11:02
 * @Description:
 */
@RestController
@RequestMapping("/content")
public class ContentController {

    @Reference
    private ContentService contentService;

    @RequestMapping("/findContentByCategoryId")
    public List<TbContent> findContentByCategoryId(Long categoryId){
        return contentService.findContentByCategoryId(categoryId);
    }

}
