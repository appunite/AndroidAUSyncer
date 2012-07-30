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

package com.example.exampleausyncer;

import java.util.Date;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.appunite.syncer.DownloadHelper;
import com.appunite.syncer.DownloadHelperStatus;
import com.example.exampleausyncer.adapter.MainAdapter;
import com.example.exampleausyncer.provider.ExampleContract;
import com.example.exampleausyncer.service.DownloadService;

public class Main extends Activity implements OnClickListener,
		DownloadHelperStatus, LoaderCallbacks<Cursor> {
	private static final int LOADER_MAIN = 0;
	private ListView mListView;
	private View mProgressBar;
	private DownloadHelper mDownloadHelper;
	private View mEmptyView;
	private View mErrorLayout;
	private TextView mErrorMessageTextView;
	private Button mErrorRefreshButton;
	private CursorAdapter mAdapter;
	private boolean mProgressIndicator;
	private View mErrorRefreshProgress;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mListView = (ListView) findViewById(android.R.id.list);
		mProgressBar = findViewById(android.R.id.progress);
		mEmptyView = findViewById(android.R.id.empty);
		mErrorLayout = findViewById(R.id.error_layout);
		mErrorMessageTextView = (TextView) findViewById(R.id.error_message);
		mErrorRefreshButton = (Button) findViewById(R.id.error_refresh_button);
		mErrorRefreshProgress = findViewById(R.id.error_refresh_progress);

		mErrorRefreshButton.setOnClickListener(this);

		mDownloadHelper = new DownloadHelper(this, DownloadService.ACTION_SYNC,
				this, ExampleContract.Example.CONTENT_URI);

		mAdapter = new MainAdapter(this, null);
		mListView.setAdapter(mAdapter);
		getLoaderManager().restartLoader(LOADER_MAIN, null, this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mDownloadHelper.onActivityPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDownloadHelper.onActivityResume();
		mDownloadHelper.startDownloading(null, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuProgress = menu.findItem(R.id.menu_progress);
		MenuItem menuRefresh = menu.findItem(R.id.menu_refresh);
		menuProgress.setVisible(mProgressIndicator);
		menuRefresh.setVisible(!mProgressIndicator);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.menu_refresh:
			refresh();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.error_refresh_button:
			refresh();
			return;
		default:
			throw new RuntimeException();
		}
	}

	private void refresh() {
		mDownloadHelper.startDownloading(null, true);
	}

	public void reportStatus(boolean screenVisible, boolean screenEmpty,
			boolean screenProgress, boolean progressIndicator, Date lastError) {
		mListView.setVisibility(screenVisible ? View.VISIBLE : View.GONE);
		mEmptyView.setVisibility(screenEmpty ? View.VISIBLE : View.GONE);
		mProgressBar.setVisibility(screenProgress ? View.VISIBLE : View.GONE);
		mErrorLayout
				.setVisibility(lastError != null ? View.VISIBLE : View.GONE);
		mErrorRefreshButton.setVisibility(!progressIndicator ? View.VISIBLE
				: View.GONE);
		mErrorRefreshProgress.setVisibility(progressIndicator ? View.VISIBLE
				: View.GONE);
		if (lastError != null) {
			long currentTimeMillis = System.currentTimeMillis();
			CharSequence lastErrorString = DateUtils.getRelativeTimeSpanString(
					currentTimeMillis, lastError.getTime(), 0,
					DateUtils.FORMAT_ABBREV_ALL);
			String errorFormat = getString(R.string.main_error_occure);
			CharSequence errorMessage = String.format(errorFormat,
					lastErrorString);
			mErrorMessageTextView.setText(errorMessage);
		}
		if (mProgressIndicator != progressIndicator) {
			this.mProgressIndicator = progressIndicator;
			invalidateOptionsMenu();
		}
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, ExampleContract.Example.CONTENT_URI,
				MainAdapter.projection, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mDownloadHelper.updateLocalData(cursor);
		mAdapter.swapCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mDownloadHelper.updateLocalData(null);
		mAdapter.swapCursor(null);
	}
}
