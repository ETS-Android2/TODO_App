package com.qadr.todo;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.qadr.todo.databasefiles.TheDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;

public class SettingsActivity extends AppCompatActivity {
    public static final int RINGTONE_CODE = 11, CALL_CODE = 122;
    String from;
    public LinearLayout parent;

    static Map<String, Integer> tones = new HashMap<>();

    static void putTones() {
        tones.put("Alarming", R.raw.alarming);
        tones.put("Break Forth", R.raw.break_forth);
        tones.put("Bus Horn", R.raw.bus_horn);
        tones.put("Clicking", R.raw.clicking);
        tones.put("Congratulations", R.raw.congratulations);
        tones.put("Door Bell", R.raw.door_bell);
        tones.put("Happy Jump", R.raw.happy_jump);
        tones.put("Oringz", R.raw.oringz_w446);
        tones.put("Pluck", R.raw.pluck);
        tones.put("Sorted", R.raw.sorted);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        from = getIntent().getExtras().getString("from");
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        parent = findViewById(R.id.coordinator);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment(parent))
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        AdView adView = findViewById(R.id.adView);
        Log.d( "onCreate: settings",  "before ad");
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public void onBackPressed() {
         if(from.equals("list")) {
            startActivity(new Intent(this, ListActivity.class));
        }else if(from.equals("todo")) {
            startActivity(new Intent(this, TodoActivity.class).putExtra("category", "all").putExtra("icon", MyListAdapter._icons[0]));
        }
         finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Preference clearAll;
        SwitchPreferenceCompat vibrate, fullscreen, notification, call;
        CustomListPreference sounds;
        private Context context;
        Handler handler;
        LinearLayout parentLayout;

        public SettingsFragment(LinearLayout linearLayout) {
            this.parentLayout = linearLayout;
        }

        ActivityResultLauncher<String> requestCallPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if (result) {
                            call.setChecked(!call.isChecked());
                        } else {
                            showSnackBar();
                        }
                    }
                }
        );

        ActivityResultLauncher<Intent> appSettingsIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkCallPermission()
        );


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            context = getActivity();
            handler = new Handler();

            vibrate = findPreference("vibrate");
            fullscreen = findPreference("fullscreen");
            notification = findPreference("notifications");
            clearAll = findPreference("clear");
            call = findPreference("call");
            sounds = findPreference("tone_list");

            call.setOnPreferenceChangeListener((preference, newValue) -> checkCallPermission());
            sounds.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.d( "onPreferenceChange: ", newValue.toString());
                return true;
            });
            sounds.setOnPreferenceClickListener(preference -> true);
            clearAll.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(getString(R.string.delete_alert_title))
                        .setMessage(getString(R.string.delete_alert_text))
                        .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteAll())
                        .show();
                return true;
            });
            SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(context);
            String title = sharedPreferences.getString("ringtone_name", "Alarming");
            sounds.setSummary(title);
        }

        private boolean checkCallPermission() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CALL_PHONE)) {
                    showSnackBar();
                } else {
                    requestCallPermission.launch(Manifest.permission.CALL_PHONE);
                }
                return false;
            }
        }

        public void showSnackBar() {
            Snackbar.make(parentLayout, R.string.contact_reason, Snackbar.LENGTH_LONG)
                    .setAction(R.string.grant_permission, v -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                        intent.setData(uri);
                        appSettingsIntent.launch(intent);
                    }).show();
        }

        private void deleteAll() {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setView(R.layout.circular_progressbar);
            SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(context);
            final boolean cancelAlarm = sharedPreferences.getBoolean("cancel _notification", false);
            final AlertDialog dialog = builder.show();
            builder.setCancelable(false);
            new Thread(() -> {
                if (cancelAlarm) {
                    ArrayList<TodoWork> todoWorks = (ArrayList<TodoWork>) TheDatabase.getInstance(context).todoDao().getAll();
                    for (TodoWork work : todoWorks) {
                        if (!work.isDone() && work.isRemindMe()) {
                            new TheAlarm(work.getTimeInMilli(), work.getUid()).cancel(context);
                        }
                    }
                }

                TheDatabase.getInstance(context).todoDao().clearAll();
                handler.post(dialog::dismiss);
            }).start();
        }

    }
}