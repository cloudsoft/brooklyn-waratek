define([
    "underscore", "jquery", "backbone", "rickshaw",
    "marketplace/status-base",
    "brooklyn-graph-utils",
], function (_, $, Backbone, Rickshaw, StatusBaseView, BGU) {

    var View = StatusBaseView.extend({
        events: _.extend({ 
        }, StatusBaseView.prototype.events),
        
        initializeApp:function () {
            this.setLogo("apps/cassandra/img/cassandra-logo.png");
            this.setSection("#title", "Cassandra GOLD");
            
            var appId = this.options.appId;
            
            this.graph = BGU.add_status_bar_graph(this,
                [
                    {
                        data: [{x:0,y:0}],
                        name: 'reads',
                        color: '#ba6'
                    },{
                        data: [{x:0,y:0}],
                        name: 'writes',
                        color: '#a38'
                    },
                ],
                'apps/cassandra/'+appId+'/nodes_load',
                {
                    $el: this.$('#status #content'),
                    nodeDataHolder: this,
                    width: 400,
                    frequency: 2000 /* 2000 = every 2s; set -1 for no reloads */
                });
        },

    });

    return View;
});
