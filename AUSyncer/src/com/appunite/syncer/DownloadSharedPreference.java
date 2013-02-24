/*
 * Copyright (C) 2012 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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
 * limitations under the License
 */

package com.appunite.syncer;


import org.json.JSONException;
import org.json.JSONObject;

import com.appunite.ausyncer.BuildConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;

public class DownloadSharedPreference {
	private static final String PREFIX_LAST_MESSAGE_OBJ = "_last_error_obj";
	private static final String PREFIX_LAST_MESSAGE = "_last_error_message";
	private static final String PREFIX_LAST_TIME = "_last_error_time";
	private static final String PREFIX_LAST_DOWNLOADED = "_last_downloaded";
	private static final String DOWNLOAD_PREFS_NAME = "download_preferences";
	
	private static final String TAG = DownloadSharedPreference.class.getCanonicalName();
	
	private Context mContext;
	private SharedPreferences mSharedPreferences;

	public DownloadSharedPreference(Context context) {
		this.mContext = context;

		mSharedPreferences = this.mContext.getSharedPreferences(
				DOWNLOAD_PREFS_NAME, Context.MODE_PRIVATE
						| Context.MODE_MULTI_PROCESS);
	}

	public AUSyncerStatus getLastStatus(Uri uri) {
		String preferenceKeyMessage = uriToPreferenceKey(uri) + PREFIX_LAST_MESSAGE;
		String preferenceKeyTime = uriToPreferenceKey(uri) + PREFIX_LAST_TIME;
		String preferenceKeyMessageObject = uriToPreferenceKey(uri) + PREFIX_LAST_MESSAGE_OBJ;
		String preferenceKeyLastDownloaded = uriToPreferenceKey(uri) + PREFIX_LAST_DOWNLOADED;
		
		if (!mSharedPreferences.contains(preferenceKeyMessage)) {
			return AUSyncerStatus.statusNeverDownloaded();
		}
		int message = mSharedPreferences.getInt(preferenceKeyMessage, -1);
		long statusTimeMs = mSharedPreferences.getLong(preferenceKeyTime, -1);
		String messageObjectStr = mSharedPreferences.getString(preferenceKeyMessageObject, null);
		long lastDownloaded = mSharedPreferences.getLong(preferenceKeyLastDownloaded, -1L);
		JSONObject messageObject = null;
		if (messageObjectStr != null) {
			try {
				messageObject = new JSONObject(messageObjectStr);
			} catch (JSONException e) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "Wrong value saved in preferences", e);
				}
				return AUSyncerStatus.statusNeverDownloaded();
			}
		}
		return new AUSyncerStatus(message, statusTimeMs, lastDownloaded,
				messageObject);
	}

	private String uriToPreferenceKey(Uri uri) {
		return uri.toString();
	}

	public void setLastStatus(Uri uri, AUSyncerStatus status) {
		String preferenceKeyMessage = uriToPreferenceKey(uri) + PREFIX_LAST_MESSAGE;
		String preferenceKeyTime = uriToPreferenceKey(uri) + PREFIX_LAST_TIME;
		String preferenceKeyMessageObject = uriToPreferenceKey(uri) + PREFIX_LAST_MESSAGE_OBJ;
		String preferenceKeyLastDownloaded = uriToPreferenceKey(uri) + PREFIX_LAST_DOWNLOADED;
		
		Editor editor = mSharedPreferences.edit();
		editor.putInt(preferenceKeyMessage, status.getMessage());
		editor.putLong(preferenceKeyTime, status.getStatusTimeMs());
		long lastDownloaded = status.getLastDownloaded();
		if (lastDownloaded != -1L) {
			editor.putLong(preferenceKeyLastDownloaded, lastDownloaded);
		}
		JSONObject object = status.getMsgObjectOrNull();
		if (object == null) {
			editor.remove(preferenceKeyMessageObject);
		} else {
			editor.putString(preferenceKeyMessageObject, object.toString());
		}
		editor.commit();
	}

	public void clear() {
		Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.commit();
	}
}
