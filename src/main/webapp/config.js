/*
 * set the require.js configuration for your application
 */
require.config({
    /* Give 30s (default is 7s) in case it's a very poor slow network */
    waitSeconds:30,
    
    /* Libraries */
//    baseUrl:"main",
    paths:{
        "jquery":"main/libs/jquery/jquery",
        "underscore":"main/libs/underscore",
        "backbone":"main/libs/backbone",
        "bootstrap":"main/libs/bootstrap/bootstrap",
        "formatJson":"main/libs/json-formatter",
        "jquery-form":"main/libs/jquery/jquery.form",
        "jquery-datatables":"main/libs/jquery/jquery.dataTables",
        "jquery-slideto":"main/libs/jquery/jquery.slideto.min",
        "jquery-wiggle":"main/libs/jquery/jquery.wiggle.min",
        "jquery-ba-bbq":"main/libs/jquery/jquery.ba-bbq.min",
        "moment":"main/libs/moment.min",
        "handlebars":"main/libs/handlebars-1.0.rc.1",
        "brooklyn-utils":"main/brooklyn-utils",
        "brooklyn-graph-utils":"main/brooklyn-graph-utils",
        "datatables-extensions":"main/libs/jquery/dataTables.extensions",
        "d3":"main/libs/d3.v3",
        "rickshaw":"main/libs/rickshaw",
        "gauge":"main/libs/gauge",
        "async":"main/libs/async",  //not explicitly referenced, but needed for google
        "text":"main/libs/text"
    },
    
    shim:{
        "underscore":{ exports:"_" },
        "jquery":{ exports:"$" },
        "backbone":{
            deps:[ "underscore", "jquery" ],
            exports:"Backbone"
        },
        "jquery-datatables": {
            deps: [ "jquery" ]
        },
        "datatables-extensions":{
            deps:[ "jquery", "jquery-datatables" ]
        },
        "jquery-form": { deps: [ "jquery" ] },
        "jquery-slideto": { deps: [ "jquery" ] },
        "jquery-wiggle": { deps: [ "jquery" ] },
        "jquery-ba-bbq": { deps: [ "jquery" ] },
        "handlebars": { deps: [ "jquery" ] },
        "bootstrap": { deps: [ "jquery" ] /* http://stackoverflow.com/questions/9227406/bootstrap-typeerror-undefined-is-not-a-function-has-no-method-tab-when-us */ },
        "rickshaw":{
            deps:[ "d3", "jquery" ],
            exports:"Rickshaw"
        },
    }
});

/*
 * Main application entry point.
 *
 * Inclusion of brooklyn module sets up logging.
 */
require([
    "main/router", "brooklyn-utils", "backbone",
], function (Router) {
    new Router();
    Backbone.history.start();
});
