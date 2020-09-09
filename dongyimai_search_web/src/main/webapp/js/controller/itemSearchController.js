app.controller('itemSearchController', function ($scope, $location,itemSearchService) {

    $scope.search = function () {

        //针对文本框的页码需要进行数据格式转换
        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);

        itemSearchService.search($scope.searchMap).success(
            function (response) {
                // $scope.itemList = response.rows;
                $scope.resultMap = response;
                buildPageLabel();
            })
    }

    //初始化查询条件对象的数据结构
    $scope.searchMap = {
        'keywords': '',
        'category': '',
        'brand': '',
        'spec': {},
        'price': '',
        'pageNo': 1,
        'pageSize': 20,
        'sort':'',
        'sortFiled':''
    };

    //添加查询条件
    $scope.addSearchItem = function (key, value) {
        //判断key值是分类、品牌还是规格
        if (key == 'category' || key == 'brand' || key == 'price') {
            $scope.searchMap[key] = value;
        } else {
            $scope.searchMap.spec[key] = value;
        }
        //重置当前页码为首页
        $scope.searchMap.pageNo=1;
        //执行查询
        $scope.search();
    }

    //撤销查询条件
    $scope.removeSearchItem = function (key) {
        if (key == 'category' || key == 'brand' || key == 'price') {
            //将字段重置为空字符串
            $scope.searchMap[key] = '';
        } else {
            //移除属性
            delete $scope.searchMap.spec[key];
        }
        //重置当前页码为首页
        $scope.searchMap.pageNo=1;
        //执行查询
        $scope.search();
    }

    buildPageLabel = function () {
        //初始化页码数组
        $scope.pageLabel = [];
        var firstPage = 1;   //页码数组的起始位置
        var lastPage = $scope.resultMap.totalPages;   //页码数组的结束位置   默认总页数
        var maxPage = $scope.resultMap.totalPages;  //最大页数

        $scope.firstDot = true;
        $scope.endDot = true;

        if (maxPage > 5) {
            if ($scope.searchMap.pageNo <= 3) {     //当前页小于3  则将结束位置固定到第5页
                lastPage = 5;
                $scope.firstDot = false     //右侧的省略号显示
            } else if ($scope.searchMap.pageNo >= lastPage - 2) {   //当前页大于等于总页数-2时，则将起始位置固定到  倒数第5页
                firstPage = lastPage - 4;
                $scope.endDot = false ;  //左侧的显示
            } else {
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        }else{
            $scope.firstDot = false;
            $scope.endDot = false;
        }


        for (var i = firstPage; i <= lastPage; i++) {
            $scope.pageLabel.push(i);
        }

    }


    //提交页码查询
    $scope.queryByPage = function (pageNo) {
        //pageNo做格式验证
        if (pageNo < 1 || pageNo > $scope.resultMap.totalPages) {
            return;
        }

        $scope.searchMap.pageNo = pageNo;
        //执行查询
        $scope.search();
    }


    //判断是否是第一页
    $scope.isTopPage = function (){
        if($scope.searchMap.pageNo==1){
            return true;
        }else{
            return false;
        }
    }


    //判断是否是最后一页
    $scope.resultMap = {'totalPages':1};
    $scope.isLastPage = function (){
        if($scope.searchMap.pageNo==$scope.resultMap.totalPages){
            return true;
        }else{
            return false;
        }
    }

    //判断是否是当前页
    $scope.isPage = function (pageNo){
        if(parseInt(pageNo) == parseInt($scope.searchMap.pageNo)){
            return true;
        }else{
            return false;
        }
    }
    //设置排序规则
    $scope.sortSearch = function (sortFiled,sort){
        $scope.searchMap.sortFiled = sortFiled;
        $scope.searchMap.sort = sort;
        //执行查询
        $scope.search();
    }


    //判断关键字是否是品牌内容
    $scope.keywordsIsBrand = function (){
        //遍历品牌列表
        for(var i=0;i<$scope.resultMap.brandList.length;i++){
            //比较关键字是否在列表中存在
            if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>-1){
                return true;
            }
        }
        return false;
    }


    //加载首页关键字查询
    $scope.loadKeywords = function (){
        $scope.searchMap.keywords = $location.search()['keywords'];
        //执行查询
        $scope.search();
    }

})