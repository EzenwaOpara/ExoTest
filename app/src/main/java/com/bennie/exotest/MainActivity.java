package com.bennie.exotest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {

    PlayerView playerView;
    SimpleExoPlayer player;

    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private ExoDatabaseProvider databaseProvider;
    private SimpleCache downloadCache;
    private DefaultHttpDataSourceFactory dataSourceFactory;
    private DownloadManager downloadManager;

//    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        playerView = findViewById(R.id.player_view);

        instantiateDownloadManager();
//        DownloadService.sendAddDownload(
//                this,
//                MyDownloadService.class,
//                downloadRequest,
//                /* foreground= */ false);

//        downloadManager.addListener(
//                new DownloadManager.Listener() {
//                    // Override methods of interest here.
//                });
    }

    private void instantiateDownloadManager() {
        String downloadDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ExoTest/";
        File path = new File(downloadDirectory);
        // Note: This should be a singleton in your app.
        databaseProvider = new ExoDatabaseProvider(this);

        // A download cache should not evict media, so should use a NoopCacheEvictor.
        downloadCache = new SimpleCache(
                path,
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
        downloadManager.setMaxParallelDownloads(3);

        //Adding download
        DownloadRequest downloadRequest =
                new DownloadRequest.Builder(getString(R.string.videoUrl),
                        Uri.parse(getString(R.string.videoUrl))).build();

        DownloadService.sendAddDownload(
                this,
                MyDownloadService.class,
                downloadRequest,
                /* foreground= */ false);

        // Set the stop reason for a single download.
        DownloadService.sendSetStopReason(
                this,
                MyDownloadService.class,
                getString(R.string.videoUrl),
                0,
                /* foreground= */ false);

        // Clear the stop reason for a single download.
        DownloadService.sendSetStopReason(
                this,
                MyDownloadService.class,
                null,
                Download.STOP_REASON_NONE,
                /* foreground= */ false);


        downloadManager.addListener(
                new DownloadManager.Listener() {
                    // Override methods of interest here.

                    @Override
                    public void onInitialized(DownloadManager downloadManager) {
                        Toast.makeText(MainActivity.this, "Download init", Toast.LENGTH_SHORT).show();
                    }
                });

        // Create a read-only cache data source factory using the download cache.
        DataSource.Factory cacheDataSourceFactory =
                new CacheDataSource.Factory()
                        .setCache(downloadCache)
                        .setUpstreamDataSourceFactory(dataSourceFactory)
                        .setCacheWriteDataSinkFactory(null); // Disable writing.

        SimpleExoPlayer player = new SimpleExoPlayer.Builder(this)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build();

        ProgressiveMediaSource mediaSource =
                new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(path + getString(R.string.videoUrl))));
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
    }



    //create media source
//    private MediaSource buildMediaSource(Uri uri) {
//        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-codelab");
//
//        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
//    }
//
    //init player to play
//    private void initializePlayer() {
//        player = new SimpleExoPlayer.Builder(this).build();
//        playerView.setPlayer(player);
//
//        Uri uri = Uri.parse(getString(R.string.videoUrl));
//        MediaSource mediaSource = buildMediaSource(uri);
//
//
//        player.setPlayWhenReady(playWhenReady);
//        player.seekTo(currentWindow, playbackPosition);
//        player.prepare(mediaSource, false, false);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (Util.SDK_INT >= 24) initializePlayer();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        hideSystemUi();
//        if ((Util.SDK_INT < 24 || player == null)) initializePlayer();
//    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                /*| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION*/);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) releasePlayer();
    }


    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }


    public void removeVideo() {
        DownloadService.sendRemoveDownload(
                this,
                MyDownloadService.class,
                getString(R.string.videoUrl),
                /* foreground= */ false);
        Toast.makeText(this, "Deleted...", Toast.LENGTH_SHORT).show();
    }

    public static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 123;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("Write Storage permission is necessary to Download Images and Videos!!!");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public void checkAgain() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle("Permission necessary");
            alertBuilder.setMessage("Write Storage permission is necessary to Download Images and Videos!!!");
            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
            });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
    }

    //Here you can check App Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Files can be saved now", Toast.LENGTH_SHORT).show();
                } else {
                    //code for deny
                    checkAgain();
                }
                break;
        }
    }

}