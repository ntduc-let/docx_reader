package com.prox.docxreader.ui.activity;

import static com.prox.docxreader.ui.activity.ReaderActivity.FILE_PATH;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.prox.docxreader.BuildConfig;
import com.prox.docxreader.LocaleHelper;
import com.prox.docxreader.databinding.ActivitySplashBinding;
import com.prox.docxreader.utils.FileUtils;
import com.proxglobal.proxads.adsv2.ads.ProxAds;
import com.proxglobal.proxads.adsv2.callback.AdsCallback;
import com.proxglobal.purchase.ProxPurchase;

import java.io.File;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    public static final String SPLASH_TO_MAIN = "SPLASH_TO_MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Load ngôn ngữ
        LocaleHelper.loadLanguage(this);

        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (ProxPurchase.getInstance().checkPurchased()) {
            binding.tittleSplash.setVisibility(View.GONE);
        }

        String action = getIntent().getAction();
        new Handler().postDelayed(() -> {
            if (action == null) {
                goToMainActivity();
            } else if (action.equals(Intent.ACTION_MAIN)) {
                showInterSplash();
            } else if (action.equals(Intent.ACTION_VIEW)) {
                showInterOutside();
            }
        }, 1000);
    }

    private void showInterSplash() {
        ProxAds.getInstance().showSplash(this, new AdsCallback() {
            @Override
            public void onShow() {
                Log.d("interstitial_splash", "onShow");
            }

            @Override
            public void onClosed() {
                Log.d("interstitial_splash", "onClosed");
                goToMainActivity();
            }

            @Override
            public void onError() {
                Log.d("interstitial_splash", "onError");
                goToMainActivity();
            }
        }, BuildConfig.interstitial_splash, null, 10000);
    }

    private void showInterOutside() {
        ProxAds.getInstance().showSplash(this, new AdsCallback() {
            @Override
            public void onShow() {
                Log.d("interstitial_outside", "onShow");
            }

            @Override
            public void onClosed() {
                Log.d("interstitial_outside", "onClosed");
                goToReaderActivity();
            }

            @Override
            public void onError() {
                Log.d("interstitial_outside", "onError");
                goToReaderActivity();
            }
        }, BuildConfig.interstitial_open_outside, null, 12000);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(SPLASH_TO_MAIN);
        startActivity(intent);
        finish();
    }


    private void goToReaderActivity() {
        Uri data = getIntent().getData();
        String path = FileUtils.getPath(data, this);

        if (new File(path).exists()){
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra(FILE_PATH, path);
            startActivity(intent);
            finish();
        }else {
            goToMainActivity();
        }


    }

    @Override
    public void onBackPressed() {
    }
}