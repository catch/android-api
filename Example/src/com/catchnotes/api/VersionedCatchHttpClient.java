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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Build;

public abstract class VersionedCatchHttpClient implements HttpClient {
	protected String mEncodedAccessToken;
	protected HttpClient mHttpClient;
	
    public static VersionedCatchHttpClient newInstance(String userAgent, Context context) {
        VersionedCatchHttpClient client;

        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
        	client = new PreFroyoCatchHttpClient(userAgent);
        } else {
        	client = new CatchHttpClient(userAgent, context);
        }
        
        return client;
    }
    
    private static class PreFroyoCatchHttpClient extends VersionedCatchHttpClient {
		public PreFroyoCatchHttpClient(String userAgent) {
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			
			// Use some properties set in the default impl of AndroidHttpdClient

			// Turn off stale checking.  Our connections break all the time anyway,
			// and it's not worth it to pay the penalty of checking every time.
			HttpConnectionParams.setStaleCheckingEnabled(params, false);

			// Default connection and socket timeout of 20 seconds.
			HttpConnectionParams.setConnectionTimeout(params, 20000);
			HttpConnectionParams.setSoTimeout(params, 20000);
			HttpConnectionParams.setSocketBufferSize(params, 8192);
			
			mHttpClient = new DefaultHttpClient(params);
		}
    }
    
    private static class CatchHttpClient extends VersionedCatchHttpClient {
		public CatchHttpClient(String userAgent, Context context) {
			mHttpClient = AndroidHttpClient.newInstance(userAgent, context);
			HttpParams httpParams = mHttpClient.getParams();
			HttpClientParams.setRedirecting(httpParams, true);
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		}
		
		public void close() {
			((AndroidHttpClient) mHttpClient).close();
		}
    }
    
    public void close() {
    }
    
	public void setAccessToken(String accessToken) {
		try {
			mEncodedAccessToken = "access_token=" + URLEncoder.encode(accessToken, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private void addAccessTokenToHttpUriRequest(HttpUriRequest httpUriRequest) {
		if (mEncodedAccessToken != null) {
			String query = httpUriRequest.getURI().getQuery();
	
			if (query == null || query.length() == 0) {
				((HttpRequestBase) httpUriRequest).setURI(URI.create(httpUriRequest.getURI() + "?" + mEncodedAccessToken));
			} else {
				((HttpRequestBase) httpUriRequest).setURI(URI.create(httpUriRequest.getURI() +  "&" + mEncodedAccessToken));
			}
		}
	}
	
	public HttpParams getParams() {
		return mHttpClient.getParams();
	}
	
	public ClientConnectionManager getConnectionManager() {
		return mHttpClient.getConnectionManager();
	}
	
	public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest(httpUriRequest);
		return mHttpClient.execute(httpUriRequest);
	}
	
	public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest(httpUriRequest);
		return mHttpClient.execute(httpUriRequest, httpContext);
	}
	
	public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest((HttpUriRequest) httpRequest);
		return mHttpClient.execute(httpHost, httpRequest);
	}
	
	public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest((HttpUriRequest) httpRequest);
		return mHttpClient.execute(httpHost, httpRequest, httpContext);
	}
	
	public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest(httpUriRequest);
		return mHttpClient.execute(httpUriRequest, responseHandler);
	}
	
	public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest(httpUriRequest);
		return mHttpClient.execute(httpUriRequest, responseHandler, httpContext);
	}
	
	public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest((HttpUriRequest) httpRequest);
		return mHttpClient.execute(httpHost, httpRequest, responseHandler);
	}
	
	public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
		addAccessTokenToHttpUriRequest((HttpUriRequest) httpRequest);
		return mHttpClient.execute(httpHost, httpRequest, responseHandler, httpContext);
	}
}
