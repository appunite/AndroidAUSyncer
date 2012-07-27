package com.appunite.syncer;

import java.util.Date;

public interface DownloadHelperStatus {

	void reportStatus(boolean screenVisible, boolean screenEmpty,
			boolean screenProgress, boolean progressIndicator,
			Date lastError);
	
}