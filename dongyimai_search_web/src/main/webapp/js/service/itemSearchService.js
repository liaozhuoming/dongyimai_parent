app.service('itemSearchService',function ($http){
    this.search = function (searchEntity){

        return $http.post('../itemsearch/search.do',searchEntity);
    }
})