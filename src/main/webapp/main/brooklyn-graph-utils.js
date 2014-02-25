define([
    'underscore', 'jquery', 'rickshaw'
], function (_, $, Rickshaw) {

    var GraphUtil = {};

    /**
     * @return {Date} date in UTC of the given UTC seconds;
     * useful for plotting graphs of days/years where input is in UTC
     * (Date uses the local time zone, for setting and printing, so this reports
     * e.g. midnight utc as midnight EDT if JS is using EDT --
     * TODO better would be to use a proper date/time library!)
     */
    GraphUtil.date_utc_seconds = function(utcSeconds) {
        var d = new Date(0); 
        d.setUTCSeconds(x); 
        return new Date( d.getTime() + (d.getTimezoneOffset() * 60000) );
    };
    
    /** Highly opinionated bar graph based on nodes in a status view, 
     * expecting data from the dataUrl rest endpoint in the format described under update_status_bar_graph_data,
     * with templateSeriesDataProvider returning a templated version of this including names, colours, etc
     * (since this may create multiple graphs, we need a function so different template instances are given to each graph,
     * else the graphs modify their copy which ends up affecting the other graphs!) */
    var StatusBarGraph = GraphUtil.StatusBarGraph = function(view, templateSeriesData, dataUrl, options) {
        var self = this;
        
        var _initialize = function(view, templateSeriesData, dataUrl, options) {
            if (!options) options = {};
            self.view = view;
            var $el = options.$el || view.$el,
              width = options.width || 400,
              height = options.height || 250,
              nodeDataHolder = options.nodeDataHolder || {},
              frequency = options.frequency || 2000,
              includeYAxis = options.includeYAxis !== undefined ? options.includeYAxis : true;
            
            $('<link rel="stylesheet" type="text/css" href="main/libs/rickshaw.css">') .appendTo("head");
            $('<link rel="stylesheet" type="text/css" href="apps/cassandra/css/graph.css">') .appendTo("head");
            $el.html('<div id="chart_border" style="display: table-cell; padding: 0 5px 0 5px; border: 2px lightgray solid; margin: 12px auto 6px auto;">'+
                      '<div id="message" style="height: '+height+'px; display: table-cell; vertical-align: middle;">Loading ...</div>'+
                      '<div id="chart_container" class="inline" style="height: '+height+'px;">'+
                        '<div id="y_axis"></div><div id="chart"></div>'+
                    '</div></div>');
            
            var graph = new Rickshaw.Graph({
                element: $el.find('#chart')[0],
                renderer: 'bar',
                series: [].concat(templateSeriesData),
                width: width,
                height: height });

            $el.find('#message').show();
            $el.find('#chart_container').hide();
            var update_graph = _.partial(self.update_status_bar_graph_data, nodeDataHolder, graph);
            self.on_data = update_graph;
            if (options.dataCallbacks) {
                self.on_data = function(data) {
                    _.each(options.dataCallbacks, function(callback) { callback(data); });
                    update_graph(data);
                };
            }
            if (dataUrl)
                self.schedule_update_graph_data(view, $el, self.on_data, 
                    dataUrl, 0, frequency);

            graph.$el = $el;
            if (includeYAxis)
                graph.y_axis = new Rickshaw.Graph.Axis.Y( {
                    graph: graph,
                    orientation: 'left',
                    tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
                    element: $el.find('#y_axis')[0],
                    suppressAxisShift: true,
                } );
            
            graph.hover = new Rickshaw.Graph.HoverDetail( {
                graph: graph,
                xFormatter: function(x) {
                    try {
                        return nodeDataHolder.nodes_data[nodeDataHolder.nodes_list[x]].name;
                    } catch (e) {
                        return x;
                    }
                },
                yFormatter: function(y) { 
                    return y.toLocaleString();
                },
            });
            
            graph.render();
            
            self.graph = graph;
            self.$el = $el;
        };
        
        self.schedule_update_graph_data = function(view, $el, updateFunction, url, initialDelayMillis, subsequentDelayMillis) {
            f = function() {
                $.ajax({
                  url: url,
                  success: function(data) {
                    if (view.viewIsClosed) 
                        return;
                    updateFunction(data);
                    if (subsequentDelayMillis && subsequentDelayMillis>0)
                        self.schedule_update_graph_data(view, $el, updateFunction, url, subsequentDelayMillis, subsequentDelayMillis);
                  },
                  error: function(data) {
                    $el.find('#chart_container').hide();
                    $el.find('#message').html("No data available.").show();
                  }
                });
              };
              if (initialDelayMillis > 0) setTimeout(f, initialDelayMillis);
              else f();
        };

        /**
         * Takes data of the form:
         * 
         * { 'sensor1': { 'node1id': 'valueSensor1AtNode2', 'node2id': 'valueSensor1AtNode2' },
         *   'sensor2': { 'node2id': 'valueSensor2AtNode2', 'node3id': 'valueSensor2AtNode3' } }
         *   
         * And feeds it in to a rickshaw graph whose series has been populated with data of the form:
         * 
         * [{ data: [{x:0,y:0}], name: 'sensor1', color: '#ba6' },
         *  { data: [{x:0,y:0}], name: 'sensor2', color: '#ba6' } ]
         *  
         * Tracking the valid nodes, and ignoring any sensorX for which there is not a correspondingly named entry
         * in the graph's series.
         * 
         * Note that all data arrays may be required to have the same length; 
         * some massaging may be done to help make this happen. 
         */
        self.update_status_bar_graph_data = function(nodeDataHolder, graph, liveData) {
            if (!liveData) {
                log("NULL live data");
                return;
            }

            if (view.viewIsClosed) 
                return;
            graph.$el.find('#chart_container').show();
            graph.$el.find('#message').html("No data available.").hide();

            var nodes = []
            for (var key in liveData) {
                nodes = _.union(_.keys(liveData[key]))
            }
            
            if (!nodeDataHolder.nodes_list) {
                nodeDataHolder.nodes_list = [ ];
                nodeDataHolder.nodes_data = {}
            }
            for (var ni in nodes) {
                var index = _.indexOf(nodeDataHolder.nodes_list, nodes[ni])
                if (index==-1) {
                    nodeDataHolder.nodes_list.push(nodes[ni]);
                    self.load_node_data(nodeDataHolder, nodes[ni]);
                }
            }
            // remove nodes not seen
            nodeDataHolder.nodes_list = _.intersection(nodeDataHolder.nodes_list, nodes)

            for (var si in graph.series) {
                var series = graph.series[si]
                var sd = liveData[series.name]
                var s = []
                var so = {}
                if (sd) {
                    for (var i in sd) {
                        var x = _.indexOf(nodeDataHolder.nodes_list, i)
                        s.push( { x: x, y: sd[i] } )
                        so[x] = true
                    }
                }
                // backfill missing elements
                for (var i=0; i<nodeDataHolder.nodes_list.length; i++)
                    if (!so[i])
                        s.push( { x: i, y: 0} );
                // ensure length >= 1
                if (s.length==0)
                    s.push( { x:0, y:0 } );
                    
                series.data = s;
            }
            
            graph.update();
        };

        /* load_node_data should be function taking a holder and an id, and ensuring 
         * that name and group_id info are available for the holder */ 
        self.load_node_data = options.load_node_data || function(nodeDataHolder, nodeId, callback) {
            nodeDataHolder.nodes_data[nodeId] = { name: nodeId, group_id: 'default' };
            log("TODO load proper data for node "+nodeId);
            if (callback) callback()
        };
        
        _initialize(view, templateSeriesData, dataUrl, options);
    };

    /** group of StatusBarGraph graphs, with shared y-axis and max;
     *  this can be wired up to caller-managed graphs with distinct URL's,
     *  or it can be given a "group_url" which returns data in the same 
     *  format as StatusBarGraph -- in data_series -- which we then cross-reference with load_node_data
     *  (which must be supplied)
     *  in order to generate the internal data_grouped model, which is a little weird, viz:
     *  <p>
     *   "cluster1": { "sensor1": {"node1": "value1"}, "sensor2": {"node1": "value1"} }, 
     *   "cluster2": { "sensor1": {"node2": "value1"}, "sensor2": {"node2": "value1"} },
     *  <p>
     *  but it fits neatly into how rickshaw wants things.
     *  in particular, if group_url is not used, then data_series will not be populated,
     *  as individual StatusBarGraph instances data_url may return the same x (node) value,
     *  but they should be treated differently of course.
     **/
    var StatusBarGraphGroup = GraphUtil.StatusBarGraphGroup = function(view, templateSeriesDataProvider, options) {
        var self = this;
        
        self.view = view;
        self.templateSeriesDataProvider = templateSeriesDataProvider;
        self.options = (options = options || {});
        self.data_series = null
        self.data_grouped = {}
        self.nodeDataHolder = options.nodeDataHolder || {};
        
        var height = options.height || 250;

        if (options.$group_el) {
            self.$group_el = options.$group_el;
            if (!self.$group_el.length) throw new Error("Invalid element supplied for group of graphs"); 
            delete options['$group_el'];
        }
        if (options.group_url) {
            self.group_url = options.group_url;
            delete options['group_url'];
        }
        if (options.data_callbacks) {
            self.data_callbacks = options.data_callbacks;
            delete options['data_callbacks'];
        }
        if (options.total_width) {
            self.total_width = options.total_width;
            delete options['total_width'];
        }
        if (options.width)
            throw new Error("WARN: width is ambiguous; use each_width or total_width");
        if (options.each_width) {
            if (self.total_width)
                throw new Error("WARN: cannot use each_width and total_width in same options");
            options.width = options.each_width;
            delete options['each_width'];
        }
        
        var maxes = {};
        var graphs = {};
        var bgu_graphs = {};
        
        self.updateMax = function(target, _data_grouped) {
            data_s = _.values(_data_grouped)
            all_keys = _.reduce( data_s, function(memo, obj) { return _.union(memo, _.keys(obj)) }, [] )
            sum = function(list) { return _.reduce(list, function(memo, num){ return memo + num; }, 0); }
            sums = _.map(all_keys, function(key) { return sum(_.values(_.pluck(data_s, key))) })
            max_data_sum = _.max(_.values(sums))
            if (!max_data_sum || max_data_sum < 10) max_data_sum = 10;
            
            maxes[target] = max_data_sum;
            global_max = _.max(_.values(maxes));
            // TODO dampen
            _.each(graphs, function(g) { g.max = global_max; g.update(); })
        };
        
        self.updateWidths = function() {
            if (!self.total_width) return;
            // TODO update width of each component so that the total is as indicated

            var sizes = {}
            var real_total = 0, notional_total = 0;
            for (var id in graphs) {
                var data_s = _.values(self.data_grouped[id])
                size = _.size( _.reduce( data_s, function(memo, obj) { return _.union(memo, _.keys(obj)) }, [] ) );
                sizes[id] = size;
                real_total += size;
                notional_total += (size==0 ? 1 : size);
            }
            var col_width = notional_total==0 ? self.total_width : self.total_width / notional_total;
            if (self.options.min_bar_width && col_width < self.options.min_bar_width)
                col_width = self.options.min_bar_width;
            
            for (var id in graphs) {
                self.setWidth(id, col_width * (sizes[id] || 1));
            }
        };

        self.setWidth = function(id, width, redraw) {
            graphs[id].setSize({ width: width, height: height });
            if (redraw) {
                graphs[id].update();
            }
        };

        var init_y_axis = function() {
            if (!self.y_axis_element) return;
            if (_.isEmpty(graphs)) return;
            if (self.y_axis) return;
            
            var g = _.values(graphs)[0];
            var y_axis = new Rickshaw.Graph.Axis.Y( _.extend({
                graph: g,
                orientation: 'left',
                tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
                element: self.y_axis_element,
                suppressAxisShift: true,
            }, self.options.y_axis_args) );
            y_axis.setSize({width: self.y_axis_width, height: height});
            
            self.y_axis = g.y_axis = y_axis;
        };
        
        self.on_data = function() {
            self.updateWidths();
            if (self.data_callbacks)
                // note callback here is different to StatusBarGraph
                _.each(self.data_callbacks, function(callback) { callback(self); } );
        };
        
        self.addGraph = function(options) {
            options = options || {};
            var id = options.id || _.size(graphs);
//            log("adding graph "+id);
            
            if (options.$el) {
                $el = options.$el;
            } else {
                if (!self.$group_el) throw new Error("$el or $group_el must be available for this graph");
                self.$group_el.append("<div id='"+_.escape(id)+"' style='display: table-cell; margin: 0px 5px; vertical-align: top;' class='one_group'>" +
                        " <div class='for_graph'></div> <div class='for_label'></div>" +
                        "</div>");
                $el = self.$group_el.find("#"+_.escape(id)+" .for_graph");
                self.$group_el.find("#"+_.escape(id)+" .for_label").html( _.escape(options.group_name || id) );
            }
            if (!options.url && !self.group_url) throw new Error("url or group_url must be available for this graph");
            if (options.url && self.group_url)
                // could perhaps be added ...
                throw new Error("Cannot combine graph url and group_url");
            
            if (!$el.length) throw new Error("Invalid element supplied to add graph to group");
            
            var tsd = options.templateSeriesData || self.templateSeriesDataProvider();
            var bgu_graph = new GraphUtil.StatusBarGraph(self.view, tsd,
                    options.url, _.extend({ height: height, nodeDataHolder: self.nodeDataHolder }, self.options, options, { 
                        $el: $el, 
                        dataCallbacks: _.compact([
                            options.url ? function(data_series_of_one_group) { 
                                self.data_grouped[id] = data_series_of_one_group; 
                            } : null,
                            // max will get updated every time and graph's data is updated,
                            // but the other callbacks get done by the local on_data if options.url is supplied
                            _.partial(self.updateMax, id), 
                            options.url ? self.on_data : null, 
                        ]) }) );
            var graph = bgu_graph.graph;
            graph.brooklyn_id = id;
            graph.renderer.gapSize = 0.2;
            graph.$el.find('#chart_border').css('border', 'none');
            graph.$el.find('#y_axis').hide();
            graphs[id] = graph;
            bgu_graphs[id] = bgu_graph;
            
            // if needed
            init_y_axis();
        };

        self.useYAxis = function(target, options) {
            if (self.options.y_axis_element) {
                self.y_axis_element = self.options.y_axis_element;
            } else {
                if (!target || (!target.length && !target.nodeName)) {
                    if (options) throw new Error("Invalid element supplied as y-axis"); 
                    if (!self.$group_el) throw new Error("Either y-axis target element or group $group.el must be supplied");
                    options = target;
                    // create target
                    self.$group_el.append("<div id='group_y_axis'></div>");
                    target = self.$group_el.find('#group_y_axis')[0];
                }
                self.y_axis_element = target;
                self.y_axis_width = self.options.y_axis_width || 30;
            }
            init_y_axis();
        };

        self.convert_data_series_to_data_grouped = function() {
            if (!self.data_series)
                // if not using group_url, this is not applicable
                return;
            
            _data_grouped = {};
            if (!self.nodeDataHolder.nodes_data)
                self.nodeDataHolder.nodes_data = {};
            _.each(self.data_series, function(nodesToValue, sensorId) {
                _.each(nodesToValue, function(sensorVal, nodeId) {
                    group_id = self.get_node_data(nodeId).group_id;
                    
                    map_at_group = _data_grouped[group_id];
                    if (!map_at_group) _data_grouped[group_id]=map_at_group={};
                    map_at_sensor = map_at_group[sensorId];
                    if (!map_at_sensor) map_at_group[sensorId]=map_at_sensor={};
                    
                    map_at_sensor[nodeId] = sensorVal;
                });
            });
            self.data_grouped = _data_grouped;
        },
        
        self.notify_data_changed_listeners = function() {
            self.updateGroupsFromData();
            self.on_data();
            _.each(bgu_graphs, function(g_g, g_id) {
                if (self.data_grouped[g_id])
                    g_g.on_data(self.data_grouped[g_id]);
            } );            
        },
        self.notify_data_changed_listeners_debounced = _.debounce(self.notify_data_changed_listeners, 500);
        
        self.convert_and_notify_debounced = _.debounce(function() { 
            self.convert_data_series_to_data_grouped(); 
            self.notify_data_changed_listeners();
        }, 500);
        
        self.reloadGroupUrlData = function(options) {
            $.ajax({
                url: self.group_url,
                success: function(_data_series) {
                  if (self.view && self.view.viewIsClosed) 
                      return;
                  self.data_series = _data_series;
                  self.convert_and_notify_debounced();
                  if (options && options.success)
                      options.success(self.data_series);
                },
                error: function(data) {
                    log("ERROR loading ajax");
                    if (options && options.error)
                        options.error(data);
                }
              });
        };
        
        var reset_y_axis = function() {
            if (self.y_axis) {
                $(self.y_axis.element).remove();
                self.y_axis = null;
            }
        };
        
        self.updateGroupsFromData = function() {
            all_nodes = _.reduce( _.values(self.data_grouped), function(memo, obj) { 
                // within each group, looking at the <sensor: <nodes-to-values-map>> map 
                return _.union(memo, _.reduce( _.values(obj), function(memo2, obj2) {
                    // here, looking at the <nodes: value> map for each sensor
                    return _.union(memo2, _.keys(obj2)) }, []));
                }, []);
            all_groups = _.compact(_.uniq(_.values(_.map(all_nodes, function(it) {
                return self.get_node_data(it).group_id; }))));
//          log("groups are: "+all_groups);

            if (!self.nodeDataHolder.groups_data)
                self.nodeDataHolder.groups_data = {};
            
            if (! self.$group_el) throw new Error("Must set a $group_el to have groups automatically created");
            
            if (self.$group_el.find('.one_group').length==0) {
                // no blocks yet; reset
                if (!_.size(self.data_grouped)) {
                    self.$group_el.find('.message').remove();
                    self.$group_el.append('<div class="message">No data available</div>');
                    return;
                }
                // remove any error/other messages, and set up the graph
                self.$group_el.find('.message').remove();
                reset_y_axis();
            }
            
            for (var group_id in self.data_grouped) {
                if (self.$group_el.find("#"+_.escape(group_id)+".one_group").length) {
//                    log("already added "+group_id);
                    continue;
                }
                group_details = self.nodeDataHolder.groups_data[group_id];
                
                group_name = group_id;
                if (group_details && group_details.name) group_name = group_details.name;
                
//                log("adding "+group_id);
                self.addGraph({id: group_id, group_name: group_name });
            }
            
            self.$group_el.find(".one_group").each(function(index, it) {
                var group_id = it.id;
                if (!self.data_grouped[group_id]) {
//                    log("removing "+group_id)
                    delete graphs[group_id];
                    $(it).remove();
                    if (self.y_axis && self.y_axis.graph && self.y_axis.graph.brooklyn_id === group_id) {
                        reset_y_axis();
                    }
                }
            });
            if (!self.y_axis)
                init_y_axis();
        };
        
        self.get_node_data = function(nodeId) {
            if (self.nodeDataHolder.nodes_data==null)
                self.nodeDataHolder.nodes_data = {};
            node_data = self.nodeDataHolder.nodes_data[nodeId];
            if  (node_data && node_data.group_id)
                return node_data;
            
            if (!self.nodeDataHolder.nodes_list)
                self.nodeDataHolder.nodes_list = [];
            if (!_.contains(self.nodeDataHolder.nodes_list, nodeId)) {
                // we've tried looking for it, don't try again
                // (it may already be loading, in which case its callback should trigger reloads)
                self.nodeDataHolder.nodes_list.push(nodeId);
                self.load_node_data(self.nodeDataHolder, nodeId, self.convert_and_notify_debounced);
                node_data = self.nodeDataHolder.nodes_data[nodeId];
                if (node_data && node_data.group_id) 
                    return node_data;
            }
            
            return { name: nodeId, group_id: "unknown" };
        };
        
        /* load_node_data should be function taking a holder and an id, 
         * and ensuring that:
         * * the node_data map in the nodeDataHolder contains,
         *   against each node id key, a map containing the node "name" and "group_id";
         * * the group_data map maps each group_id to a map containing the group "name" */ 
        self.load_node_data = self.options.load_node_data || function(nodeDataHolder, nodeId, callback) {
            nodeDataHolder.nodes_data[nodeId] = { name: nodeId, group_id: 'default' };
            if (!nodeDataHolder.groups_data) nodeDataHolder.groups_data={};
            if (!nodeDataHolder.groups_data['default'])
                nodeDataHolder.groups_data['default'] = { name: "Default" };
            log("TODO faked group id and name info for node "+nodeId);
            log(nodeId);
            
            if (callback) callback();
        };
        
        self.reloadAndQueue = function() {
            self.reloadGroupUrlData({ success: function() { setTimeout(self.reloadAndQueue, 3000); } });
        };
        
        self.start = function() {
            if (!self.group_url) throw new Error("Start only applies with group_url");
            self.reloadAndQueue();
        };
        
    };

    /** returns a load_node_data function which gets details for individual nodes from the given URL;
     * optionally additional nodes_data and groups_data info can be supplied */
    GraphUtil.new_load_node_data_function_from_node_id_url = function(url_prefix_for_node, optional_extra_properties) {
        var init = _.once(function(nodeDataHolder) { _.extend(nodeDataHolder, optional_extra_properties); });
        return function(nodeDataHolder, nodeId, callback) {
            if (optional_extra_properties) init(nodeDataHolder);
            
            $.ajax({url: url_prefix_for_node+nodeId, 
                success: function(data) {
                    nodeDataHolder.nodes_data[nodeId] = data;
                    if (data.group_id) {
                        if (!nodeDataHolder.groups_data) nodeDataHolder.groups_data = {};
                        if (!nodeDataHolder.groups_data[data.group_id]) nodeDataHolder.groups_data[data.group_id] = {}
                        if (data.group_name) nodeDataHolder.groups_data[data.group_id]['name'] = data.group_name;
                        // TODO some type of priority for sequencing?
                    }
                    if (callback) callback();
                },
                error: function(x) { log("ERROR reading informative node data (ignoring): "+x); }
            });
        };
    }
    
    return GraphUtil;
});

