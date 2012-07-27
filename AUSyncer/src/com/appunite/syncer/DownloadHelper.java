package com.appunite.syncer;

import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;

public class DownloadHelper implements ServiceConnection {
	
	private boolean mHaveLocalData = true;
	private boolean mLocalDataIsEmpty = true;
	
	public void updateLocalData(boolean haveLocalData, boolean dataIsEmpty) {
		mHaveLocalData = haveLocalData;
		mLocalDataIsEmpty = dataIsEmpty;
		setProgressStatus();
	}
	
	private DownloadHelperStatus mDownloadHelperStatus;
	
	private Date getLastSuccess() {
		try {
			long lastSuccess = mDownloadService.getLastSuccess(mUri);
			return new Date(lastSuccess);
		} catch (RemoteException e) {
			reconnect();
			return null;
		}
	}
	
	private Date getLastError() {
		try {
			long lastError = mDownloadService.getLastError(mUri);
			return new Date(lastError);
		} catch (RemoteException e) {
			reconnect();
			return null;
		}
	}
	
	private boolean isInProgress() {
		try {
			return mDownloadService.inProgress(mUri);
		} catch (RemoteException e) {
			reconnect();
			return true;
		}
	}
	
	private final Context context;
	
	private IDownloadService mDownloadService = null;
	private Uri mUri = null;
	private Bundle mBundle = null;
	private boolean mWithForce;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Parcelable uriParcelable = intent.getParcelableExtra(DownloadService.ON_PROGRESS_CHANGE_EXTRA_URI);
			Uri uri = (Uri) uriParcelable;
			if (mUri != null && mUri.equals(uri)) {
				setProgressStatus();
			}
		}
	};

	public DownloadHelper(Context context, DownloadHelperStatus downloadHelperStatus) {
		this.context = context;
		mDownloadHelperStatus = downloadHelperStatus;
	}
	
	public void onActivityResume() {
		Intent downloadService = new Intent();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(DownloadService.ON_PROGRESS_CHANGE);
		context.registerReceiver(mReceiver, intentFilter );
		context.bindService(downloadService , this, Service.BIND_AUTO_CREATE);
		setProgressStatus();
	}
	
	public void startDownloading(Uri uri, Bundle bundle, boolean withForce) {
		mUri  = uri;
		mBundle = bundle;
		this.mWithForce = withForce;
		if (mDownloadService != null) {
			try {
				mDownloadService.download(mUri, mBundle, mWithForce);
			} catch (RemoteException e) {
				reconnect();
			}
		}
	}
	
	private void reconnect() {
		mDownloadService = null;
		// TODO reconnect to service if fail
	}

	public void onActivityPause() {
		context.unregisterReceiver(mReceiver);
		context.unbindService(this);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mDownloadService = IDownloadService.Stub.asInterface(service);
		if (mUri != null) {
			try {
				mDownloadService.download(mUri, mBundle , mWithForce);
			} catch (RemoteException e) {
				reconnect();
			}
		}
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
		if (!isBound && !mHaveLocalData) {
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
		mDownloadHelperStatus.reportStatus(screenVisible, screenEmpty, screenProgress, progressIndicator, lastError);
	}
	
}