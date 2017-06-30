/**
 * Created by cookeem on 16/4/19.
 */
app.controller('contentAppCtl', function($rootScope, $scope, $http, $timeout) {
    $rootScope.isLoading = false;

    $scope.cuid = $rootScope.params.cuid;

    $scope.content = {
        "luid": "",
        "sid": 0,
        "sitename": "",
        "title": "",
        "url": "",
        "author": "",
        "postdate": "",
        "content": "",
        "lastupdate": "",
        "titleimage": ""
    };
    $scope.errmsg = "";

    $scope.searchSubmit = function() {
        $rootScope.isLoading = true;
        $http({
            method  : 'GET',
            url     : '/json/content?cuid='+$scope.cuid
            //data    : $.param($scope.searchData),
            //headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            $rootScope.isLoading = false;
            $scope.content = response.data.content;
            if ($scope.content.sid) {
                $rootScope.sid = $scope.content.sid;
            }
            $timeout(function() {
                //加载imglazy
                $('#article-content img').each(function() {
                    var val = jQuery.attr( this, 'src' );
                    jQuery.attr( this, 'data-original', val);
                    jQuery.removeAttr( this, 'src');
                    jQuery.attr( this, 'class', 'lazy');
                });
                $("img.lazy").lazyload({
                    effect : "fadeIn"
                });
            }, 500);
            $scope.errmsg = response.data.errmsg;
        }, function errorCallback(response) {
            $rootScope.isLoading = false;
            console.info("error:" + response.data);
        });
    }
    $scope.hideAlert = function() {
        $scope.errmsg = "";
    }
    $scope.searchSubmit();
});

app.filter('trustHtml', function ($sce) {
    return function (input) {
        return $sce.trustAsHtml(input);
    }
});
