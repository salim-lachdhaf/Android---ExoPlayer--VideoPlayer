package com.example.adtcvideoplayer;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.io.File;

public class ExoPlayer extends Activity {

    private static final int MY_PERMISSIONS_REQUEST = 100;
    private static ProgressDialog pDialog;
    private SimpleExoPlayer player;
    private InterstitialAd mInterstitialAd;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();


    private DataSource.Factory mediaDataSourceFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_video);

        MobileAds.initialize(this, getString(R.string.ADMOB_APP_ID));

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.ADMOB_INTERSTITIEL));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializePlayer();

        getVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }

    private void getVideo() {
        if (getIntent() != null && getIntent().getStringExtra("URL") != null) {
            playVideo(Uri.parse(getIntent().getStringExtra("URL")));
        } else if (getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath() != null) {
            playVideo(Uri.parse(getIntent().getData().toString()));
            //handlePermission();
        } else {
            Toast.makeText(this, android.R.string.httpErrorBadUrl, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void ShowProgress(boolean show, String msg) {
        try {
            if (show) {
                if (pDialog == null) {
                    pDialog = new ProgressDialog(ExoPlayer.this);
                }
                pDialog.setMessage(msg);
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            } else {
                if (pDialog != null) {
                    pDialog.dismiss();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void initializePlayer() {

        mediaDataSourceFactory = new DefaultDataSourceFactory(this, getUserAgent(), BANDWIDTH_METER);

        PlayerView mPlayerView = findViewById(R.id.exoplayer);

        mPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);

        mPlayerView.requestFocus();

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        mPlayerView.setPlayer(player);

        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                ShowProgress(isLoading, getString(R.string.loading));
            }

            @Override
            public void onPlayerStateChanged(boolean play, int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    ShowProgress(false, getString(R.string.loading));
                    if (!play)//video paused
                        showAds();
                } else if (playbackState == Player.STATE_BUFFERING) {
                    ShowProgress(true, getString(R.string.loading));
                } else {
                    ShowProgress(false, getString(R.string.loading));
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            }

            @Override
            public void onSeekProcessed() {
            }
        });

    }

    private void playVideo(Uri UrlUri) {
        try {
            MediaSource mediaSource = buildMediaSource(UrlUri);

            player.prepare(mediaSource, true, true);
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                DataSource.Factory sSmanifestDataSourceFactory = new DefaultHttpDataSourceFactory(getUserAgent());
                SsChunkSource.Factory sSChunkSourceFactory = new DefaultSsChunkSource.Factory(mediaDataSourceFactory);
                return new SsMediaSource.Factory(sSChunkSourceFactory, sSmanifestDataSourceFactory).createMediaSource(uri);
            case C.TYPE_DASH:
                DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory(getUserAgent());
                DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(mediaDataSourceFactory);
                return new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private String getUserAgent() {
        return Util.getUserAgent(this, getString(R.string.app_name));
    }

    private void showAds() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    File myFile = new File(getIntent().getData().getPath());
                    playVideo(Uri.fromFile(myFile));
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void handlePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            File myFile = new File(getIntent().getData().getPath());
            playVideo(Uri.parse(getIntent().getData().toString()));
            // Permission has already been granted
        }
    }
}
