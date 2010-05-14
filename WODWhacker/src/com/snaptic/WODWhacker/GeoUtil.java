package com.snaptic.WODWhacker;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

public class GeoUtil implements LocationListener {
	public static int MILLION = 1000000;
	private static final long RETAIN_GPS_MILLIS = 30000L;
	private boolean mHaveLocation = false;
	private long mLastGpsFixTime;
	private Location mNetworkLocation;
	private boolean mGpsAvailable;
	private boolean mNetworkAvailable;
	volatile Location realLocation;

	public GeoUtil(Context context) {
		LocationManager lm = (LocationManager)
			context.getSystemService(Context.LOCATION_SERVICE);
		Location lastGpsLocation =
			lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastNetworkLocation =
			lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		long now = System.currentTimeMillis();

		// Initialize realLocation, if possible
		if (lastGpsLocation != null) {
			if (lastNetworkLocation != null &&
				(now - lastNetworkLocation.getTime() <
				 now - lastGpsLocation.getTime())) {
				// last network location is newer, use it
				realLocation = new Location(lastNetworkLocation);
				mHaveLocation = true;
			} else {
				// use last GPS location
				realLocation = new Location(lastGpsLocation);
				mHaveLocation = true;
			}
		} else if (lastNetworkLocation != null) {
			// use last network location
			realLocation = new Location(lastNetworkLocation);
			mHaveLocation = true;
		} else {
			handleUnknownLocation();
		}
	}

	public void onLocationChanged(Location location) {
		if (!mHaveLocation) {
			mHaveLocation = true;
		}

		final long now = SystemClock.uptimeMillis();
		boolean useLocation = false;
		final String provider = location.getProvider();
		
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			// Use GPS if available
			mLastGpsFixTime = SystemClock.uptimeMillis();
			useLocation = true;
		} else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
			// Use network provider if GPS is getting stale
			useLocation = now - mLastGpsFixTime > RETAIN_GPS_MILLIS;
			
			if (mNetworkLocation == null) {
				mNetworkLocation = new Location(location);
			} else {
				mNetworkLocation.set(location);
			}

			mNetworkAvailable = true;
			mLastGpsFixTime = 0L;
		}
		
		if (useLocation) {
			realLocation = new Location(location);
		}
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			switch (status) {
				case LocationProvider.AVAILABLE:
					mGpsAvailable = true;
					break;
				case LocationProvider.OUT_OF_SERVICE:
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					mGpsAvailable = false;

					if (mNetworkLocation != null && mNetworkAvailable) {
						// Fall back to network location
						mLastGpsFixTime = 0L;
						onLocationChanged(mNetworkLocation);
					} else {
						handleUnknownLocation();
					}

					break;
			}
		} else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
			switch (status) {
				case LocationProvider.AVAILABLE:
					mNetworkAvailable = true;
					break;
				case LocationProvider.OUT_OF_SERVICE:
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					mNetworkAvailable = false;

					if (!mGpsAvailable) {
						handleUnknownLocation();
					}
					
					break;
			}
		}
	}

	private void handleUnknownLocation() {
		mHaveLocation = false;
	}

	public void onProviderEnabled(String s) {
	}

	public void onProviderDisabled(String s) {
	}
	
	public boolean hasLocation() {
		return mHaveLocation;
	}
	
	public Location getLocation() {
		return realLocation;
	}
}
