define([
    "main/view/default",
    'brooklyn-utils', 'brooklyn-graph-utils', 'underscore', 'jquery', 'backbone', 
], function (DefaultView, BU, BGU) {

    // add close method to all views for clean-up
	// (NB we have to update the prototype _here_ before any views are instantiated;
	//  see "close" called below in "showView") 
    Backbone.View.prototype.close = function () {
        // call user defined close method if exists
        this.viewIsClosed = true;
        if (this.beforeClose) {
            this.beforeClose();
        }
        _.each(this._periodicFunctions, function(i) {
            clearInterval(i);
        });
        this.remove();
        this.unbind();
    };
    Backbone.View.prototype.viewIsClosed = false;

    /**
     * Registers a callback (cf setInterval) that is unregistered cleanly when the view
     * closes. The callback is run in the context of the owning view, so callbacks can
     * refer to 'this' safely.
     */
    Backbone.View.prototype.callPeriodically = function (uid, callback, interval) {
        if (!this._periodicFunctions) {
            this._periodicFunctions = {};
        }
        var old = this._periodicFunctions[uid];
        if (old) clearInterval(old);

        // Wrap callback in function that checks whether updates are enabled
        var periodic = function() {
            if (BU.refresh) {
                callback.apply(this);
            }
        };
        // Bind this to the view
        periodic = _.bind(periodic, this);
        this._periodicFunctions[uid] = setInterval(periodic, interval);
    };

    var Router = Backbone.Router.extend({
        routes:{
            'waratek':'dashboardPage',
            'waratek/*path':'dashboardPage',
            'status/*app/*id':'statusPage',
            '*path':'defaultRoute'
        },

        showView:function (selector, view) {
            // close the previous view - does binding clean-up and avoids memory leaks
            if (this.currentView) this.currentView.close();
            
            // global decorators
            $('#account').html('Demo');
            
            // render the view inside the selector element
            $(selector).html(view.render().el);
            this.currentView = view;
            return view;
        },
        
        defaultRoute:function () {
            this.defaultPage();
        },
        defaultPage: function(path) {
            var that = this;
            $('#app-name').html('Brooklyn Waratek Console');
            var view = new DefaultView({
                router:that
            });
            that.showView("#main-content", view);
        },
        
        dashboardPage: function(path) {
            var that = this;
            require(["waratek/waratek"], function(WaratekDashView) {
                $('#app-name').html('Brooklyn Waratek Dashboard');
                $('.bottom #footer').html('<div id="copyright">Copyright 2014 by Cloudsoft Corporation Limited</div>');
                var view = new WaratekDashView({
                    router:that,
                    path:path
                });
                that.showView("#main-content", view);
            }); 
        },
        
        statusPage: function(app, appId) {
            var that = this;
            if (!appId) {
                this.defaultPage(appId);
                return;
            }
            require(["apps/"+app+"/status/status.js"], function(AppStatusView) {
                $('#app-name').html('Brooklyn Waratek Application Status');
                $('.bottom #footer').html('<div id="copyright">Copyright 2014 by Cloudsoft Corporation Limited</div>');
                var view = new AppStatusView({
                    router:that,
                    appId:appId,
                });
                that.showView("#main-content", view);
            }, _.partial(this.noSuchAppErrPage, that));
        },

        noSuchAppErrPage: function(that, err) {
            log("Unknown application when loading page")
            log(err)
            that.defaultPage();
        },
        
    });
    
    return Router;
});