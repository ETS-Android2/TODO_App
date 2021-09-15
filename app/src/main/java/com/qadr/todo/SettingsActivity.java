package com.qadr.todo;

import android.Manifest;
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
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static com.qadr.todo.AddActivity.SETTINGS_CODE;

public class SettingsActivity extends AppCompatActivity {
    public static final int RINGTONE_CODE = 11, CALL_CODE = 122;
    String from;
    public static LinearLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        from = getIntent().getExtras().getString("from");
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        parent = findViewById(R.id.coordinator);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public void onBackPressed() {
         if(from.equals("list")) {
            startActivity(new Intent(SettingsActivity.this, ListActivity.class));
        }else if(from.equals("todo")) {
            startActivity(new Intent(SettingsActivity.this, TodoActivity.class).putExtra("category", "all").putExtra("icon", MyListAdapter._icons[0]));
        } else {
            super.onBackPressed();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Preference ringtone, clearAll;
        SwitchPreferenceCompat vibrate, fullscreen, notification, call;
        private Context context;
        Handler handler;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            context = getActivity();
            handler = new Handler();

            ringtone = findPreference("ringtone");
            vibrate = findPreference("vibrate");
            fullscreen = findPreference("fullscreen");
            notification = findPreference("notifications");
            clearAll = findPreference("clear");
            call = findPreference("call");

            call.setOnPreferenceClickListener(preference -> {
                call.setChecked(!call.isChecked());
                checkCallPermission();
                return false;
            });

            ringtone.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM | TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
//                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, true);
                if(intent.resolveActivity(context.getPackageManager()) != null){
                    startActivityForResult(intent, RINGTONE_CODE);
                }
                return false;
            });
            clearAll.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(getString(R.string.delete_alert_title))
                        .setMessage(getString(R.string.delete_alert_text))
                        .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(getString(R.string.delete), (dialog, which) ->deleteAll())
                        .show();
                return false;
            });
            SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(context);
            Ringtone tone = RingtoneManager.getRingtone(this.getContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            String title = tone.getTitle(this.getContext());
            title = sharedPreferences.getString("ringtone_name", title);
            ringtone.setSummary(title);
        }

        private void checkCallPermission() {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                call.setChecked(!call.isChecked());
            }else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CALL_PHONE)){
                    showSnackBar();
                }else{
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CALL_PHONE}, CALL_CODE);
                }
            }
        }
        public void showSnackBar() {
            Snackbar.make(parent, R.string.contact_reason, Snackbar.LENGTH_LONG)
                    .setAction(R.string.grant_permission, v -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:"+ context.getPackageName()));
                        startActivityForResult(intent, SETTINGS_CODE);
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
                if (cancelAlarm){
                    ArrayList<TodoWork> todoWorks = (ArrayList<TodoWork>) TheDatabase.getInstance(context).todoDao().getAll();
                    for (TodoWork work : todoWorks){
                        if(!work.isDone() && work.isRemindMe()){
                            new TheAlarm(work.getTimeInMilli(), work.getUid()).cancel(context);
                        }
                    }
                }

                TheDatabase.getInstance(context).todoDao().clearAll();
                handler.post(dialog::dismiss);
            }).start();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == RINGTONE_CODE && resultCode == RESULT_OK && data != null){
                Uri uri = data.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI);

                // get ringtone title
                Ringtone tone = RingtoneManager.getRingtone(this.getContext(), uri);
                String title = tone.getTitle(this.getContext());
                ringtone.setSummary(title);

                // saved ringtone
                SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(this.getContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ringtone_name", title);
                editor.putString("ringtone_uri", String.valueOf(uri));
                editor.apply();
                Log.e("onActivityResult- ",title);
                Log.i("onActivityResult- ",String.valueOf(uri));
            }else if (requestCode == SETTINGS_CODE && resultCode == RESULT_OK){
                checkCallPermission();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == CALL_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    call.setChecked(!call.isChecked());
                }else {
                    call.setChecked(false);
                }
            }
        }
    }
}