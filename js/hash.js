/* global hash object */
var hash = {
	_deviceReadyDeferred : null,
	_jqmReadyDeferred : null,
	_isDeviceReady : false,
	_isJqmReady : false,

	/*
	 * Compare contacts against known users using API
	 * Save known users to our local datastore ( for faster retrieval later )
	 *
	 */
	find_users : function(args) {
		if(!args.contacts) {
			throw "contacts argument is required";
		}

		var all_phone_numbers = new Array();
		for(var i = 0 ; i < args.contacts.length; i++) {
		    if ( args.contacts[i].phoneNumbers ) {
    			for(var j = 0; j < args.contacts[i].phoneNumbers.length; j++) {
    				all_phone_numbers.push(args.contacts[i].phoneNumbers[j].value);
    			}
			}
		}
		

		/* todo */
		setTimeout(function() {

			if(args.onsuccess) {
				args.onsuccess(args.contacts);
			} else {
				if(args.onerror) {
					args.onerror();
				}
			}
		}, 3000);
	},
};
