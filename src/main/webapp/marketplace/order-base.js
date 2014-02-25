define([
    "text!marketplace/order.html",
    "underscore", "jquery", "backbone", "bootstrap"
], function (PageHtml) {

    var view = Backbone.View.extend({
        tagName:"div",
        events:{
            'click .tab-clickable': 'showTab'
//            'click #add-new-application':'createApplication',
//            'click #reload-brooklyn-properties': 'reloadBrooklynProperties',
//            'click .addApplication':'createApplication'
        },
        
        initialize:function () {
            this.$el.html(_.template(PageHtml, {} ));
            this.initializeApp();
        },
        
        // cleaning code goes here
        beforeClose:function () {
//            this.collection.off("reset", this.render)
//            this.options.locations.off("reset", this.renderSummaries)
//            // iterate over all (sub)views and destroy them
//            _.each(this._appViews, function (value) {
//                value.close()
//            })
//            this._appViews = null
        },

        render:function () {
//            this.renderSummaries()
//            this.renderCollection()
            return this;
        },
        
        setLogo: function(imgPath) {
            this.setSection('#logo', 
                '<img src="'+imgPath+'" style="max-width: 96px;"/>');
        },
        
        setSection: function(selector, html) {
            this.$el.find(selector).html(html);
        },
        
        numTabs: 0,
        addTab: function(name, html) {
            var $tabs = this.$el.find('.order-tabs-parent');
            $tabs.show();
            $tabs.find('.order-tabs').append(
                    '<span class="tab tab-clickable" id="tab-'+this.numTabs+'">'+name+'</span>');
            $tabs.append(
                    '<div id="tab-'+this.numTabs+'-main" class="text-left hide tab-content" style="margin: 24px;">'+
                    html+'</div>');
            this.numTabs++;
        },
        showTab: function(evt) {
            var id = $(evt.target).attr('id');
            var $tabs = this.$el.find('.order-tabs-parent');
            $tabs.find('.tab.active').removeClass('active');
            $(evt.target).addClass('active');
            $tabs.find('.tab-content').hide();
            $tabs.find('#'+id+"-main").show();
        }
    });

    return view;
});
