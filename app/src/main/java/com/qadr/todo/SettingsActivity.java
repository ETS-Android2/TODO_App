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
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
    public LinearLayout parent;

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
                    .replace(R.id.settings, new SettingsFragment(parent))
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
        LinearLayout parentLayout;

        public SettingsFragment(LinearLayout linearLayout) {
            this.parentLayout = linearLayout;
        }

        ActivityResultLauncher<Intent> mStartForResult =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                if (result.getResultCode() == Activity.RESULT_OK) {
                                    Intent intent = result.getData();
                                    // Handle the Intent
                                    Uri uri = intent.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI);

                                    // get ringtone title
                                    Ringtone tone = RingtoneManager.getRingtone(getContext(), uri);
                                    String title = tone.getTitle(getContext());
                                    ringtone.setSummary(title);

                                    // saved ringtone
                                    SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(getContext());
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("ringtone_name", title);
                                    editor.putString("ringtone_uri", String.valueOf(uri));
                                    editor.apply();
                                    Log.e("onActivityResult- ", title);
                                    Log.i("onActivityResult- ", String.valueOf(uri));
                                    Log.i("onActivityResult- ", intent.getExtras().toString());
                                }
                            }
                        });

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
                result -> {
                    checkCallPermission();
                }
        );


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

            call.setOnPreferenceChangeListener((preference, newValue) -> checkCallPermission());

            ringtone.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM | TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
//                intent.putExtra("REQUEST_CODE", RINGTONE_CODE);
//                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, true);
                // Try to invoke the intent.
                try {
                    mStartForResult.launch(intent);
//                    startActivityForResult(intent, RINGTONE_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), "No app to handle the request", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
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
            Ringtone tone = RingtoneManager.getRingtone(this.getContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            String title = tone.getTitle(this.getContext());
            title = sharedPreferences.getString("ringtone_name", title);
            ringtone.setSummary(title);
        }

        private boolean checkCallPermission() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CALL_PHONE)) {
                    requestCallPermission.launch(Manifest.permission.CALL_PHONE);
                } else {
                    showSnackBar();
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