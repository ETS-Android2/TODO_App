package com.qadr.todo;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class NotificationActivity extends AppCompatActivity {
    private TodoWork todoWork;
    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        Bundle bundle = getIntent().getBundleExtra("bundle");
        if(bundle != null){
            todoWork = bundle.getParcelable("work");
            category = todoWork.getCategory();
        }
        ExtendedFloatingActionButton cancelBtn = findViewById(R.id.close_btn);
        ExtendedFloatingActionButton actionButton = findViewById(R.id.action_fab);
        if(category.equalsIgnoreCase("call")){
            actionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.notification_call_24));
            actionButton.setText(R.string.call_number);
            actionButton.setOnClickListener(v -> call());
        }else {
            actionButton.setIcon(ContextCompat.getDrawable(this, R.drawable.notification_done_outline_24));
            actionButton.setText(R.string.mark_as_done);
            actionButton.setOnClickListener(v -> markDone());
        }
        cancelBtn.setOnClickListener(v -> finish());
        TextView name, date , time, note;
        name = findViewById(R.id.name);
        date = findViewById(R.id.date);
        time = findViewById(R.id.time);
        note = findViewById(R.id.note);
        name.setText(todoWork.getName());
        date.setText(todoWork.getDate());
        note.setText(todoWork.getNote());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(todoWork.getTimeInMilli());
        time.setText(TodoRecyclerAdapter.timeText(calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE)));
    }


    private void call() {
        if(AlarmReceiver.SettingsSharedPreference.getInstance(NotificationActivity.this).getBoolean("call", false)){
            AlarmReceiver.directCall(NotificationActivity.this, todoWork.getNumber());
        }else{
            AlarmReceiver.dialNumber(NotificationActivity.this, todoWork.getNumber());
        }
        finish();
    }

    private void markDone() {
        AlarmReceiver.updateWork(NotificationActivity.this, todoWork);
        finish();
    }
}