define([
    "marketplace/order-base",
    "text!apps/cassandra/order/edit-account.html",
    "text!apps/cassandra/order/todo.html",
    
    "underscore", "jquery", "backbone",
], function (OrderBaseView, EditAccountHtml, TodoHtml) {

    var View = OrderBaseView.extend({
        events: _.extend({ 
            'click .account-change': 'onAccountChange' 
        }, OrderBaseView.prototype.events),
        
        initializeApp:function () {
            this.setLogo("apps/cassandra/img/cassandra-logo.png");
            this.setSection("#title", "Cassandra Modular Managed Service: GOLD");
            this.setSection('#description',
                    'This edition of the Cassandra Modular Managed Service includes '+
                    'resilience and nearly a whopping terabyte of on-disk storage. '+
                    'Confirm by clicking at right, or select a further customization below.');
            
            this.addTab("Account", EditAccountHtml)
            this.addTab("Resilience", TodoHtml)
            this.addTab("Notifications", TodoHtml)
        },

        onAccountChange: function(evt) {
            var details = this.$el.find('#account-user-details');
            if (evt.target.value == 'user')
                details.show();
            else
                details.hide();
        }
    });

    return View;
});
