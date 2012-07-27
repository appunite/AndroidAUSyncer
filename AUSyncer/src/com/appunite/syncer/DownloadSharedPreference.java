package com.appunite.syncer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class DownloadSharedPreference {
	private static final String DOWNLOAD_PREFS_NAME = "download_preferences";
	private Context mContext;
	private SharedPreferences mSharedPreferences;

	public DownloadSharedPreference(Context context) {
		this.mContext = context;

		mSharedPreferences = this.mContext.getSharedPreferences(
				DOWNLOAD_PREFS_NAME, Context.MODE_PRIVATE
						| Context.MODE_MULTI_PROCESS);
	}
	
	public long getLastSuccess(Uri uri) {
		String preferenceKey = uriToPreferenceKey(uri) + "_last_success";
		return mSharedPreferences.getLong(preferenceKey, -1);
	}
	
	public long getLastError(Uri uri) {
		String preferenceKey = uriToPreferenceKey(uri) + "_last_error";
		return mSharedPreferences.getLong(preferenceKey, -1);
	}
	
	private String uriToPreferenceKey(Uri uri) {
		return uri.toString();
	}
}
