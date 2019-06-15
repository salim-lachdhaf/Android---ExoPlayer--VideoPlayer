package com.example.adtcvideoplayer;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.net.URL;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, getString(R.string.ADMOB_APP_ID));
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        EditText url = findViewById(R.id.url);
        ImageView play = findViewById(R.id.exo_play);

        VideoView simpleVideoView = findViewById(R.id.videoBG);
        simpleVideoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bg));
        simpleVideoView.start();
        simpleVideoView.setOnPreparedListener(mp -> mp.setLooping(true));

        play.setOnClickListener(v -> {
            if (isValidURL(url.getText().toString()))
                startActivity(new Intent(this, ExoPlayer.class).putExtra("URL", url.getText().toString()));
            else
                Toast.makeText(this, android.R.string.httpErrorBadUrl, Toast.LENGTH_LONG).show();
        });
    }

    private boolean isValidURL(String urlStr) {
        try {
            new URL(urlStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
