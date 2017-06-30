/**
 * Created by cookeem on 16/4/18.
 */
app.controller('contentListAppCtl', function($rootScope, $scope, $http, $timeout) {
    $rootScope.isLoading = false;

    $scope.sid = $rootScope.params.sid;
    $scope.page = 1;
    $scope.count = 10;

    $scope.contentLists = [];
    $scope.errmsg = "";
    $scope.rscount = 0;
    $scope.pages = 0;
    $scope.showpages = 9;
    $scope.range = [];

    $scope.searchSubmit = function() {
        $rootScope.isLoading = true;
        $http({
            method  : 'GET',
            url     : '/json/contentList?sid='+$scope.sid+'&page='+$scope.page+'&count='+$scope.count
            //data    : $.param($scope.searchData),
            //headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            $rootScope.isLoading = false;
            $scope.contentLists = response.data.contentlists;
            $scope.errmsg = response.data.errmsg;
            $scope.rscount = response.data.rscount;
            $scope.pages = Math.ceil($scope.rscount / $scope.count);
            var minpage = 0;
            var maxpage = 0;
            var slide = Math.ceil(($scope.showpages-1)/2);
            if ($scope.pages <= $scope.showpages) {
                minpage = 1;
                maxpage = $scope.pages;
            } else {
                if ($scope.page - slide < 1) {
                    minpage = 1;
                    if ($scope.showpages > $scope.pages) {
                        maxpage = $scope.pages;
                    } else {
                        maxpage = $scope.showpages;
                    }
                } else {
                    if ($scope.page + slide > $scope.pages) {
                        minpage = $scope.pages - $scope.showpages + 1;
                        maxpage = $scope.pages;
                    } else {
                        minpage = $scope.page - slide;
                        maxpage = $scope.page + slide;
                    }
                }
            }
            var range = [];
            for(var i=minpage;i<=maxpage;i++) {
                range.push(i);
            }
            $scope.range = range;
            $timeout(function() {
                ////关闭左侧菜单的蒙板
                //$('#sidenav-overlay').remove();
                //加载imglazy
                $("img.lazy").lazyload({
                    effect : "fadeIn"
                });
            }, 500);
            $('html, body').animate({scrollTop:0}, 0);
        }, function errorCallback(response) {
            $rootScope.isLoading = false;
            console.info("error:" + response.data);
        });
    }
    $scope.pageChange = function(targetPage) {
        $scope.page = targetPage;
        $scope.searchSubmit();
    }
    $scope.hideAlert = function() {
        $scope.errmsg = "";
    }
    $scope.searchSubmit();
});