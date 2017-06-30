/**
 * Created by cookeem on 16/4/18.
 */

//左侧导航栏二级菜单设置为Active
function activeChange(locType) {
    $('#nav-mobile li').removeClass('active');
    $('#link-'+locType).parent().addClass('active');
    $('#link-'+locType).parent().parent().parent().parent().addClass('active');
}

//左侧导航栏菜单router
var app = angular.module('app', ['ngRoute', 'ngAnimate']);

app.config(function($routeProvider, $locationProvider) {
    $routeProvider
        .when('/', {
            templateUrl : 'template/content-list.html',
            controller : 'contentCtl'
        })
        .when('/changePwd', {
            templateUrl: 'template/change-password.html',
            controller: 'contentCtl'
        })
        .when('/logout', {
            templateUrl: 'template/logout.html',
            controller: 'contentCtl'
        })
        .when('/content-list/:sid', {
            templateUrl: 'template/content-list.html',
            controller: 'contentCtl'
        })
        .when('/content/:cuid', {
            templateUrl: 'template/content.html',
            controller: 'contentCtl'
        })
    ;
    //使用#!作为路由前缀
    $locationProvider.html5Mode(false).hashPrefix('!');
});

app.controller('headerCtl', function($scope, $rootScope, $routeParams, $http) {
    $scope.closeOnClick = false;
    if ($(window).width() < 992) {
        $('.button-collapse').sideNav('hide');
        setTimeout(function(){
            $('.button-collapse').sideNav('hide');
        }, 500);
        $scope.closeOnClick = true;
    }
    //sideNav初始化
    var sidebarWidth = 240;
    $('.button-collapse').sideNav({
            menuWidth: sidebarWidth, // Default is 240
            edge: 'left', // Choose the horizontal origin
            closeOnClick: $scope.closeOnClick // Closes side-nav on <a> clicks, useful for Angular/Meteor
        }
    );
    if (!$rootScope.sid) {
        $scope.$on('$routeChangeSuccess', function() {
            // $routeParams will be populated here if
            // this controller is used outside ng-view
            if ($routeParams.sid) {
                $rootScope.sid = $routeParams.sid;
            }
        });
    }
    $scope.siteLists = [];
    $scope.getSites = function() {
        $http({
            method  : 'GET',
            url     : '/json/siteList'
            //data    : $.param($scope.searchData),
            //headers : { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' }
        }).then(function successCallback(response) {
            $scope.siteLists = response.data.sitelists;
            $scope.siteLists.splice(0, 0, {"sid":0, "sitename":"全部","siteurl":""});
        }, function errorCallback(response) {
            console.info("error:" + response.data);
        });
    }
    $scope.getSites();
});

app.controller('contentCtl', function($rootScope, $scope, $route, $routeParams, $location) {
    $rootScope.params = $routeParams;
    $scope.sid = 1;
    $scope.loginRole = 'admin';
    $scope.activeMenu = '';
    $scope.locType = '';
    var location = $location.path();
    if (location.indexOf('/changePwd') == 0) {
        $scope.locType = 'changePwd';
    } else if (location.indexOf('/logout') == 0) {
        $scope.locType = 'logout';
    } else if (location.indexOf('/content-list') == 0) {
        $scope.locType = 'content-list';
    } else if (location.indexOf('/content') == 0) {
        $scope.locType = 'content';
    }
    activeChange($scope.locType);
});
