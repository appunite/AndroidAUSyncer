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

public interface DownloadHelperStatus {

	/**
	 * Reports progress/error status
	 * 
	 * <p>
	 * Example use case:
	 * 
	 * <pre class="prettyprint">
	 * public void reportStatus(boolean screenVisible, boolean screenEmpty,
	 * 		boolean screenProgress, boolean progressIndicator, Date lastError) {
	 * 	mListView.setVisibility(screenVisible ? View.VISIBLE : View.GONE);
	 * 	mEmptyView.setVisibility(screenEmpty ? View.VISIBLE : View.GONE);
	 * 	mScreenProgressBar.setVisibility(screenProgress ? View.VISIBLE : View.GONE);
	 * 	mErrorLayout.setVisibility(lastError != null ? View.VISIBLE : View.GONE);
	 * 	mErrorRefreshButton.setVisibility(!progressIndicator ? View.VISIBLE
	 * 			: View.GONE);
	 * 	mErrorRefreshProgress.setVisibility(progressIndicator ? View.VISIBLE
	 * 			: View.GONE);
	 * 	if (lastError != null) {
	 * 		long currentTimeMillis = System.currentTimeMillis();
	 * 		CharSequence lastErrorString = DateUtils.getRelativeTimeSpanString(
	 * 				currentTimeMillis, lastError.getTime(), 0,
	 * 				DateUtils.FORMAT_ABBREV_ALL);
	 * 		String errorFormat = getString(R.string.main_error_occure);
	 * 		CharSequence errorMessage = String.format(errorFormat, lastErrorString);
	 * 		mErrorMessageTextView.setText(errorMessage);
	 * 	}
	 * 	if (mProgressIndicator != progressIndicator) {
	 * 		this.mProgressIndicator = progressIndicator;
	 * 		invalidateOptionsMenu();
	 * 	}
	 * }
	 * 
	 * &#064;Override
	 * public boolean onPrepareOptionsMenu(Menu menu) {
	 * 	MenuItem menuProgress = menu.findItem(R.id.menu_progress);
	 * 	MenuItem menuRefresh = menu.findItem(R.id.menu_refresh);
	 * 	menuProgress.setVisible(mProgressIndicator);
	 * 	menuRefresh.setVisible(!mProgressIndicator);
	 * 	return true;
	 * }
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param screenVisible
	 *            should screen be visible to user
	 * @param screenEmpty
	 *            should empty view indicator be displayed to user
	 * @param screenProgress
	 *            should covering progress bar be displayed to user
	 * @param progressIndicator
	 *            should progress indicator not covering user view be displayed
	 * @param lastStatus
	 *            status of last connection
	 */
	void onReportStatus(boolean screenVisible, boolean screenEmpty,
			boolean screenProgress, boolean progressIndicator,
			AUSyncerStatus lastStatus);
	
}