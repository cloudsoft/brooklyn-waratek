define([
    'underscore'
], function (_) {

    var Util = {};

    /**
     * @return {string} empty string if s is null or undefined, otherwise result of _.escape(s)
     */
    Util.escape = function (s) {
        if (s == undefined || s == null) return "";
        return _.escape(s);
    };

    function isWholeNumber(v) {
        return (Math.abs(Math.round(v) - v) < 0.000000000001);
    }

    Util.roundIfNumberToNumDecimalPlaces = function (v, mantissa) {
        if (!_.isNumber(v) || mantissa < 0)
            return v;

        if (isWholeNumber(v))
            return Math.round(v);

        var vk = v, xp = 1;
        for (var i=0; i < mantissa; i++) {
            vk *= 10;
            xp *= 10;
            if (isWholeNumber(vk)) {
                return Math.round(vk)/xp;
            }
        }
        return Number(v.toFixed(mantissa));
    };

    Util.toDisplayString = function(data) {
    	var escaped = Util.roundIfNumberToNumDecimalPlaces(data, 4);
    	if (escaped != null) {
    		if (typeof escaped === 'string')
    			escaped = Util.escape(escaped);
    		else
    			escaped = JSON.stringify(escaped);
    	}
    	return escaped;
    };
    if (!String.prototype.trim) {
    	// some older javascripts do not support 'trim' (including jasmine spec runner) so let's define it
    	String.prototype.trim=function(){return this.replace(/^\s+|\s+$/g, '');};
    }

    if (typeof String.prototype.startsWith != 'function') {
        String.prototype.startsWith = function (str){
            return this.slice(0, str.length) == str;
        };
    }
    if (typeof String.prototype.endsWith != 'function') {
        String.prototype.endsWith = function (str){
            return this.slice(-str.length) == str;
        };
    }

    /**
     * Makes the console API safe to use:
     *  - Stubs missing methods to prevent errors when no console is present.
     *  - Exposes a global `log` function that preserves line numbering and formatting.
     *
     * Idea from https://gist.github.com/bgrins/5108712
     */
    (function () {
        var noop = function () {},
            consoleMethods = [
                'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
                'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
                'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
                'timeStamp', 'trace', 'warn'
            ],
            length = consoleMethods.length,
            console = (window.console = window.console || {});

        while (length--) {
            var method = consoleMethods[length];

            // Only stub undefined methods.
            if (!console[method]) {
                console[method] = noop;
            }
        }

        if (Function.prototype.bind) {
            window.log = Function.prototype.bind.call(console.log, console);
        } else {
            window.log = function () {
                Function.prototype.apply.call(console.log, console, arguments);
            };
        }
    })();

    return Util;

});

