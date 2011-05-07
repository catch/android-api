//
//  Copyright 2011 Catch.com, Inc.
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//      http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.catchnotes.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

public class CatchNotesXmlParser {
	public static final long ERROR_RESPONSE_NULL = -1;
	public static final long ERROR_PARAMETERS_NULL = -2;

	private static final String XML_TAG_NOTES = "notes";
	private static final String XML_TAG_NOTE = "note";
	private static final String XML_TAG_CREATED = "created_at";
	private static final String XML_TAG_MODIFIED = "modified_at";
	private static final String XML_TAG_REMINDER = "reminder_at";
    private static final String XML_TAG_SERVER_MODIFIED = "server_modified_at";
	private static final String XML_TAG_ID = "id";
	private static final String XML_TAG_SOURCE = "source";
	private static final String XML_TAG_SOURCE_URL = "source_url";
	private static final String XML_TAG_TEXT = "text";
	private static final String XML_TAG_SUMMARY = "summary";
	private static final String XML_TAG_USER = "user";
	private static final String XML_TAG_USER_NAME = "user_name";
    private static final String XML_TAG_COMMENTS = "comments";
    private static final String XML_TAG_COMMENT = "comment";
	private static final String XML_TAG_MODE = "mode";
	private static final String XML_TAG_BROWSER_URL = "browser_url";
	private static final String XML_TAG_TAGS = "tags";
	private static final String XML_TAG_TAG = "tag";
	private static final String XML_TAG_MEDIA_LIST = "media_list";
	private static final String XML_TAG_MEDIA = "media";
	private static final String XML_TAG_CONTENT_TYPE = "type";
	private static final String XML_TAG_SRC = "src";
	private static final String XML_TAG_SIZE = "size";
	private static final String XML_TAG_FILENAME = "filename";
	private static final String XML_TAG_VOICE_HINT = "voicenote_hint";
	private static final String XML_TAG_LOCATION = "location";
	private static final String XML_TAG_LATITUDE = "latitude";
	private static final String XML_TAG_LONGITUDE = "longitude";
	private static final String XML_TAG_ALTITUDE = "altitude";
	private static final String XML_TAG_SPEED = "speed";
	private static final String XML_TAG_BEARING = "bearing";
	private static final String XML_TAG_ACCURACY_POSITION = "accuracy_position";
	private static final String XML_TAG_ACCURACY_ALTITUDE = "accuracy_altitude";

	// Enable this for extra API call tracing output in the logcat.
	private static final boolean PARSE_TRACING_OUTPUT_ENABLED = false;
	private static final String LOGCAT_NAME = "CatchParser";
    public static final String CATCH_NAMESPACE_PREFIX = "catch";

    private Time timestamper;
	private SimpleDateFormat rfc3339;

	public CatchNotesXmlParser() {		
		try {
			if (Integer.parseInt(Build.VERSION.SDK) >= Build.VERSION_CODES.ECLAIR_0_1) {
				// We can use Time & TimeFormatException on Android 2.0.1
				// (API level 6) or greater. It crashes the VM otherwise.
				timestamper = new Time();
			}
		} catch (Exception e) { }
	}

	public void parseSyncV2XML(HttpResponse response, List<CatchNoteRef> noteRefs)
		throws XmlPullParserException, IOException, IllegalArgumentException {
		if (response == null || noteRefs == null) {
			throw new IllegalArgumentException();
		}

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(response.getEntity().getContent(), null);

		int eventType = parser.getEventType();

		String startTag;
		String endTag;
		String nextText = "";

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_DOCUMENT) {
				parse_trace("Beginning XML pullparse.");
			} else if (eventType == XmlPullParser.START_TAG) {
				startTag = parser.getName();

				if (XML_TAG_NOTES.equals(startTag)) {
					parse_trace("Parsing noteRefs.");
				} else if (XML_TAG_NOTE.equals(startTag)) {
					parse_trace("Parsing a note.");
					CatchNoteRef noteRef = new CatchNoteRef();

					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_TAG) {
							startTag = parser.getName();
							
							// one-liners:
							if (XML_TAG_ID.equals(startTag)) {
								nextText = parser.nextText();
								noteRef.nodeId = nextText;
								parse_trace("Note ID is " + noteRef.nodeId);
							} else if (XML_TAG_SERVER_MODIFIED.equals(startTag)) {
								nextText = parser.nextText();
								noteRef.serverModified = nextText;
								parse_trace("Server modification time is " + noteRef.serverModified);
							}
						} else if (eventType == XmlPullParser.END_TAG) {
							endTag = parser.getName();
							
							if (XML_TAG_NOTE.equals(endTag)) {
								parse_trace("Parsing noteRef complete.");
								break;
							}
						}

						eventType = parser.next();
					}
					
					noteRefs.add(noteRef);
				} else  {
					parse_trace("(parseSyncV2XML) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				endTag = parser.getName();

				if (XML_TAG_NOTES.equals(endTag)) {
					parse_trace("Parsed " + noteRefs.size() + " noteRefs.");
				}
			}
			
			eventType = parser.next();
		}
	}

    public void parseNotesXml(HttpResponse response,
			ArrayList<CatchNote> notes, long parentId)
	throws XmlPullParserException, IOException, IllegalArgumentException {		
		if (response == null || notes == null) {
			throw new IllegalArgumentException();
		}
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(response.getEntity().getContent(), null);

		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_DOCUMENT) {
				parse_trace("Beginning XML pullparse.");
			} else if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_NOTES.equals(startTag)) {
					parse_trace("Parsing notes.");
				} else if (XML_TAG_NOTE.equals(startTag)) {
					parse_trace("Parsing a note.");
					parseNote(parser, notes, parentId);
				} else {
					parse_trace("(parseNotesXml) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_NOTES.equals(endTag)) {
					parse_trace("Parsed " + notes.size() + " notes.");
				}
			}

			eventType = parser.next();
		}
	}
	
	private void parseNote(XmlPullParser parser,
			ArrayList<CatchNote> returnVal, long parentId)
	throws XmlPullParserException, IOException {
		// Create a blank note object
		CatchNote note = new CatchNote();
		returnVal.add(note);
		note.parentId = parentId;
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();
				String nextText = "";

                if (parser.getNamespace().equals(XmlPullParser.NO_NAMESPACE)) { // default properties
                    // one-liners:
                    if (XML_TAG_ID.equals(startTag)) {
                        nextText = parser.nextText();
                        note.id = nextText;
                        parse_trace("Note ID is " + note.id);
                    } else if (XML_TAG_SOURCE.equals(startTag)) {
                        nextText = parser.nextText();
                        note.source = nextText;
                        parse_trace("Note source is " + note.source);
                    } else if (XML_TAG_SOURCE_URL.equals(startTag)) {
                        nextText = parser.nextText();
                        note.sourceUrl = nextText;
                        parse_trace("Note source URL is " + note.sourceUrl);
                    } else if (XML_TAG_CREATED.equals(startTag)) {
                        nextText = parser.nextText();
                        note.creationTime = parse3339(nextText);
                        parse_trace("Creation time is " + nextText + " (" +
                                note.creationTime + ')');
                    } else if (XML_TAG_MODIFIED.equals(startTag)) {
                        nextText = parser.nextText();
                        note.modificationTime = parse3339(nextText);
                        parse_trace("Modification time is " + nextText +
                                " (" + note.modificationTime + ')');
                    } else if (XML_TAG_REMINDER.equals(startTag)) {
                        nextText = parser.nextText();
                        note.reminderTime = parse3339(nextText);
                        parse_trace("Reminder time is " + nextText + " (" +
                                note.reminderTime + ')');
                    } else if (XML_TAG_SERVER_MODIFIED.equals(startTag)) {
                        nextText = parser.nextText();
                        note.serverModifiedAt= parse3339(nextText);
                        parse_trace("ServerModifiedAt time is " + nextText + " (" +
                                note.serverModifiedAt + ')');
                    } else if (XML_TAG_TEXT.equals(startTag)) {
                        nextText = parser.nextText();
                        note.text = nextText;
                        parse_trace("Note text is \"" + note.text + '"');
                    } else if (XML_TAG_SUMMARY.equals(startTag)) {
                        nextText = parser.nextText();
                        note.summary = nextText;
                        parse_trace("Note summary is \"" + note.summary + '"');
                    } else if (XML_TAG_COMMENTS.equals(startTag)) {
                        note.children = 0;

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                startTag = parser.getName();

                                if (XML_TAG_COMMENT.equals(startTag)) {
                                    note.children++;
                                    parse_trace("found comment");
                                }
                            } else if (eventType == XmlPullParser.END_TAG) {
                                String endTag = parser.getName();

                                if (XML_TAG_COMMENTS.equals(endTag)) {
                                    parse_trace("Parsing comments complete.");
                                    break;
                                }
                            }

                            eventType = parser.next();
                        }

                        parse_trace("Comment count is " + note.children);
                    }  else if (XML_TAG_MODE.equals(startTag)) {
                        nextText = parser.nextText();
                        note.mode = nextText;
                        parse_trace("Mode is " + note.mode);
                    } else if (XML_TAG_BROWSER_URL.equals(startTag)) {
                        nextText = parser.nextText();
                        note.browserUrl = nextText;
                        parse_trace("Browser URL is " + note.browserUrl);
                    }
                    // new blocks:
                    else if (XML_TAG_USER.equals(startTag)) {
                        parse_trace("Parsing note owner data.");
                        parseNoteOwner(parser, note);
                    } else if (XML_TAG_TAGS.equals(startTag)) {
                        parse_trace("Parsing note tags.");
                        parseTags(parser, note);
                    } else if (XML_TAG_LOCATION.equals(startTag)) {
                        parse_trace("Parsing geotag.");
                        parseLocation(parser, note);
                    } else if (XML_TAG_MEDIA_LIST.equals(startTag)) {
                        parse_trace("Parsing media list.");
                        parseMediaList(parser, note);
                    } else {
                        parse_trace("(parseNote) unknown XML tag: <" + startTag + ">");
                    }
                } else if (parser.getPrefix().equals(CATCH_NAMESPACE_PREFIX)) { // XXXaes. use full path once we fix server side uris
                    parse_trace("Parsing annotation.");
                    if (note.annotations == null) {
                        note.annotations = new HashMap<String, String>();
                    }
                    note.annotations.put(startTag, parser.nextText());
                }
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_NOTE.equals(endTag)) {
					parse_trace("Parsing note complete.");
					break;
				}
			}

			eventType = parser.next();
		}
	}

	private void parseTags(XmlPullParser parser, CatchNote note)
	throws XmlPullParserException, IOException {
		ArrayList<CharSequence> tags = new ArrayList<CharSequence>();
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_TAG.equals(startTag)) {
					String tag = parser.nextText();
					tags.add(tag);
					parse_trace("Added tag \"" + tag + '"');
				} else {
					parse_trace("(parseTags) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_TAGS.equals(endTag)) {
					note.tags = tags;
					parse_trace("Note had " + note.tags.size()
							+ " tag(s).");
					break;
				}
			}

			eventType = parser.next();
		}
	}

	private void parseMediaList(XmlPullParser parser, CatchNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_MEDIA.equals(startTag)) {
					parse_trace("Parsing a media item.");
					parseMedia(parser, note);
				} else {
					parse_trace("(parseMediaList) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_MEDIA_LIST.equals(endTag)) {
					parse_trace("Parsing media list complete.");
					break;
				}
			}

			eventType = parser.next();
		}
	}

	private void parseMedia(XmlPullParser parser, CatchNote note)
	throws XmlPullParserException, IOException {
		CatchMedia media = new CatchMedia();
		media.note_id = note.id;
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();
				String nextText = "";

				if (XML_TAG_SRC.equals(startTag)) {
					nextText = parser.nextText();
					media.src = nextText;
					parse_trace("Source = " + media.src);
				} else if (XML_TAG_ID.equals(startTag)) {
					nextText = parser.nextText();
					media.id = nextText;
					parse_trace("API ID = " + media.id);
				} else if (XML_TAG_CREATED.equals(startTag)) {
					nextText = parser.nextText();
					media.created_at = parse3339(nextText);
					parse_trace("Creation time = " + nextText + " (" + media.created_at + ')');
				} else if (XML_TAG_CONTENT_TYPE.equals(startTag)) {
					nextText = parser.nextText();
					media.content_type = nextText;
					parse_trace("Type = " + media.content_type);
				} else if (XML_TAG_SIZE.equals(startTag)) {
					try {
						nextText = parser.nextText();
						long size = Long.parseLong(nextText);
						media.size = size;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for media size: \"" + nextText + '"');
						media.size = 0;
					}
					
					parse_trace("Size = " + media.size);
				} else if (XML_TAG_VOICE_HINT.equals(startTag)) {
					nextText = parser.nextText();
					media.voice_hint = Boolean.parseBoolean(nextText);
					parse_trace("voice hint = " + media.voice_hint);
				} else if (XML_TAG_FILENAME.equals(startTag)) {
					nextText = parser.nextText();
					media.filename = nextText;
					parse_trace("Filename = " + media.filename);
				} else {
					parse_trace("(parseMedia) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_MEDIA.equals(endTag)) {
					if (note.mediaList == null) {
						note.mediaList = new ArrayList<CatchMedia>();
					}

					note.mediaList.add(media);
					parse_trace("Parsing media item complete.");
					break;
				}
			}

			eventType = parser.next();
		}
	}

	private void parseNoteOwner(XmlPullParser parser, CatchNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();
				String nextText = "";

				if (XML_TAG_ID.equals(startTag)) {
					try {
						nextText = parser.nextText();
						long ownerId = Long.parseLong(nextText);
						note.ownerId = ownerId;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for note owner ID: \"" + nextText + '"');
						note.ownerId = 0;
					}

					parse_trace("Owner ID = " + note.ownerId);
				} else if (XML_TAG_USER_NAME.equals(startTag)) {
					String owner = parser.nextText();
					note.owner = owner;
					parse_trace("Owner name = " + note.owner);
				} else {
					parse_trace("(parseNoteOwner) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_USER.equals(endTag)) {
					parse_trace("Parsing owner data complete.");
					break;
				}
			}

			eventType = parser.next();
		}
	}

	private void parseLocation(XmlPullParser parser, CatchNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();
				String nextText = "";

				if (XML_TAG_LATITUDE.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double latitude = Double.parseDouble(nextText);
						note.latitude = latitude;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for latitude: \"" + nextText + '"');
						note.latitude = 0.0;
					}
					
					parse_trace("Latitude = " + note.latitude);
				} else if (XML_TAG_LONGITUDE.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double longitude = Double.parseDouble(nextText);
						note.longitude = longitude;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for longitude: \"" + nextText + '"');
						note.longitude = 0.0;
					}
					
					parse_trace("Longitude = " + note.longitude);
				} else if (XML_TAG_ALTITUDE.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double altitude = Double.parseDouble(nextText);
						note.altitude = altitude;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for altitude: \"" + nextText + '"');
						note.altitude = 0.0;
					}

					parse_trace("Altitude = " + note.altitude);
				} else if (XML_TAG_SPEED.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double speed = Double.parseDouble(nextText);
						note.speed = speed;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for speed: \"" + nextText + '"');
						note.speed = 0.0;
					}
					
					parse_trace("Speed = " + note.speed);
				} else if (XML_TAG_BEARING.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double bearing = Double.parseDouble(nextText);
						note.bearing = bearing;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for bearing: \"" + nextText + '"');
						note.bearing = 0.0;
					}
	
					parse_trace("Bearing = " + note.bearing);
				} else if (XML_TAG_ACCURACY_POSITION.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double accuracyPosition = Double.parseDouble(nextText);
						note.accuracyPosition = accuracyPosition;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for accuracyPosition: \"" + nextText + '"');
						note.accuracyPosition = 0.0;
					}
					
					parse_trace("Positional accuracy = "
							+ note.accuracyPosition);
				} else if (XML_TAG_ACCURACY_ALTITUDE.equals(startTag)) {
					try {
						nextText = parser.nextText();
						double accuracyAltitude = Double.parseDouble(nextText);
						note.accuracyAltitude = accuracyAltitude;
					} catch (NumberFormatException nfe) {
						Log.e(LOGCAT_NAME, "unable to parse value for accuracyAltitude: \"" + nextText + '"');
						note.accuracyAltitude = 0.0;
					}
					
					parse_trace("Altitude accuracy = "
							+ note.accuracyAltitude);
				} else {
					parse_trace("(parseLocation) unknown XML tag: <" + startTag + ">");
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_LOCATION.equals(endTag)) {
					parse_trace("Parsing geotag complete.");
					break;
				}
			}

			eventType = parser.next();
		}
	}
	
	private void parse_trace(String msg) {
		if (PARSE_TRACING_OUTPUT_ENABLED) {
			Log.d(LOGCAT_NAME, msg);
		}
	}
	
	private synchronized long parse3339(String time) {
		if (time == null || time.length() == 0) {
			return 0;
		}
		
		if (timestamper != null) {
			try {
				timestamper.parse3339(time);
			} catch (TimeFormatException e) {
				Log.e(LOGCAT_NAME, "got TimeFormatException parsing timestamp: \"" + time + '"', e);
				return 0;
			}

			return timestamper.normalize(false);
		} else {
			Date timestamp = null;

			if (rfc3339 == null) {
				rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				rfc3339.setTimeZone(TimeZone.getTimeZone("GMT+0"));
				rfc3339.setLenient(true);
			}
			
			try {
				timestamp = rfc3339.parse(time);
			} catch (ParseException e) {
				Log.e(LOGCAT_NAME, "got ParseException parsing timestamp: \"" + time + '"', e);
				return 0;
			}

			return timestamp.getTime();			
		}
	}
}
