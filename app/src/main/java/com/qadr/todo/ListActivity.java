package com.qadr.todo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.qadr.todo.databasefiles.TheDatabase;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

import static com.qadr.todo.MyListAdapter.existIn;
import static com.qadr.todo.R.string.already_exist;

public class ListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyListAdapter listAdapter;
    private ArrayList<String> categories = new ArrayList<>();
    public ArrayList<String> icons, cat_list;
    private ArrayList<TodoWork.TaskNumber> taskNumbers;
    FloatingActionButton actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        initViews();
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
    }

    private void initViews(){
        actionButton = findViewById(R.id.add_cat);
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
        actionButton.setOnClickListener(v -> {
            showDialog();
            if ((categories.size() + cat_list.size()) > 21) {
                v.setVisibility(View.GONE);
            }
        });
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
                if ((categories.size() + cat_list.size()) > 21) {
                    actionButton.setVisibility(View.GONE);
                }
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
    public void showDialog() {
        if (categories.size() < 21) {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_edittext, null);
            TextInputEditText editText = view.findViewById(R.id.dialog_edit);
            TextInputLayout inputLayout = view.findViewById(R.id.outlinedTextField);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.customize))
                    .setView(view)
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(getString(R.string.create), (dialog, which) -> {

                        @NonNull String text  = editText.getText().toString().trim();
                        if(!text.isEmpty()){
                            if(!existIn(categories, text)){
                                listAdapter.updateData(text);
                                new Thread(() ->{
                                    Gson gson = new Gson();

                                    SharedPreferences sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(this);
                                    String jsonText = sharedPreferences.getString("categories",null);
                                    String jsonIcon =  sharedPreferences.getString("icons", null);
                                    ArrayList<String> textList = (jsonText == null) ? new ArrayList<>(1) : gson.fromJson(jsonText, ArrayList.class);
                                    ArrayList<String> iconList = jsonIcon == null ? new ArrayList<>(1) : gson.fromJson(jsonIcon, ArrayList.class);

                                    textList.add(0, StringUtils.capitalize(text.toLowerCase()));
                                    iconList.add(0, null);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("categories", gson.toJson(textList));
                                    editor.putString("icons", gson.toJson(iconList));
                                    editor.apply();
                                    // convert drawable to bitmap
//                        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.customize_item);
//                        // convert bitmap to string
//                        if (null != bitmap) {
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                            byte[] bytes = baos.toByteArray();
//                            String encodedBitmap = Base64.encodeToString(bytes, Base64.DEFAULT);
//                            iconList.add(encodedBitmap);
//                            try {
//                                baos.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
                                }).start();
                            }else {
                                inputLayout.setError(getString(already_exist));
                            }
                        }
                    })
                    .show();
        } else {
            Toast.makeText(this, "You have reached 21 categories limit", Toast.LENGTH_SHORT).show();
        }
    }
}