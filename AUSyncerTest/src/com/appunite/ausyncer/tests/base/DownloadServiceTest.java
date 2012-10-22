package com.appunite.ausyncer.tests.base;

import static com.appunite.ausyncer.tests.base.Tests.assertThatWithTimeout;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.test.ServiceTestCase;

import com.appunite.ausyncer.tests.DownloadService;
import com.appunite.ausyncer.tests.base.Tests.ValueRunnable;
import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.DownloadSharedPreference;
import com.appunite.syncer.IDownloadService;

public class DownloadServiceTest extends ServiceTestCase<DownloadService> {

	private DownloadService mService;
	private IDownloadService mInterface;

	private static String AUTHORITY;
	private static Uri AUTHORITY_URI;

	public DownloadServiceTest() {
		super(DownloadService.class);
		setName("DownloadServiceTest");
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		AUTHORITY = "com.example.exampleausyncer";
		AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

		setContext(getSystemContext());

		Intent intent = new Intent(DownloadService.ACTION_SYNC);
		mInterface = IDownloadService.Stub.asInterface(bindService(intent));
		mService = getService();
		DownloadSharedPreference downloadSharedPreference = new DownloadSharedPreference(
				getContext());
		downloadSharedPreference.clear();
	}

	public void test1Prepare() {
		assertNotNull(AUTHORITY);
		assertNotNull(AUTHORITY_URI);
		assertNotNull(mService);
		assertNotNull(mInterface);
	}

	public void testSharedPreferences() {
		DownloadSharedPreference downloadSharedPreference = new DownloadSharedPreference(
				getContext());
		assertThat("should be not downloaded set at start Point",
				downloadSharedPreference.getLastStatus(AUTHORITY_URI),
				equalTo(AUSyncerStatus.statusNeverDownloaded()));
		
		AUSyncerStatus success = AUSyncerStatus.statusSuccess();
		downloadSharedPreference.setLastStatus(AUTHORITY_URI, success);
		assertThat("saved LastSync should be 1000", downloadSharedPreference
				.getLastStatus(AUTHORITY_URI).getStatusTimeMs(),
				equalTo(success.getStatusTimeMs()));
	}

	public void testPassingArguments() throws Exception {

		assertThat("should be not downloaded set at start Point",
				mInterface.getLastStatus(AUTHORITY_URI),
				equalTo(AUSyncerStatus.statusNeverDownloaded()));

		long startTime = System.currentTimeMillis();
		mInterface.download(AUTHORITY_URI, null, false);
		assertThatWithTimeout(new ValueRunnable<Integer>() {
			@Override
			public Integer getValue() throws Exception {
				return mService.numberOfCalls;
			}
		}, equalTo(1), 1000);
		assertEquals("Uri does not match", mService.mLastCalledUri,
				AUTHORITY_URI);
		assertNull("Passed bundle does not match", mService.mLastCalledBundle);
		assertFalse(
				"Called without force, but arguments does appear to be good",
				mService.mLastCalledWithForce);
		assertThatWithTimeout(new ValueRunnable<AUSyncerStatus>() {

			@Override
			public AUSyncerStatus getValue() throws Exception {
				return mInterface.getLastStatus(AUTHORITY_URI);
			}
		}, equalTo(AUSyncerStatus.statusSuccess()), 1000);
		long nowTime = System.currentTimeMillis();
		AUSyncerStatus lastStatus = mInterface.getLastStatus(AUTHORITY_URI);
		assertThat("Last success should be equalt or greater than start time",
				lastStatus.getStatusTimeMs(),
				greaterThanOrEqualTo(startTime));
		assertThat("Last success should be lower or grater that actual time",
				lastStatus.getStatusTimeMs(),
				lessThanOrEqualTo(nowTime));

		Bundle bundle = new Bundle();
		mInterface.download(AUTHORITY_URI, bundle, true);
		assertThatWithTimeout(new ValueRunnable<Integer>() {
			@Override
			public Integer getValue() throws Exception {
				return mService.numberOfCalls;
			}
		}, equalTo(2), 1000);
		assertEquals("Uri does not match", mService.mLastCalledUri,
				AUTHORITY_URI);
		assertEquals("Passed bundle does not match",
				mService.mLastCalledBundle, bundle);
		assertTrue("Called with force, but arguments does appear to be good",
				mService.mLastCalledWithForce);
	}

	public void testProgressInformation() throws Exception {
		assertFalse("In progress should be false at start point",
				mInterface.inProgress(AUTHORITY_URI));
		assertThat("should be not downloaded set at start Point",
				mInterface.getLastStatus(AUTHORITY_URI),
				equalTo(AUSyncerStatus.statusNeverDownloaded()));

		mInterface.download(AUTHORITY_URI, null, false);
		assertTrue("While downloading in progress should be true",
				mInterface.inProgress(AUTHORITY_URI));
		assertThatWithTimeout(new ValueRunnable<Boolean>() {
			@Override
			public Boolean getValue() throws RemoteException {
				return mInterface.inProgress(AUTHORITY_URI);
			}
		}, equalTo(false), 1000);
		assertEquals("After invocation number of call should be 1", 1,
				mService.numberOfCalls);

		mInterface.download(AUTHORITY_URI, null, true);
		assertThatWithTimeout(new ValueRunnable<Integer>() {
			@Override
			public Integer getValue() throws Exception {
				return mService.numberOfCalls;
			}
		}, equalTo(2), 1000);
		assertThatWithTimeout(new ValueRunnable<Boolean>() {

			@Override
			public Boolean getValue() throws Exception {
				return mInterface.inProgress(AUTHORITY_URI);
			}
		}, equalTo(false), 1000);

		mInterface.download(AUTHORITY_URI, null, false);
		assertFalse(
				"Should not be in progress after second non force download",
				mInterface.inProgress(AUTHORITY_URI));
		Thread.sleep(1000);
		assertThat("call should not be executed", mService.numberOfCalls,
				equalTo(2));
	}
	
	public void testNetworkUriAvailability() throws Exception {
		mService.requireConnectionCalls = 0;
		mInterface.download(AUTHORITY_URI, null, true);
		assertThatWithTimeout(new ValueRunnable<Integer>() {

			@Override
			public Integer getValue() throws Exception {
				return mService.requireConnectionCalls;
			}
		}, is(equalTo(1)), 500);
	}
}
