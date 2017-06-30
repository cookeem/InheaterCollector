/**
 * Created by cookeem on 16/4/15.
 */
//materialize css 相关组件初始化
$(document).ready(function() {
    //初始化下拉选择
    $('select').material_select();

    //初始化输入计数器
    $('input#input_text, textarea#textarea1').characterCounter();
    //初始化导航栏下拉菜单
    $(".dropdown-button").dropdown(
        {
            inDuration: 300,
            outDuration: 225,
            //constrain_width: false, // Does not change width of dropdown to that of the activator
            hover: false, // Activate on hover
            gutter: 0, // Spacing from edge
            belowOrigin: false, // Displays dropdown below the button
            alignment: 'left' // Displays dropdown with edge aligned to the left of button
        }
    );
    //collapsible初始化
    $('.collapsible').collapsible({
        accordion : false // A setting that changes the collapsible behavior to expandable instead of the default accordion style
    });
    //sideNav初始化
    var sidebarWidth = 240;
    $('.button-collapse').sideNav({
            menuWidth: sidebarWidth, // Default is 240
            edge: 'left', // Choose the horizontal origin
            closeOnClick: false // Closes side-nav on <a> clicks, useful for Angular/Meteor
        }
    );
    //toast点击关闭
    $(document).on('click', '#toast-container .toast', function() {
        $(this).fadeOut(function(){
            $(this).remove();
        });
    });
    //modal初始化
    $(document).ready(function(){
        // the "href" attribute of .modal-trigger must specify the modal ID that wants to be triggered
        $('.modal-trigger').leanModal(
            {
                dismissible: true, // Modal can be dismissed by clicking outside of the modal
                opacity: .5, // Opacity of modal background
                in_duration: 300, // Transition in duration
                out_duration: 200, // Transition out duration
                ready: function() { alert('Ready'); }, // Callback for Modal open
                complete: function() { alert('Closed'); } // Callback for Modal close
            }
        );
    });
});

//初始化日期选择
$('.datepicker').pickadate({
    selectMonths: true, // Creates a dropdown to control month
    selectYears: 16 // Creates a dropdown of 15 years to control year
});
