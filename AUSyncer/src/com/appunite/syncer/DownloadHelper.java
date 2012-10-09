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

import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * DownloadHelper is a class designed to simplify communication between server
 * API and user. It provides nice interaction to user providing error handling
 * and progress indication. Every call is send to subclass of
 * {@link AbsDownloadService}.
 * 
 * <p>
 * To start working with this helper you have:
 * <ul>
 * <li>create instance
 * {@link #DownloadHelper(Context, String, DownloadHelperStatus, Uri)} in your
 * activity/fragment {@link Activity#onCreate} method.</li>
 * <li>call {@link #onActivityPause()} in {@link Activity#onPause} method</li>
 * <li>call {@link #onActivityResume()} in {@link Activity#onResume} method</li>
 * <li>call {@link #updateLocalData(Cursor)} or
 * {@link #updateLocalData(boolean, boolean)} when your data was loaded and
 * unloaded, i.e. by {@link CursorLoader}</li>
 * <li>probably call {@link #startDownloading(Bundle, boolean)} after
 * {@link #onActivityResume()}</li>
 * <li>probably call {@link #startDownloading(Bundle, boolean)} after user hit
 * refresh button</li>
 * </ul>
 * </p>
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
@SuppressLint("HandlerLeak")
public class DownloadHelper implements ServiceConnection {

	private boolean mHaveLocalData = false;
	private boolean mLocalDataIsEmpty = true;

	private DownloadHelperStatus mDownloadHelperStatus;

	private Date getLastSuccess() {
		if (mDownloadService == null)
			return null;
		try {
			long lastSuccess = mDownloadService.getLastSuccess(mUri);
			if (lastSuccess < 0)
				return null;
			return new Date(lastSuccess);
		} catch (RemoteException e) {
			reconnect();
			return null;
		}
	}

	private Date getLastError() {
		if (mDownloadService == null)
			return null;
		try {
			long lastError = mDownloadService.getLastError(mUri);
			if (lastError < 0)
				return null;
			return new Date(lastError);
		} catch (RemoteException e) {
			reconnect();
			return null;
		}
	}

	private boolean isInProgress() {
		if (mDownloadService == null)
			return true;
		try {
			return mDownloadService.inProgress(mUri);
		} catch (RemoteException e) {
			reconnect();
			return true;
		}
	}

	private final Context mContext;

	private IDownloadService mDownloadService = null;
	private Uri mUri = null;
	private Bundle mBundle = null;
	private boolean mWithForce;
	private final String mServiceActionName;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Parcelable uriParcelable = intent
					.getParcelableExtra(AbsDownloadService.ON_PROGRESS_CHANGE_EXTRA_URI);
			Uri uri = (Uri) uriParcelable;
			if (mUri != null && mUri.equals(uri)) {
				setProgressStatus();
			}
		}
	};
	private boolean mIsActive = false;
	private MyHandler mMyHandler;
	private boolean mRequestDownload;

	private class MyHandler extends Handler {

		private static final int MSG_REFRESH_PROGRESS = 0;

		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			setProgressStatus();
		}

	}

	/**
	 * Create DownloadHelper
	 * 
	 * @param context
	 *            activity/fragment context
	 * @param serviceActionName
	 *            action name that will start your implementation of
	 *            {@link AbsDownloadService}, i.e.
	 *            <code>DownloadService.ACTION_SYNC</code>
	 * @param downloadHelperStatus
	 *            an status observer
	 * @param uri
	 *            uri that you want to observer. i.e.
	 *            <code>Uri.withAppendedPath(
	 *            DownloadService.AUTHORITY_URI, DownloadService.CONTENT_PATH)</code>
	 */
	public DownloadHelper(Context context, String serviceActionName,
			DownloadHelperStatus downloadHelperStatus, Uri uri) {
		this.mContext = context;
		this.mServiceActionName = serviceActionName;
		mDownloadHelperStatus = downloadHelperStatus;
		this.mUri = uri;
		mMyHandler = new MyHandler(context.getMainLooper());
	}

	/**
	 * Initialize DownloadHelper. Should be called in {@link Activity#onResume}.
	 */
	public void onActivityResume() {
		assert (mIsActive == false);
		mIsActive = true;

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(AbsDownloadService.ON_PROGRESS_CHANGE);
		mContext.registerReceiver(mReceiver, intentFilter);

		reconnect();
	}

	/**
	 * Pause DownloadHelper. Should be called in {@link Activity#onPause}.
	 */
	public void onActivityPause() {
		assert (mIsActive == true);
		mIsActive = false;
		mContext.unregisterReceiver(mReceiver);
		mContext.unbindService(this);
		mMyHandler.removeMessages(MyHandler.MSG_REFRESH_PROGRESS);
	}

	/**
	 * Informs {@link DownloadHelper} that your data was loaded/unloaded
	 * 
	 * @param haveLocalData
	 *            <code>true</code> if your data was downloaded,
	 *            <code>false</code> otherwise
	 * @param dataIsEmpty
	 *            <code>true</code> if downloaded data was empty,
	 *            <code>false</code> otherwise. This value is only read if
	 *            <code>haveLocalData == true</code>
	 */
	public void updateLocalData(boolean haveLocalData, boolean dataIsEmpty) {
		mHaveLocalData = haveLocalData;
		mLocalDataIsEmpty = dataIsEmpty;
		if (mIsActive)
			setProgressStatus();
	}

	/**
	 * Simple wrapper for
	 * {@link DownloadHelper#updateLocalData(boolean, boolean)} that can be used
	 * with cursor.
	 * 
	 * <p>
	 * In Example:
	 * 
	 * <pre class="prettyprint">
	 * &#064;Override
	 * public void onLoadFinished(Loader&lt;Cursor&gt; loader, Cursor cursor) {
	 * 	mDownloadHelper.updateLocalData(cursor);
	 * 	mAdapter.swapCursor(cursor);
	 * }
	 * 
	 * &#064;Override
	 * public void onLoaderReset(Loader&lt;Cursor&gt; loader) {
	 * 	mDownloadHelper.updateLocalData(null);
	 * 	mAdapter.swapCursor(null);
	 * }
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param cursor
	 *            <code>cursor</code> if your data was loaded, <code>null</code>
	 *            otherwise
	 */
	public void updateLocalData(Cursor cursor) {
		boolean haveLocalData = cursor != null;
		boolean dataIsEmpty = cursor == null || cursor.getCount() == 0;
		this.updateLocalData(haveLocalData, dataIsEmpty);
	}

	/**
	 * Request download user data. Usually should be invoked
	 * <code>DownloadHelper.startDownloading(null, false)</code> in
	 * {@link Activity#onResume} and
	 * <code>DownloadHelper.startDownloading(null, true)</code> while user
	 * pushes refresh button.
	 * 
	 * @param bundle
	 *            data that should be delivered to subclass of
	 *            {@link AbsDownloadService}.
	 * @param withForce
	 *            should downloading be performed even if the refresh time has
	 *            not expired
	 */
	public void startDownloading(Bundle bundle, boolean withForce) {
		mBundle = bundle;
		this.mWithForce = withForce;
		this.mRequestDownload = true;
		if (mDownloadService != null) {
			try {
				mDownloadService.download(mUri, mBundle, mWithForce);
				mRequestDownload = false;
			} catch (RemoteException e) {
				reconnect();
			}
			setProgressStatus();
		}
	}

	private void reconnect() {
		mDownloadService = null;
		Intent downloadService = new Intent(mServiceActionName);
		boolean foundService = mContext.bindService(downloadService, this,
				Service.BIND_AUTO_CREATE);
		if (!foundService) {
			throw new IllegalArgumentException(
					"Service: "
							+ mServiceActionName
							+ " does not found, did you forgot to add it to AndroidManifest file?");
		}

		setProgressStatus();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mDownloadService = IDownloadService.Stub.asInterface(service);
		if (mRequestDownload)
			startDownloading(mBundle, mWithForce);
		setProgressStatus();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mDownloadService = null;
	}

	protected void setProgressStatus() {
		Date lastError;
		boolean screenProgress;
		boolean progressIndicator;
		boolean screenVisible;
		boolean screenEmpty;

		boolean isBound = mDownloadService != null;
		if (!isBound || !mHaveLocalData) {
			progressIndicator = true;
			screenProgress = true;
			screenVisible = false;
			screenEmpty = false;
			lastError = null;
		} else {
			boolean isInProgress = isInProgress();

			boolean hasDownloadedData = getLastSuccess() != null;

			progressIndicator = isInProgress;
			screenProgress = !hasDownloadedData && isInProgress;
			screenVisible = !mLocalDataIsEmpty;
			screenEmpty = hasDownloadedData && mLocalDataIsEmpty;
			lastError = getLastError();
		}
		mMyHandler.removeMessages(MyHandler.MSG_REFRESH_PROGRESS);
		if (lastError != null) {
			mMyHandler.sendEmptyMessageDelayed(MyHandler.MSG_REFRESH_PROGRESS,
					1000);
		}
		mDownloadHelperStatus.reportStatus(screenVisible, screenEmpty,
				screenProgress, progressIndicator, lastError);
	}
	
}