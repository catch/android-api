package com.snaptic.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

import com.android.http.multipart.FilePart;
import com.android.http.multipart.MultipartEntity;
import com.android.http.multipart.Part;
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
 * A library that provides a Java interface to the Snaptic API
 */

public class SnapticAPI {
	public static final int RESULT_ERROR = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_UNAUTHORIZED = 2;
	public static final int RESULT_ERROR_PARSER = 3;
	public static final int RESULT_ERROR_RESPONSE = 4;

	private static final String ENCODING = "UTF-8";
	private static final String USER_AGENT = "SnapticAPI; Java; (Android)";
	private String defaultSource = "android-lib";
    private static final String API_ENDPOINT_PASSWORD_RESET = "/forgotPassword.action";
	private static final String SNAPTIC_BASE_URL = "https://api.snaptic.com";
	private static final String API_ENDPOINT_ACCOUNT_INFO = "/v1/user";
	private static final String API_ENDPOINT_NOTES = "/v1/notes";
	private static final String API_ENDPOINT_IMAGES = "/v1/images";
	private static final String API_ENDPOINT_SEARCH = "/v1/search.xml";
	// Enable this for extra API call tracing output in the logcat.
	private static final boolean API_TRACING_OUTPUT_ENABLED = true;//false;
	private static final String LOGCAT_NAME = "SnapticAPI";

	private String basicUserAuth;
	private HttpClient httpClient;
	private Time timestamper;

	/**
	 * Java Android interface to the Snaptic API.
	 *
	 * @param String user snaptic username.
	 * @param String password password for account.
	 */
	public SnapticAPI(String user, String password) {
		String auth = user + ':' + password;
		basicUserAuth = "Basic " +  new String(Base64.encodeBase64(auth.getBytes()));
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUserAgent(params, USER_AGENT);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		httpClient = new DefaultHttpClient(params);

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

	/**
	 * Get information about a users account (i.e user name, date created, etc).
	 * 
 	 * @param  SnapticAccount data structure containing username/password.
     * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int getAccountInfo(SnapticAccount account) {
		int returnCode = RESULT_ERROR;

		if (account == null) {
			return returnCode;
		}

		HttpResponse response = null;		
		response = performGET(API_ENDPOINT_ACCOUNT_INFO, null);	

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					JSONObject json = new JSONObject(istreamToString(response.getEntity().getContent()));
					JSONObject user = json.getJSONObject("user");
					account.id = user.getLong("id");
					account.username = user.getString("user_name");
					account.email = user.getString("email");
					//account.password = password;
					account.accountCreatedOn = parse3339(user.getString("created_at")); 
					parseResult = true;
				} catch (JSONException e) {
					log("caught a JSONException processing response from GET " + API_ENDPOINT_ACCOUNT_INFO);
					e.printStackTrace();
				} catch (IOException e) {
					log("caught an IOException processing response from GET " + API_ENDPOINT_ACCOUNT_INFO);
					e.printStackTrace();
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}

	/**
	 * Get notes from users account that match query string.
	 *  
	 * @param String query search for notes that match this string.
	 * @param ArrayList<SnapticNote> notes reference to an array of SnapticNote where fetched
	 * notes will be placed.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 * */
	public int searchNotes(String query, ArrayList<SnapticNote> notes) {
		int returnCode = RESULT_ERROR;

		if (notes == null || query == null ) {
			return returnCode;
		}
		
		String endpoint = API_ENDPOINT_SEARCH;
		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("q", query));
		HttpResponse response = performGET(endpoint, params);
		
		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					SnapticNotesXmlParser xmlParser = new SnapticNotesXmlParser();
					xmlParser.parseNotesXml(response, notes);
					sync_trace("parsed " + notes.size() + " notes");
					parseResult = true;
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from GET " + endpoint);
					e.printStackTrace();
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from GET " + endpoint);
					e.printStackTrace();
				} catch (IOException e) {
					log("caught an IOException processing response from GET " + endpoint);
					e.printStackTrace();
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}
		
		return returnCode;
	}
	/**
	 * Get notes from users account.
	 *
	 * @param ArrayList<SnapticNote> notes reference to an array of SnapticNote where fetched
	 * notes will be placed.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int getNotes(ArrayList<SnapticNote> notes) {
		int returnCode = RESULT_ERROR;

		if (notes == null) {
			return returnCode;
		}

		String endpoint = API_ENDPOINT_NOTES + ".xml";
		HttpResponse response = performGET(endpoint, null);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					SnapticNotesXmlParser xmlParser = new SnapticNotesXmlParser();
					xmlParser.parseNotesXml(response, notes);
					sync_trace("parsed " + notes.size() + " notes");
					parseResult = true;
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from GET " + endpoint);
					e.printStackTrace();
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from GET " + endpoint);
					e.printStackTrace();
				} catch (IOException e) {
					log("caught an IOException processing response from GET " + endpoint);
					e.printStackTrace();
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}
	
	/**
	 * Get image data associated with Id number.
	 * 
 	 * @param long id of image to be returned.
 	 * @param File image image file returned from call.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int getImage(long id, File image) {
		int returnCode = RESULT_ERROR;
		
		if (image == null || !image.canWrite()) {
			return returnCode;
		}
		
		String endpoint = API_ENDPOINT_IMAGES + '/' + id;
		
		try {
			HttpResponse response = performGET(endpoint, null);
	
			if (response != null) {
				if (isResponseOK(response)) {
					// Copy the response data containing the image to the File
					// specified by the caller.
					if (!image.exists()) {
						image.createNewFile();
					}
					
					InputStream in = response.getEntity().getContent();
				    OutputStream out = new FileOutputStream(image);
				    
					byte[] b = new byte[8 * 1024];
					int read;
					
					while ((read = in.read(b)) != -1) {
						out.write(b, 0, read);
					}
					
					out.flush();
				    out.close();
				    in.close();
					returnCode = RESULT_OK;
				} else if (isResponseUnauthorized(response)) {
					returnCode = RESULT_UNAUTHORIZED;
				}
	
				consumeResponse(response);
			}
		} catch (FileNotFoundException e) {
			log("caught a FileNotFoundException in addImage() for image file: "
					+ image.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e) {
			log("caught a IOException in getImage() for " + endpoint);
			e.printStackTrace();
			
			// File is likely no good.
			if (image.exists()) {
				image.delete();
			}
		}

		return returnCode;
	}

	/**
	 * Post a new note.
	 *
	 * @param SnapticNote note to be posted.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int addNote(SnapticNote note) {
		int returnCode = RESULT_ERROR;

		if (note == null) {
			return returnCode;
		}
		
		String source = defaultSource;
		
		if (note.source != null && note.source.length() > 0) {
			source = note.source.toString();
		}

		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("text", note.text.toString()));
		params.add(new BasicNameValuePair("source", source));
		params.add(new BasicNameValuePair("created_at", Long.toString(note.creationTime)));
		params.add(new BasicNameValuePair("modified_at", Long.toString(note.modificationTime)));
		params.add(new BasicNameValuePair("reminder_at", Long.toString(note.reminderTime)));
		params.add(new BasicNameValuePair("latitude", Double.toString(note.latitude)));
		params.add(new BasicNameValuePair("longitude", Double.toString(note.longitude)));
		params.add(new BasicNameValuePair("altitude", Double.toString(note.altitude)));
		params.add(new BasicNameValuePair("speed", Double.toString(note.speed)));
		params.add(new BasicNameValuePair("bearing", Double.toString(note.bearing)));
		params.add(new BasicNameValuePair("accuracy_position", Double.toString(note.accuracyPosition)));
		params.add(new BasicNameValuePair("accuracy_altitude", Double.toString(note.accuracyAltitude)));
		params.add(new BasicNameValuePair("mode", note.mode.toString()));

		String endpoint = API_ENDPOINT_NOTES + ".xml";
		HttpResponse response = performPOST(endpoint, params, null);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					SnapticNotesXmlParser xmlParser = new SnapticNotesXmlParser();
					ArrayList<SnapticNote> notes = new ArrayList<SnapticNote>(1);
					xmlParser.parseNotesXml(response, notes);

					if (notes.size() > 0 && notes.get(0) != null) {
						// Copy the returned note values into the original note object
						note.copy(notes.get(0));
						parseResult = true;
					}
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from POST " + endpoint);
					e.printStackTrace();
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from POST " + endpoint);
					e.printStackTrace();
				} catch (IOException e) {
					log("caught an IOException processing response from POST " + endpoint);
					e.printStackTrace();
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}

	/**
	 * Add image to a given note.
	 *
	 * @param long id of note to append image to.
	 * @param File image file to be appended. 
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int addImage(long id, File image) {
		int returnCode = RESULT_ERROR;
		
		if (image == null) {
			return returnCode;
		}
		
		try {
			String endpoint = API_ENDPOINT_IMAGES + '/' + id;
			Part[] parts = {
					new FilePart("image", image, "image/jpeg", null)
			};
			
			HttpResponse response = performPOST(endpoint, null, parts);
	
			if (response != null) {
				if (isResponseOK(response)) {
					returnCode = RESULT_OK;
				} else if (isResponseUnauthorized(response)) {
					returnCode = RESULT_UNAUTHORIZED;
				}
	
				consumeResponse(response);
			}
		} catch (FileNotFoundException e) {
			log("caught a FileNotFoundException in addImage() for image file: "
					+ image.getAbsolutePath());
			e.printStackTrace();
		}

		return returnCode;
	}
	
	/**
	 * Edit an existing note.
	 *
	 * @param SnapticNote note to be updated. note.id must be a valid existing note id.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int editNote(SnapticNote note) {
		int returnCode = RESULT_ERROR;

		if (note == null) {
			return returnCode;
		}
		
		String source = defaultSource;
		
		if (note.source != null && note.source.length() > 0) {
			source = note.source.toString();
		}

		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("text", note.text.toString()));
		params.add(new BasicNameValuePair("source", source));
		params.add(new BasicNameValuePair("created_at", Long.toString(note.creationTime)));
		params.add(new BasicNameValuePair("modified_at", Long.toString(note.modificationTime)));
		params.add(new BasicNameValuePair("reminder_at", Long.toString(note.reminderTime)));
		params.add(new BasicNameValuePair("latitude", Double.toString(note.latitude)));
		params.add(new BasicNameValuePair("longitude", Double.toString(note.longitude)));
		params.add(new BasicNameValuePair("altitude", Double.toString(note.altitude)));
		params.add(new BasicNameValuePair("speed", Double.toString(note.speed)));
		params.add(new BasicNameValuePair("bearing", Double.toString(note.bearing)));
		params.add(new BasicNameValuePair("accuracy_position", Double.toString(note.accuracyPosition)));
		params.add(new BasicNameValuePair("accuracy_altitude", Double.toString(note.accuracyAltitude)));

		String endpoint = API_ENDPOINT_NOTES + '/' + note.id + ".xml";
		HttpResponse response = performPOST(endpoint, params, null);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					SnapticNotesXmlParser xmlParser = new SnapticNotesXmlParser();
					ArrayList<SnapticNote> notes = new ArrayList<SnapticNote>(1);
					xmlParser.parseNotesXml(response, notes);

					if (notes.size() > 0 && notes.get(0) != null) {
						// Copy the returned note values into the original note object
						note.copy(notes.get(0));
						parseResult = true;
					}
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from POST " + endpoint);
					e.printStackTrace();
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from POST " + endpoint);
					e.printStackTrace();
				} catch (IOException e) {
					log("caught an IOException processing response from POST " + endpoint);
					e.printStackTrace();
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}

	/**
	 * Delete a note.
	 *
	 * @param lond id of note to be deleted.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int deleteNote(long id) {
		int returnCode = RESULT_ERROR;
		HttpResponse response = performDELETE(API_ENDPOINT_NOTES + '/' + id);

		if (response != null) {
			if (isResponseOK(response)) {
				returnCode = RESULT_OK;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}

	public int resetPassword(String email) {
		int returnCode = RESULT_ERROR;
		HttpResponse response = null;

		if (email != null && email.length() > 0) {              
			List <NameValuePair> params = new ArrayList <NameValuePair>();
			params.add(new BasicNameValuePair("email", email));
			response = performPOST(API_ENDPOINT_PASSWORD_RESET, params, null);

			if (response != null) {
				if (isResponseOK(response)) {
					returnCode = RESULT_OK;
				}

				consumeResponse(response);
			}
		}

		return returnCode;
	}
	
	/**
	 * Delete a image.
	 *
	 * @param lond id of image to be deleted.
	 * @return int returnCode enum code indicating whether call was successful or not.
	 */	
	public int deleteImage(long id) {
		int returnCode = RESULT_ERROR;
		HttpResponse response = performDELETE(API_ENDPOINT_IMAGES + '/' + id);

		if (response != null) {
			if (isResponseOK(response)) {
				returnCode = RESULT_OK;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			}

			consumeResponse(response);
		}

		return returnCode;
	}

	private HttpResponse performGET(String method,
			List<NameValuePair> httpParams) {
		HttpGet httpget;

		if (httpParams == null || httpParams.isEmpty()) {
			httpget = new HttpGet(SNAPTIC_BASE_URL + method);
		} else {
			httpget = new HttpGet(SNAPTIC_BASE_URL + method + '?' + URLEncodedUtils.format(httpParams, "UTF-8"));
		}

		httpget.addHeader("Authorization", basicUserAuth);
		HttpResponse response = null;

		try {
			response = httpClient.execute(httpget);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing GET " + httpget.getURI());
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			log("caught IOException performing GET " + httpget.getURI());
			e.printStackTrace();
			return null;
		}

		sync_trace("GET " + httpget.getURI() + " returned " +
				response.getStatusLine().getStatusCode() + ' ' +
				response.getStatusLine().getReasonPhrase());
		return response;
	}

	private HttpResponse performPOST(String method,
			List<NameValuePair> parameters, Part[] parts) {
		HttpPost httppost = new HttpPost(SNAPTIC_BASE_URL + method);
		httppost.addHeader("Authorization", basicUserAuth);

		if (parameters != null && !parameters.isEmpty()) {
			try {
				httppost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
			} catch (UnsupportedEncodingException e) {
				log("caught UnsupportedEncodingException performing POST " + httppost.getURI());
				e.printStackTrace();
				return null;
			}
		} else if (parts != null) {
			HttpProtocolParams.setUseExpectContinue(httppost.getParams(), false);
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
		}

		HttpResponse response = null;

		try {
			response = httpClient.execute(httppost);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing POST " + httppost.getURI());
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			log("caught IOException performing POST " + httppost.getURI());
			e.printStackTrace();
			return null;
		}

		sync_trace("POST " + httppost.getURI() + " returned " +
				response.getStatusLine().getStatusCode() + ' ' +
				response.getStatusLine().getReasonPhrase());
		return response;
	}

	private HttpResponse performDELETE(String method) {
		HttpDelete httpdelete = new HttpDelete(SNAPTIC_BASE_URL + method);
		httpdelete.addHeader("Authorization", basicUserAuth);
		HttpResponse response = null;

		try {
			response = httpClient.execute(httpdelete);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing DELETE " + httpdelete.getURI());
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			log("caught IOException performing DELETE " + httpdelete.getURI());
			e.printStackTrace();
			return null;
		}

		sync_trace("DELETE " + httpdelete.getURI() + " returned " +
				response.getStatusLine().getStatusCode() + ' ' +
				response.getStatusLine().getReasonPhrase());
		return response;
	}

	private boolean isResponseOK(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}

	private boolean isResponseUnauthorized(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED);
	}
	
	private void consumeResponse(HttpResponse response) {
		if (response != null && response.getEntity() != null) {
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				log("caught an IOException attempting to consume the content of an HttpResponse");
				e.printStackTrace();
			}
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
				log("caught a TimeFormatException parsing timestamp: \"" + time + '"');
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
				log("caught a ParseException parsing timestamp: \"" + time + '"');
				e.printStackTrace();
				return 0;
			}

			return timestamp.getTime();			
		}
	}

	// from Translate.java
	/*
	 * Copyright (C) 2008 Google Inc.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	private static String istreamToString(InputStream inputStream)
	throws IOException {
		StringBuilder outputBuilder = new StringBuilder();
		String string;

		if (inputStream != null) {
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(inputStream, ENCODING));

			while (null != (string = reader.readLine())) {
				outputBuilder.append(string).append('\n');
			}

			reader.close();
		}

		return outputBuilder.toString();
	}

	// Helper function to log error messages.
	private void log(String msg) {
		Log.e(LOGCAT_NAME, msg);
	}

	// Helper function to log tracing messages.
	private void sync_trace(String msg) {
		if (API_TRACING_OUTPUT_ENABLED) {
			Log.d(LOGCAT_NAME, msg);
		}
	}

	/**
	 * Helper function to turn internal function return codes into readable text.
	 *
	 * @param int code enum code indicating whether call was successful or not.
	 * @return String code converted into readable string.
	 */	
	public static String resultToString(int code) {
		switch (code) {
			case RESULT_ERROR:
				return "RESULT_ERROR";
			case RESULT_OK:
				return "RESULT_OK";
			case RESULT_UNAUTHORIZED:
				return "RESULT_UNAUTHORIZED";
			case RESULT_ERROR_PARSER:
				return "RESULT_ERROR_PARSER";
			case RESULT_ERROR_RESPONSE:
				return "RESULT_ERROR_RESPONSE";
			default:
				return "UNKNOWN (" + code + ")";
		}
	}
}
