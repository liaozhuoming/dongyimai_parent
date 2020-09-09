app.controller('itemPageController', function ($scope,$http) {

    //点击 +  -  对数量进行赋值
    $scope.addNum = function (num) {
        $scope.num = $scope.num + num;
        //判断购买数量不能小于1
        if ($scope.num < 1) {
            $scope.num = 1;
        }
    }

    $scope.specification = {};//初始化规格对象    {‘name’:'value'}
    //规格点击选中方法  name 规格名称  value  规格选项
    $scope.selectSpecification = function (name, value) {
        $scope.specification[name] = value;
        searchSku();
    }
    //判断规格是否被选中
    $scope.isSelect = function (name, value) {
        if ($scope.specification[name] == value) {
            return true;
        } else {
            return false;
        }
    }

    //加载SKU信息
    $scope.loadSku = function () {
        $scope.sku = skuList[0]; //默认取出SKU集合中的第一个对象
        $scope.specification = JSON.parse(JSON.stringify($scope.sku.spec));   //深克隆

    }

    // 匹配集合对象是否相同
    matchObject = function (map1, map2) {
        for (var k in map1) {
            if (map1[k] != map2[k]) {
                return false;
            }
        }

        for (var k in map2) {
            if (map2[k] != map1[k]) {
                return false;
            }
        }
        return true;
    }
    //刷新SKU数据
    searchSku = function (){
        for(var i=0;i<skuList.length;i++){
            //匹配规格，如果相同，则对页面的SKU对象进行赋值
            if(matchObject(skuList[i].spec,$scope.specification)){
                $scope.sku = skuList[i];
                return;
            }
        }
        //如果没有匹配数据，则赋值默认值
        $scope.sku = {'id':0,'title':'--------'};
    }

    //添加购物车
    $scope.addToCart = function (){
        //alert("skuId:----"+$scope.sku.id);
        $http.get('http://localhost:9107/cart/addGoodsToCartList.do?itemId='+$scope.sku.id+'&num='+$scope.num,{'withCredentials':true}).success(
            function(response){
            if(response.success){
                location.href='http://localhost:9107/cart.html';
            }else{
                alert(response.message);
            }
        });
    }


})