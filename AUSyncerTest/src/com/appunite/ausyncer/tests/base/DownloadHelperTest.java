package com.appunite.ausyncer.tests.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.AbsDownloadService;
import com.appunite.syncer.DownloadHelper;
import com.appunite.syncer.DownloadHelperStatus;
import com.appunite.syncer.IDownloadService;

public class DownloadHelperTest extends AndroidTestCase {

	private Uri AUTORITY_URI;
	private String AUTHORITY;
	private DownloadHelper mDownloadHelper;

	public static class DownloadService extends IDownloadService.Stub {

		public Uri mUri = null;
		public Bundle mBundle;
		public boolean mWithForce;
		public boolean mInProgress = false;
		public boolean mIsNetworkNeeded = true;
		public AUSyncerStatus mLastStatus;

		@Override
		public void download(Uri uri, Bundle bundle, boolean withForce)
				throws RemoteException {
			mUri = uri;
			mBundle = bundle;
			mWithForce = withForce;
			mInProgress = true;
			mIsNetworkNeeded = true;
		}
		
		@Override
		public AUSyncerStatus getLastStatus(Uri arg0) throws RemoteException {
			return mLastStatus;
		}

		@Override
		public boolean inProgress(Uri uri) throws RemoteException {
			return mInProgress;
		}

	}

	@Mock
	private DownloadHelperStatus mDownloadHelperStatusMock;
	private DownloadService mDownloadServiceMock;
	@Mock
	private MockContext mMockContext;

	// public class ContextMock extends MockContext {
	// private ServiceConnection mConn;
	// private boolean mService;
	//
	// @Override
	// public boolean bindService(Intent service, ServiceConnection conn,
	// int flags) {
	// mService = true;
	// mConn = conn;
	// return true;
	// }
	// }

	public DownloadHelperTest() {
		super();
		setName("DownloadHelperTest");
	}

	protected void setUp() throws Exception {
		super.setUp();
		AUTHORITY = "com.appunite.syncer";
		AUTORITY_URI = Uri.parse("content://" + AUTHORITY);
		String SERVICE_ACTION_NAME = "com.appunite.syncer.ACTION_SYNC";

		MockitoAnnotations.initMocks(this);
		mDownloadServiceMock = new DownloadService();
		when(mMockContext.getMainLooper()).thenReturn(Looper.getMainLooper());

		setContext(mMockContext);
		mDownloadHelper = new DownloadHelper(mMockContext, SERVICE_ACTION_NAME,
				mDownloadHelperStatusMock, AUTORITY_URI);
	}

	public void test1Setup() {
		assertNotNull(mMockContext);
		assertNotNull(mDownloadServiceMock);
		assertNotNull(mDownloadHelper);
		assertNotNull(mDownloadHelperStatusMock);
	}

	public void testForNotFoundService() {
		when(
				mMockContext.bindService(Mockito.any(Intent.class),
						Mockito.any(ServiceConnection.class), Mockito.anyInt()))
				.thenReturn(false);
		try {
			mDownloadHelper.onActivityResume();
			fail("Download helper should throw IllegalArgumentException when not found service to bind");
		} catch (IllegalArgumentException e) {
		}
		when(
				mMockContext.bindService((Intent) Mockito.anyObject(),
						(ServiceConnection) Mockito.anyObject(),
						Mockito.anyInt())).thenReturn(true);
		mDownloadHelper.onActivityResume();
	}
	
	public void testIfReceiveBroadcasts() {
		Matcher<IntentFilter> correctIntentFilterMatcher = new BaseMatcher<IntentFilter>() {

			@Override
			public boolean matches(Object obj) {
				if (!(obj instanceof IntentFilter))
					return false;
				IntentFilter filter = (IntentFilter) obj;
				if (filter.hasAction(AbsDownloadService.ON_PROGRESS_CHANGE))
					return true;
				return false;
			}

			@Override
			public void describeTo(Description description) {
			}
		};
		ArgumentCaptor<BroadcastReceiver> receiverCaptor = ArgumentCaptor
				.forClass(BroadcastReceiver.class);
		when(
				mMockContext.registerReceiver(
						argThat(notNullValue(BroadcastReceiver.class)),
						argThat(correctIntentFilterMatcher))).thenReturn(null);
		when(
				mMockContext.bindService(Mockito.any(Intent.class),
						Mockito.any(ServiceConnection.class), anyInt()))
				.thenReturn(true);
		mDownloadHelper.onActivityResume();
		verify(mMockContext).registerReceiver(receiverCaptor.capture(),
				notNull(IntentFilter.class));
		BroadcastReceiver value = receiverCaptor.getValue();

		reset(mDownloadHelperStatusMock);

		sendOnProgressChange(value, AUTORITY_URI);
		verify(mDownloadHelperStatusMock, times(1)).onReportStatus(false,
				false, true, true, AUSyncerStatus.statusNeverDownloaded());
	}

	public void testLastError() throws InterruptedException {
		BroadcastReceiver receiver = bind();

		assertThat(receiver, is(notNullValue()));

		mDownloadServiceMock.mLastStatus = AUSyncerStatus.statusInternalIssue();
		mDownloadHelper.updateLocalData(true, false);
		verify(mDownloadHelperStatusMock).onReportStatus(Mockito.anyBoolean(),
				Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), Mockito.notNull(AUSyncerStatus.class));
		
		
		mDownloadHelper.onActivityPause();
		verify(mMockContext).unregisterReceiver(receiver);
		reset(mDownloadHelperStatusMock);
		
		
		mDownloadHelper.updateLocalData(true, true);
		Thread.sleep(2000);
		
		// After onActivityPause nothing should be reported
		verify(mDownloadHelperStatusMock, times(0)).onReportStatus(
				Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.any(AUSyncerStatus.class));
	}

	public void testSyncStatuses() throws RemoteException {
		BroadcastReceiver receiver = bind();

		assertNull("before startDownloading uri should be null",
				mDownloadServiceMock.mUri);
		mDownloadHelper.startDownloading(null, true);
		assertThat("requested url does not match", mDownloadServiceMock.mUri,
				equalTo(AUTORITY_URI));
		assertTrue(mDownloadServiceMock.mWithForce);
		verify(mDownloadHelperStatusMock).onReportStatus(false, false, true,
				true, AUSyncerStatus.statusNeverDownloaded());
		reset(mDownloadHelperStatusMock);

		mDownloadServiceMock.mLastStatus = AUSyncerStatus.statusSuccess();
		mDownloadServiceMock.mInProgress = false;
		sendOnProgressChange(receiver, AUTORITY_URI);
		verify(mDownloadHelperStatusMock).onReportStatus(false, false, true,
				true, AUSyncerStatus.statusNeverDownloaded());
		reset(mDownloadHelperStatusMock);

		mDownloadHelper.updateLocalData(true, false);
		verify(mDownloadHelperStatusMock).onReportStatus(true, false, false,
				false, AUSyncerStatus.statusSuccess());
		reset(mDownloadHelperStatusMock);

		mDownloadServiceMock.mInProgress = true;
		sendOnProgressChange(receiver, AUTORITY_URI);
		verify(mDownloadHelperStatusMock).onReportStatus(true, false, false,
				true, AUSyncerStatus.statusSuccess());
		reset(mDownloadHelperStatusMock);

		mDownloadHelper.updateLocalData(false, false);
		verify(mDownloadHelperStatusMock).onReportStatus(false, false, true,
				true, AUSyncerStatus.statusNeverDownloaded());
		reset(mDownloadHelperStatusMock);

		mDownloadHelper.updateLocalData(true, true);
		verify(mDownloadHelperStatusMock).onReportStatus(false, true, false,
				true, AUSyncerStatus.statusSuccess());
		reset(mDownloadHelperStatusMock);

		mDownloadServiceMock.mInProgress = false;
		sendOnProgressChange(receiver, AUTORITY_URI);
		verify(mDownloadHelperStatusMock).onReportStatus(false, true, false,
				false, AUSyncerStatus.statusSuccess());
		reset(mDownloadHelperStatusMock);
	}

	private void sendOnProgressChange(BroadcastReceiver receiver, Uri uri) {
		Intent intent = new Intent(AbsDownloadService.ON_PROGRESS_CHANGE);
		intent.putExtra(AbsDownloadService.ON_PROGRESS_CHANGE_EXTRA_URI, uri);
		receiver.onReceive(mMockContext, intent);
	}

	private BroadcastReceiver bind() {

		when(
				mMockContext.bindService(Mockito.any(Intent.class),
						argThat(notNullValue(ServiceConnection.class)),
						anyInt())).thenReturn(true);

		mDownloadHelper.onActivityResume();

		ArgumentCaptor<ServiceConnection> serviceConnectionCaptor = ArgumentCaptor
				.forClass(ServiceConnection.class);
		verify(mMockContext).bindService(Mockito.any(Intent.class),
				serviceConnectionCaptor.capture(), Mockito.anyInt());
		ServiceConnection serviceConnection = serviceConnectionCaptor
				.getValue();
		assertNotNull(serviceConnection);

		verify(mDownloadHelperStatusMock, times(1)).onReportStatus(false, false,
				true, true, AUSyncerStatus.statusNeverDownloaded());

		ArgumentCaptor<BroadcastReceiver> broadcastReceiverCaptor = ArgumentCaptor
				.forClass(BroadcastReceiver.class);
		verify(mMockContext).registerReceiver(
				broadcastReceiverCaptor.capture(),
				Mockito.any(IntentFilter.class));
		assertNotNull(broadcastReceiverCaptor);

		reset(mDownloadHelperStatusMock);

		// simulate bind service
		serviceConnection.onServiceConnected(null, mDownloadServiceMock);
		verify(mDownloadHelperStatusMock, times(1)).onReportStatus(false, false,
				true, true, AUSyncerStatus.statusNeverDownloaded());

		reset(mDownloadHelperStatusMock);

		return broadcastReceiverCaptor.getValue();
	}

}
