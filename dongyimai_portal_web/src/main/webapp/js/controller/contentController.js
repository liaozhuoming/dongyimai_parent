app.controller('contentController',function ($scope,contentService){

    $scope.contentList = [];
    $scope.findContentList = function(categoryId){
        contentService.findContentByCategoryId(categoryId).success(
            function(response){
                $scope.contentList[categoryId] = response;
        })
    }

    //关键字搜索
    $scope.search = function (){
        location.href="http://localhost:9104/search.html#?keywords="+$scope.keywords;
    }
})