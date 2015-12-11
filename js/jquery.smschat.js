(function($) {

    // here it goes!
    $.fn.smschat = function(method) {

        // public methods
        // to keep the $.fn namespace uncluttered, collect all of the plugin's methods in an object literal and call
        // them by passing the string name of the method to the plugin
        //
        // public methods can be called as
        // element.smschat('methodName', arg1, arg2, ... argn)
        // where "element" is the element the plugin is attached to, "smschat" is the name of your plugin and
        // "methodName" is the name of a function available in the "methods" object below; arg1 ... argn are arguments
        // to be passed to the method
        //
        // or, from inside the plugin:
        // methods.methodName(arg1, arg2, ... argn)
        // where "methodName" is the name of a function available in the "methods" object below
        var methods = {

            // this the constructor method that gets called when the object is created
            init : function(options) {
                this.smschat.settings = $.extend({}, this.smschat.defaults, options);

                // iterate through all the DOM elements we are attaching the plugin to
                return this.each(function() {
                    var $element = $(this), // reference to the jQuery version of the current DOM element
                    element = this;     // reference to the actual DOM element
                    $element.data('smschat_messages', $element.find('.smschat-messages').first());
                });
            },
            
            
            appendRight: function(html) {
            	var newnode = helpers.create_msg_bubble(html, false);
            	var msgelm = $(this).data('smschat_messages');
		msgelm.append(newnode);
		$.mobile.silentScroll(newnode.position().top);
            },
            
            appendLeft: function(html) {
            	var newnode = helpers.create_msg_bubble(html, true);
            	var msgelm = $(this).data('smschat_messages');
		msgelm.append(newnode);
		//msgelm.scrollTop( newnode.position().top + newnode.height() );
		$.mobile.silentScroll(newnode.position().top);
            }
        }

        // private methods
        // these methods can be called only from inside the plugin
        //
        // private methods can be called as
        // helpers.methodName(arg1, arg2, ... argn)
        // where "methodName" is the name of a function available in the "helpers" object below; arg1 ... argn are
        // arguments to be passed to the method
        var helpers = {

            // a private method. for demonstration purposes only - remove it!
            create_msg_bubble: function(content, left) {
                var message = $("<div></div>");
                message.addClass('smschat-message');
                
                var bubblewrap = $("<div></div>");
                bubblewrap.addClass('smschat-bubblewrap');                
                var bubble = $("<div></div>");
                if ( left ) {
                	bubble.addClass('smschat-bubble-left');
                	bubblewrap.addClass('smschat-bubblewrap-left');
                }
                else {
                	bubble.addClass('smschat-bubble-right');
                	bubblewrap.addClass('smschat-bubblewrap-right');
                }
                
                bubble.append(content.replace(/\n/g, "<br />"));
                
                bubblewrap.append(bubble);
                message.append(bubblewrap);
                
		return message;
            }

        }

        // if a method as the given argument exists
        if (methods[method]) {

            // call the respective method
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));

        // if an object is given as method OR nothing is given as argument
        } else if (typeof method === 'object' || !method) {

            // call the initialization method
            return methods.init.apply(this, arguments);

        // otherwise
        } else {

            // trigger an error
            $.error( 'Method "' +  method + '" does not exist in smschat plugin!');

        }

    }

    // plugin's default options
    $.fn.smschat.defaults = {

        foo: 'bar'

    }

    // this will hold the merged default and user-provided options
    // you will have access to these options like:
    // this.smschat.settings.propertyName from inside the plugin or
    // element.smschat.settings.propertyName from outside the plugin, where "element" is the element the
    // plugin is attached to;
    $.fn.smschat.settings = {}

})(jQuery);
