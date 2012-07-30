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

package com.example.exampleausyncer.service;

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

import com.appunite.syncer.AbsDownloadService;
import com.example.exampleausyncer.ApiConsts;
import com.example.exampleausyncer.provider.ExampleContract;


public class DownloadService extends AbsDownloadService {
	public static final String ACTION_SYNC = "com.example.exampleausyncer.ACTION_SYNC";
	
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	

	private static final int EXAMPLE = 0;
	private static final int EXAMPLE_ID = 1;


	static {
		sURIMatcher.addURI(ExampleContract.AUTHORITY,
				ExampleContract.Example.CONTENT_PATH, EXAMPLE);
		sURIMatcher.addURI(ExampleContract.AUTHORITY,
				ExampleContract.Example.CONTENT_PATH + "/#", EXAMPLE_ID);
	}
	
	@Override
	protected boolean onHandleUri(Uri uri, Bundle bundle, boolean withForce) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case EXAMPLE:
		case EXAMPLE_ID:
			Downloader downloader = new Downloader(this);
			return downloader.download(ApiConsts.TICKETOMATS_API);
		default:
			throw new IllegalArgumentException();

		}
	}

	@Override
	protected boolean forceDownload(Uri uri, long lastSuccessMillis,
			long currentTimeMillis) {
		return currentTimeMillis - lastSuccessMillis > 10000;
	}
	
	@Override
	protected long taskWakeLockTimeout(Uri uri, Bundle bundle) {
		return 30 * 1000; // 30s
	}

}
