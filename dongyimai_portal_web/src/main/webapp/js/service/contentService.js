app.service('contentService',function($http){
    this.findContentByCategoryId = function(categoryId){
        return $http.get('../content/findContentByCategoryId.do?categoryId='+categoryId);
    }

})