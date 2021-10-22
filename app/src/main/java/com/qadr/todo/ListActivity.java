package com.qadr.todo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.qadr.todo.databasefiles.TheDatabase;

import java.util.ArrayList;
import java.util.Arrays;

public class ListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyListAdapter listAdapter;
    private ArrayList<String> categories = new ArrayList<>();
    public ArrayList<String> icons, cat_list;
    private ArrayList<TodoWork.TaskNumber> taskNumbers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        initViews();
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
    }

    private void initViews(){
        // TODO: 2/19/2021 retrieve customize categories
        Gson gson = new Gson();
        String jsonText = AlarmReceiver.SettingsSharedPreference.getInstance(this).getString("categories", null);
        String iconText = AlarmReceiver.SettingsSharedPreference.getInstance(this).getString("icons", null);

        cat_list = jsonText  == null ? new ArrayList<>() : gson.fromJson(jsonText, ArrayList.class);
        icons = iconText == null ? new ArrayList<>() : gson.fromJson(iconText, ArrayList.class);
        categories = new ArrayList<>();
        categories.addAll(Arrays.asList(getResources().getStringArray(R.array.categories))) ;

        recyclerView = findViewById(R.id.list_recyclerview);
        recyclerView.setHasFixedSize(true);
        getFromDatabase();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
    }

    //get all task from database
    private void getFromDatabase(){
        final Thread thread = new Thread(() -> {
            taskNumbers = (ArrayList<TodoWork.TaskNumber>) TheDatabase.getInstance(ListActivity.this).todoDao().getTaskNumber();
            ArrayList<TodoWork.TaskNumber> numbers = (ArrayList<TodoWork.TaskNumber>) TheDatabase.getInstance(ListActivity.this).todoDao().getPendingTaskNumber();
            Log.e( "getFromDatabase: ", numbers.toString());
            for (TodoWork.TaskNumber task2 : numbers){
                for (TodoWork.TaskNumber task1 : taskNumbers){
                    if(task1.getCategory().equalsIgnoreCase(task2.getCategory())){
                        task1.setPending_tasks(task2.task_number);
                        break;
                    }
                }
            }
            runOnUiThread(() -> {
                listAdapter = new MyListAdapter(categories, cat_list, ListActivity.this, taskNumbers, icons);
                recyclerView.setAdapter(listAdapter);
            });
        });

        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings){
            startActivity(new Intent(ListActivity.this, SettingsActivity.class).putExtra("from", "list"));
            finish();
        }
        return false;
    }
}