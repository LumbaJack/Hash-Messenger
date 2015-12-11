# Hash Messenger message format #

## The Wrapper ##

  Since Hash messenger uses XMPP as its transport protocol we add our Hash messenger specific data in the
  body of the message using XML.  This data is called the wrapper.  The actual user message is included
  along with our markup in the body of the message.

  The wrapper data is not encrypted so that it is not effected by encryption issues ( expired keys, etc... )

  Examples:
       <hm>
           <xp>10</xp>
           <msg key="0eb67614-c673-4727-a662-db9047cbec43">
           <!-- Base64 encoded encryption data goes here -->
           </msg>
       </hm>


  Each XMPP message *may* have multiple <hm> elemens in it.

## The Key Id ##
    Each time the user generates a encryption key pair, a "key-id" is also generated.  The "key-id" is a unique string that
    identifies this pair of encryption keys.  The client uploads the public key and corresponding "key-id" to the Hash messenger
    server.  Each reciver gets a copy of the public key and the key-id

    When a message is recieved by the reciever should check the key-id against the latest known key-id to see if it matches.  If it matches
    then the reciever can proceed to decrypt the message.  If it does not match, the reciever should send a retransmit (rt) message to
    the sender.

## <hm> nodes ##

### <msg> ###
  The actual user generated message.  The message is a base64 encoded string of the encrypted text.

  @key
    The key attribute contains a "key-id" ( a unique identifier that identifies which key the message was encrypted with )
    If the reciever is unable to locate the key-id then the user must resend the message.

### <rt> ###
  Retransmit the message.  In the event of an encryption issue or other problem, the reciever requests that the sender
  resend the message.  The content of the element should be the message id.

  When a retransmit message is recieved by the sender the sender should fetch the latest public key from the hash messenger servers
  re-encrypt the message and send resend it.

### <xp> ###

  eXPires time - The time in seconds after the message is read when the reciever should delete the message.
  This provides a "self destruct" where the user can control how long a message is viewable by the reciever.
  All trace of this message should be deleted on the receievers client. ( database, window, memory etc. )

### <del> ###

  delete command - allows sender to delete message from recievers device at will.

### <delall> ###

  delete all command - allows sender to delete all messages from receivers device at will.  This
  includes chat entry.  This command should erase all traces of communication on the reciever side.

### <fmsg> ###

  fake message - a fake message used to disgise the true intent of the conversation.  The UI may include
  secret key strokes to reveal the true message.

    <msg>I am in room #432</msg>
    <fmsg>I'll see you at work on monday</fmsg>

### <scap> ###

  scap - automatically sent to alert the sender that the reciever took a screen shot of the app
  during the chat session or viewing one of the media files


## Media ##

  All media is encrypted before it is uploaded to the hash messenger servers.  After uploaded a url is generated and returned to
  the sender.  The sender then uses this URL in the message.  If the sender receives a retransmit request for a media message
  it must re-encrypt and reupload the media before resending the message.

### <img> ###

  a html-ish img tag for including images.  <img src="https://...." />

### <geo> ###

  a html-ish img tag for including location.  <geo lat="29.7628" lon="95.3831" >Houston Texas</geo>

### <audio> ###

  a html-ish img tag for audio files.  <audio src="https://...." >Description of sound</audio>

### <video> ###

  a html-ish img tag for audio files.  <video src="https://...." >Video description</video>





