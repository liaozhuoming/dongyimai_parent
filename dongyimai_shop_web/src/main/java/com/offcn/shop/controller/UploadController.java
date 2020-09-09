package com.offcn.shop.controller;

import com.offcn.entity.Result;
import com.offcn.util.FastDFSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Auther: lhq
 * @Date: 2020/8/17 15:15
 * @Description: 上传文件控制器
 */
@RestController
public class UploadController {

    //读取属性文件中的信息
    @Value("${FILE_SERVER_URL}")
    private String FILE_SERVER_URL;

    @RequestMapping("/uploadFile")
    public Result uploadFile(MultipartFile file) {
        //1.获取文件的名称，并截取扩展名   xxxx.jpg
        String originalFileName = file.getOriginalFilename();
        String extName = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        //2.获取上传文件的对象
        try {
            FastDFSClient fastDFSClient = new FastDFSClient("classpath:config/fdfs_client.conf");
            //3.完成文件上传
            String path = fastDFSClient.uploadFile(file.getBytes(), extName);
            //4.拼接全路径
            String url = FILE_SERVER_URL + path;
            return new Result(true, url);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "上传失败");
        }
    }


}
