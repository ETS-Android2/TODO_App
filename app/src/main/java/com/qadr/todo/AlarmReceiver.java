package com.qadr.todo;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.qadr.todo.databasefiles.TheDatabase;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "todolist_channelId";
    public static final String CHANNEL_NAME = "alarm channel";
    public static final String CHANNEL_DESCRIPTION = "alarm to show the scheduled call, todo list and their details";
    public static final String MARK_WORK_ACTION = "com.qadr.mark_work_done";
    public static final String MY_ALARM_ACTION = "com.qadr.alarm";
    public static final String MY_CALL_ACTION_DIAL = "com.qadr.call.dialer";
    public static final String MY_CALL_ACTION_CALL = "com.qadr.call.directCall";
    public static final long[] Vibrate = new long[]{1000, 50, 50, 100, 1000};

    @Override
    public void onReceive(Context context, Intent mIntent) {
        if (null != mIntent) {

            final int id = (int) mIntent.getExtras().getLong("uid", 0);
            Log.e(" "+ id, "onReceive: ");
            Handler handler = new Handler();

            switch (mIntent.getAction()) {
                case MY_ALARM_ACTION:
                    final PendingResult result = goAsync();
                    new Thread(() -> {
                        final TodoWork work = TheDatabase.getInstance(context).todoDao().getWork(id);
                        handler.post(() -> {
                            if (null != work){
                                //Settings checked : Check if notification is enabled
                                if (SettingsSharedPreference.getInstance(context).getBoolean("notifications", true)) {

                                    createNotification(context, work);
                                }
                            }

                            result.finish();
                        });
                        TheDatabase.getInstance(context).todoDao().updateRemindMe(id);
                    }).start();


                    break;
                case MARK_WORK_ACTION:

                    final PendingResult result1 = goAsync();
                    new Thread(() -> {
                        final TodoWork work = TheDatabase.getInstance(context).todoDao().getWork(id);
                        handler.post(() -> {
                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                notificationManager.cancel(id);
                                updateWork(context, work);

                            result1.finish();
                        });
                    }).start();
                    break;
                case MY_CALL_ACTION_DIAL: {
                    if (id > 0){
                        String number = mIntent.getExtras().getString("number");
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(context);
                        notificationManager1.cancel(id);
                        updateWork(context, id);
                        dialNumber(context, number);
                    }

                    break;
                }
                case MY_CALL_ACTION_CALL: {
                    if (id > 0){
                        String number = mIntent.getExtras().getString("number");
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(context);
                        notificationManager1.cancel(id);
                        updateWork(context, id);
                        directCall(context, number);
                    }

                    break;
                }
                default:
                    break;
            }


        }
    }


    private static void createNotification(Context context, TodoWork work) {
        createNotificationChannel(context);
        playSound(context, 9000, 3000);
        Intent markIntent = new Intent(context, AlarmReceiver.class);
        markIntent.setAction(MARK_WORK_ACTION);
        markIntent.putExtra("uid", work.getUid());
        PendingIntent markPendingIntent =
                PendingIntent.getBroadcast(context, (int) work.getUid(), markIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.app_notification);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(work.getName());
        if(work.getName().length() > 30 || !work.getNote().isEmpty()){
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(work.getName() + "\n" + work.getNote()));
            Log.e("big text", "createNotification: ");
        }
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.addAction(R.drawable.ic_baseline_done_24, context.getString(R.string.mark_as_done), markPendingIntent);
        builder.setDefaults(Notification.DEFAULT_LIGHTS);

        // for call reminders
        if (work.getCategory().equalsIgnoreCase("call")) {
            Intent callIntent = new Intent(context, AlarmReceiver.class);
            //Settings check : check if user wants to make call directly
            if (SettingsSharedPreference.getInstance(context).getBoolean("call", false)) {
                callIntent.setAction(MY_CALL_ACTION_CALL);
            } else {
                callIntent.setAction(MY_CALL_ACTION_DIAL);
            }

            callIntent.putExtra("uid", work.getUid());
            callIntent.putExtra("number", work.getNumber());
            PendingIntent callPendingIntent =
                    PendingIntent.getBroadcast(context, (int) work.getUid(), callIntent, 0);
            // end for call intent
            builder.addAction(R.drawable.add_local_phone_24, "Dial Now", callPendingIntent);
        }
        builder.setSound(null);
        if (SettingsSharedPreference.getInstance(context).getBoolean("vibrate", false)) {
            builder.setVibrate(Vibrate);
        }
        builder.setAutoCancel(true);

        if (SettingsSharedPreference.getInstance(context).getBoolean("fullscreen", false)) {
            Intent fullScreenIntent = new Intent();
            fullScreenIntent.setClassName(context.getPackageName(), NotificationActivity.class.getName());
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle mBundle = new Bundle();
            mBundle.putParcelable("work", work);
            fullScreenIntent.putExtra("bundle", mBundle);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                    fullScreenIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }
        builder.setChannelId(CHANNEL_ID);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify((int) work.getUid(), builder.build());
    }

    private static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);

            channel.enableLights(true);
            channel.enableVibration(true);

//            //Settings check : get the uri of the ringtone
//            AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
//                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
//                    .build();
            channel.setSound(null, null);

            if (SettingsSharedPreference.getInstance(context).getBoolean("vibrate", false)) {
                channel.setVibrationPattern(Vibrate);
            }

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void directCall(Context context, String number) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));//change the number.
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
        } else {
            dialNumber(context, number);
        }
    }

    public static void dialNumber(Context context, String number) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + number));//change the number.
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }

    // mark work as done
    public static void updateWork(Context context, TodoWork work) {
        if (null != work) {
            Thread thread = new Thread(() -> {
                if (!work.isDone()) {
                    TheDatabase.getInstance(context).todoDao().updateDone(work.getUid(), true);
                }
            });
            thread.start();
        }
    }
    public static void updateWork(Context context, long id) {
        Thread thread = new Thread(() -> TheDatabase.getInstance(context).todoDao().updateDone(id, true));
        thread.start();
    }


    // Get ringtone from sharedPreference
    public static Uri getRingtone(Context context) {
        String str = SettingsSharedPreference.getInstance(context).getString("ringtone_uri",
                String.valueOf(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)));
        Log.d("getRingtone: ", str);
        return Uri.parse(str);
    }

    public static void playSound(Context context, long milli, long interval) {
        Uri uri = getRingtone(context);
        final Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setVolume((float) 1.0);
        }
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();
        ringtone.setAudioAttributes(audioAttributes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(false);
        }

        new CountDownTimer(milli, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                ringtone.play();
            }

            @Override
            public void onFinish() {
                ringtone.stop();
                Log.e("onFinish: ", "finish here");
            }
        }.start();
    }

    public static class SettingsSharedPreference {

        private static SharedPreferences instance;

        public static synchronized SharedPreferences getInstance(Context context) {
            if (instance == null) {
                instance = PreferenceManager.getDefaultSharedPreferences(context);
            }
            return instance;
        }
    }
}
