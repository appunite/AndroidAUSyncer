package com.appunite.ausyncer.tests.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.appunite.syncer.AUSyncerStatus;
import com.appunite.syncer.DownloadSharedPreference;

public class DownloadSharedPreferencesTest extends AndroidTestCase {

	private DownloadSharedPreference mPreference;
	private String AUTHORITY;
	private Uri AUTHORITY_URI;

	public DownloadSharedPreferencesTest() {
		setName("DownloadSharedPreferencesTest");
	}

	protected void setUp() throws Exception {
		super.setUp();
		mPreference = new DownloadSharedPreference(mContext);
		mPreference.clear();

		AUTHORITY = "com.appunite.syncer.test";
		AUTHORITY_URI = Uri.parse("content+//" + AUTHORITY);
	}

	public void test1Prepare() {
		assertNotNull(mPreference);
		assertNotNull(AUTHORITY);
		assertNotNull(AUTHORITY_URI);
	}

	public void testForStartingPoint() {
		assertThat("At start point status should be never downloaded",
				mPreference.getLastStatus(AUTHORITY_URI),
				equalTo(AUSyncerStatus.statusNeverDownloaded()));
	}

	public void testForSaveing() {
		AUSyncerStatus statusSuccess = AUSyncerStatus.statusSuccess();
		mPreference.setLastStatus(AUTHORITY_URI, statusSuccess);
		assertThat(mPreference.getLastStatus(AUTHORITY_URI), equalTo(statusSuccess));
		assertThat(mPreference.getLastStatus(AUTHORITY_URI).getStatusTimeMs(), equalTo(statusSuccess.getStatusTimeMs()));
	}

}
