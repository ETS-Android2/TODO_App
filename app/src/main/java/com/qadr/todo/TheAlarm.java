package com.qadr.todo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class TheAlarm{
    private final long time;
    private final long id;

    public TheAlarm(long time, long id) {
        this.time = time;
        this.id = id;
    }

    public  void create(Context context){

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("uid", this.id);
            Log.e(" "+this.id, "TheAlarm: ");
            intent.setAction(AlarmReceiver.MY_ALARM_ACTION);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, (int) this.id, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, this.time, alarmIntent);
        }else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, this.time, alarmIntent);
        }
        ComponentName receiver = new ComponentName(context, BootUp.class);
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }


    public void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("uid", this.id);
        intent.setAction(AlarmReceiver.MY_ALARM_ACTION);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, (int) this.id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.e( "cancel: ", "alarm canceled");
        }
    }
}
