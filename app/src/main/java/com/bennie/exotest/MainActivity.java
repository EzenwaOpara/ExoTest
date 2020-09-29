package com.bennie.exotest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadHelper;
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
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private File mDownloadDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        dataSourceFactory = new DefaultHttpDataSourceFactory();
        downloadMedia();

        Cache cache = getDownloadCache();

        CacheDataSource.Factory factory = buildReadOnlyCacheDataSource(dataSourceFactory, cache);
        player = new SimpleExoPlayer.Builder(this)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(factory))
                .build();
        playerView.setPlayer(player);

        ProgressiveMediaSource mediaSource =
                new ProgressiveMediaSource.Factory(factory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(getString(R.string.videoUrl))));
        player.setMediaSource(mediaSource);
        player.prepare();

        player.play();
    }

    private void downloadMedia() {

        final JSONObject[] jsonObject = new JSONObject[1];

        DownloadHelper helper = DownloadHelper.forProgressive(this,
                Uri.parse(getString(R.string.videoUrl)));

        helper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {
                jsonObject[0] = new JSONObject();
                try {
                    jsonObject[0].put("title", "Robotic presentation");
                    jsonObject[0].put("artist", "Opara Benjamin");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                DownloadRequest download = helper.getDownloadRequest("First Download", Util.getUtf8Bytes(Arrays.toString(jsonObject)));

                DownloadService.sendAddDownload(MainActivity.this, DownloadService.class, download, true);
                Toast.makeText(MainActivity.this, "Downloading...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareError(DownloadHelper helper, IOException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory =
                    new File(getDownloadDirectory(), "Downloads");
            downloadCache =
                    new SimpleCache(
                            downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider());
        }
        return downloadCache;
    }

    private synchronized File getDownloadDirectory() {
        mDownloadDirectory = null;
        if (mDownloadDirectory == null) {
            mDownloadDirectory = this.getExternalFilesDir(/* type= */ null);
            if (mDownloadDirectory == null) {
                mDownloadDirectory = this.getFilesDir();
            }
        }
        return mDownloadDirectory;
    }

    private synchronized DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(this);
        }
        return databaseProvider;
    }

    private CacheDataSource.Factory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

//    private void getDownloadedItems() {
//
//        dataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(this, getString(R.string.app_name)));
//        val cachedDataSourceFactory =CacheDataSourceFactory((application as App).appContainer.downloadCache, dataSourceFactory)
//        val mediaSources = ProgressiveMediaSource.Factory(cachedDataSourceFactory).createMediaSource(
//                Uri.parse(media.url))
//        player.prepare(mediaSources)
//
//        DataSource.Factory cacheDataSourceFactory = null;
//        ProgressiveMediaSource mediaSource =
//                new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
//                        .createMediaSource(MediaItem.fromUri(Uri.parse(getString(R.string.videoUrl))));
//        player.setMediaSource(mediaSource);
//        player.prepare();

    // Create a read-only cache data source factory using the download cache.
//        DataSource.Factory cacheDataSourceFactory =
//                new CacheDataSource.Factory()
//                        .setCache(downloadCache)
//                        .setUpstreamDataSourceFactory(httpDataSourceFactory)
//                        .setCacheWriteDataSinkFactory(null); // Disable writing.
//
//        SimpleExoPlayer player = new SimpleExoPlayer.Builder(context)
//                .setMediaSourceFactory(
//                        new DefaultMediaSourceFactory(cacheDataSourceFactory))
//                .build();


//        ArrayList<MediaStore>  downloadTracks = new ArrayList<>();
//
//        DownloadCursor downloadCursor = new DownloadCursor() {
//            @Override
//            public Download getDownload() {
//                return null;
//            }
//
//            @Override
//            public int getCount() {
//                return 0;
//            }
//
//            @Override
//            public int getPosition() {
//                return 0;
//            }
//
//            @Override
//            public boolean moveToPosition(int position) {
//                return false;
//            }
//
//            @Override
//            public boolean isClosed() {
//                return false;
//            }
//
//            @Override
//            public void close() {
//
//            }
//        };
//
//        return downloadTrack
}