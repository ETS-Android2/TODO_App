package com.qadr.todo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.qadr.todo.databasefiles.TheDatabase;

import java.util.ArrayList;

public class BootUp extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("TODO", "onReceive: PHONE BOOTED");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // TODO: 12/26/2020 test this
            Handler handler = new Handler();
            final PendingResult result = goAsync();
            Thread thread = new Thread(() -> {
                final ArrayList<TodoWork> todoList = (ArrayList<TodoWork>) TheDatabase.getInstance(context).todoDao().getAll();
                handler.post(()-> onPostExecute(context, todoList));
                result.finish();
            });
            thread.start();
        }
    }
    private void onPostExecute(Context context, ArrayList<TodoWork> works){
        if(works != null && !works.isEmpty()){
            for (TodoWork work : works){
                if (work.isRemindMe() && !work.isDone()){
                    TheAlarm alarm = new TheAlarm(work.getTimeInMilli(), work.getUid());
                    alarm.create(context);
                }
            }
        }
    }
}
