//控制层
app.controller('goodsController', function ($scope, $controller, $location, goodsService, uploadService, itemCatService, typeTemplateService) {

    $controller('baseController', {$scope: $scope});//继承

    //读取列表数据绑定到表单中  
    $scope.findAll = function () {
        goodsService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    }

    //分页
    $scope.findPage = function (page, rows) {
        goodsService.findPage(page, rows).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }

    //查询实体
    $scope.findOne = function () {
        //地址路由接收参数   goods_edit.html#?id=
        var id = $location.search()['id'];
        if (id == null) {
            return;
        }
        goodsService.findOne(id).success(
            function (response) {
                $scope.entity = response;
                //给富文本编辑器赋值
                editor.html($scope.entity.goodsDesc.introduction);
                //商品图片和扩展属性的值进行数据类型转换
                $scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
                $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);
                //规格进行数据类型转换
                $scope.entity.goodsDesc.specificationItems = JSON.parse($scope.entity.goodsDesc.specificationItems);

                //对SKU中的spec数据进行数据类型转换
                for (var i = 0; i < $scope.entity.itemList.length; i++) {
                    $scope.entity.itemList[i].spec = JSON.parse($scope.entity.itemList[i].spec);
                }
            }
        );
    }
    //规格选中
    $scope.checkAttributeValue = function (specName, optionName) {
        var items = $scope.entity.goodsDesc.specificationItems;
        var object = $scope.searchObjectByKey(items, 'attributeName', specName);
        //判断规格在集合中是否存在
        if (object == null) {
            return false;
        } else {
            //判断规格选项在规格集合中是否存在
            if (object.attributeValue.indexOf(optionName) >= 0) {
                return true;
            } else {
                return false;
            }
        }
    }


    //保存
    $scope.save = function () {
        //获得富文本编辑器的内容，并给对象属性赋值
        $scope.entity.goodsDesc.introduction = editor.html();
        var serviceObject;//服务层对象
        if ($scope.entity.goods.id != null) {//如果有ID
            serviceObject = goodsService.update($scope.entity); //修改
        } else {
            serviceObject = goodsService.add($scope.entity);//增加
        }
        serviceObject.success(
            function (response) {
                if (response.success) {
                    //重新查询
                    //$scope.reloadList();//重新加载
                    location.href = 'goods.html';
                } else {
                    alert(response.message);
                }
            }
        );
    }


    //添加商品
   /* $scope.add = function () {
        //获得富文本编辑器的内容，并给对象属性赋值
        $scope.entity.goodsDesc.introduction = editor.html();
        goodsService.add($scope.entity).success(
            function (response) {
                if (response.success) {
                    //清空表单
                    //$scope.entity = {};
                    $scope.entity = {'goodsDesc': {'itemImages': [], 'specificationItems': []}};
                    //清空富文本编辑器的内容
                    editor.html('');
                } else {
                    alert(response.message);
                }
            })
    }
*/

    //批量删除
    $scope.dele = function () {
        //获取选中的复选框
        goodsService.dele($scope.selectIds).success(
            function (response) {
                if (response.success) {
                    $scope.reloadList();//刷新列表
                    $scope.selectIds = [];
                }
            }
        );
    }

    $scope.searchEntity = {};//定义搜索对象

    //搜索
    $scope.search = function (page, rows) {
        goodsService.search(page, rows, $scope.searchEntity).success(
            function (response) {
                $scope.list = response.rows;
                $scope.paginationConf.totalItems = response.total;//更新总记录数
            }
        );
    }


    $scope.image_entity = {};  //初始化上传图片对象的数据结构
    $scope.uploadFile = function () {
        uploadService.uploadFile().success(
            function (response) {
                if (response.success) {
                    $scope.image_entity.url = response.message;  //将上传成功后的图片路径返回
                } else {
                    alert(response.message);
                }
            }).error(function () {
            alert('上传发生错误');
        })
    }


    $scope.entity = {'goods': {}, 'goodsDesc': {'itemImages': [], 'specificationItems': []}};  //初始化复合实体的数据结构
    $scope.add_image_entity = function (image_entity) {
        $scope.entity.goodsDesc.itemImages.push(image_entity);
    }
    $scope.dele_image_entity = function (index) {
        $scope.entity.goodsDesc.itemImages.splice(index, 1);
    }


    //查询一级商品分类列表
    $scope.selectItemCat1List = function () {
        itemCatService.findByParentId(0).success(
            function (response) {
                $scope.itemCat1List = response;
            })
    }

    //二级列表联动查询商品分类
    $scope.$watch('entity.goods.category1Id', function (newValue, oldValue) {
        //观察是否有新的值发生变化
        if (newValue) {
            itemCatService.findByParentId(newValue).success(
                function (response) {
                    $scope.itemCat2List = response;
                })
        }
    })
    //三级列表联动查询商品分类
    $scope.$watch('entity.goods.category2Id', function (newValue, oldValue) {
        //观察是否有新的值发生变化
        if (newValue) {
            itemCatService.findByParentId(newValue).success(
                function (response) {
                    $scope.itemCat3List = response;
                })
        }
    })

    //根据分类ID查询分类对象
    $scope.$watch('entity.goods.category3Id', function (newValue, oldValue) {
        if (newValue) {
            itemCatService.findOne(newValue).success(
                function (response) {
                    $scope.entity.goods.typeTemplateId = response.typeId;
                })
        }
    })

    //根据模板ID查询模板对象，并展示品牌信息
    $scope.$watch('entity.goods.typeTemplateId', function (newValue, oldValue) {
        if (newValue) {
            typeTemplateService.findOne(newValue).success(
                function (response) {
                    $scope.typeTemplate = response;
                    //查询品牌信息
                    $scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);    //需要数据类型转换 JSON
                    //查询扩展属性信息
                    if ($location.search()['id'] == null) {
                        $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);
                    }
                })
            //查询规格及规格选项列表
            typeTemplateService.findSpecList(newValue).success(
                function (response) {
                    $scope.specList = response;
                })
        }
    })


    //name  规格名称  value 规格选项
    $scope.updateSpecAttribute = function ($event, name, value) {
        //1.判断选中的规格是否在specificationItems集合中存在
        var object = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems, 'attributeName', name);
        // 2.如果不存在则创建  specificationItems.push
        if (object == null) {
            $scope.entity.goodsDesc.specificationItems.push({"attributeValue": [value], "attributeName": name});
        } else {
            // 3.如果存在	判断是否选中/反选
            if ($event.target.checked) {
                //如果是选中  则  在规格选项的数组中attributeValue.push
                object.attributeValue.push(value);
            } else {
                //		如果是反选  则  在规格选项的数组中attributeValue.splice()
                object.attributeValue.splice(object.attributeValue.indexOf(value), 1);
                //判断如果attributeValue的元素为0的情况，则需要specificationItems.splice
                if (object.attributeValue.length == 0) {
                    $scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(object), 1);
                }
            }
        }

    }

    $scope.createItem = function () {
        //1.初始化Item对象的数据结构
        $scope.entity.itemList = [{'price': 0, 'num': 9999, 'status': '0', 'isDefault': '0', 'spec': {}}];
        //2.遍历specificationItems规格集合
        var items = $scope.entity.goodsDesc.specificationItems;
        for (var i = 0; i < items.length; i++) {
            //3.将遍历后的规格选项，添加到Item数据结构中，增加列
            $scope.entity.itemList = addColumn($scope.entity.itemList, items[i].attributeName, items[i].attributeValue);
        }
    }


    /**
     *
     * @param list     item sku的集合
     * @param columnName        规格名称
     * @param columnValue       规格选项集合
     */
    addColumn = function (list, columnName, columnValues) {
        var newList = [];  //初始化新的集合
        for (var i = 0; i < list.length; i++) {
            var oldRow = list[i];    //先将SKU对象赋值给一个变量
            //遍历规格选项集合，构建行数
            for (var j = 0; j < columnValues.length; j++) {
                //深克隆
                var newRow = JSON.parse(JSON.stringify(oldRow));
                //增加列的操作
                newRow.spec[columnName] = columnValues[j];
                newList.push(newRow);
            }
        }
        return newList;
    }

    //定义一个关于审核状态值的数组  0.未审核  1审核通过  2.驳回  3.关闭
    $scope.status = ['未审核', '审核通过', '驳回', '关闭'];

    $scope.categoryList = [];

    //查询全部分类
    $scope.findItemCatList = function () {
        itemCatService.findAll().success(
            function (response) {
                for (var i = 0; i < response.length; i++) {
                    $scope.categoryList[response[i].id] = response[i].name;
                }
            })
    }


});