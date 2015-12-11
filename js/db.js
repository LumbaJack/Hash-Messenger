/*
 * helper method that just gets the db with all settings pre-configured and
 * keeps our main codebase clean
 */

function DataStore() {

    this._db = null;
    this._localstorage = null;
    this.open = function() {
        this._db = window.openDatabase("hash_messenger", "1.0", "Hash Messenger DB", 1000000);
        //this._db = window.sqlitePlugin.openDatabase("hash_messenger.db", "1.0", "Hash Messenger DB", 1000000);
    };

    this._error_cb = function(tx, err) {
        alert("Database error SQL: " + err);
    };

    this.initdb = function(args) {
        if(!this._db) {
            this.open();
        }
        // TODO this gets called when the database is created .. this
        // is where you would handle any upgrade to database schema
        if(!args) {
            args = {};
        }
        newargs = {};
    	if (typeof(args.onsuccess) == "function") {
    		newargs['onsuccess'] = args.onsuccess;
    	}
    	if (typeof(args.oncomplete) == "function") {
            newargs['oncomplete'] = args.oncomplete;
        }

        newargs['sql'] = [
        'CREATE TABLE IF NOT EXISTS tbl_chats ( userid integer NOT NULL PRIMARY KEY, subject text, created_at real, updated_at real)',
        'CREATE TABLE IF NOT EXISTS tbl_messages ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, sender text, receiver text, subject text, message text, sentat real, readat real, status integer  )',
        'CREATE TABLE IF NOT EXISTS contacts ( localid integer, userid integer, displayname text, localname text, icon text, localicon text, pubkey text, PRIMARY KEY(userid))'];
        this.query(newargs);
    };

    this.query = function(args) {
        var self = this;

        if(!this._db) {
            this.open();
        }

        if(!args) {
            args = {};
        }
        if(!args.onsuccess) {
            throw "onsuccess argument required";
        }
        
        sqlparams = [];
        if ( args.sqlparams ) {
            sqlparams = args.sqlparams;
        }

        var _onsuccess = args.onsuccess;
        var _onerror = this.error_cb;
        var _oncomplete = function() {};

        if(args.onerror) {
            _onerror = args.onerror;
        }
        if (args.oncomplete) {
            _oncomplete = args.oncomplete;
        }

        if(args.sql instanceof Array) {
            this._db.transaction(function(tx) {
                for(var i = 0; i < args.sql.length; i++) {
                    var sqlarg = args.sql[i];
                    tx.executeSql(sqlarg, sqlparams, _onsuccess, _onerror);
                }

            }, _onerror, _oncomplete);
        } else {
            this._db.transaction(function(tx) {
                tx.executeSql(args.sql, sqlparams, _onsuccess, _onerror);
            }, _onerror, _oncomplete);
        }
    };




    this.get_conv = function(args) {
        /*
         * Return all messages from a conversation
         * 	args:
         * 		onsuccess: callback when successful retrieval
         * 		onerror: callback when there is a SQL error
         * 		from: get messages from this user
         * 		to: get messages from from to this user
         */
        if(!args) {
            args = {};
        }
        if(args.from) {
            var sqlstr = 'SELECT * FROM tbl_messages WHERE sender = "' + args.from + '" or receiver ="' + args.from + '"';
            args['sql'] = sqlstr;
            this.query(args);
        }
    };
    this.get_chats = function(args) {
        if(!args) {
            args = {};
        }

        var sqlstr = 'SELECT * FROM tbl_chats, contacts WHERE contacts.userid = tbl_chats.userid';
        
        if ( args.userid ) {
            sqlstr += ' AND tbl_chats.userid = "' + args.userid + '"';
        }        

        args['sql'] = sqlstr;
        this.query(args);
    };

    this.create_chat = function(args) {
        var self = this;

        if(!args) {
            throw "arguments required";
        }

        var now = Date.now().toISOString();
        args['sql'] = 'REPLACE INTO tbl_chats ( userid, subject, created_at, updated_at  )  VALUES ( "' + args.contactid + '", "", "'+ now +'", "'+ now + '")';

        return this.query(args);
    };

    this.insertmsg = function(args) {
        if(!args) {
            throw "arguments required";
        }
        args['sql'] = 'INSERT INTO tbl_messages ( sender, receiver,  subject, message, sentat, readat, status  ) VALUES ( "' + args.from + '","' + args.to + '", "", "' + args.msg + '", "' + Date.now() + '", "' + Date.now() + '",' + args.msgstatus + ')';
        this.query(args);
    };



    this.get_eula_accepted = function() {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        var eulaval = this._localstorage.getItem('eula-accepted');
        if(eulaval) {
            return true;
        } else {
            return false;
        }
    };

    this.set_eula_accepted = function(eulaval) {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        if(eulaval) {
            this._localstorage.setItem('eula-accepted', true);
        } else {
            this._localstorage.setItem('eula-accepted', false);
        }
    };

    this.ispushreg = function() {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        var pushreg = this._localstorage.getItem('pushreg');
        if(pushreg) {
            return true;
        } else {
            return false;
        }
    };

    this.setpushreg = function(pushreg) {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        if(pushreg) {
            this._localstorage.setItem('pushreg', true);
        } else {
            this._localstorage.setItem('pushreg', false);
        }
    };

    this.get_reginfo = function() {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        app.phoneid = this._localstorage.getItem('phoneid');
        app.phonepwd = this._localstorage.getItem('phonepwd');
        var keypriv = this._localstorage.getItem('keypriv');
        if(app.phoneid == null || keypriv == null || app.phonepwd == null) {
            return false;
        } else {
            app.keypriv = cryptico.recreateRSAKey(keypriv);
            return true;
        }
    };
    this.set_reginfo = function() {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        var jsonpriv = JSON.stringify(app.keypriv.toJSON());
        this._localstorage.setItem('phoneid', app.phoneid);
        this._localstorage.setItem('keypriv', jsonpriv);
        this._localstorage.setItem('phonepwd', app.phonepwd);
    };

    this.get_message_history_pref = function() {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        var prefval = this._localstorage.getItem('message-history-pref');
        app.message_history_pref = prefval;
        return prefval;
    };

    this.set_message_history_pref = function(prefval) {
        if(!this._localstorage) {
            this._localstorage = window.localStorage;
        }
        this._localstorage.setItem('message-history-pref', prefval);
        app.message_history_pref = prefval;
    };

    this.sync_contacts = function(data) {
        var args = {};
        args.onsuccess = function() {
        };
        args['sql'] = new Array();
        for(var i = 0; i < data.length; i++) {
            args['sql'].push('REPLACE INTO contacts ( localid, displayname, icon, pubkey, userid  ) VALUES ( "' + data[i].id + '", "' + data[i].disp + '", "", "' + data[i].pubkey + '","' + data[i].phone + '");');
        }
        this.query(args);
    };
    
    
    this.update_contact_localname = function(args) {
        if ( ! args ) {
            throw "argument required";
        }
        
        if ( ! args.contactid ) {
            throw "contactid argument required";
        }
        
        if ( ! args.localname ) {
            throw "contactid argument required";
        }       
        var contactid = args.contactid;
        var sqlstr = 'UPDATE contacts SET localname = "' + args.localname + '" where localid = "'+ args.contactid + '"';

        args['sql'] = sqlstr;
        this.query(args);
    };

    this.update_contact_icon = function(args) {
        if ( ! args ) {
            throw "argument required";
        }
        
        if ( ! args.contactid ) {
            throw "contactid argument required";
        }
        
        if ( ! args.icon ) {
            throw "icon argument required";
        }       
        var contactid = args.contactid;
        var sqlstr = 'UPDATE contacts SET icon = "' + args.icon + '" where localid = "'+ args.contactid + '"';

        args['sql'] = sqlstr;
        this.query(args);
    };
    
    this.update_contact_localicon = function(args) {
        if ( ! args ) {
            throw "argument required";
        }
        
        if ( ! args.contactid ) {
            throw "contactid argument required";
        }
        
        if ( ! args.localicon ) {
            throw "icon argument required";
        }       
        var contactid = args.contactid;
        var sqlstr = 'UPDATE contacts SET localicon = "' + args.icon + '" where localid = "'+ args.contactid + '"';

        args['sql'] = sqlstr;
        this.query(args);
    };
    
    this.get_contacts = function(args) {
        if(!args) {
            args = {};
        }
        var sqlstr = 'SELECT * FROM contacts';
        if ( args.userid ) {
            sqlstr += ' WHERE userid = "' + args.userid + '"';
        }

        args['sql'] = sqlstr;
        this.query(args);
    };
}

var db = new DataStore();
