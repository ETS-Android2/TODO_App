package com.qadr.todo;

import android.app.Application;
import android.content.SharedPreferences;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

/** The Application class that manages AppOpenManager. */
public class MyApplication extends Application {

    private static AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(
                this,
                initializationStatus -> {

                });

        SharedPreferences sharedPref = AlarmReceiver.SettingsSharedPreference.getInstance(getApplicationContext());
        boolean isFirstTime = sharedPref.getBoolean("first_time", true);
        if (!isFirstTime) {
            appOpenManager = new AppOpenManager(this);
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("first_time", false);
        editor.apply();
    }
}