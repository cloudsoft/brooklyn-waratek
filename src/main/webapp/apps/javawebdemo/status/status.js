define([
    "underscore", "jquery", "backbone", "rickshaw", "gauge",
    "marketplace/status-base",
    "brooklyn-graph-utils",
], function (_, $, Backbone, Rickshaw, _Gauge, StatusBaseView, BGU) {

    var View = StatusBaseView.extend({
        events: _.extend({
        }, StatusBaseView.prototype.events),
        
        initializeApp:function () {
            _.bindAll(this);
            
            var that = this;
//            this.setLogo("apps/javawebdemo/img/web-icon.png");
            this.setSection("#title", "Web App");
                        
            var appId = this.options.appId;
  
            // gauge settings, for reference
//          lines: 12, // The number of lines to draw
//          angle: 0.1, // The length of each line
//          lineWidth: 0.3, // The line thickness
//          pointer: {
//            length: 0.9, // The radius of the inner circle
//            strokeWidth: 0.06, // The rotation offset
//            color: '#000000' // Fill color
//          },
//          limitMax: 'false',   // If true, the pointer will not go past the end of the gauge
//
//          colorStart: '#6FADCF',   // Colors
//          colorStop: '#8FC0DA',    // just experiment with them
//          strokeColor: '#E0E0E0',   // to see which ones work best for you
//          generateGradient: true

            content = this.$('#status #content');
            content.append(img = '<div style="display: inline-block;"><img src="apps/javawebdemo/img/web-icon.png" '+
                    'style="max-width: 150px; max-height: 150px; margin-right: 15px;"></div>');
            
            content.append(content2 = $('<div>'));
            content2.css("display", "inline-block").css("vertical-align", "top").css("margin-top", "25px");
            
            content2.append(top_bar = $('<div>'));
            top_bar.css("text-align", "left");
            top_bar.append(label = $('<span id="main_url">'));
            label.css("font-size", "150%").css("font-weight", "750");
            
            col1 = $('<div style="display: table-cell; vertical-align: top;">');
            
            col1.append(target = $('<canvas id="main_gauge" width="160" height="120">'));
            this.gauge = new Gauge(target[0]).setOptions({ pointer: { color: '#082C54' },
                colorStart: '#FF0000', colorStop: '#000000'
                });
            this.gauge.maxValue = 3000;
            this.gauge.set(0);
            content2.append(col1);

            col1.append(label = $('<div>'));
            label.append('<b><span  id="main_value">.?.</span></b>');
            label.css("font-size", "300%").css("font-weight", "750");
            label.css("top", "-40px").css("padding-left", "25px");
            
            label.append(units = $('<table><tr><td>reqs /</td></tr><tr><td>sec</td></tr></table>'));
            units.css("font-size", "45%").css("line-height", "18px").css("top", "-5px").css("position", "relative")
                .addClass("small-multiline").css("margin-left", "9px");
    
            
            content2.append(target = $('<div id="regions_load">'));
            target.css("display", "table-cell").css("padding-top", "30px").css("padding-left", "30px");
            target.append(graph_target = $('<div style="display: inline-block;">'));
            target.append(y_axis_target = $('<div style="display: inline-block;" id="group_y_axis">'));
            y_axis_target.css("width", "30");
            
            this.usage_graphs = new BGU.StatusBarGraphGroup(this, 
                    function() { return [{ data: [{x:0,y:0}], name: 'reqs_per_sec', color: '#082C54' } ]},
                    {
                        $group_el: graph_target,
                        group_url: 'apps/javawebdemo/'+appId+'/nodes_load',
                        load_node_data: BGU.new_load_node_data_function_from_node_id_url('main/brooklyn/'+appId+'/node_info/',
                                { groups_data: { region1: { name: "Region 1" }, region2: { name: "Region 2 (special)" }, }}),
                        total_width: 150,
                        min_bar_width: 15,
                        height: 120,
                        frequency: 3000,
                        data_callbacks: [ that.refresh_reqs_per_sec ],
                        y_axis_args: { orientation: "right" },
                        y_axis_element: y_axis_target[0]
                    });
            
            this.usage_graphs.useYAxis();
            this.usage_graphs.start();
            this.poll('apps/javawebdemo/'+appId+'/info', this.refresh_info);
        },

        render: function() {
            this.$('#summary').html('');
            this.$('#summary').append('<b><span id="num_regions">.?.</span></b> environments<br/>');
            this.$('#summary').append('<b><span id="num_servers">.?.</span></b> servers<br/>');
            return this;
        },
        
        refresh_reqs_per_sec: function() {
            if (this.usage_graphs && this.usage_graphs.data_grouped) {
                num_regions = _.size(this.usage_graphs.data_grouped);
                this.$('#num_regions').html(num_regions);
                
                if (this.usage_graphs.data_series) {
                    num_servers = _.size(_.values(this.usage_graphs.data_series)[0]);
                    this.$('#num_servers').html(num_servers);
                }
                
                v = _.values( _.values(this.usage_graphs.data_series)[0] )
                sum = _.reduce(v, function(memo, num){ return memo + num; }, 0);
                this.gauge.set(sum);
                this.$('#main_value').html(Math.round(sum));
            }
        },
        
        refresh_info: function(data) {
            error = !data ? "No data available" : data.error ? data.error : null;
            url = data['root.url'];
            this.$("#main_url").html(error ? _.escape(error) : '<a href="'+_.escape(url)+'">'+_.escape(url)+"</a>");
        },
        poll: function(url, callback) {
            that = this;
            if (this.viewIsClosed) return;
            $.ajax({ url: url, success: function(data) { callback(data); setTimeout(function() { that.poll(url, callback) }, 5000) }, 
                    error: function(data) { callback({error: "Data not available"}) } });
        }

    });

    return View;
});
