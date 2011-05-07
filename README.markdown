# CatchAPI
CatchAPI is a Java library for Android.  It makes it easy to use the Catch.com API.  Here's a quick preview...

```java
CatchAPI api = new CatchAPI("MyCoolApp", context);

// sign in
api.signIn("username", "pass1234", null);

// add a new note
CatchNote note = new CatchNote();

long timestamp = System.currentTimeMillis();
note.creationTime = timestamp;
note.modificationTime = timestamp;

note.text = "Hello World!";

api.addNote(note);
```

Check out the [online documentation](http://catch.github.com/android-api/Documentation/) for more.  The [CatchAPI class](http://catch.github.com/android-api/Documentation/com/catchnotes/api/CatchAPI.html) is the primary interface, so you might want to start there.

Also, take a look at the Example app to see an example of the library in use.  The interesting stuff is in [Dashboard.java](https://github.com/catch/android-api/blob/master/Example/src/com/example/CatchApiDemo/Dashboard.java), mostly near the bottom of the file.

