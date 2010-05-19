package com.snaptic.WODWhacker;
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
 *
 * AutoUpdate
 * 
 * A class for updating an android app based on an xml file containing a version number 
 * and a link to an apk.
 * 
 * Harry Tormey   <harry@snaptic.com>
 */

import java.io.IOException;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.snaptic.WODWhacker.R;

public class AutoUpdate extends Service {
	private AutoUpdateTask mTask;
	private final String PACKAGE_NAME     		= "com.snaptic.WODWhacker";//Is their a way to get this at runtime? -htormey 
	private final String UPDATE_XML_FILE  		= "/update.xml";
	private final String UPDATE_XML_FILE_TAG	= "wodwhacker-beta-update";
	private final String APP_NAME         		= "wodwhackerbeta";
	private final String HTTPS_BASE_URL   		= "http://harrytormey.com/";
	private final String HTTPS_UPDATE_URL 		= HTTPS_BASE_URL + APP_NAME + UPDATE_XML_FILE;
	private final String USER_AGENT       		= "AutoUpdate/1.0; Java; (Android)";
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		(mTask = new AutoUpdateTask()).execute(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
			mTask.cancel(true);
		}
	}

	public IBinder onBind(Intent intent) {
		return null;
	}

	public static void schedule(Context context) {
		Random generator = new Random();

		final Intent intent = new Intent(context, AutoUpdate.class);
		final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);
		final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pending);
		alarm.setRepeating(AlarmManager.ELAPSED_REALTIME,
						   AlarmManager.INTERVAL_HOUR * generator.nextInt(24),
						   AlarmManager.INTERVAL_DAY, pending);
	}

	private class AutoUpdateTask extends AsyncTask<Service, Void, Void> {
		private HttpClient mClient;

		private NotificationManager mManager;
		
		@Override
		public void onPreExecute() {
			final HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setUserAgent(params, USER_AGENT);
			HttpProtocolParams.setContentCharset(params, "UTF-8");
			mClient = new DefaultHttpClient(params);
			mManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		}

		@Override
		protected Void doInBackground(Service... services) {
			try {
				
				PackageInfo info = services[0].getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
				int versionCode = info.versionCode;
				HttpGet httpget = new HttpGet(HTTPS_UPDATE_URL);
				
				try {
					HttpResponse response =  mClient.execute(httpget);
					
					if (isResponseOK(response)) {
						Log.d(WorkoutEditor.LOGCATNAME, "DEBUG AUTOUPDATE Response from get is ok! ");
						Uri uri = checkVersion(response, versionCode);
						
						if (uri != null) {
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							PendingIntent contentIntent = PendingIntent.getActivity(services[0], 0, intent, 0);
							Notification notification = new Notification(
									R.drawable.ic_notification,
									getString(R.string.auto_update_menu),
									System.currentTimeMillis());
							notification.setLatestEventInfo(AutoUpdate.this,
									getString(R.string.auto_update_title),
									getString(R.string.auto_update_summary),
									contentIntent);
							notification.flags |= Notification.FLAG_AUTO_CANCEL;
							mManager.notify(1, notification);
						}
					}
					Log.d(WorkoutEditor.LOGCATNAME, "DEBUG AUTOUPDATE Response from get is not ok! ");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		
			services[0].stopSelf();
			return null;
		}

		private boolean isResponseOK(HttpResponse response) {
			return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		}

		private Uri checkVersion(HttpResponse response, double currentVersion)
				throws XmlPullParserException, IllegalStateException, IOException {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(response.getEntity().getContent(), null);
			double newVersion = 0;
			Uri updateUri = null;
			int eventType = parser.next();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					String startTag = parser.getName();
					
					if (UPDATE_XML_FILE_TAG.equals(startTag)) {
						newVersion = Double.parseDouble(parser.getAttributeValue(null, "version"));

						if (newVersion > currentVersion) {
							Log.d(WorkoutEditor.LOGCATNAME, "DEBUG AUTOUPDATE xml update to newVersion : " + newVersion);
							updateUri = Uri.parse(HTTPS_BASE_URL +
									parser.getAttributeValue(null, "updateURI"));//Move this up with other const string -htormey
						}

						break;
					}
				}

				eventType = parser.next();
			}

			return updateUri;
		}
	}
}