app.service('loginService',function($http){
    //获得登录人信息
    this.getLoginName = function(){
        return $http.get('../login/getLoginName.do');
    }
})