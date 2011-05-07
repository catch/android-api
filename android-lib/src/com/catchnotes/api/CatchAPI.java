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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

import com.android.http.multipart.FilePart;
import com.android.http.multipart.MultipartEntity;
import com.android.http.multipart.Part;
import com.android.http.multipart.StringPart;

/**
 * Java Android interface to the Catch API.
 */
public class CatchAPI {
    public static final int RESULT_ERROR = 0;
    public static final int RESULT_OK = 1;
    public static final int RESULT_UNAUTHORIZED = 2;
    public static final int RESULT_ERROR_PENDING = 3;
    public static final int RESULT_ERROR_PARSER = 4;
    public static final int RESULT_ERROR_RESPONSE = 5;
    public static final int RESULT_AVAILABLE = 6;
    public static final int RESULT_UNAVAILABLE = 7;
    public static final int RESULT_BAD_REQUEST = 8;
    public static final int RESULT_NOT_FOUND = 9;
    public static final int RESULT_OVER_QUOTA = 10;
    public static final int RESULT_SERVER_ERROR = 11;
    public static final int RESULT_UNSUPPORTED_MEDIA = 12;

	private static final String ENCODING = "UTF-8";
    private String userAgent = "CatchAPI (Android)";
    private String source = "CatchAPI for Android";
	public static final String LOGCAT_NAME = "CatchAPI";
    private static final int BUFFER_SIZE = 8 * 1024;
    
    private String catchBaseUrl = "https://api.catch.com";
	private static final String API_ENDPOINT_ACCOUNT_INFO = "/v2/user";
	private static final String API_ENDPOINT_NOTES = "/v2/notes";
    private static final String API_ENDPOINT_BULK_NOTES = "/v2/bulk_notes";
	private static final String API_ENDPOINT_COMMENTS = "/v2/comments";
	private static final String API_ENDPOINT_COMMENT = "/v2/comment";
	private static final String API_ENDPOINT_MEDIA = "/v2/media";
    
    public static final String IMAGE_SMALL = "small";
    public static final String IMAGE_MEDIUM = "medium";
    public static final String IMAGE_LARGE = "large";

    private VersionedCatchHttpClient httpClientAccessToken = null;
    private HttpClient httpClientNoToken = null;

	private Time timestamper;
	private SimpleDateFormat rfc3339;
	private Context mContext;
	public boolean loggingEnabled = false;
	
	/** 
	 * Constructor.
	 * 
	 * @param appName The name of your application.
	 * @param context Android context.
	 */
	public CatchAPI(String appName, Context context) {
		source = appName;
		mContext = context;
		String version  = "x.xx";

		try {
			PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			version = info.versionName;
		} catch (Exception e) { }
		
		StringBuilder locale = new StringBuilder(5);
		String language = Locale.getDefault().getLanguage();

		if (language != null) {
			locale.append(language);
			String country = Locale.getDefault().getCountry();

			if (country != null) {
				locale.append('-');
				locale.append(country);
			}
		}

		userAgent = appName + '/' + version + " (Android; " +
			Build.VERSION.RELEASE + "; " + Build.BRAND + "; " +
			Build.MODEL + "; " + locale.toString() + ')';

		try {
			if (Integer.parseInt(Build.VERSION.SDK) >= Build.VERSION_CODES.ECLAIR_0_1) {
				// We can use Time & TimeFormatException on Android 2.0.1
				// (API level 6) or greater. It crashes the VM otherwise.
				timestamper = new Time();
			}
		} catch (Exception e) { }
	}
	
	/**
	 * Sign in with a username and password.
	 * 
	 * @param user Username or email address of the account.
	 * @param password Account password.
	 * @param account An optional CatchAccount object that will be filled in with 
	 * account information.
	 * @return RESULT_OK on success.
	 */
	public int signIn(String user, String password, CatchAccount account) {
		CatchAccount acct = account;
		if (acct == null) {
			acct = new CatchAccount();
		}
		
		int returnCode = getAccountInfo(user, password, acct);
		if (returnCode == RESULT_OK) {
			setAccessToken(acct.auth_token);
		}
		
		return returnCode;
	}
	
	/**
	 * Sign out.
	 */
	public void close() {
		if (httpClientAccessToken != null) {
			httpClientAccessToken.close();
		}
	}
	
	private VersionedCatchHttpClient getHttpClient() {
		if (httpClientAccessToken == null) {
			httpClientAccessToken = VersionedCatchHttpClient.newInstance(userAgent, mContext);
		}

		return httpClientAccessToken;
	}
	
	private HttpClient getHttpClientNoToken() {
		if (httpClientNoToken == null) {
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setContentCharset(params, "UTF-8");
			httpClientNoToken = new DefaultHttpClient(params);
		}

		return httpClientNoToken;
	}

	/**
	 * Sign-in using a previously obtained access token.
	 * 
	 * @param accessToken The access token.
	 */
	public void setAccessToken(String accessToken) {
        getHttpClient().setAccessToken(accessToken);
	}

	/**
	 * Get information about a user's account (user name, date created, etc).
	 * 
	 * @param user Username or email address.  Pass null if already signed in.
	 * @param password Account password.  Pass null if already signed in.
	 * @param account CatchAccount object where information will be returned.
	 * @return RESULT_OK on success.
	 */
	public int getAccountInfo(String user, String password, CatchAccount account) {
		int returnCode = RESULT_ERROR;

		if (account == null) {
			return returnCode;
		}

		HttpResponse response = null;		

		if (user != null && password != null && user.length() > 0 && password.length() > 0) {
			// Use user auth to get user info and obtain auth_token
			response = performPOST(API_ENDPOINT_ACCOUNT_INFO, null, null, false, user + ':' + password);
		} else {
			response = performPOST(API_ENDPOINT_ACCOUNT_INFO, null, null, true, null);
		}

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					JSONObject json = new JSONObject(istreamToString(response.getEntity().getContent()));
					
					// "user" block
					JSONObject userblock = json.getJSONObject("user");
					account.id = userblock.getLong("id");
					account.username = userblock.getString("user_name");
					account.email = userblock.getString("email");
					account.accountCreatedOn = parse3339(userblock.getString("created_at"));
					account.auth_token = userblock.getString("access_token");
					
					// "account_limits" block
					if (userblock.has("account_limits")) {
						JSONObject limitsblock = userblock.getJSONObject("account_limits");
						account.periodLimit = limitsblock.getLong("monthly_upload_limit");
					}
					
					// "account_upload_activity_periodic" block
					if (userblock.has("account_upload_activity_periodic")) {
						JSONArray activityarray = userblock.getJSONArray("account_upload_activity_periodic");
						
						if (activityarray.length() > 0) {
							JSONObject currentPeriod = activityarray.getJSONObject(0);
							account.periodStart = parse3339(currentPeriod.getString("period_start"));
							account.periodUsage = currentPeriod.getLong("period_activity");
							account.periodEnd = parse3339(currentPeriod.getString("period_end"));
						}
					}
					
					// account info
					if (userblock.has("account_level")) {
						account.accountLevel = userblock.getInt("account_level");
					}
					
					if (userblock.has("account_level_desc")) {
						account.accountDescription = userblock.getString("account_level_desc");
					}
					
					// "account_subscription_end_advisory"
					if (userblock.has("account_subscription_end_advisory")) {
						String subEnd = userblock.getString("account_subscription_end_advisory");
						
						if (subEnd != null && !"null".equals(subEnd)) {
							account.subscriptionEnd = parse3339(subEnd);
						}
					}
					
					parseResult = true;
				} catch (JSONException e) {
					log("caught a JSONException processing response from POST " + API_ENDPOINT_ACCOUNT_INFO, e);
				} catch (IOException e) {
					log("caught an IOException processing response from POST " + API_ENDPOINT_ACCOUNT_INFO, e);
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			} else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			}

			consumeResponse(response);
		}

		return returnCode;
	}


	/**
	 * Fetches a list of note references representing all of the user's notes.
	 * 
	 * @param noteRefs List where fetched note refs will be returned.
	 * @return RESULT_OK on success.
	 */
    public int getSyncV2(List<CatchNoteRef> noteRefs) {
        int returnCode = RESULT_ERROR;

        if (noteRefs == null) {
            return returnCode;
        }

        String endpoint = API_ENDPOINT_NOTES + ".xml";
        HttpResponse response = performGET(endpoint, null, true);

        if (response != null) {
            if (isResponseOK(response)) {
                boolean parseResult = false;

                try {
                    CatchNotesXmlParser xmlParser = new CatchNotesXmlParser();
                    xmlParser.parseSyncV2XML(response, noteRefs);
                    parseResult = true;
                } catch (IllegalArgumentException e) {
                    log("caught an IllegalArgumentException processing response from GET " + endpoint, e);
                } catch (XmlPullParserException e) {
                    log("caught an XmlPullParserException processing response from GET " + endpoint, e);
                } catch (IOException e) {
                    log("caught an IOException processing response from GET " + endpoint, e);
                }

                returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
            } else if (isResponseUnauthorized(response)) {
                returnCode = RESULT_UNAUTHORIZED;
            } else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			}

            consumeResponse(response);
        }

        return returnCode;
    }

    /**
     * Fetches a list of specific requested notes.
     * 
     * @param noteRefs List of note refs representing the notes you want to fetch.
     * @param notes List where fetched notes will be returned.
     * @return RESULT_OK on success.
     */
    public int getNotesBulk(List<CatchNoteRef> noteRefs, ArrayList<CatchNote> notes) {
        int returnCode = RESULT_ERROR;

        if (notes == null) {
            return returnCode;
        }

        String endpoint;
        endpoint = API_ENDPOINT_BULK_NOTES + ".xml";
        StringBuilder bulkNotes = new StringBuilder("{\"notes\": [\"");
        Iterator<CatchNoteRef> syncIterator = noteRefs.iterator();
        
        while (syncIterator.hasNext()) {
            bulkNotes.append(syncIterator.next().nodeId);
            
            if (syncIterator.hasNext()) {
                bulkNotes.append("\", \"");
            }
        }
        
        bulkNotes.append("\"]}");
        List <NameValuePair> params = new ArrayList <NameValuePair>();
        params.add(new BasicNameValuePair("bulk", bulkNotes.toString()));
        HttpResponse response = performPOST(endpoint, params, null, true, null);

        if (response != null) {
            if (isResponseOK(response)) {
                boolean parseResult = false;

                try {
                    CatchNotesXmlParser xmlParser = new CatchNotesXmlParser();
                    xmlParser.parseNotesXml(response, notes, -1);
                    parseResult = true;
                } catch (IllegalArgumentException e) {
                    log("caught an IllegalArgumentException processing response from POST " + endpoint, e);
                } catch (XmlPullParserException e) {
                    log("caught an XmlPullParserException processing response from POST " + endpoint, e);
                } catch (IOException e) {
                    log("caught an IOException processing response from POST " + endpoint, e);
                }

                returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
            } else if (isResponseUnauthorized(response)) {
                returnCode = RESULT_UNAUTHORIZED;
            } else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			}

            consumeResponse(response);
        }
        
        return returnCode;
    }

    /**
     * Fetches a list of all of the user's notes.
     * 
     * @param notes List where fetched notes will be returned.
     * @return RESULT_OK on success.
     */
	public int getNotes(ArrayList<CatchNote> notes) {
		return getComments(-1, notes);
	}

	/**
	 * Fetches a list of comments belonging to a certain note.
	 * 
	 * @param parentId The parent note of the requested comments.
	 * @param notes List where fetched comments will be returned.
	 * @return RESULT_OK on success.
	 */
	public int getComments(long parentId, ArrayList<CatchNote> notes) {
		int returnCode = RESULT_ERROR;

		if (notes == null) {
			return returnCode;
		}

		String endpoint;
		List<NameValuePair> params = null;
		
		if (parentId != -1) {
			endpoint = API_ENDPOINT_COMMENTS + '/' + parentId + ".xml";
		} else {
			endpoint = API_ENDPOINT_NOTES + ".xml";
			params = new ArrayList <NameValuePair>();
	        params.add(new BasicNameValuePair("full", "1"));
		}
		
		HttpResponse response = performGET(endpoint, params, true);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					CatchNotesXmlParser xmlParser = new CatchNotesXmlParser();
					xmlParser.parseNotesXml(response, notes, parentId);
					parseResult = true;
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from GET " + endpoint, e);
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from GET " + endpoint, e);
				} catch (IOException e) {
					log("caught an IOException processing response from GET " + endpoint, e);
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			} else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			}

			consumeResponse(response);
		}

		return returnCode;
	}
	
	/**
	 * Gets a data stream for a media item.
	 * 
	 * @param uri The URI of the media to fetch.  See {@link CatchMedia#src}.
	 * @param size If the media is an image, then you may pass IMAGE_SMALL, 
	 * IMAGE_MEDIUM, or IMAGE_LARGE.  Otherwise pass null.
	 * @param outputStream Data is returned here.
	 * @return RESULT_OK on success.
	 */
	public int getMedia(String uri, String size, OutputStream outputStream) {
		int returnCode = RESULT_ERROR;
		
		if (uri == null || outputStream == null) {
			return returnCode;
		}
		
		try {
            List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
            
			if (size != null) {
	            httpParams.add(new BasicNameValuePair("size", size));
			}
			
			if (uri.contains("viewImage.action?")) {
				// Legacy viewImage.action image source URLs need to be pulled
				// apart a bit so as to fit into the new logic
				httpParams.add(new BasicNameValuePair("imageId", Uri.parse(uri).getQueryParameter("imageId")));
				uri = uri.split("\\?")[0];
			}
			
			HttpResponse response = performGET(uri, (httpParams.size() > 0) ? httpParams : null, true);
	
			if (response != null) {
				if (isResponseOK(response)) {
					BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent(), BUFFER_SIZE);
				    
					byte[] b = new byte[BUFFER_SIZE];
					int read;
					
					while ((read = inputStream.read(b)) != -1) {
						outputStream.write(b, 0, read);
					}
					
					outputStream.flush();
					outputStream.close();
				    inputStream.close();
					returnCode = RESULT_OK;
				} else if (isResponseUnauthorized(response)) {
					returnCode = RESULT_UNAUTHORIZED;
				} else if (isResponseNotFound(response)) {
					returnCode = RESULT_NOT_FOUND;
				} else if (isResponseServerError(response)) {
					returnCode = RESULT_SERVER_ERROR;
				}
	
				consumeResponse(response);
			}
		} catch (IOException e) {
			log("caught a IOException in getImage() for " + uri, e);
		}

		return returnCode;
	}
	
	/**
	 * Adds a new note to the account.
	 * 
	 * @param note Note to be added.
	 * @return RESULT_OK on success.
	 */
	public int addNote(CatchNote note) {
		int returnCode = RESULT_ERROR;

		if (note == null) {
			return returnCode;
		}
		
		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("text", (note.text == null) ? "" : note.text.toString()));
		params.add(new BasicNameValuePair("created_at", Long.toString(note.creationTime)));
		params.add(new BasicNameValuePair("modified_at", Long.toString(note.modificationTime)));
		
		if (note.source == null || note.source.length() == 0 || note.source.equals(CatchNote.NOT_SET)) {
			params.add(new BasicNameValuePair("source", source));
		} else {
			params.add(new BasicNameValuePair("source", note.source));
			
			if (note.sourceUrl != null && note.source.length() > 0 && note.sourceUrl.equals(CatchNote.NOT_SET)) {
				params.add(new BasicNameValuePair("source_url", note.sourceUrl));
			}
		}

		//XXXaes
        if (note.annotations != null) {
            for (String key: note.annotations.keySet()) {
                params.add(new BasicNameValuePair(CatchNotesXmlParser.CATCH_NAMESPACE_PREFIX + ":" + key, note.annotations.get(key)));
            }
        }

		String endpoint;

		if (note.parentId != -1) {
			// adding a comment
			endpoint = API_ENDPOINT_COMMENTS + '/' + note.parentNodeId + ".xml";
		} else {
			// adding a note
			endpoint = API_ENDPOINT_NOTES + ".xml";
			params.add(new BasicNameValuePair("mode", (note.mode == null) ? CatchNote.MODE_PRIVATE : note.mode.toString()));
			params.add(new BasicNameValuePair("reminder_at", Long.toString(note.reminderTime)));
			params.add(new BasicNameValuePair("latitude", Double.toString(note.latitude)));
			params.add(new BasicNameValuePair("longitude", Double.toString(note.longitude)));
			params.add(new BasicNameValuePair("altitude", Double.toString(note.altitude)));
			params.add(new BasicNameValuePair("speed", Double.toString(note.speed)));
			params.add(new BasicNameValuePair("bearing", Double.toString(note.bearing)));
			params.add(new BasicNameValuePair("accuracy_position", Double.toString(note.accuracyPosition)));
			params.add(new BasicNameValuePair("accuracy_altitude", Double.toString(note.accuracyAltitude)));
		}
		
		HttpResponse response = performPOST(endpoint, params, null, true, null);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					CatchNotesXmlParser xmlParser = new CatchNotesXmlParser();
					ArrayList<CatchNote> notes = new ArrayList<CatchNote>(1);
					xmlParser.parseNotesXml(response, notes, note.parentId);

					if (notes.size() > 0 && notes.get(0) != null) {
						// Copy the returned note values into the original note object
						note.copy(notes.get(0));
						parseResult = true;
					}
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from POST " + endpoint, e);
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from POST " + endpoint, e);
				} catch (IOException e) {
					log("caught an IOException processing response from POST " + endpoint, e);
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			} else if (isResponseNotFound(response)) {
				returnCode = RESULT_NOT_FOUND;
			} else if (isResponsePaymentRequired(response)) {
				returnCode = RESULT_OVER_QUOTA;
			} else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			} else {
                returnCode = RESULT_ERROR_PENDING;
            }

			consumeResponse(response);
		}

		return returnCode;
	}

	/**
	 * Adds a media item to a note.
	 * 
	 * @param media A CatchMedia object that specifies the media data and note ID.
	 * @return RESULT_OK on success.
	 */
	public int addMedia(CatchMedia media) {
		int returnCode = RESULT_ERROR;
		
		if (media == null || media.data == null || media.content_type == null || media.note_id == null) {
			return returnCode;
		}
		
		try {
			String endpoint = API_ENDPOINT_MEDIA + '/' + media.note_id;
			
			StringPart contentType = new StringPart("content_type", media.content_type, HTTP.UTF_8);
			StringPart createdAt = new StringPart("created_at", Long.toString(media.created_at), HTTP.UTF_8);
			StringPart voiceHint = new StringPart("voicenote_hint", "true", HTTP.UTF_8);
			FilePart data = new FilePart("data", media.data, media.content_type, null);
			
			HttpResponse response;
			
			if (media.voice_hint) {
				response = performPOST(endpoint, null, new Part[] { contentType, createdAt, voiceHint, data }, true, null);
			} else {
				response = performPOST(endpoint, null, new Part[] { contentType, createdAt, data }, true, null);
			}
	
			if (response != null) {
				if (isResponseOK(response)) {
					boolean parseResult = false;
					
					try {
						JSONObject json = new JSONObject(istreamToString(response.getEntity().getContent()));
						media.id = json.getString("id");
						media.src = json.getString("src");
						media.size = json.getLong("size");
						media.content_type = json.getString("type");
						media.created_at = parse3339(json.getString("created_at"));
						media.voice_hint = json.getBoolean("voicenote_hint");
						parseResult = true;
					} catch (JSONException e) {
						log("caught a JSONException processing response from POST " + endpoint, e);
					} catch (IOException e) {
						log("caught an IOException processing response from POST " + endpoint, e);
					}
					
					returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
				} else if (isResponsePaymentRequired(response)) {
					returnCode = RESULT_OVER_QUOTA;
				} else if (isResponseUnsupportedMedia(response)) {
					returnCode = RESULT_UNSUPPORTED_MEDIA;
				} else if (isResponseUnauthorized(response)) {
					returnCode = RESULT_UNAUTHORIZED;
				} else if (isResponseBadRequest(response)) {
					returnCode = RESULT_BAD_REQUEST;
				} else if (isResponseServerError(response)) {
					returnCode = RESULT_SERVER_ERROR;
				}
	
				consumeResponse(response);
			}
		} catch (FileNotFoundException e) {
			log("caught a FileNotFoundException in addMedia() for file: " + media.data.getAbsolutePath(), e);
		}

		return returnCode;
	}
	
	/**
	 * Edits an existing note.
	 * 
	 * @param note Note to be updated. note.id must be a valid existing note id.
	 * @return RESULT_OK on success.
	 */
	public int editNote(CatchNote note) {
		int returnCode = RESULT_ERROR;

		if (note == null) {
			return returnCode;
		}

		List <NameValuePair> params = new ArrayList <NameValuePair>();
		params.add(new BasicNameValuePair("text", (note.text == null) ? "" : note.text.toString()));
		params.add(new BasicNameValuePair("source", source));
		
		// TODO: if we supply server_modified_at, the backend will perform conflict detection
		//params.add(new BasicNameValuePair("server_modified_at", Long.toString(note.serverModifiedAt)));
		
		params.add(new BasicNameValuePair("modified_at", Long.toString(note.modificationTime)));

		String endpoint;
		
		if (note.parentId != -1) {
			// editing a comment
			params.add(new BasicNameValuePair("comment", note.id));
			endpoint = API_ENDPOINT_COMMENT + '/' + note.parentNodeId + ".xml";
		} else {
			// editing a note
			endpoint = API_ENDPOINT_NOTES + '/' + note.id + ".xml";
			params.add(new BasicNameValuePair("mode", (note.mode == null) ? CatchNote.MODE_PRIVATE : note.mode.toString()));
			params.add(new BasicNameValuePair("reminder_at", Long.toString(note.reminderTime)));
		}

		//XXXaes
        if (note.annotations != null) {
            for (String key: note.annotations.keySet()) {
                params.add(new BasicNameValuePair(CatchNotesXmlParser.CATCH_NAMESPACE_PREFIX + ":" + key, note.annotations.get(key)));
            }
        }

		HttpResponse response = performPOST(endpoint, params, null, true, null);

		if (response != null) {
			if (isResponseOK(response)) {
				boolean parseResult = false;

				try {
					CatchNotesXmlParser xmlParser = new CatchNotesXmlParser();
					ArrayList<CatchNote> notes = new ArrayList<CatchNote>(1);
					xmlParser.parseNotesXml(response, notes, note.parentId);

					if (notes.size() > 0 && notes.get(0) != null) {
						// Copy the returned note values into the original note object
						note.copy(notes.get(0));
						parseResult = true;
					}
				} catch (IllegalArgumentException e) {
					log("caught an IllegalArgumentException processing response from POST " + endpoint, e);
				} catch (XmlPullParserException e) {
					log("caught an XmlPullParserException processing response from POST " + endpoint, e);
				} catch (IOException e) {
					log("caught an IOException processing response from POST " + endpoint, e);
				}

				returnCode = (parseResult == true) ? RESULT_OK : RESULT_ERROR_RESPONSE;
			} else if (isResponseUnauthorized(response)) {
				returnCode = RESULT_UNAUTHORIZED;
			} else if (isResponseNotFound(response)) {
				returnCode = RESULT_NOT_FOUND;
			} else if (isResponsePaymentRequired(response)) {
				returnCode = RESULT_OVER_QUOTA;
			} else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			} else {
                returnCode = RESULT_ERROR_PENDING;
            }

			consumeResponse(response);
		}

		return returnCode;
	}

	/**
	 * Deletes a note.
	 * 
	 * @param id ID of the note to be deleted
	 * @param parentNodeId If you are deleting a comment, this is the parent note's ID.
	 * @return RESULT_OK on success
	 */
    public int deleteNote(String id, String parentNodeId) {
        int returnCode = RESULT_ERROR;
        HttpResponse response;

        if (parentNodeId == null || CatchNote.NODE_ID_NEVER_SYNCED.equals(parentNodeId)) {
            response = performDELETE(API_ENDPOINT_NOTES + '/' + id, null);
        } else {
            List <NameValuePair> params = new ArrayList <NameValuePair>();
            params.add(new BasicNameValuePair("comment", id));
            response = performDELETE(API_ENDPOINT_COMMENT + '/' + parentNodeId, params);
        }

        if (response != null) {
            if (isResponseOK(response)) {
                returnCode = RESULT_OK;
            } else if (isResponseUnauthorized(response)) {
                returnCode = RESULT_UNAUTHORIZED;
            } else if (isResponseNotFound(response)) {
                returnCode = RESULT_NOT_FOUND;
            } else if (isResponseServerError(response)) {
				returnCode = RESULT_SERVER_ERROR;
			} else {
                returnCode = RESULT_ERROR_PENDING;
            }

            consumeResponse(response);
        }

        return returnCode;
    }
    
    /**
     * Deletes a media item.
     * 
     * @param noteId The note containing the media you want to remove.
     * @param mediaId ID of the media.
     * @return RESULT_OK on success.
     */
	public int deleteMedia(String noteId, String mediaId) {
		int returnCode = RESULT_ERROR;
		HttpResponse response = performDELETE(API_ENDPOINT_MEDIA + '/' + noteId + '/' + mediaId, null);

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
			List<NameValuePair> httpParams, boolean useToken) {
		HttpGet httpget;
		String uri = method;
		
		if (!uri.startsWith("http")) {
			// method isn't a fully-qualified URI
			uri = catchBaseUrl + uri;
		}
		
		if (httpParams == null || httpParams.isEmpty()) {
			httpget = new HttpGet(uri);
		} else {
			httpget = new HttpGet(uri + '?' + URLEncodedUtils.format(httpParams, "UTF-8"));
		}

		HttpResponse response = null;

		try {
			response = useToken ? getHttpClient().execute(httpget) : getHttpClientNoToken().execute(httpget);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing GET " + httpget.getURI(), e);
			return null;
		} catch (UnknownHostException e) {
			log("caught UnknownHostException performing GET " + httpget.getURI(), null);
			return null;
		} catch (IOException e) {
			log("caught IOException performing GET " + httpget.getURI(), e);
			return null;
		}

		sync_trace("GET " + httpget.getURI() + " returned " +
				response.getStatusLine().getStatusCode() + ' ' +
				response.getStatusLine().getReasonPhrase());
		return response;
	}
	
	private HttpResponse performPOST(String method,
			List<NameValuePair> parameters, Part[] parts, boolean useToken, String auth) {
		HttpPost httppost = new HttpPost(catchBaseUrl + method);

		if (parameters != null && !parameters.isEmpty()) {
			try {
				httppost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
			} catch (UnsupportedEncodingException e) {
				log("caught UnsupportedEncodingException performing POST " + httppost.getURI(), e);
				return null;
			}
		} else if (parts != null) {
			HttpProtocolParams.setUseExpectContinue(httppost.getParams(), false);
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
		}
		
		if (auth != null) {
			if (useToken) {
				return null;
			}
			
			try {
				httppost.addHeader("Authorization", "Basic " +  new String(Base64.encodeBase64(auth.getBytes("UTF-8"))));
			} catch (UnsupportedEncodingException e) {
				Log.e(LOGCAT_NAME, "unable to perform basic auth, UTF-8 encoding not supported!");
				return null;
			}
		}

		HttpResponse response = null;

		try {
			response = useToken ? getHttpClient().execute(httppost) : getHttpClientNoToken().execute(httppost);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing POST " + httppost.getURI(), e);
			return null;
		} catch (IOException e) {
			log("caught IOException performing POST " + httppost.getURI(), e);
			return null;
		}

		sync_trace("POST " + httppost.getURI() + " returned " +
				response.getStatusLine().getStatusCode() + ' ' +
				response.getStatusLine().getReasonPhrase());
		return response;
	}

	private HttpResponse performDELETE(String method, List<NameValuePair> httpParams) {
		HttpDelete httpdelete;
		
		if (httpParams == null || httpParams.isEmpty()) {
			httpdelete = new HttpDelete(catchBaseUrl + method);
		} else {
			httpdelete = new HttpDelete(catchBaseUrl + method + '?' + URLEncodedUtils.format(httpParams, "UTF-8"));
		}
		
		HttpResponse response = null;

		try {
			response = getHttpClient().execute(httpdelete);
		} catch (ClientProtocolException e) {
			log("caught ClientProtocolException performing DELETE " + httpdelete.getURI(), e);
			return null;
		} catch (IOException e) {
			log("caught IOException performing DELETE " + httpdelete.getURI(), e);
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

	private boolean isResponseNotFound(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND);
	}

	private boolean isResponseBadRequest(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST);
	}
	
	private boolean isResponsePaymentRequired(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_PAYMENT_REQUIRED);
	}
	
	private boolean isResponseUnsupportedMedia(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
	}
	
	private boolean isResponseServerError(HttpResponse response) {
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}
	
	private void consumeResponse(HttpResponse response) {
		if (response != null && response.getEntity() != null) {
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				log("caught an IOException attempting to consume the content of an HttpResponse", e);
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
				log("caught a TimeFormatException parsing timestamp: \"" + time + '"', e);
				return 0;
			}

			return timestamper.normalize(false);
		} else {
			Date timestamp = new Date();

			if (rfc3339 == null) {
				rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				rfc3339.setTimeZone(TimeZone.getTimeZone("GMT+0"));
				rfc3339.setLenient(true);
			}

			try {
				timestamp = rfc3339.parse(time);
			} catch (ParseException e) {
				log("caught a ParseException parsing timestamp: \"" + time + '"', e);
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
	private static String istreamToString(InputStream inputStream) throws IOException {
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

	// Helper function to log error messages & exceptions.
	private void log(String msg, Exception e) {
		Log.e(LOGCAT_NAME, msg, e);
	}

	// Helper function to log tracing messages.
	private void sync_trace(String msg) {
		if (loggingEnabled) {
			Log.d(LOGCAT_NAME, msg);
		}
	}

	/**
	 * Helper function to turn return codes into readable text.
	 * 
	 * @param code The code to convert.
	 * @return The String representation of the code.
	 */
	public static String resultToString(int code) {
		switch (code) {
			case RESULT_ERROR:
				return "RESULT_ERROR";
			case RESULT_OK:
				return "RESULT_OK";
			case RESULT_UNAUTHORIZED:
				return "RESULT_UNAUTHORIZED";
			case RESULT_ERROR_PENDING:
				return "RESULT_ERROR_PENDING";
			case RESULT_ERROR_PARSER:
				return "RESULT_ERROR_PARSER";
			case RESULT_ERROR_RESPONSE:
				return "RESULT_ERROR_RESPONSE";
			case RESULT_AVAILABLE:
				return "RESULT_AVAILABLE";
			case RESULT_UNAVAILABLE:
				return "RESULT_UNAVAILABLE";
			case RESULT_BAD_REQUEST:
				return "RESULT_BAD_REQUEST";
			case RESULT_NOT_FOUND:
				return "RESULT_NOT_FOUND";
			case RESULT_OVER_QUOTA:
				return "RESULT_OVER_QUOTA";
			case RESULT_SERVER_ERROR:
				return "RESULT_SERVER_ERROR";
			case RESULT_UNSUPPORTED_MEDIA:
				return "RESULT_UNSUPPORTED_MEDIA";
			default:
				return "UNKNOWN (" + code + ")";
		}
	}
}
