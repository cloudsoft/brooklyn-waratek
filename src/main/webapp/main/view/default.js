/**
 * Renders the Applications page. From it we create all other application related views.
 */

define([
    "underscore", "jquery", "backbone",  
    "text!main/view/default.html",
    "bootstrap"
], function (_, $, Backbone,
        DefaultHtml) {

    var view = Backbone.View.extend({
        tagName:"div",
        events:{
            'click #add-new-application':'createApplication',
            'click #reload-brooklyn-properties': 'reloadBrooklynProperties',
            'click .addApplication':'createApplication'
        },
        
        initialize:function () {
//            var that = this;
            
            this.$el.html(_.template(DefaultHtml, {} ));
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
        }
    })

    return view;
})
