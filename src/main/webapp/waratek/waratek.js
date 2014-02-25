define([
    "underscore", "jquery", "backbone", "brooklyn-graph-utils",  
    "text!../waratek/waratek-dash.html","text!../waratek/app-loz.html",
    "bootstrap"
], function (_, $, Backbone, BGU,
        PageHtml, AppLozHtml) {

    var refresh_enabled = true;
    var all_apps_fetch_frequency = !refresh_enabled ? -1 : 5*1000;
    var infra_usage_frequency = !refresh_enabled ? -1 : 5*1000;
    var infra_relist_frequency = !refresh_enabled ? -1 : 15*1000;
    
    var view = Backbone.View.extend({
        tagName:"div",
        events:{
            'click .app-loz': 'selectApp',
            'click .add_app': 'on_click_add_app',
            'click .open_app_ops': 'on_click_open_app_ops',
        },
        
        initialize:function () {
            _.bindAll(this);
            
            if (this.options.path && this.options.path.startsWith('mock')) {
                this.mock_mode = true;
                this.options.path = this.options.path.slice(5);
            }
        },
        
        beforeClose:function () {
        },

        on_click_add_app: function() {
            window.open("waratek/redirect/add_application");
        },
        on_click_open_app_ops: function() {
            var id = this.$("#application-box").attr('data-app-id');
            window.open("waratek/redirect/#v1/applications/"+id);
        },
        
        selectApp: function(event) {
            this.options.path = $(event.currentTarget).attr('id');
            $('.app-loz').removeClass('selected');
            $(event.currentTarget).addClass('selected');
            this.renderAppBox();
        },
        deselectApp: function(event) {
            this.options.path = '';
            $('.app-loz').removeClass('selected');
            this.renderAppBox();
        },

        safe: function(f, name) {
            try {
                f();
            } catch (e) {
                log("ERROR in call to "+(name || f) );
                log(e.stack);
            }
        },
        
        render:function () {
            this.$el.html(_.template(PageHtml, {}));

            this.safe(this.renderAppsBox, "render apps");
            this.safe(this.renderAppBox, "render app");
            this.safe(this.renderInfraBox, "render infra");
            
            return this;
        },
        
        renderAppsBox: function() {
            this.$(".app-header").html("Applications");

            main = this.$("#applications-box .app-main");
            info = this.$("#applications-box .app-info");

            main.html(_.template(AppLozHtml, { custom: {header: "", message: "Loading..." } }));
            info.html('');
            info.append("<b><span id='app_count'>.?.</span></b> applications<br/>");
            info.append("<br/>");
            info.append("<div id='blueprint_count_line'><b><span id='blueprint_count'>.?.</span></b> blueprints<br/><br/></div>");
            info.append("<button class='add_app'>Add Application</button>");

            this.reloadApps();
        },
        
        reloadApps: function() {
            var that = this;
            $.ajax({
                url: 'waratek/apps' + (this.mock_mode ? '/mock' : ''),
                success: function(data) {
                  if (that.viewIsClosed) 
                      return;

                  var first_load = (that.apps===undefined);
                  that.apps = data;
                  that.relistApps();
                  if (all_apps_fetch_frequency>0)
                      setTimeout(that.reloadApps, all_apps_fetch_frequency);
                  
                  if (first_load) 
                      // in case an app needs loading, e.g. specified with waratek/app_id
                      that.renderAppBox();
                },
                error: function(data) {
                    that.$("#applications-box .app-main").html(_.template(AppLozHtml, { custom: {header: "ERROR", message: "Application data not available" } }));
                }
              });
            
            if (this.mock_mode) {
                that.$("#applications-box .app-info #blueprint_count").html("42");
                that.$("#applications-box .app-info #blueprint_count_line").show();                
            } else {
              $.ajax({
                url: 'waratek/catalog/apps',
                success: function(data) {
                  if (that.viewIsClosed) return;
                  that.$("#applications-box .app-info #blueprint_count").html(_.size(data));
                  that.$("#applications-box .app-info #blueprint_count_line").show();
                },
                error: function(data) {
                  that.$("#applications-box .app-info #blueprint_count_line").hide();
                }                
              });
            }
        },
        
        relistApps: function() {
//            $('.app-loz').attr('app_id')
            var that = this;
            var main = that.$("#applications-box .app-main");
            
            if (this.apps.length==0) {
                // no apps
                if (main.find('.app-loz.no-apps#custom').length) {
                    // already saying no apps
                    return;
                }
                main.html(_.template(AppLozHtml, { custom: {header: "NO APPS", message: "No applications currently running." } }));
                
            } else {
                if (main.find('.app-loz#custom').length) {
                    // had warning/etc previously, clear it
                    main.html('');
                }

                var updated_apps=[];
                
                for (var app_id in this.apps) {
                    updated_apps.push(app_id);
                    var app_details = this.apps[app_id];
                    var existing = main.find('.app-loz#'+app_id);
                    template_app_details = _.extend( { app_id: app_id }, app_details );
                    // use controller to make icons available
                    if (template_app_details.icon && template_app_details.icon.startsWith('/v1')) 
                        template_app_details = _.extend( template_app_details, { icon: "main/brooklyn/app/"+app_id+"/icon" } );
                    var tpl = _.template(AppLozHtml, template_app_details );
                    if (existing.length) {
                        var new_html = $(tpl).html();
                        if (existing.html()==new_html) {
//                            log("no update to "+app_id);
                        } else {
//                            log("updating "+app_id);
                            // just change body, not header, as styles may have been set
                            existing.html(new_html);
                        }
                    } else {
//                        log("adding "+app_id);
                        main.append(tpl);
                    }
                }
                
                // and remove stale apps
                main.find('.app-loz').each(function(idx,it) {
                    if (!_.contains(updated_apps, it['id'])) 
                        $(it).remove();
                });
            }
            
            that.$("#applications-box .app-info #app_count").html(_.size(this.apps));
        },
        
        
        

        renderAppBox: function() {
            var app_id = null;
            
            if (this.options.path) {
                paths = _.compact(this.options.path.split('/'));
                if (paths[0]=='waratek') paths = paths.slice(1);
                app_id = paths[0];
            }
            
            if (!app_id) {
                this.$("#application-box").hide().html('No app selected.');
                return;
            }
            
            paths = paths.slice(1);

            var that = this;

            if (!this.apps) {
                log("no apps loaded yet, not showing app "+app_id);
                // TODO - indicate not yet loaded
                return;
            }
            
            app = this.apps[app_id];
            type = app ? app.type : null;
            mock_app_id = (app && app.mock_app_id) ? app.mock_app_id : null;
            
            if (type === 'javawebdemo') {
                require(["apps/javawebdemo/status/status.js"], function(AppStatusView) {
                    var view = new AppStatusView({
                        router: that,
                        appId: mock_app_id || app_id,
                        app_back_function: that.deselectApp 
                    });
                    that.$("#application-box").html( view.render().el ).show();
                    that.$("#application-box").attr('data-app-id', mock_app_id || app_id);
                    that.$("#application-box #summary_footer").html('<br/><button class="open_app_ops">Open Ops Console</button>').show();
                });
            } else {
                // TODO general status page?
                that.$("#application-box").html("<center>APP "+app_id+" of type "+app.type+": detailed status not supported").show();
                setTimeout(that.deselectApp, 3000);
            }
        },
        
        
        
        renderInfraBox: function() {
            var that = this;
            var info = this.$("#infrastructure-box .box-info");
            info.html('');
            info.append("<b><span id='env_count'>.?.</span></b> environments<br/>");
            info.append("<br/>");
            info.append("<b><span id='servers_used_count'>.?.</span></b> servers used<br/>");
            info.append("<b><span id='memory_used_percent'>.?.</span>%</b> memory used<br/>");
            info.append("<b><span id='cpu_used_percent'>.?.</span>%</b> CPU used<br/>");
            info.append("<br/>");
            info.append("<a href='mailto:bob@johnson.com'><button>Add Environment</button></a>");
            
            var templateDataProvider = function() { return [
                { data: [{x:0,y:0}], name: 'used',  color: '#082C54', },
                { data: [{x:0,y:0}], name: 'spare', color: '#6FC5F0', },
            ]; };
            
            var $infra = this.$("#infrastructure-box .box-full");
            $infra.css('text-align', 'center');
            $infra.append('<div style="display: inline-block; margin-top: 10px;" class="_infra_main"></div>');
            $infra = $infra.children();
            
            this.cloud_graphs = new BGU.StatusBarGraphGroup(this, 
                    templateDataProvider,
                    {
                        $group_el: $infra, 
                        total_width: 400,
                        min_bar_width: 15,
                        height: 160,
                        frequency: infra_usage_frequency,
                        data_callbacks: [ that.refreshInfraInfo ]
                    });
            
            var super_setWidth = this.cloud_graphs.setWidth;
            this.cloud_graphs.setWidth = function(id, width) {
                var graph_wrapper = $infra.find('#'+_.escape(id));
                var changed = graph_wrapper.css('width') && graph_wrapper.css('width') != width+"px";
//                log("setting width "+id+" to "+width+" -- "+changed);
                graph_wrapper.css('width', width);
                super_setWidth(id, width, changed);
            };
            
            this.reloadInfra();
        },
        
        reloadInfra: function() {
            var that = this;
            $.ajax({
                url: 'waratek/clouds' + (this.mock_mode ? '/mock' : ''),
                success: function(data) {
                  if (that.viewIsClosed) 
                      return;

                  that.infras = data;
                  that.relistInfra();
                  if (infra_relist_frequency>0)
                      setTimeout(that.reloadInfra, infra_relist_frequency);
                },
                error: function(data) {
                    that.$("._infra_main").html('<div class="infra_message">Infrastructure details not available</div>');
                }
              });
        },
        relistInfra: function() {
            var clouds = this.infras;
            var $group_el = this.$("._infra_main");
            
            if ($group_el.find('.one_group').length==0) {
                // no blocks yet; reset
                if (!_.size(clouds)) {
                    $group_el.html('<div class="infra_message">No infrastructures configured.</div>');
                    return;
                }
                // remove any error/other messages, and set up the graph
//                log("resetting infra");
                $group_el.html('');
                this.cloud_graphs.useYAxis();
            }
            
            for (var cloud in clouds) {
                cloud_details = clouds[cloud];
                if (this.$("#"+_.escape(cloud)+".one_group").length) {
//                    log("already added "+cloud);
                    continue;
                }
//                log("adding "+cloud);
                this.cloud_graphs.addGraph({ 
                    url: cloud_details.url || 'waratek/'+cloud+'/usage',
                    id: cloud, 
                    group_name: cloud_details.name });
            }
            
            $group_el.find(".one_group").each(function(index, it) {
                var cloud = it.id;
                if (!clouds[cloud]) {
                    $(it).remove();
                }
            });
        },
        
        refreshInfraInfo: function(graph) {
            data = graph.data_grouped;
            
            servers_used = _.reduce( data, function(memo, obj) { return memo + obj.used.servers; }, 0 );
            mem_used = _.reduce( data, function(memo, obj) { return memo + obj.used.ram; }, 0 );
            mem_spare = _.reduce( data, function(memo, obj) { return memo + obj.spare.ram; }, 0 );
            cpu_used = _.reduce( data, function(memo, obj) { return memo + obj.used.cpu_ghz; }, 0 );
            cpu_spare = _.reduce( data, function(memo, obj) { return memo + obj.spare.cpu_ghz; }, 0 );
            
            this.$('#infrastructure-box #env_count').html(_.size(data));
            this.$('#infrastructure-box #servers_used_count').html(servers_used);
            this.$('#infrastructure-box #memory_used_percent').html(Math.round( (100*mem_used)/(mem_used+mem_spare) ));
            this.$('#infrastructure-box #cpu_used_percent').html(Math.round( (100*cpu_used)/(cpu_used+cpu_spare) ));
        },
        
    });

    return view;
});
