package com.qadr.todo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qadr.todo.databasefiles.TheDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TodoActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, AppBarRecyclerAdapter.OnDateClicked {
    private TextView noTaskTextView;
    private RecyclerView today_recyclerView;
    private RecyclerView others_recyclerview;
    private LiveData<List<TodoWork>> todoListLive;
    private ArrayList<TodoWork> todayWorks, othersWorks, allList;
    private TodoRecyclerAdapter todayAdapter, othersAdapter;
    private RelativeLayout todayRelative, othersRelative;
    private SearchView searchView;
    private String category;
    private int icon, todayPositionInAppBar;
    private ImageView iconView;
    private String noText;
    private List<String> categories;
    private final SimpleDateFormat dateTitleFormat = new SimpleDateFormat("EEEE dd, MMM", Locale.getDefault());
    private final ArrayList<AppBarRecyclerAdapter.ADate> dateArrayList = new ArrayList<>();
    private boolean isAllBtn = false;
    private AppBarRecyclerAdapter appBarRecyclerAdapter;
    private Button allBtn;
    private long dateSelectedMilli = MaterialDatePicker.todayInUtcMilliseconds();
    private MaterialDatePicker.Builder<Long> builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_todo);
        new Thread(() -> {
            createDateNavigation();
            runOnUiThread(() -> {
                // initialize the date navigation
                RecyclerView appBarRecycler = findViewById(R.id.appbarRecyclerview);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(TodoActivity.this);
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                appBarRecycler.setLayoutManager(linearLayoutManager);
                appBarRecyclerAdapter = new AppBarRecyclerAdapter(TodoActivity.this, dateArrayList, appBarRecycler, todayPositionInAppBar);
                appBarRecycler.addItemDecoration(new ItemSpaceDecoration(0, 10));
                appBarRecycler.setAdapter(appBarRecyclerAdapter);
                appBarRecycler.scrollToPosition(todayPositionInAppBar);
                // end of date navigation
            });

        }).start();
        categories = Arrays.asList(getResources().getStringArray(R.array.categories));
        if(getIntent().getExtras() != null){
            category = getIntent().getExtras().getString("category", categories.get(0));
            icon = getIntent().getIntExtra("icon", MyListAdapter._icons[0]);
        }
        noText = category.equalsIgnoreCase("all") ? getResources().getString(R.string.no_task) :
                "You have no task in your " + category + " list";


        initViews();
    }


    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        allBtn = findViewById(R.id.all_btn);
        ImageView calendarIcon = findViewById(R.id.calendarIcon);
        calendarIcon.setOnClickListener(this::showCalendar);
        allBtn.setOnClickListener(v -> {
            if(!isAllBtn) {
                showAll();
            }
        });


        FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionBtn);
        noTaskTextView = findViewById(R.id.textView);
        todayRelative = findViewById(R.id.today_relative);
        othersRelative = findViewById(R.id.others_relative);
        today_recyclerView = findViewById(R.id.today_recylerview);
        others_recyclerview = findViewById(R.id.others_recylerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(TodoActivity.this);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(TodoActivity.this);
        today_recyclerView.setLayoutManager(layoutManager);
        today_recyclerView.addItemDecoration(new ItemSpaceDecoration(10, 30));
        others_recyclerview.setLayoutManager(layoutManager1);
        others_recyclerview.addItemDecoration(new ItemSpaceDecoration(10, 30));
        iconView = findViewById(R.id.icon);
        iconView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), icon));
        floatingActionButton.setOnClickListener(v -> startActivity(new Intent(TodoActivity.this, AddActivity.class).putExtra("category", category)));
        toolbar.setTitle(dateTitleFormat.format(Calendar.getInstance().getTime()));
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

        builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText(R.string.select_date);
    }

    private void createDateNavigation() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date todayDate = calendar.getTime();
        int thisMonth = calendar.get(Calendar.MONTH);
        int nextMonth = (thisMonth + 1) % 12;
        int previousMonth = (thisMonth - 1);
        previousMonth = (previousMonth < 0)? 12 + previousMonth : previousMonth;

        // add today date to the list first
        dateArrayList.add(new AppBarRecyclerAdapter.ADate(todayDate));
        // add a month before
        while ((calendar.get(Calendar.MONTH)) == thisMonth || calendar.get(Calendar.MONTH) == previousMonth) {
            calendar.add(Calendar.DATE, -1);
            if (calendar.get(Calendar.MONTH) == thisMonth || calendar.get(Calendar.MONTH) == previousMonth) {
                dateArrayList.add(0, new AppBarRecyclerAdapter.ADate(calendar.getTime()));
            }
        }

        todayPositionInAppBar = dateArrayList.size() - 1;
        calendar.clear();
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(todayDate.getTime());

        // add a month after today to the list
        while (calendar.get(Calendar.MONTH) == thisMonth || calendar.get(Calendar.MONTH) == nextMonth) {
            calendar.add(Calendar.DATE, 1);
            if (calendar.get(Calendar.MONTH) == thisMonth || calendar.get(Calendar.MONTH) == nextMonth) {
                dateArrayList.add(new AppBarRecyclerAdapter.ADate(calendar.getTime()));
            }
        }
    }

    public void showCalendar(View v) {
        builder.setSelection(dateSelectedMilli);
        final MaterialDatePicker<Long> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            calendar.setTimeInMillis(selection);
            appBarRecyclerAdapter.checkDate(calendar.getTime());
            filterListByDate(calendar.getTime());
            dateSelectedMilli = selection;
//            Toast.makeText(this, calendar.toString(), Toast.LENGTH_SHORT).show();
        });
        datePicker.show(getSupportFragmentManager(), "datePicker");
    }

    private void filterListByDate(Date time) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(time);
        dateSelectedMilli = cal.getTimeInMillis();
        ArrayList<TodoWork> newList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        if (allList != null && !allList.isEmpty()) {
            for (TodoWork work : allList) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.clear();
                calendar.setTimeInMillis(work.getTimeInMilli());
                if (dateFormat.format(calendar.getTime()).equals(dateFormat.format(time))) {
                    newList.add(work);
                }
            }
            todayRelative.setVisibility(View.GONE);
            isAllBtn = false;
            if (!newList.isEmpty()) {
                noTaskTextView.setVisibility(View.GONE);
                if (othersRelative.getVisibility() == View.GONE) {
                    othersRelative.setVisibility(View.VISIBLE);
                    othersAdapter = new TodoRecyclerAdapter(newList, TodoActivity.this, others_recyclerview);
                    others_recyclerview.setAdapter(othersAdapter);
                } else {
                    othersAdapter.setList(newList);
                }
            } else {
                othersRelative.setVisibility(View.GONE);
                noTaskTextView.setVisibility(View.VISIBLE);
                noTaskTextView.setText(R.string.no_task_day);
            }
        }
        allBtn.setBackgroundColor(getResources().getColor(R.color.Transparent));
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            startActivity(new Intent(TodoActivity.this, ListActivity.class));
            finish();
        }
    }

    private void showAll(){
        if(!allList.isEmpty()){
            dateSelectedMilli = 0;
            separateWorks(allList);
            isAllBtn = true;
            appBarRecyclerAdapter.changeActiveBackground(-1);
            allBtn.setBackgroundColor(getResources().getColor(R.color.white));
            if (noTaskTextView.getVisibility() == View.VISIBLE) {
                noTaskTextView.setVisibility(View.GONE);
                noTaskTextView.setText(R.string.no_task);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bar_menu, menu);
        searchView = (SearchView) menu.findItem(R.id.toolbar_search).getActionView();
        searchView.setQueryHint("Search");
        searchView.setIconifiedByDefault(true);
        searchView.setOnCloseListener(() -> {
            if (searchView.getQuery().toString().isEmpty()) {
                showAll();
            }
            searchView.onActionViewCollapsed();
            return false;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ArrayList<TodoWork> newWorks = new ArrayList<>();
                if (!newText.isEmpty() && !allList.isEmpty()) {
                    for (TodoWork work : allList) {
                        if (work.getName().contains(newText) || work.getDate().contains(newText) || work.getStartingTime().contains(newText)) {
                            newWorks.add(work);
//                                        Log.e("onQueryTextChange: ", work.toString());
                        }
                    }
                    dateSelectedMilli = 0;
                    appBarRecyclerAdapter.changeActiveBackground(-1);
                    isAllBtn = false;
                    allBtn.setBackgroundColor(getResources().getColor(R.color.Transparent));
                    if (!newWorks.isEmpty()) {
                        todayRelative.setVisibility(View.GONE);
                        othersRelative.setVisibility(View.VISIBLE);
                        noTaskTextView.setVisibility(View.GONE);
                        if (othersAdapter != null) {
                            othersAdapter.setList(newWorks);
                        } else {
                            othersAdapter = new TodoRecyclerAdapter(newWorks, TodoActivity.this, others_recyclerview);
                            others_recyclerview.setAdapter(othersAdapter);
                        }
                    } else {
                        todayRelative.setVisibility(View.GONE);
                        othersRelative.setVisibility(View.GONE);
                        noTaskTextView.setText(R.string.no_result);
                        noTaskTextView.setVisibility(View.VISIBLE);
                    }
                }

                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.toolbar_search) {
            searchView.onActionViewExpanded();
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(TodoActivity.this, SettingsActivity.class).putExtra("from", "todo"));
        }
        return false;
    }

    @Override
    protected void onResume() {
        getTasksFromDb(category);
        super.onResume();
    }

    private void getTasksFromDb(String category) {
        Thread thread = new Thread(() -> {
            if (category.equalsIgnoreCase("all")) {
                todoListLive = TheDatabase.getInstance(getApplicationContext()).todoDao().getAllLiveData();
            } else if (category.equalsIgnoreCase("others")) {
                todoListLive = TheDatabase.getInstance(getApplicationContext()).todoDao().getAllLiveData();
            } else {
                todoListLive = TheDatabase.getInstance(getApplicationContext()).todoDao().getTasksByCategory(category);
            }
            runOnUiThread(() -> todoListLive.observe(TodoActivity.this, todoWorks -> {
                if (category.equalsIgnoreCase("others")) {
                    allList = new ArrayList<>();
                    for (TodoWork work : todoWorks) {
                        if (work.getCategory().equalsIgnoreCase("others") ||
                                !MyListAdapter.existIn(categories, work.getCategory())) {
                            allList.add(work);
                        }
                    }
                } else {
                    allList = (ArrayList<TodoWork>) todoWorks;
                }
                if (!allList.isEmpty()) {
                    noTaskTextView.setVisibility(View.GONE);
                    iconView.setVisibility(View.GONE);
                    separateWorks(allList);
                } else {
                    todayRelative.setVisibility(View.GONE);
                    othersRelative.setVisibility(View.GONE);
                    noTaskTextView.setVisibility(View.VISIBLE);
                    noTaskTextView.setText(noText);
                    iconView.setVisibility(View.VISIBLE);
                }
            }));
        });
        thread.start();
    }

    protected void separateWorks(ArrayList<TodoWork> works) {
        if (works != null && !works.isEmpty()) {
            todayWorks = new ArrayList<>();
            othersWorks = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            String todayString = dateFormat.format(calendar.getTime());
            for (TodoWork work : works) {
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTimeInMillis(work.getTimeInMilli());
                String str = dateFormat.format(calendar1.getTime());
                if (str.equals(todayString)) {
                    todayWorks.add(work);
                } else {
                    othersWorks.add(work);
                }
            }

            checkLists();
        }

    }

    protected void checkLists() {
        //check if there is any task today
        if (!todayWorks.isEmpty()) {
            if (todayRelative.getVisibility() == View.GONE) {
                todayRelative.setVisibility(View.VISIBLE);
                todayAdapter = new TodoRecyclerAdapter(todayWorks, TodoActivity.this, today_recyclerView);
                today_recyclerView.setAdapter(todayAdapter);
            } else {
                todayAdapter.setList(todayWorks);
            }
        } else todayRelative.setVisibility(View.GONE);

        //check others
        if (!othersWorks.isEmpty()) {
            if (othersRelative.getVisibility() == View.GONE) {
                othersRelative.setVisibility(View.VISIBLE);
                othersAdapter = new TodoRecyclerAdapter(othersWorks, TodoActivity.this, others_recyclerview);
                others_recyclerview.setAdapter(othersAdapter);
            } else {
                othersAdapter.setList(othersWorks);
            }

        } else othersRelative.setVisibility(View.GONE);

        if (dateSelectedMilli > 0) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(dateSelectedMilli);
            filterListByDate(calendar.getTime());
        }
    }

    @Override
    public void onFilterDate(Date date) {
        filterListByDate(date);
    }
}