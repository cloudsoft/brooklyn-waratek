define([
    "text!marketplace/status.html",
    "underscore", "jquery", "backbone", "bootstrap"
], function (PageHtml) {

    var view = Backbone.View.extend({
        tagName:"div",
        events:{
            'click .app-back': 'app_back',
        },
        
        initialize:function () {
            this.$el.html(_.template(PageHtml, {} ));
            this.initializeApp();
            
            if (this.options.app_back_function) {
                this.$("#title").append('<div style="float: right; margin-right: 12px; margin-top: 6px; opacity: 0.7;" class="icon-remove app-back handy"></div>');
            }
        },
        
        // cleaning code goes here
        beforeClose:function () {
        },

        render:function () {
            return this;
        },
        
        setLogo: function(imgPath) {
            this.setSection('#status #logo', 
                '<img src="'+imgPath+'" style="max-width: 60px;"/>');
        },
        
        setSection: function(selector, html) {
            this.$el.find(selector).html(html);
        },
        

        app_back: function() {
            this.options.app_back_function();
        },

    });

    return view;
});
