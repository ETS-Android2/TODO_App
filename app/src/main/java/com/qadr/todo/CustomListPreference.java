package com.qadr.todo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.ListPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CustomListPreference extends ListPreference {
    Context context;
    List<String> tones;
    int res = 0; String selected;

    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        tones = Arrays.asList(context.getResources().getStringArray(R.array.ten_tones));
        SettingsActivity.putTones();
        getTone();
    }

    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        tones = Arrays.asList(context.getResources().getStringArray(R.array.ten_tones));
        SettingsActivity.putTones();
        getTone();
    }

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        tones = Arrays.asList(context.getResources().getStringArray(R.array.ten_tones));
        SettingsActivity.putTones();
        getTone();
    }

    public CustomListPreference(Context context) {
        super(context);
        this.context = context;
        tones = Arrays.asList(context.getResources().getStringArray(R.array.ten_tones));
        SettingsActivity.putTones();
        getTone();
    }

    void getTone() {
        SharedPreferences sharedPref = AlarmReceiver.SettingsSharedPreference.getInstance(context);
        String tone = sharedPref.getString("ringtone_name", "Alarming");
        res = tones.indexOf(tone);
    }


    @Override
    protected void onClick() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Select sound")
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    AlarmReceiver.stopSound();
                    dialog.dismiss();
                })
                .setPositiveButton("Select", (dialog, which) -> {
                    AlarmReceiver.stopSound();
                    SharedPreferences sharedPreference = AlarmReceiver.SettingsSharedPreference.getInstance(context);
                    SharedPreferences.Editor editor = sharedPreference.edit();
                    editor.putString("ringtone_name",selected);
                    editor.putInt("ringtone_res", res);
                    setSummary(selected);
                    editor.apply();
                })
                .setCancelable(false)
                .setSingleChoiceItems(R.array.ten_tones, res, (dialog, which) -> {
                    // play sound here
                    selected = tones.get(which);
                    res = SettingsActivity.tones.get(selected);
                    Log.d( "onClick: ", selected);
                    Uri sound = Uri.parse("android.resource://"+context.getPackageName()+"/" + res);
                    AlarmReceiver.playSound(context, sound, 4500, 1000);
                }).show();
    }
}
