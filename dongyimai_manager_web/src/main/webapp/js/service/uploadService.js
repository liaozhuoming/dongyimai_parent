app.service('uploadService',function ($http){
    this.uploadFile = function (){
        //获取表单上的上传文件控件的数据
        var formData = new FormData();
        formData.append('file',file.files[0]);
        return $http({
            'method':'POST',
            'url':'../uploadFile.do',
            'data': formData,
            'headers': {'Content-Type': undefined},   //默认使用文件流的数据类型进行请求
            'transformRequest': angular.identity  //使用angularJs框架对请求数据进行序列化
        });
    }
})