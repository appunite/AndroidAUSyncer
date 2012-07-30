package com.appunite.ausyncer.tests.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import android.net.Uri;
import android.test.AndroidTestCase;

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
		assertThat("At start point last success should be -1",
				mPreference.getLastSuccess(AUTHORITY_URI), equalTo(-1L));
		assertThat("At start point last error should be -1",
				mPreference.getLastError(AUTHORITY_URI), equalTo(-1L));
	}

	public void testForSaveing() {
		mPreference.setLastSync(AUTHORITY_URI, 123L);
		assertThat("value should be exacly as saved",
				mPreference.getLastSuccess(AUTHORITY_URI), equalTo(123L));
	}

}
