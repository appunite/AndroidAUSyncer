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

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

/**
 * AbsDownloadService is an abstract service which simplify using downloaded
 * data from remote services. It uses REST-like path format and is almost
 * necessary when used both with ContentProviders and REST API.
 * 
 * AbsDownloadService is usually used with {@link DownloadHelper} to provide
 * full user friendly interface with progress indication and error handling.
 * 
 * This class take care of keeping device woken when you downloading a data.
 * 
 * Typically you have to
 * 
 * <pre class="prettyprint">
 * public class DownloadService extends AbsDownloadService {
 * 
 * 	public static final String ACTION_SYNC = &quot;com.example.exampleausyncer.ACTION_SYNC&quot;;
 * 
 * 	public static final String AUTHORITY = &quot;com.example.exampleausyncer&quot;;
 * 	public static final String CONTENT_PATH = &quot;example&quot;;
 * 
 * 	private static final int EXAMPLE = 0;
 * 	private static final int EXAMPLE_ID = 1;
 * 	static {
 * 		sURIMatcher.addURI(AUTHORITY, CONTENT_PATH, EXAMPLE);
 * 		sURIMatcher.addURI(AUTHORITY, CONTENT_PATH + &quot;/#&quot;, EXAMPLE_ID);
 * 	}
 * 
 * 	&#064;Override
 * 	protected boolean onHandleUri(Uri uri, Bundle bundle, boolean withForce) {
 * 		int match = sURIMatcher.match(uri);
 * 		switch (match) {
 * 		case EXAMPLE:
 * 		case EXAMPLE_ID:
 * 			// download data and return true if successed
 * 			return true;
 * 		default:
 * 			throw new IllegalArgumentException();
 * 
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * And do not forget adding INTERNET permission and your service to your
 * manifest file:
 * 
 * <pre class="prettyprint">
 * &lt;uses-permission android:name="android.permission.INTERNET" /&gt;
 * &lt;service
 * 	android:name=".service.DownloadService"
 * 	android:exported="false"&gt;
 * 	&lt;intent-filter&gt;
 * 		&lt;action android:name="com.example.exampleausyncer.ACTION_SYNC" /&gt;
 * 	&lt;/intent-filter&gt;
 * &lt;/service&gt;
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * There are several methods that you would like to override:
 * <ul>
 * <li>{@link AbsDownloadService#forceDownload(Uri, long, long)}</li>
 * <li>{@link AbsDownloadService#taskWakeLockTimeout(Uri, Bundle)}</li>
 * </ul>
 * </p>
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public abstract class AbsDownloadService extends Service {

	public static final int DEFAULT_FORCE_UPDATE_TIME_MS = 10000;
	public static final String ON_PROGRESS_CHANGE_EXTRA_URI = "uri";
	public static final String ON_PROGRESS_CHANGE = "com.appunite.syncer.ON_PROGRESS_CHANGE";

	private static class MyThread extends Thread {
		private final AbsDownloadService mDownloadService;

		public MyThread(AbsDownloadService downloadService, String threadName) {
			super(threadName);
			this.mDownloadService = downloadService;
		}

		@Override
		public void run() {
			mDownloadService.run();
		}
	}

	private static class Task {

		public Uri uri;
		public Bundle bundle;
		public boolean withForce;

	}

	private IDownloadService.Stub mBinder = new IDownloadService.Stub() {

		@Override
		public long getLastSuccess(Uri uri) throws RemoteException {
			return AbsDownloadService.this.getLastSuccess(uri);
		}

		@Override
		public long getLastError(Uri uri) throws RemoteException {
			return AbsDownloadService.this.getLastError(uri);
		}

		@Override
		public boolean inProgress(Uri uri) throws RemoteException {
			return AbsDownloadService.this.inProgress(uri);
		}

		@Override
		public void download(Uri uri, Bundle bundle, boolean withForce)
				throws RemoteException {
			AbsDownloadService.this.download(uri, bundle, withForce);
		}

	};

	protected long getLastSuccess(Uri uri) throws RemoteException {
		return mDownloadSharedPreference.getLastSuccess(uri);
	}

	private void setLastSync(Uri uri) {
		long currentTimeMillis = System.currentTimeMillis();
		mDownloadSharedPreference.setLastSync(uri, currentTimeMillis);
		mDownloadSharedPreference.setLastError(uri, -1);
	}

	private void setLastError(Uri uri) {
		long currentTimeMillis = System.currentTimeMillis();
		mDownloadSharedPreference.setLastError(uri, currentTimeMillis);
	}

	protected long getLastError(Uri uri) throws RemoteException {
		return mDownloadSharedPreference.getLastError(uri);
	}

	protected void download(Uri uri, Bundle bundle, boolean withForce)
			throws RemoteException {
		if (withForce != true) {
			long lastSuccessMillis = getLastSuccess(uri);
			if (lastSuccessMillis > 0) {
				long currentTimeMillis = System.currentTimeMillis();
				if (!forceDownload(uri, lastSuccessMillis, currentTimeMillis)) {
					return;
				}
			}
		}
		Task task = new Task();
		task.uri = uri;
		task.bundle = bundle;
		task.withForce = withForce;
		synchronized (this) {
			mQueue.add(task);
			this.notifyAll();
		}
	}

	/**
	 * This method decides if the refresh should be executed when no force
	 * request was indicated. Default implementation will return true if last
	 * success was later than {@value #DEFAULT_FORCE_UPDATE_TIME_MS}ms.
	 * 
	 * <p>
	 * Example implementation:
	 * </p>
	 * 
	 * <pre class="prettyprint">
	 * &#064;Override
	 * protected boolean forceDownload(Uri uri, long lastSuccessMillis,
	 * 		long currentTimeMillis) {
	 * 	int match = sURIMatcher.match(uri);
	 * 	if (match == EXAMPLE)
	 * 		return currentTimeMillis - lastSuccessMillis &gt; 30 * 1000;
	 * 	else
	 * 		return super.forceDownload(uri, lastSuccessMillis, currentTimeMillis);
	 * }
	 * </pre>
	 * 
	 * @param uri
	 *            uri that was requested to refresh non force refresh
	 * @param lastSuccessMillis
	 *            last time when url was successfully refreshed
	 * @param currentTimeMillis
	 *            current time
	 * @return true if download should occure
	 */
	protected boolean forceDownload(Uri uri, long lastSuccessMillis,
			long currentTimeMillis) {
		return currentTimeMillis - lastSuccessMillis > DEFAULT_FORCE_UPDATE_TIME_MS;
	}

	/**
	 * Return time how long {@link PowerManager#PARTIAL_WAKE_LOCK} should be
	 * acquired for given uri. Default implementation will not acquire wake
	 * lock.
	 * 
	 * <p>
	 * Example implementation
	 * 
	 * <pre class="prettyprint">
	 * &#064;Override
	 * protected long taskWakeLockTimeout(Uri uri, Bundle bundle) {
	 * 	return 30000;
	 * }
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * And do not forget adding WAKE_LOCK permission to your manifest file:
	 * 
	 * <pre class="prettyprint">
	 * &lt;uses-permission android:name="android.permission.WAKE_LOCK" /&gt;
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param uri
	 *            that was requested to refresh
	 * @param bundle
	 *            data that was given
	 * @return Return <code>x < 0</code> to not acquire wake lock, or
	 *         <code>0</code> to remove timeout, or <code>x > 0</code> to set
	 *         timeout im ms
	 * @see PowerManager#WakeLock
	 */
	protected long taskWakeLockTimeout(Uri uri, Bundle bundle) {
		return -1;
	}

	protected boolean inProgress(Uri uri) {
		synchronized (this) {
			for (Task task : mQueue) {
				if (task.uri.equals(uri))
					return true;
			}
		}
		return false;
	}

	private MyThread thread = new MyThread(this, "DownloadService");

	// protected with this
	private ArrayList<Task> mQueue = new ArrayList<AbsDownloadService.Task>();
	protected int mNumberOfListeners = 0;
	private DownloadSharedPreference mDownloadSharedPreference;
	private boolean mClose;
	private PowerManager.WakeLock mWakeLock;

	@Override
	public IBinder onBind(Intent intent) {
		synchronized (this) {
			mNumberOfListeners++;
		}
		return mBinder;
	}

	protected void run() {
		for (;;) {
			try {
				Task task;
				synchronized (this) {
					for (;;) {
						if (mClose)
							return;
						if (mQueue.size() != 0) {
							task = mQueue.get(0);
							break;
						}
						this.wait();
					}
				}
				long timeout = taskWakeLockTimeout(task.uri, task.bundle);
				if (timeout == 0) {
					mWakeLock.acquire();
				} else if (timeout > 0) {
					mWakeLock.acquire(timeout);
				}
				boolean success = false;
				try {
					success = onHandleUri(task.uri, task.bundle, task.withForce);
				} finally {
					if (timeout >= 0 && mWakeLock.isHeld())
						mWakeLock.release();
				}

				if (success) {
					setLastSync(task.uri);
				} else {
					setLastError(task.uri);
				}
				synchronized (this) {
					mQueue.remove(0);
				}
				Intent broadcastIntent = new Intent(ON_PROGRESS_CHANGE);
				broadcastIntent
						.putExtra(ON_PROGRESS_CHANGE_EXTRA_URI, task.uri);
				this.sendBroadcast(broadcastIntent);
			} catch (InterruptedException e) {
			}
		}
	}

	protected abstract boolean onHandleUri(Uri uri, Bundle bundle,
			boolean withForce);

	@Override
	public boolean onUnbind(Intent intent) {
		synchronized (this) {
			mNumberOfListeners--;
			this.notifyAll();
		}
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"Downloading data");

		mDownloadSharedPreference = new DownloadSharedPreference(this);
		mClose = false;
		thread.start();
	}

	@Override
	public void onDestroy() {
		synchronized (this) {
			mClose = true;
		}
		super.onDestroy();
	}
}
