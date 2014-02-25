/*
 * jQuery DataTables fnStandingRedraw plug-in.
 *
 * http://www.datatables.net/plug-ins/api#fnStandingRedraw
 */
define([
    "jquery", "jquery-datatables"
], function($, dataTables) {

$.fn.dataTableExt.oApi.fnStandingRedraw = function(oSettings) {
    if (oSettings.oFeatures.bServerSide === false) {
        var before = oSettings._iDisplayStart;
        oSettings.oApi._fnReDraw(oSettings);
        // iDisplayStart has been reset to zero - so lets change it back
        oSettings._iDisplayStart = before;
        oSettings.oApi._fnCalculateEnd(oSettings);
    }
    // draw the 'current' page
    oSettings.oApi._fnDraw(oSettings);
};


jQuery.fn.dataTableExt.oApi.fnProcessingIndicator = function ( oSettings, onoff )
{
    if( typeof(onoff) == 'undefined' )
    {
        onoff=true;
    }
    this.oApi._fnProcessingDisplay( oSettings, onoff );
};

});