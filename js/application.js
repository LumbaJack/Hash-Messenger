// steroids.view.navigationBar.show("Hello World");

var PRODUCTION = false;
// change to true for production

var app = {
	_deviceReadyDeferred : null,
	_jqmReadyDeferred : null,
	_isDeviceReady : false,
	_isJqmReady : false,

	active_chat_id : null,
	active_chat_contact : null,
	phoneid: null,
        keypriv: null,
	phonepwd: null,
	
	active_contacts : null,
	contact_photos_synced : false,
	all_contacts : null,


	initialize : function() {
		var self = this;
		
		self.active_contacts = new Array();
        self.all_contacts = new Array();
		
		/*
		 * this isn't working?
		 *
		 this._deviceReadyDeferred = jQuery.Deferred();
		 this._jqmReadyDeferred = jQuery.Deferred();

		 jQuery.when(this._deviceReadyDeferred, this._jqmReadyDeferred).then(function() {
		 onFullyLoaded();
		 });
		 */
		if(self.isCordova()) {
			document.addEventListener("deviceReady", function() {
				self._deviceReady();
			}, false);
		} else {
			self._deviceReady();
		}

		jQuery(document).ready(function() {
			//		this._jqmReadyDeferred.resolve();
			self._isJqmReady = true;
			self._isLoadComplete();
		});
	},
	_deviceReady : function() {
		//this._deviceReadyDeferred.resolve();
		this._isDeviceReady = true;
		this._isLoadComplete();

	},

    _isLoadComplete : function() {
        var self = this;
        if(this._isDeviceReady && this._isJqmReady) {
            if ( app.isCordova() ) {
                //window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function(fileSystem) {
                    onFullyLoaded();
                //}, app.onerror);
            }
            else {
                onFullyLoaded();
            }
        }
    },


    _ios_sync_images : function() {
        // iOS uses different contact image urls each time app loads
        // let's prefetch them here ( we only have to do this once each time app loads )
        db.initdb({
            onsuccess : function() {
                db.get_contacts({
                    onsuccess : function(tx, results) {
                        var rows = results.rows;

                        var options = new ContactFindOptions();
                        options.filter = '';
                        options.multiple = true;
                        var fields = ["id", "displayName", "name", "photos"];
                        // crashes on android unless we have the name fields
                        navigator.contacts.find(fields, function(lcontacts) {
                            if(lcontacts) {
                                for(var i = 0; i < rows.length; i++) {
                                    for(var j = 0; j < lcontacts.length; j++) {
                                        if(lcontacts[j].photos && lcontacts[j].photos.length > 0) {
                                            if(rows.item(i).localid.toString() === lcontacts[j].id.toString()) {
                                                db.update_contact_icon({
                                                    contactid : lcontacts[j].id,
                                                    icon : lcontacts[j].photos[0].value,
                                                    onsuccess : function() {
                                                    }
                                                });

                                            }
                                        }
                                    }
                                }
                            }
                        }, function() {

                        }, options);
                    },
                    onerror : function() {}
                });
            }
        })

    },




	
	isCordova : function() {
		//return true;
		if(document.URL.indexOf('http://') === -1 && document.URL.indexOf('https://') === -1) {
			return true;
		} else {
			return false;
		}
	},
	onerror_sql : function(err) {
		alert("Error processing SQL (" + err.code + ") :  " + err.message + " TODO WE NEED TO CATCH THIS AND LOG IT");
		alert(err.stack);

	},
	onerror : function(err) {
		alert("Error  (" + err.code + ") :  " + err.message +  + " TODO WE NEED TO CATCH THIS AND LOG IT");
        alert(err.stack);
	},
	quit : function() {
		var self = this;
		if(self.isCordova()) {
			if(navigator.app) {
				navigator.app.exitApp();
			} else if(navigator.device) {
				navigator.device.exitApp();
			}
		}

	},



    sync_contacts: function (args) {
        var options = new ContactFindOptions();
        options.filter = '';
        options.multiple = true;
        var fields = ["displayName", "name", "phoneNumbers", "photos"];
        navigator.contacts.find(fields, function(contacts) {
            var c = new Array();
            var contacts_sent = new Array();
            for( i = 0, j = contacts.length; i < j; i++) {
                var contactname = app.get_contact_name_str(contacts[i]);
                if(contacts[i].phoneNumbers != null && contactname != null) {
                    c.push({
                        id : contacts[i].id,
                        phone : contacts[i].phoneNumbers,
                        disp : ''
                    });
                    contacts_sent.push(contacts[i]);
                }
            }
            if ( c.length > 0 ) {
                jQuery.ajax({
                    type : "POST",
                    dataType : 'json',
                    url : "http://23.21.90.204/api/sync_contacts.php",
                    data : {
                        data : JSON.stringify(c)
                    },
                    success : function(data) {
                        for(var i = 0; i < data.length; i++) {
                            for(var j = 0; j < contacts_sent.length; j++) {
                                if ( data[i].id === contacts_sent[j].id ) {
                                    var contactid = contacts_sent[j].id;
                                    data[i].disp = app.get_contact_name_str(contacts_sent[j]);
                                    if ( device.platform === "Android") {
                                        // don't overwrite cached value on iOS
                                        app.get_contact_photo(contacts_sent[j], function(answer) {
                                            if(answer) {
                                                db.update_contact_icon({
                                                    contactid : contactid,
                                                    icon : answer,
                                                    onsuccess : function() {
                                                    }
                                                });
                                            }
                                        });
                                    }

                                }
                            }
                        }

                        db.sync_contacts(data);
                        if ( args && args.onsuccess ) {
                            args.onsuccess();
                        }
                    },
                    error : function(err) {
                        alert("Error " + JSON.stringify(err));
                    }
                });
            }
            else {
                if ( args.onsuccess ) {
                    args.onsuccess();
                }
            }
        }, app.onerror, options);
    },


    
    get_active_contacts : function(args) {
        if ( ! args ) {
            args = {}
        }
        
        _onsuccess = function() {};
        _onerror = app.onerror;
        if ( args.onsuccess ) {
            _onsuccess = args.onsuccess;
        }
        if ( args.onerror ) {
            _onerror = args.onerror;
        }
        
        db.get_contacts({
            userid: args.contactids,
            onsuccess: function(tx, results) {
                var resultobjs = new Array();
                var rows = results.rows;

                for(var i = 0; i < rows.length; i++) {
                    var include = false;
                    if ( args.contactids ) {
                        if ( rows.item(i).userid == args.contactids )  {
                            include = true;
                        }
                    }
                    else {
                        include = true;
                    }
                    if ( include ) {
                        var contactobj = {id: '', displayName: '', icon: '', localName: '', userid: ''}; // looks like a Contact object
                        contactobj.id = rows.item(i).localid;
                        contactobj.displayName = rows.item(i).displayname;
                        contactobj.localName = rows.item(i).localname;
                        contactobj.icon = rows.item(i).icon;
                        contactobj.userid = rows.item(i).userid;
                        resultobjs.push(contactobj);
                    }
                }
                _onsuccess(resultobjs);
            }
            
        });
    },


    fill_all_contacts : function(args) {
        if(!args) {
            args = {};
        }
        _onsuccess = function() {
        };
        _onerror = app.onerror;
        if(args.onsuccess) {
            _onsuccess = args.onsuccess;
        }
        if(args.onerror) {
            _onerror = args.onerror;
        }

        var options = new ContactFindOptions();
        options.filter = '';
        options.multiple = true;
        var fields = ["id", "name", "displayName", "phoneNumbers", "emails", "ims", "photos"];
 
        // crashes on android unless we have the name fields
        navigator.contacts.find(fields, function(lcontacts) {
            app.all_contacts = [];
            for(var i = 0; i < lcontacts.length; i++) {
                var include = false;
                if(args.contactids) {
                    if(lcontacts[i].id == args.contactids) {
                        include = true;
                    }
                } else {
                    include = true;
                }
                if(include) {
                    var contactobj = {
                        id : '',
                        displayName : '',
                        icon : '',
                        localName : '',
                        userid : '',
                        phoneNumbers: [],
                        emails: []
                    };
                    // looks like a Contact object
                    contactobj.id = lcontacts[i].id;
                    contactobj.displayName = app.get_contact_name_str(lcontacts[i]);
                    contactobj.localName = app.get_contact_name_str(lcontacts[i]);
                    contactobj.icon = '';
                    if (lcontacts[i].photos && lcontacts[i].photos.length > 0) {
                        contactobj.icon = lcontacts[i].photos[0].value;
                    }
                    contactobj.phoneNumbers = lcontacts[i].phoneNumbers;
                    contactobj.emails = lcontacts[i].emails;
                    contactobj.userid = lcontacts[i].id;
                    app.all_contacts.push(contactobj);
                }
            }
            _onsuccess(app.all_contacts);
        }, app.onerror, options);
    },

	get_contact_name_str: function(contact) {
        if ( contact.localName && contact.localName != null ) {
            return contact.localName;
        }
	    if ( contact.displayName != null ) {
	        return contact.displayName;
	    }
		return contact.name.formatted;
	},
	
	get_valid_photo : function(url,callback){
        var img = new Image();
        img.onload = function() {
            callback(url);
        };

        img.onerror = function(err) {
            callback("images/default-user.png");
        };

        img.src = url;
    },



    get_contact_photo : function(contactobj,callback) {
        if(contactobj.icon) {
            app.get_valid_photo(contactobj.icon, function(answer) {
                if(answer) {
                    callback(answer);
                }
            });
        }
        else {
            callback("images/default-user.png");
        }
    },


    
	create_contact_listitem : function (contactobj) {
	    var self = this;
		var contactname = app.get_contact_name_str(contactobj);
		if ( contactname === null ) {
		  return null;
		}
		var lielm = jQuery('<li></li>');
		
		
		var aelm = jQuery('<a href="#" data-contact-id="' + contactobj.userid + '" class="start-chat"></a>');
		var thumbnail = jQuery('<img class="contact-photo" ></img>');
		var h3 = jQuery('<h3>' + contactname + '(' + contactobj.userid + ')</h3>');
		var subtitlestr = '';

		if ( contactobj.phoneNumbers ) {
		    var phonenums = new Array();
		    for(var i = 0; i < contactobj.phoneNumbers.length; i++) {
		        phonenums.push(contactobj.phoneNumbers[i].value);
		    }
            subtitlestr = phonenums.join(',');
        }
        var subtitle = jQuery('<p>' + subtitlestr + '</p>');
    

		aelm.append(thumbnail);
		aelm.append(h3);
		aelm.append(subtitle);

		lielm.append(aelm);

        app.get_contact_photo(contactobj, function(answer) {
            if(answer) {
                thumbnail.attr('src', answer);
            }
        });

		return lielm;
	},

    create_contact_selitem : function (contactobj) {
        var self = this;
        var contactname = app.get_contact_name_str(contactobj);
        if ( contactname === null ) {
          return null;
        }
        var lielm = jQuery('<li></li>');
        
        
        var aelm = jQuery('<a href="#" data-contact-id="' + contactobj.userid + '" class="start-chat"></a>');
        var thumbnail = jQuery('<img class="contact-photo" ></img>');
        var h3 = jQuery('<h3>' + contactname + '(' + contactobj.userid + ')</h3>');
        var subtitlestr = '';

        if ( contactobj.phoneNumbers && contactobj.phoneNumbers.length > 0) {
            aelm.attr('data-contact-tele', contactobj.phoneNumbers[0].value);
            subtitlestr = contactobj.phoneNumbers[0].value;
        }
        var subtitle = jQuery('<p>' + subtitlestr + '</p>');

        if ( contactobj.emails && contactobj.emails.length > 0) {
            aelm.attr('data-contact-email', contactobj.emails[0].value);
        }
        
        aelm.append(thumbnail);
        aelm.append(h3);
        aelm.append(subtitle);

        lielm.append(aelm);
        aelm.click(function(event) {
            if (jQuery(this).hasClass('selected')) {
                jQuery(this).removeClass('selected');
            } else {
                jQuery(this).addClass('selected');
            }
        });
        
        app.get_contact_photo(contactobj, function(answer) {
            if(answer) {
                thumbnail.attr('src', answer);
            }
        });

        return lielm;
    },

	
    create_invite_listitem : function () {
        var self = this;

        var lielm = jQuery('<li></li>');
        
        
        var aelm = jQuery('<a href="#" class="invite"></a>');
        var h3 = jQuery('<h3>Invite Friends to try #</h3>');
        var subtitlestr = '';
        var subtitle = jQuery('<p>' + subtitlestr + '</p>');

        aelm.append(h3);
        aelm.append(subtitle);
        aelm.click(function() {
            jQuery.mobile.changePage( "invite.html", { changeHash: true });
        });
        lielm.append(aelm);
        return lielm;
    },


    

    start_chat_onclick: function(event) {
        var contactid = jQuery(this).attr("data-contact-id");
        app.active_chat_id = contactid;

        db.create_chat({
            contactid : app.active_chat_id,
            onsuccess : function() {
                jQuery.mobile.changePage('chat.html', {
                    transition : "slide",
                    changeHash : true
                });
            },
            oncomplete : function() {

            },
            onerror : app.onerror_sql
        });
    },



	bind_contacts_to_list: function(contacts, ulsel, options) {
		var ulelm = jQuery(ulsel);
		ulelm.empty();
		for(var i = 0; i < contacts.length; i++) {
			var lielm = app.create_contact_listitem(contacts[i]);
			ulelm.append(lielm);
		}
		
		if ( !options ) {
		    options = {};
		}


       if ( options.invite ) {
           ulelm.append(jQuery('<li data-role="list-divider"></li>'));
           ulelm.append(app.create_invite_listitem());
       }

        var _onclick = app.start_chat_onclick;
        
        if ( options.onclick ) {
            _onclick = options.onclick;
        }
		ulelm.listview('refresh');
		jQuery('.start-chat').click(_onclick);
	},


    bind_contacts_invite_to_list: function(contacts, sel, options) {

        
        var ulelm = jQuery(sel);
        ulelm.empty();

        for(var i = 0; i < contacts.length; i++) {
            var lielm = app.create_contact_selitem(contacts[i]);
            ulelm.append(lielm);
        }

        
        if ( !options ) {
            options = {};
        }
        ulelm.listview('refresh');
    },
    
    sync_contacts_scroller : function() {
        app.sync_contacts({
            onsuccess : function() {
                app.get_active_contacts({
                    onsuccess: function(results) {
                        app.active_contacts = results;
                        if ( app.active_contacts.length > 0) {
                            app.bind_contacts_to_list(app.active_contacts, '#contacts-ul', {invite: true});
                            jQuery('#contacts-filter').show();
                        }
                        else {
                            // we have fresh data and still no contacts so give up
                            // TODO at this point we should probably
                            // redirect them to an invite list where the can select
                            // any  of their contacts and invite them to use #chat
                            alert("TODO: you need to invite your frields to use #messenger");
                        }
                   }
               });
            }
        });
    },
    
	escape_regexp : function(string){
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    },

	successHandler: function(result) {
		alert('Callback Success! Result = '+result)
	},
	errorHandler:function(error) {
		alert(error);
	},
	onNotificationGCM: function(e) {
		switch( e.event )
		{
		case 'registered':
                if ( e.regid.length > 0 )
                {
                    if(!db.ispushreg()) { 
                          jQuery.post( "http://23.21.90.204/api/push_reg.php", { user: app.phoneid + "@hashmessenger.com" , id: e.regid}, function() {db.setpushreg()});
                    }
                }
                break;

		case 'message':
			if(e.foreground) {
				alert("Play soun");
			} else {
				if(e.coldstart) {
					alert("ColdStart");
				} else {
					alert("Background");
				}
			}
			// this is the actual push notification. its format depends on the data model from the push server
			alert('message = '+e.message+' msgcnt = '+e.msgcnt);
			break;
		case 'error':
			alert('GCM error = '+e.msg);
			break;
		default:
			alert('An unknown GCM event has occurred');
			break;
		}
	},
	
	show_panel : function(panelsel) {
	    jQuery('#home-page_panels').children().hide();
	    jQuery(panelsel).show();
	},
	

    capture_photo : function(args) {
        var capture_opts = {
            quality : 50,
            sourceType : Camera.PictureSourceType.SAVEDPHOTOALBUM,
            //destinationType : Camera.DestinationType.DATA_URL,
            destinationType : Camera.DestinationType.FILE_URI,
            mediaType : Camera.MediaType.PICTURE
        };
        
        var prompt_div = jQuery('<div/>')
            .attr('id', 'capture_photo_prompt')
            .attr('data-role', 'popup')
            .attr('data-theme', 'a')
            .attr('data-dismissible', 'false')
        ;
        
        var ul_div = jQuery('<ul/>')
            .attr('data-role', 'listview')
            .attr('data-inset', 'true')
            .attr('data-theme', 'a')
        ;
        
        prompt_div.append(ul_div);

        var take_photo_btn = jQuery('<a>Take a new photo</a>')
            .attr('href', '#')
            .attr('data-role', 'button')
            .attr('data-inline', 'true')
            .attr('data-theme', 'a')
            .addClass('ui-btn')
        ;
        
        take_photo_btn.click(function() {
            capture_opts.sourceType = Camera.PictureSourceType.CAMERA;
            navigator.camera.getPicture(function(imagedata) {
                prompt_div.popup("close");
                jQuery.mobile.activePage.remove( prompt_div );
                args.onsuccess(imagedata);
            }, app.onerror, capture_opts);             
        });

        var existing_photo_btn = jQuery('<a>Select existing photo</a>')
            .attr('href', '#')
            .attr('data-role', 'button')
            .attr('data-inline', 'true')
            .attr('data-theme', 'a')
            .addClass('ui-btn')
        ;
        
        existing_photo_btn.click(function() {
            capture_opts.sourceType = Camera.PictureSourceType.PHOTOLIBRARY;
            navigator.camera.getPicture(function(imagedata) {
                prompt_div.popup("close");
                jQuery.mobile.activePage.remove( prompt_div );
                args.onsuccess(imagedata);
            }, app.onerror, capture_opts); 
        });

        var cancel_btn = jQuery('<a>Cancel</a>')
            .attr('href', '#')
            .attr('data-role', 'button')
            .attr('data-inline', 'true')
            .attr('data-theme', 'c')
            .attr('data-rel', 'back')
            .css('margin-top', '4em')
            .addClass('ui-btn')
        ;
        
        ul_div.append(
            jQuery('<li></li>').append(take_photo_btn)
        );        

        ul_div.append(
            jQuery('<li></li>').append(existing_photo_btn)
        );        

        ul_div.append(
            jQuery('<li></li>').append(cancel_btn)
        );



      jQuery.mobile.activePage.append( prompt_div );
      prompt_div.popup();
      prompt_div.popup('open');
    }


};



// onError: Failed to get the contacts

function onContactError(contactError) {
	alert('onError!');
}


jQuery(document).on("pageinit", "#eula-page", function() {
	jQuery('#accept-btn').click(function() {
		db.set_eula_accepted(true);
		jQuery.mobile.changePage('signup.html', {transition: "flow", changeHash: false });
	});
});



jQuery(document).on("pagecreate", "#home-page", function() {
    jQuery(document).on("swipeleft swiperight", "#home-page", function(e) {
        // We check if there is no open panel on the page because otherwise
        // a swipe to close the left panel would also open the right panel (and v.v.).
        // We do this by checking the data that the framework stores on the page element (panel: open).
        if(jQuery.mobile.activePage.jqmData("panel") !== "open") {
            if(e.type === "swipeleft") {
                jQuery("#home-page_right-panel").panel("open");
            } else if(e.type === "swiperight") {
                jQuery("#home-page_left-panel").panel("open");
            }
        }
    });
});


jQuery(document).on("pageinit", "#home-page", function() {

    if ( device.platform === "iOS" && app.contact_photos_synced === false ) {
        app._ios_sync_images();
        app.contact_photos_synced = true;
    }

    function loadChats() {
        var chats = db.get_chats({
            onsuccess : function(tx, results) {
                rows = results.rows;
                if(rows.length === 0) {
                    jQuery('#nochats').removeClass('none');
                    jQuery('#with-chats').addClass('none');
                } else {
                    jQuery('#nochats').addClass('none');
                    jQuery('#with-chats').removeClass('none');
                    
                    // make contact-like objects
                    var contacts = new Array();
                    for(var i = 0; i < rows.length; i++) {
                        var contactobj = {id: '', displayName: '', icon: '', localName: '', userid: ''}; // looks like a Contact object
                        contactobj.id = rows.item(i).localid;
                        contactobj.displayName = rows.item(i).displayname;
                        contactobj.localName = rows.item(i).localname;
                        contactobj.userid = rows.item(i).userid;
                        contactobj.icon = rows.item(i).icon;
                        contacts.push(contactobj);
                    }

                    app.bind_contacts_to_list(contacts, '#contacts-chat-ul');

                }
            },
            onerror : app.onerror_sql
        });
    }

    function bindData() {
        db.open();
        db.initdb({
            onsuccess: function() {},
            
            oncomplete : function() {
                loadChats();
            },
            onerror : app.onerror_sql
        });
    }


    jQuery("#newchat").click(function() {
        jQuery.mobile.changePage('contacts.html', {
            transition : "slideup",
            changeHash: true
        });
    });

    jQuery(".panel-link").click(function() {
        app.show_panel('#' + jQuery(this).attr('panel-id'));
        jQuery("#home-page_left-panel").panel("close");
    });
    
    jQuery('.newstatus-li').click(function() {
        jQuery('#home-page_currentstatus').val(jQuery(this).text());
    });

    jQuery("#home_panel-btn").click(function() {
        if(jQuery.mobile.activePage.jqmData("panel") !== "open") {
            jQuery("#home-page_left-panel").panel("open");
        }
    });
    

    
    jQuery('#home_chat-history-btn').change(function() {
        db.set_message_history_pref(jQuery('#home_chat-history-btn').val());
    })
    
    jQuery('#home_gen-keys-btn').click(function() {
        jQuery('#gen_keys_prompt-link').trigger('click');
    });
    
    
    jQuery('#home_profile-image').click(function(){
        app.capture_photo({
            onsuccess: function(imagedata) {
                alert("HERE");
            }
        });
    });




          
    bindData();

});

jQuery(document).on("pageinit", "#gen_keys_prompt", function() {
    jQuery('#gen_keys_prompt-continue_btn').click(function() {
       jQuery.mobile.changePage('gen_keys.html', {transition : "slide", changeHash : false });
    }); 
});


jQuery(document).on("pageinit", "#attach_prompt", function() {
    jQuery('#attach_prompt-capture_new_photo').click(function() {
        var capture_opts = {};
        capture_opts.sourceType = Camera.PictureSourceType.CAMERA;
        navigator.camera.getPicture(function(imagedata) {
            jQuery('#attach_prompt').dialog("close");
            alert("attach image now");
        }, app.onerror, capture_opts);

    });
    
    jQuery('#attach_prompt-capture_existing_photo').click(function() {
        var capture_opts = {};
        capture_opts.sourceType = Camera.PictureSourceType.PHOTOLIBRARY;
        navigator.camera.getPicture(function(imagedata) {
            jQuery('#attach_prompt').dialog("close");
            alert("attach image now");
        }, app.onerror, capture_opts);

    });
});



jQuery(document).on("pageinit", "#signup", function() {
	jQuery("#btoverify").click(function() {
		var tel = jQuery("#countrycode").val() + jQuery('#tel').val();
		jQuery.post("http://23.21.90.204/api/verify.php", { tel: tel } );
		jQuery("#btosms").removeAttr('disabled');
		jQuery("#smscode").removeAttr('disabled');
		jQuery("#smscode").focus();
	});
	jQuery("#btosms").click(function() {
		var tel = jQuery('#tel').val();
		var countycode = jQuery("#countrycode").val();
		var smscode =  jQuery('#smscode').val();
		jQuery.ajax({
			type: "POST",
			dataType: 'json',
			url:  "http://23.21.90.204/api/validate.php",
			data: { cc: countycode, tel:tel, smscode: smscode, device: device.platform },
			success: function (data) {
				if (data.retcode==200) { 
				    app.phoneid = countycode + tel;
				    app.phonepwd = data.pass;
				    xmpp.connect();
				    if ( app.isCordova() ) {
				        pushNotification = window.plugins.pushNotification;
                                        pushNotification.register(app.successHandler, app.errorHandler,{"senderID":"215473675714","ecb":"app.onNotificationGCM"});

                    }
                    jQuery.mobile.changePage('gen_keys.html',{transition : "slide", changeHash : false });
				} else {
					alert("Error sms code invalid");
				}
					
			},
			error: function (data) {
				alert("Error "  +  data.responseText);
			}
		});
	});
});




jQuery(document).on("pageinit", "#gen-keys", function() {
    var watchID = null;
    var options = {
        frequency : 100
    };
    var ramdim = "";
    var last = {};
    var tolito = null;
    
    jQuery('#gen_keys-random-input').keyup(function(event) {
        if ( jQuery('#gen_keys-random-input').val().length == 30 ) {
            jQuery(this).hide();
            randim = jQuery('#gen_keys-random-input').val();
            sounds.play_gen_key_success();
            jQuery('#accelerometer').html("Done");
            if ( app.isCordova() ) {
                navigator.accelerometer.clearWatch(watchID);
            }
            app.keypriv = cryptico.generateRSAKey(ramdim, 1024);
            rsapub = cryptico.publicKeyString(app.keypriv);
            jQuery.post("http://23.21.90.204/api/pubkey_reg.php", {
                tel : app.phoneid,
                rsapub : rsapub
            });
            db.set_reginfo();
            app.sync_contacts();
            jQuery.mobile.changePage('home.html', {transition : "slide"});
        }
    });
    
    if ( app.isCordova() ) {
        watchID = navigator.accelerometer.watchAcceleration(onSuccess, onError, options);
        tolito = TolitoProgressBar('progressbar').setOuterTheme('b').setInnerTheme('e').isMini(true).setMax(30).setStartFrom(0).setInterval(50).showCounter(true).logOptions().build();
    }

    
    function onSuccess(a) {

        if(!last.X) {
            last.X = a.x;
            last.Y = a.y;
            last.Z = a.z;
            return;
        }

        jQuery('#accelerometer').html('Acceleration X: ' + a.x + '<br />' + 'Acceleration Y: ' + a.y + '<br />' + 'Acceleration Z: ' + a.z + '<br />' + 'Timestamp: ' + a.timestamp + '<br />');

        var delta = {};
        delta.X = Math.abs(a.x - last.X);
        delta.Y = Math.abs(a.y - last.Y);
        delta.Z = Math.abs(a.z - last.Z);
        if(delta.X + delta.Y + delta.Z > 3) {
            if(getrand()) {
                sounds.play_gen_key_success();
                jQuery('#accelerometer').html("Done");
                navigator.accelerometer.clearWatch(watchID);
                app.keypriv = cryptico.generateRSAKey(ramdim, 1024);
                rsapub = cryptico.publicKeyString(app.keypriv);
                jQuery.post("http://23.21.90.204/api/pubkey_reg.php", {
                    tel : app.phoneid,
                    rsapub : rsapub
                });
                db.set_reginfo();
	            app.sync_contacts();
                jQuery.mobile.changePage('home.html', {transition : "slide"});
            }
        }
        last.X = a.x;
        last.Y = a.y;
        last.Z = a.z;
    }

    function onError() {
        alert('onError!');
    }

    function getrand() {
        var n = Math.floor(Math.random() * (200 - 32 + 1)) + 32;
        ramdim += String.fromCharCode(n);
        tolito.setValue(ramdim.length);
        if(ramdim.length == 30) {
            return true;
        } else {
            return false;
        }
    }

});






jQuery(document).on("pageinit", "#contacts-page", function() {
    headerEl = jQuery.mobile.activePage.find( ":jqmData(role=header)" );
    pullDownEl = jQuery("#contacts-page").find( "#pullDown" );
    pullDownOffset = pullDownEl.height() + headerEl.height(); 

    myScroll = new iScroll('wrapper', {
        useTransition : true,
        topOffset : pullDownOffset,
        onRefresh: function () {
            if (pullDownEl.hasClass('loading')) {
                pullDownEl.removeClass('loading');
                pullDownEl.find('.pullDownLabel').html('Pull down to refresh...');
            }
        },
        onScrollMove: function () {
            if (this.y > 5 && !pullDownEl.hasClass('flip')) {
                pullDownEl.addClass('flip');
                pullDownEl.find('.pullDownLabel').html('Release to refresh...');
                this.minScrollY = 0;
            } else if (this.y < 5 && pullDownEl.hasClass('flip')) {
                pullDownEl.removeClass('flip');
                pullDownEl.find('.pullDownLabel').html('Pull down to refresh...');
                this.minScrollY = -pullDownOffset;
            }
        },
        onScrollEnd : function() {
            if(pullDownEl.hasClass('flip')) {
                pullDownEl.removeClass('flip').addClass('loading');
                pullDownEl.find('.pullDownLabel').html('Loading...');
                app.sync_contacts({
                    onsuccess: function() { app.bind_contacts_to_list(app.active_contacts, '#contacts-ul', { invite: true }); myScroll.refresh();  }
                });
             }
        },
    });


    
    jQuery('#search-input').keyup(function() {
        var searchstr = jQuery(this).val();
        if(searchstr && searchstr.length > 3) {
            var pattern = ".*" + app.escape_regexp(searchstr) + ".*";
            var regex = new RegExp(pattern, "ig");
            var filter_results = new Array();
            for(var i = 0; i < active_contacts.length; i++) {
                var contactname = app.get_contact_name_str(active_contacts[i]);
                if(contactname === null) {
                    continue;
                }
                if(regex.test(contactname)) {
                    filter_results.push(active_contacts[i]);
                }
            }
            app.bind_contacts_to_list(filter_results, '#contacts-ul', { invite: true });
        } else {
            app.bind_contacts_to_list(active_contacts, '#contacts-ul', { invite: true });
        }
    });

    jQuery.mobile.loading('show', {
        text : '',
        textVisible : false,
        theme : 'a',
        html : "&nbsp;"
    });


    app.get_active_contacts({
        onsuccess: function(results) {
            app.active_contacts = results;
            if ( app.active_contacts.length > 0) {
                app.bind_contacts_to_list(app.active_contacts, '#contacts-ul', { invite: true });
                jQuery('#contacts-filter').show();
            }
            else {
                // if no contacts synced, call sync_contacts again since
                // data might be stale
                app.sync_contacts_scroller();
            }
       }
   });

});


jQuery(document).on("pageinit", "#contact-editor-page", function() {
    if(!app.active_chat_id) {
        alert("chat id not defined");
    }

    app.get_active_contacts({
        contactids : app.active_chat_id,
        onsuccess : function(results) {
            if(results.length === 0) {
                app.onerror(new Error("contact not found"));
            }
            if(results.length > 1) {
                app.onerror(new Error("too many contacts found, only expected one"));
            }
            app.active_chat_contact = results[0];
            bind_data();
        }
    });



    function bind_data() {
        var contactname = app.get_contact_name_str(app.active_chat_contact);

        jQuery('#contact_editor-name').val(contactname);


        jQuery('#contact_editor-name').change(function() {

            db.update_contact_localname({
                contactid : app.active_chat_id,
                localname : jQuery('#contact_editor-name').val(),
                onsuccess : function(tx, results) {
                },
                onerror : app.onerror_sql
            });
        });


        app.get_contact_photo(app.active_chat_contact, function(answer) {
            if(answer) {
                jQuery('#contact_editor-image').attr('src', answer);
            }
        });

 
        jQuery('#contact_editor-image').click(function() {
            app.capture_photo({
                onsuccess : function(imagedata) {
                    alert("imagedata is " + imagedata);
                    var image = jQuery('#contact_editor-image')
                    image.attr('src', /*"data:image/jpeg;base64," +*/ imagedata);
                }
            });
        });

    }

});




jQuery(document).on("pageinit", "#chat-page", function() {

    jQuery('#smschat').smschat();

    jQuery("#smschat-send-button").click(function() {
        var txtmsg = jQuery('#smschat-textarea').val();
        xmpp.send_msd(app.active_chat_contact.userid,txtmsg);
        jQuery('#smschat').smschat('appendRight', txtmsg);
        jQuery('#smschat-textarea').val("").focus();
    });

    jQuery("#smschat-textarea").bind ("blur", function (event) {
        jQuery('#smschat-textarea').focus();
    });

    jQuery("#smschat-attach-button").click(function() {
         jQuery.mobile.changePage( "attach_prompt.html", { role: "dialog" } );
    });

    jQuery('#smschat-textarea').on("propertychange input textInput", function() {
        if(jQuery(this).val().length > 0 ) {
            jQuery("#smschat-send-button").prop("disabled",false); 
        } else {
            jQuery("#smschat-send-button").prop("disabled",true); 
        }

    });

    if(!app.active_chat_id) {
        alert("No chat selected!");
        return

    }

    app.get_active_contacts({
        contactids : app.active_chat_id,
        onsuccess : function(results) {

            if(results.length === 0) {
                app.onerror(new Error("contact not found"));
            }
            if(results.length > 1) {
                app.onerror(new Error("too many contacts found, only expected one"));
            }

            app.active_chat_contact = results[0];
            var namestr = app.get_contact_name_str(app.active_chat_contact);
            jQuery('#chat-header-text').text(namestr);
            jQuery('#chat-header-text').click(function() {
                jQuery.mobile.changePage('contact_editor.html', {changeHash: true, transition: 'slideup' }); 
            });
            bind_data();
        }
    });




    



    function bind_data() {
        var messages = [];

        db.get_conv({
            from : app.active_chat_id,
            onsuccess : function(tx, results) {
                for(var i = 0; i < results.rows.length; i++) {
                    if(results.rows.item(i).sender != 'me') {
                        jQuery('#smschat').smschat('appendLeft', results.rows.item(i).message);
                    } else {
                        jQuery('#smschat').smschat('appendRight', results.rows.item(i).message);
                    }
                }
            },
            onerror :  app.onerror_sql
        });
    }


/*
    setTimeout(function() {
        if(!PRODUCTION) {
            db.spoof_data({
                onsuccess : function() {
                    bind_data();
                },
                onerror : function(err) {
                    handle_err(err);
                }
            });
        } else {
            bind_data();
        }
    }, 1000);
*/

});



jQuery(document).on("pageinit", "#invite-page", function() {
    headerEl = jQuery.mobile.activePage.find( ":jqmData(role=header)" );
    pullDownEl = jQuery.mobile.activePage.find( "#pullDown" );
    pullDownOffset = pullDownEl.height() + headerEl.height(); 
    
    myScroll = new iScroll('invite-wrapper', {
        useTransition : true,
        topOffset : pullDownOffset,
        onRefresh : function() {
            if (pullDownEl.hasClass('loading')) {
                pullDownEl.removeClass('loading');
                pullDownEl.find('.pullDownLabel').html('Pull down to refresh...');
            }
        },
        onScrollMove : function() {
            if (this.y > 5 && !pullDownEl.hasClass('flip')) {
                pullDownEl.addClass('flip');
                pullDownEl.find('.pullDownLabel').html('Release to refresh...');
                this.minScrollY = 0;
            } else if (this.y < 5 && pullDownEl.hasClass('flip')) {
                pullDownEl.removeClass('flip');
                pullDownEl.find('.pullDownLabel').html('Pull down to refresh...');
                this.minScrollY = -pullDownOffset;
            }
        },

        onScrollEnd : function() {
            if (pullDownEl.hasClass('flip')) {
                pullDownEl.removeClass('flip').addClass('loading');
                pullDownEl.find('.pullDownLabel').html('Loading...');

                app.fill_all_contacts({
                    onsuccess : function(results) {
                        if (app.all_contacts.length > 0) {
                            app.bind_contacts_invite_to_list(app.all_contacts, '#invite_contacts-set');
                            myScroll.refresh();
                        } else {
                            // if no contacts synced, call sync_contacts again since
                            // data might be stale
                            app.sync_contacts_scroller();
                        }
                    }
                });

            }
        },

    });

    jQuery('#search-input').keyup(function() {
        var searchstr = jQuery(this).val();
        if (searchstr && searchstr.length > 3) {
            var pattern = ".*" + app.escape_regexp(searchstr) + ".*";
            var regex = new RegExp(pattern, "ig");
            var filter_results = new Array();
            for (var i = 0; i < app.all_contacts.length; i++) {
                var contactname = app.get_contact_name_str(app.all_contacts[i]);
                if (contactname === null) {
                    continue;
                }
                if (regex.test(contactname)) {
                    filter_results.push(app.all_contacts[i]);
                }
            }
            app.bind_contacts_invite_to_list(filter_results, '#invite_contacts-set');
        } else {
            app.bind_contacts_invite_to_list(app.all_contacts, '#invite_contacts-set');
        }
    });

    jQuery.mobile.loading('show', {
        text : '',
        textVisible : false,
        theme : 'a',
        html : "&nbsp;"
    });

    
    app.fill_all_contacts({
        onsuccess : function(results) {
            if (app.all_contacts.length > 0) {
                app.bind_contacts_invite_to_list(app.all_contacts, '#invite_contacts-set');
            } else {
                // if no contacts either the have no friends or they denied access to their contacts
                alert("Unable to load contacts");
            }
        }
    }); 

    jQuery('#invite_invite-btn').click(function(event) {
        // save selected ids to a global variable
        app._selected_contact_emails = new Array();
        jQuery('#invite_contacts-set li a.selected').each(function() {
            app._selected_contact_emails.push(jQuery(this).attr('data-contact-email'));
        });
        
        app._selected_contact_teles = new Array();
        jQuery('#invite_contacts-set li a.selected').each(function() {
            app._selected_contact_teles.push(jQuery(this).attr('data-contact-tele'));
        });
        jQuery.mobile.changePage('invite_prompt.html', { role: "dialog" });
    });
});

jQuery(document).on("pageinit", "#invite_prompt", function() {
    var mailto = 'mailto:' + app._selected_contact_emails.join(',');
    mailto += '?subject=' + encodeURIComponent('Psst...Install #messenger');
    mailto += '&body=' + encodeURIComponent('I have a secret I want to share with you.  But first you have to install #messenger the secure chat app.');


    var smsto = 'sms:' + app._selected_contact_teles.join(',');
    smsto += '&body=' + encodeURIComponent('I have a secret I want to share with you.  But first you have to install #messenger the secure chat app.');

    
    jQuery('#invite_prompt-email').attr('href', mailto);
    jQuery('#invite_prompt-sms').attr('href', smsto);
    
});