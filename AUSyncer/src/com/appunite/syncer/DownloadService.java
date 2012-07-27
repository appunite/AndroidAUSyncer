package com.appunite.syncer;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public abstract class DownloadService extends Service {

	protected static final String ON_PROGRESS_CHANGE_EXTRA_URI = null;
	public static final String ON_PROGRESS_CHANGE = null;

	private IDownloadService.Stub mBinder = new IDownloadService.Stub() {

		@Override
		public long getLastSuccess(Uri uri) throws RemoteException {
			return DownloadService.this.getLastSuccess(uri);
		}

		@Override
		public long getLastError(Uri uri) throws RemoteException {
			return DownloadService.this.getLastError(uri);
		}

		@Override
		public boolean inProgress(Uri uri) throws RemoteException {
			return DownloadService.this.inProgress(uri);
		}

		@Override
		public void download(Uri uri, Bundle bundle, boolean withForce) throws RemoteException {
			DownloadService.this.download(uri, bundle, withForce);
		}

	};
	
	protected long getLastSuccess(Uri uri) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private void setLastSync(Uri uri) {
		// TODO save last sync
		// TODO clear last error
	}
	
	private void setLastError(Uri uri) {
		// TODO save last error
	}
	
	protected long getLastError(Uri uri) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected void download(Uri uri, Bundle bundle, boolean withForce) throws RemoteException {
		Task task = new Task();
		task.uri = uri;
		task.bundle = bundle;
		task.withForce = withForce;
		synchronized (this) {
			mQueue.add(task);
		}
	}
	
	protected boolean inProgress(Uri uri) {
		synchronized (this) {
			for (Task task : mQueue) {
				if (task.uri.equals(uri))
					return true;
			}
		}
		return false;
	}

	private static class MyThread extends Thread {
		private final DownloadService mDownloadService;

		public MyThread(DownloadService downloadService, String threadName) {
			super(threadName);
			this.mDownloadService = downloadService;
		}

		@Override
		public void run() {
			mDownloadService.run();
		}
	}

	private static class Task {

		public Uri uri;
		public Bundle bundle;
		public boolean withForce;

	}

	private MyThread thread = new MyThread(this, "DownloadService");
	private ArrayList<Task> mQueue = new ArrayList<DownloadService.Task>();

	protected int mNumberOfListeners = 0;

	@Override
	public IBinder onBind(Intent intent) {
		mNumberOfListeners++;
		return mBinder;
	}

	protected void run() {
		for (;;) {
			try {
				Task task;
				synchronized (this) {
					if (mNumberOfListeners <= 0)
						return;
					while (mQueue.size() == 0)
						this.wait();
					task = mQueue.get(0);
					
				}
				boolean success = onHandleUri(task.uri, task.bundle, task.withForce);
				if (success) {
					setLastSync(task.uri);
				} else {
					setLastError(task.uri);
				}
				synchronized (this) {
					mQueue.remove(0);
				}
				Intent broadcastIntent = new Intent(ON_PROGRESS_CHANGE);
				broadcastIntent.putExtra(ON_PROGRESS_CHANGE_EXTRA_URI, task.uri);
				this.sendBroadcast(broadcastIntent );
			} catch (InterruptedException e) {
			}
		}
	}

	protected abstract boolean onHandleUri(Uri uri, Bundle bundle, boolean withForce);

	@Override
	public boolean onUnbind(Intent intent) {
		mNumberOfListeners--;
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		thread.start();
	}
}
