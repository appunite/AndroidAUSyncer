package com.appunite.syncer;

import android.net.Uri;
import android.os.Bundle;

interface IDownloadService {
	long getLastSuccess(in Uri uri);
	long getLastError(in Uri uri);
    boolean inProgress(in Uri uri);
    void download(in Uri uri, in Bundle bundle, in boolean withForce);
}