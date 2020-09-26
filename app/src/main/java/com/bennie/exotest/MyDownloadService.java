package com.bennie.exotest;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;

public class MyDownloadService extends DownloadService {

    private ExoDatabaseProvider databaseProvider;
    private SimpleCache downloadCache;
    private DefaultHttpDataSourceFactory dataSourceFactory;
    private DownloadManager downloadManager;
    private File mDir;

    MyDownloadService(int foregroundNotificationId) {
        super(foregroundNotificationId);
    }

    @Override
    protected DownloadManager getDownloadManager() {
        // Note: This should be a singleton in your app.
        databaseProvider = new ExoDatabaseProvider(this);

        // A download cache should not evict media, so should use a NoopCacheEvictor.
        checkFolder();
        downloadCache = new SimpleCache(
                mDir,
                new NoOpCacheEvictor(),
                databaseProvider);
        // Create a factory for reading the data from the network.
        dataSourceFactory = new DefaultHttpDataSourceFactory();

        // Choose an executor for downloading data. Using Runnable::run will cause each download task to
        // download data on its own thread. Passing an executor that uses multiple threads will speed up
        // download tasks that can be split into smaller parts for parallel execution. Applications that
        // already have an executor for background downloads may wish to reuse their existing executor.
        Executor downloadExecutor = Runnable::run;

        // Create the download manager.
        downloadManager = new DownloadManager(
                this,
                databaseProvider,
                downloadCache,
                dataSourceFactory,
                downloadExecutor);

        // Optionally, setters can be called to configure the download manager.
//        downloadManager.setRequirements(requirements);
//        downloadManager.setMaxParallelDownloads(3);
        return downloadManager;
    }

    @Nullable
    @Override
    protected Scheduler getScheduler() {
        return null;
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads) {
        return null;
    }

    //here you can check folder where you want to store download Video
    public void checkFolder() {
        String mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ExoTest/";
        mDir = new File(mPath);
        boolean isDirectoryCreated = mDir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated = mDir.mkdir();
        }
        if (isDirectoryCreated) {
            // do something\
            Log.d("Folder", "Already Created");
        }
    }
}
