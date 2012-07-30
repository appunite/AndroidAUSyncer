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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

	public void setLastError(Uri uri, long lastErrorTimeMs) {
		String preferenceKey = uriToPreferenceKey(uri) + "_last_error";
		Editor editor = mSharedPreferences.edit();
		editor.putLong(preferenceKey, lastErrorTimeMs);
		editor.commit();
	}

	public void setLastSync(Uri uri, long lastSuccesTimeMs) {
		String preferenceKey = uriToPreferenceKey(uri) + "_last_success";
		Editor editor = mSharedPreferences.edit();
		editor.putLong(preferenceKey, lastSuccesTimeMs);
		editor.commit();
	}
	
	public void clear() {
		Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.commit();
	}
}
