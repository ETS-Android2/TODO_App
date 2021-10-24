package com.qadr.todo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import android.os.Bundle;
import android.os.Handler;

import com.qadr.todo.databasefiles.TheDatabase;

import java.util.List;

public class ReportActivity extends AppCompatActivity {
    Handler handler;
    List<TodoWork> allTasks;
    LiveData<List<TodoWork>> listLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        handler = new Handler();
        new Thread(()-> {
            // get all tasks from database
            getTask();
            handler.post(() -> {
               // code after database call to populate the ui
                listLiveData.observe(ReportActivity.this, todoWorks -> {
                    allTasks = todoWorks;

                });
            });
        }).start();
    }

    public void getTask() {
        listLiveData = TheDatabase.getInstance(this).todoDao().getAllLiveData();
    }
}