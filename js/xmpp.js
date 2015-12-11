/* global xmmp */
var xmpp = {
    connection: null,
    status: 0,

    _log: function(msg) {
        console.log("xmpp:" + msg)
    },

   init: function() {
        var BOSH_SERVICE = 'http://23.21.90.204/http-bind/';
        this.connection = new Strophe.Connection(BOSH_SERVICE);
        this._log("init xmpp");
    },

    connect: function() {
        if(this.connection == null) {
            this.init();
        }
        console.log("xmpp: connect " + app.phoneid + " " + app.phonepwd);
        this.connection.connect( app.phoneid + "@hashmessenger.com", app.phonepwd,  xmmp_onConnect);
    },

   send_msd: function (to, txt) {
        if(txt && to){
                var reply = $msg({
                        to: to + "@hashmessenger.com",
                        type: 'chat'
                })
                .cnode(Strophe.xmlElement('body', txt)).up()
                .c('active', {xmlns: "http://jabber.org/protocol/chatstates"});
                this.connection.send(reply);
                this._log('sent ' + to + ': ' + txt);

                db.insertmsg({
                    from: "me",
                    to: to,
                    msg: txt,
                    msgstatus: 0,
                    onsuccess : function() {},
                    onerror : app.onerror_sql
                });
        }

     }

};

function xmmp_onConnect(status) {
    if (status == Strophe.Status.CONNECTING) {
        xmpp._log('Strophe is connecting.');
    } else if (status == Strophe.Status.CONNFAIL) {
        xmpp._log('Strophe failed to connect.');
    } else if (status == Strophe.Status.DISCONNECTING) {
        xmpp._log('Strophe is disconnecting.');
    } else if (status == Strophe.Status.DISCONNECTED) {
        xmpp._log('Strophe is disconnected.');
    } else if (status == Strophe.Status.AUTHENTICATING) {
        xmpp._log('Strophe is authenticating.');
    } else if (status == Strophe.Status.AUTHFAIL) {
        xmpp._log('Strophe is authfail.');
    } else if (status == Strophe.Status.CONNECTED) {
        xmpp.connection.addHandler(xmpp_onMessage, null,    'message', null, null,  null); 
        xmpp.connection.addHandler(xmpp_onOwnMessage, null, 'iq', 'set', null,  null); 
        xmpp.connection.send($pres().tree());
        xmpp.status = Strophe.Status.CONNECTED;
        xmpp._log('Strophe is connected.');
	document.addEventListener("pause", app_onPause, false);
	document.addEventListener("resume", app_onResume, false);
    } else {
        xmpp._log('Strophe status .' + status);
    }
};

function xmpp_onMessage(msg) {
    var to = msg.getAttribute('to');
    var from = msg.getAttribute('from');
    var type = msg.getAttribute('type');
    var elems = msg.getElementsByTagName('body');
    if (type == "chat" && elems.length > 0) {
         var body = elems[0];
         var message = Strophe.getText(body);
    }
    var fromtel = from.substr(0, from.indexOf("@"));
    var msgstatus = 1;
    if (jQuery.mobile.activePage.attr('id') == "chat-page" && app.active_chat_contact.userid == fromtel) {
         jQuery('#smschat').smschat('appendLeft', message);
         msgstatus = 0;
    } else {
         db.create_chat({
                contactid : fromtel,
                onsuccess : function() {},
                onerror : app.onerror_sql
         });
    };
    db.insertmsg({
         from: fromtel,
         to: "me",
         msg: message,
         msgstatus: msgstatus,
         onsuccess : function() {},
         onerror : app.onerror_sql
    });
 
    return true;
};


function xmpp_onOwnMessage(msg) {
    var elems = msg.getElementsByTagName('own-message');
    if (elems.length > 0) {
             var own = elems[0];
             var to = msg.getAttribute('to');
             var from = msg.getAttribute('from');
             var iq = $iq({
                 to: from,
                 type: 'error',
                 id: msg.getAttribute('id')
             }).cnode(own).up().c('error', {type: 'cancel', code: '501'})
             .c('feature-not-implemented', {xmlns: 'urn:ietf:params:xml:ns:xmpp-stanzas'});
             xmpp.connection.sendIQ(iq);
    }
    return true;
};

function app_onPause() {
    xmpp.connection.disconnect("", true);
    xmpp._log('Event OnPause');
}
function app_onResume () {
    xmpp._log('Event OnResume');
    xmpp.connect();
}
