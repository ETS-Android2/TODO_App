package com.qadr.todo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.qadr.todo.databasefiles.TheDatabase;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    Handler handler;
    List<TodoWork> allTasks;
    List<String> categories;
    LiveData<List<TodoWork>> listLiveData;
    ArrayList<PieEntry> pieEntries, pieEntryLabels;
    PieChart pieChart;
    PieData pieData;
    PieDataSet pieDataSet;
    AutoCompleteTextView autoCompleteTextView;
    String currentCat = "all";
    private static int[] ALL_COLORS;

    {
       ALL_COLORS = ArrayUtils.addAll(ColorTemplate.LIBERTY_COLORS, ColorTemplate.JOYFUL_COLORS);
       ALL_COLORS = ArrayUtils.addAll(ALL_COLORS, ColorTemplate.COLORFUL_COLORS);
        ALL_COLORS = ArrayUtils.addAll(ALL_COLORS, ColorTemplate.VORDIPLOM_COLORS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        autoCompleteTextView = findViewById(R.id.autoComplete);
        getCategories();
        pieChart = findViewById(R.id.pie_chart);
        setupChart();

        getTask();

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void getTask() {
        handler = new Handler();
        new Thread(()-> {
            // get all tasks from database
            listLiveData = TheDatabase.getInstance(this).todoDao().getAllLiveData();
            handler.post(() -> {
               // code after database call to populate the ui
                listLiveData.observe(ReportActivity.this, todoWorks -> {
                    allTasks = todoWorks;
                    Log.d("getTask: ", allTasks.toString());
                    setupChartForCategory(currentCat);
                });
            });
        }).start();
    }

    private void setupChart () {
        if (pieChart != null) {
            pieChart.setDrawHoleEnabled(true);
            pieChart.setUsePercentValues(true);
            pieChart.setEntryLabelTextSize(12f);
            pieChart.setEntryLabelColor(Color.BLACK);
            pieChart.setCenterTextSize(24f);
            pieChart.getDescription().setEnabled(false);

            Legend legend = pieChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            legend.setOrientation(Legend.LegendOrientation.VERTICAL);
            legend.setDrawInside(false);
            legend.setEnabled(true);
        }
    }

    private void setPieEntries(Map<String, Float> map) {
        pieChart.setCenterText(StringUtils.capitalize(currentCat));
        if (pieEntries == null) pieEntries = new ArrayList<>();
        else pieEntries.clear();
        for(String cat : map.keySet()) {
            float val = map.get(cat);
            if (val > 0) {
                pieEntries.add(new PieEntry(val, StringUtils.capitalize(cat)));
            }
        }
        pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ALL_COLORS);
        pieData = new PieData(pieDataSet);
        pieData.setDrawValues(true);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(12f);
        pieData.setValueTextColor(Color.BLACK);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    void getCategories () {
        categories = Arrays.asList(getResources().getStringArray(R.array.categories));
        /**
         * this is important
         * */
        categories = AddActivity.removeFromList(categories, 0);
        Gson gson = new Gson();
        String jsonText = AlarmReceiver.SettingsSharedPreference.getInstance(this).getString("categories", null);
        if (jsonText != null && !jsonText.isEmpty()) {
            List list = gson.fromJson(jsonText, ArrayList.class);
            categories.addAll(categories.size() - 1, list);
            Log.d( "getCategories: ", list.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.category_list_item, categories);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            // do the needful here
            currentCat = autoCompleteTextView.getText().toString().toLowerCase();
            setupChartForCategory(currentCat);
        });
    }

    /**
     * change the pie chart when new category is selected
     * @param curCat - the current category
     * */
    private void setupChartForCategory(String curCat) {
        if (curCat.equalsIgnoreCase("all")) {
            Map<String, Float> result = new HashMap<>();
            for (String cat : categories) {
                Iterator<TodoWork> iterator = allTasks.iterator();
                int count = 0;
                while(iterator.hasNext()) {
                    TodoWork work = iterator.next();
                    if (work.getCategory().equalsIgnoreCase(cat)) {
                        count++;
                    }
                }
                float percent = (count * 100) / allTasks.size();
                result.put(cat, percent);
                setPieEntries(result);
            }
            Log.d( "setupChartForCategory: ", result.toString());
        } else {
            Iterator<TodoWork> iterator = allTasks.iterator();
            Map<String, Float> result = new HashMap<>();
            int doneWorks = 0, size = 0;
            while(iterator.hasNext()) {
                TodoWork work = iterator.next();
                if (work.getCategory().equalsIgnoreCase(curCat)) {
                    size++;
                    if (work.isDone()) {
                        doneWorks++;
                    }
                }
            }
            float donePercent = (doneWorks * 100) / size, notDonePercent = ((size - doneWorks) * 100) / size;
            result.put("Work Done", donePercent);
            result.put("Pending", notDonePercent);
           setPieEntries(result);
        }

    }
}