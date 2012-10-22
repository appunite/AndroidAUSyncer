package com.appunite.ausyncer.tests;

import android.net.Uri;
import android.os.Bundle;

import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.AbsDownloadService;

public class DownloadService extends AbsDownloadService {

	public static String ACTION_SYNC = "com.appunite.ausyncer.tests.ACTION_SYNC";
	
	public int numberOfCalls = 0;

	public boolean mLastCalledWithForce;

	public Bundle mLastCalledBundle;

	public Uri mLastCalledUri;

	public int requireConnectionCalls = 0;

	@Override
	protected AUSyncerStatus onHandleUri(Uri uri, Bundle bundle, boolean withForce) {
		numberOfCalls++;
		mLastCalledUri = uri;
		mLastCalledBundle = bundle;
		mLastCalledWithForce = withForce;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		return AUSyncerStatus.statusSuccess();
	}
	
	@Override
	protected boolean isNetworkNeeded(Uri uri, Bundle bundle) {
		requireConnectionCalls += 1;
		return false;
	}

}
