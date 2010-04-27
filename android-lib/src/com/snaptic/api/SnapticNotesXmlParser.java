package com.snaptic.api;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

/*
 * Copyright (c) 2010 Snaptic, Inc
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * http://snaptic.com Java Android library
 * 
 * Hugh Johnson   <hugh@snaptic.com>
 * Harry Tormey   <harry@snaptic.com>
 */

/**
 * @author Hugh Johnson
 * @author Harry Tormey
 * @version 0.1
 * 
 * Data structure that represents a Snaptic note.
 */

public class SnapticNotesXmlParser {
	public static final long ERROR_RESPONSE_NULL = -1;
	public static final long ERROR_PARAMETERS_NULL = -2;

	private static final String XML_TAG_NOTES = "notes";
	private static final String XML_TAG_NOTE = "note";
	private static final String XML_TAG_CREATED = "created_at";
	private static final String XML_TAG_MODIFIED = "modified_at";
	private static final String XML_TAG_REMINDER = "reminder_at";
	private static final String XML_TAG_ID = "id";
	private static final String XML_TAG_PARENT_ID = "parent_id";
	private static final String XML_TAG_SOURCE = "source";
	private static final String XML_TAG_SOURCE_URL = "source_url";
	private static final String XML_TAG_TEXT = "text";
	private static final String XML_TAG_SUMMARY = "summary";
	private static final String XML_TAG_USER = "user";
	private static final String XML_TAG_USER_NAME = "user_name";
	private static final String XML_TAG_CHILDREN = "children";
	private static final String XML_TAG_MODE = "mode";
	private static final String XML_TAG_PUBLIC_URL = "public_url";
	private static final String XML_TAG_TAGS = "tags";
	private static final String XML_TAG_TAG = "tag";
	private static final String XML_TAG_MEDIA = "media";
	private static final String XML_TAG_IMAGE = "image";
	private static final String XML_TAG_SRC = "src";
	private static final String XML_TAG_WIDTH = "width";
	private static final String XML_TAG_HEIGHT = "height";
	private static final String XML_TAG_ORDER = "order";
	private static final String XML_TAG_MD5 = "md5";
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
	private static final String LOGCAT_NAME = "SnapticParser";

	private Time timestamper;

	public SnapticNotesXmlParser() {		
		try {
			@SuppressWarnings("unused")
			Field field = Build.VERSION.class.getDeclaredField("SDK_INT");
			
			if (Build.VERSION.SDK_INT >= 6) {
				// We can use Time & TimeFormatException on Android 2.0.1
				// (API level 6) or greater. It crashes the VM otherwise.
				timestamper = new Time();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseNotesXml(HttpResponse response,
			ArrayList<SnapticNote> notes)
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

				if (XML_TAG_NOTE.equals(startTag)) {
					parse_trace("Parsing a note.");
					parseNote(parser, notes);
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
			ArrayList<SnapticNote> returnVal)
	throws XmlPullParserException, IOException {
		// Create a blank note object
		SnapticNote note = new SnapticNote();
		returnVal.add(note);
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				// one-liners:
				if (XML_TAG_ID.equals(startTag)) {
					long nodeId = Long.parseLong(parser.nextText());
					note.id = nodeId;
					parse_trace("Note ID is " + note.id);
				} else if (XML_TAG_PARENT_ID.equals(startTag)) {
					long parentId = Long.parseLong(parser.nextText());
					note.parentId = parentId;
					parse_trace("Parent ID is " + note.parentId);
				} else if (XML_TAG_SOURCE.equals(startTag)) {
					String source = parser.nextText();
					note.source = source;
					parse_trace("Note source is " + note.source);
				} else if (XML_TAG_SOURCE_URL.equals(startTag)) {
					String sourceUrl = parser.nextText();
					note.sourceUrl = sourceUrl;
					parse_trace("Note source URL is " + note.sourceUrl);
				} else if (XML_TAG_CREATED.equals(startTag)) {
					String creationTime = parser.nextText();
					note.creationTime = parse3339(creationTime);
					parse_trace("Creation time is " + creationTime + " (" +
							note.creationTime + ')');
				} else if (XML_TAG_MODIFIED.equals(startTag)) {
					String modificationTime = parser.nextText();
					note.modificationTime = parse3339(modificationTime);
					parse_trace("Modification time is " + modificationTime +
							" (" + note.modificationTime + ')');
				} else if (XML_TAG_REMINDER.equals(startTag)) {
					String reminderTime = parser.nextText();
					note.reminderTime = parse3339(reminderTime);
					parse_trace("Reminder time is " + reminderTime + " (" +
							note.reminderTime + ')');
				} else if (XML_TAG_TEXT.equals(startTag)) {
					String text = parser.nextText();
					note.text = text;
					parse_trace("Note text is \"" + note.text + '"');
				} else if (XML_TAG_SUMMARY.equals(startTag)) {
					String summary = parser.nextText();
					note.summary = summary;
					parse_trace("Note summary is \"" + note.summary
							+ '"');
				} else if (XML_TAG_CHILDREN.equals(startTag)) {
					long childCount = Long.parseLong(parser.nextText());
					note.children = childCount;
					parse_trace("Child count is " + note.children);
				} else if (XML_TAG_MODE.equals(startTag)) {
					String mode = parser.nextText();
					note.mode = mode;
					parse_trace("Mode is " + note.mode);
				} else if (XML_TAG_PUBLIC_URL.equals(startTag)) {
					String publicUrl = parser.nextText();
					note.publicUrl = publicUrl;
					parse_trace("Public URL is " + note.publicUrl);
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
				} else if (XML_TAG_MEDIA.equals(startTag)) {
					parse_trace("Parsing media list.");
					parseMedia(parser, note);
				}

			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_NOTE.equals(endTag)) {
					parse_trace("Parsing note complete.");
				}

				break;
			}

			eventType = parser.next();
		}
	}

	private void parseTags(XmlPullParser parser, SnapticNote note)
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
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_TAGS.equals(endTag)) {
					note.tags = tags;
					parse_trace("Note had " + note.tags.size()
							+ " tag(s).");
				}

				break;
			}

			eventType = parser.next();
		}
	}

	private void parseMedia(XmlPullParser parser, SnapticNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_IMAGE.equals(startTag)) {
					parse_trace("Parsing an image.");
					parseImage(parser, note);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_MEDIA.equals(endTag)) {
					parse_trace("Parsing media list complete.");
				}

				break;
			}

			eventType = parser.next();
		}
	}

	private void parseImage(XmlPullParser parser, SnapticNote note)
	throws XmlPullParserException, IOException {
		SnapticImage image = new SnapticImage();
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_SRC.equals(startTag)) {
					String src = parser.nextText();
					image.src = src;
					parse_trace("Source = " + src);
				} else if (XML_TAG_WIDTH.equals(startTag)) {
					int width = Integer.parseInt(parser.nextText());
					image.width = width;
					parse_trace("Width = " + width);
				} else if (XML_TAG_HEIGHT.equals(startTag)) {
					int height = Integer.parseInt(parser.nextText());
					image.height = height;
					parse_trace("Height = " + height);
				} else if (XML_TAG_ID.equals(startTag)) {
					long id = Long.parseLong(parser.nextText());
					image.id = id;
					parse_trace("Image ID = " + id);
				} else if (XML_TAG_ORDER.equals(startTag)) {
					int order = Integer.parseInt(parser.nextText());
					image.order = order;
					parse_trace("Image order = " + order);
				} else if (XML_TAG_MD5.equals(startTag)) {
					String md5 = parser.nextText();
					image.md5 = md5;
					parse_trace("Image MD5 = " + md5);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_IMAGE.equals(endTag)) {
					List<Object> mediaList = note.mediaList;

					if (mediaList == null) {
						mediaList = new ArrayList<Object>();
					}

					mediaList.add(image);
					parse_trace("Parsing image complete.");
				}

				break;
			}

			eventType = parser.next();
		}
	}

	private void parseNoteOwner(XmlPullParser parser, SnapticNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_ID.equals(startTag)) {
					long ownerId = Long.parseLong(parser.nextText());
					note.ownerId = ownerId;
					parse_trace("Owner ID = " + note.ownerId);
				} else if (XML_TAG_USER_NAME.equals(startTag)) {
					String owner = parser.nextText();
					note.owner = owner;
					parse_trace("Owner name = " + note.owner);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_USER.equals(endTag)) {
					parse_trace("Parsing owner data complete.");
				}

				break;
			}

			eventType = parser.next();
		}
	}

	private void parseLocation(XmlPullParser parser, SnapticNote note)
	throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				String startTag = parser.getName();

				if (XML_TAG_LATITUDE.equals(startTag)) {
					double latitude = Double.parseDouble(parser.nextText());
					note.latitude = latitude;
					parse_trace("Latitude = " + note.latitude);
				} else if (XML_TAG_LONGITUDE.equals(startTag)) {
					double longitude = Double.parseDouble(parser.nextText());
					note.longitude = longitude;
					parse_trace("Longitude = " + note.longitude);
				} else if (XML_TAG_ALTITUDE.equals(startTag)) {
					double altitude = Double.parseDouble(parser.nextText());
					note.altitude = altitude;
					parse_trace("Altitude = " + note.altitude);
				} else if (XML_TAG_SPEED.equals(startTag)) {
					double speed = Double.parseDouble(parser.nextText());
					note.speed = speed;
					parse_trace("Speed = " + note.speed);
				} else if (XML_TAG_BEARING.equals(startTag)) {
					double bearing = Double.parseDouble(parser.nextText());
					note.bearing = bearing;
					parse_trace("Bearing = " + note.bearing);
				} else if (XML_TAG_ACCURACY_POSITION.equals(startTag)) {
					double accuracyPosition =
						Double.parseDouble(parser.nextText());
					note.accuracyPosition = accuracyPosition;
					parse_trace("Positional accuracy = "
							+ note.accuracyPosition);
				} else if (XML_TAG_ACCURACY_ALTITUDE.equals(startTag)) {
					double accuracyAltitude =
						Double.parseDouble(parser.nextText());
					note.accuracyAltitude = accuracyAltitude;
					parse_trace("Altitude accuracy = "
							+ note.accuracyAltitude);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				String endTag = parser.getName();

				if (XML_TAG_LOCATION.equals(endTag)) {
					parse_trace("Parsing geotag complete.");
				}

				break;
			}

			eventType = parser.next();
		}
	}
	
	private void parse_trace(String msg) {
		if (PARSE_TRACING_OUTPUT_ENABLED) {
			Log.d(LOGCAT_NAME, msg);
		}
	}
	
	private long parse3339(String time) {
		if (time == null || time.length() == 0) {
			return 0;
		}
		
		if (timestamper != null) {
			try {
				timestamper.parse3339(time);
			} catch (TimeFormatException e) {
				parse_trace("got TimeFormatException parsing timestamp: \"" + time + '"');
				e.printStackTrace();
				return 0;
			}

			return timestamper.normalize(false);
		} else {
			Date timestamp = new Date();

			SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			rfc3339.setLenient(true);

			try {
				timestamp = rfc3339.parse(time);
			} catch (ParseException e) {
				parse_trace("got ParseException parsing timestamp: \"" + time + '"');
				e.printStackTrace();
				return 0;
			}

			return timestamp.getTime();			
		}
	}
}
