/* global sound player */
var sounds = {
    _media : null,
    _onsuccess : function() {
        // console.log("sounds.onsuccess");
    },
    _onerror : function(err) {
        alert("Error  (" + err.code + ") :  " + err.message);
    },
    _play: function(file) {
	if ( device.platform === 'Android') {
	    file = '/android_asset/www/' + file;
	}
        if (this._media != null && this._media.isPlaying()){
	    _media.stop()
	    _media.release()
	} else if (this._media != null) {
	    _media.release()
	}
	_media = new Media(file, this._onsuccess, this._onerror);
        _media.play();
    },
    play_gen_key_success : function() {
        this._play('res/sounds/notification.mp3');
    }
};
