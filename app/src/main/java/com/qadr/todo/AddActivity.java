package com.qadr.todo;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment;
import com.qadr.todo.databasefiles.TheDatabase;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.qadr.todo.MyListAdapter._icons;

public class AddActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int PERMISSION_CONTACT_CODE = 101, READ_CONTACT_CODE = 100, SETTINGS_CODE = 102;
    private String from = "";
    private EditText dateTime, number;
    private TextInputLayout inputLayout, categoryInput, noteLayout;
    private TextInputEditText name, note;
    private ImageView contactBtn;
    private RelativeLayout numberRelative;
    private AutoCompleteTextView category;
    private MaterialButton createBtn;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());
    private LabeledSwitch labeledSwitch;
    private Date dateSelected = Calendar.getInstance().getTime();
    private LinearLayout parentLayout;
    private TodoWork workToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        Bundle bundle = getIntent().getBundleExtra("bundle");
        if(bundle != null) {
            workToEdit = bundle.getParcelable("work");
        }
        if (getIntent().getExtras() != null){
            from = getIntent().getExtras().getString("category");
        }

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        initViews();


        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    void initViews() {
        inputLayout = findViewById(R.id.planning);
        categoryInput = findViewById(R.id.addCategory);
        name = findViewById(R.id.name);
        dateTime = findViewById(R.id.addTime);
        number = findViewById(R.id.addNumber);
        dateTime.setText(dateFormat.format(Calendar.getInstance().getTime()));
        category = findViewById(R.id.autoComplete);
        numberRelative = findViewById(R.id.numberRelative);
        List<String> categories =  Arrays.asList(getResources().getStringArray(R.array.categories));
        categories = removeFromList(categories, categories.size() - 1);
        Gson gson = new Gson();
        String jsonText = AlarmReceiver.SettingsSharedPreference.getInstance(this).getString("categories", null);
        categories.addAll(1, (jsonText  == null ? new ArrayList<>() : gson.fromJson(jsonText, ArrayList.class)));
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                R.layout.category_list_item, removeFromList(categories, 0));
        category.setText(StringUtils.capitalize(from));
        category.setAdapter(adapter);
        category.setOnItemClickListener((parent, view, position, id) -> {
            String str = category.getText().toString().trim();
            categoryInput.setError(null);
            if(str.equalsIgnoreCase("call")){
                numberRelative.setVisibility(View.VISIBLE);
            }else { numberRelative.setVisibility(View.GONE); }
        });
        if(from != null && from.equalsIgnoreCase("call"))numberRelative.setVisibility(View.VISIBLE);
        note = findViewById(R.id.note);
        noteLayout = findViewById(R.id.addNote);
        createBtn = findViewById(R.id.createBtn);
        dateTime.setOnClickListener(this);
        createBtn.setOnClickListener(this);
        labeledSwitch = findViewById(R.id.labeledSwitch);
        parentLayout = findViewById(R.id.coordinator);
        contactBtn = findViewById(R.id.contactBtn);
        contactBtn.setOnClickListener(this);


        //show details for  Edit work
        //initialize with the values in the work model
        if (workToEdit != null){
            category.setText(StringUtils.capitalize(workToEdit.getCategory()));
            note.setText(workToEdit.getNote());
            number.setText(workToEdit.getNumber());
            labeledSwitch.setOn(workToEdit.isRemindMe());
            name.setText(workToEdit.getName());
            dateTime.setText(workToEdit.getDate().concat("  ".concat(workToEdit.getStartingTime())));
            Calendar calendar =  Calendar.getInstance();
            calendar.setTimeInMillis(workToEdit.getTimeInMilli());
            dateSelected = calendar.getTime();
            createBtn.setText(R.string.update);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == dateTime.getId()){
            showDateTimePicker();
        }else if (v.getId() == createBtn.getId()){
            if (workToEdit != null)updateWork();
            else addWork();
        }else if(v.getId() == contactBtn.getId()){
            checkContactPermission();
        }
    }

    private void updateWork() {
        workToEdit.setName(name.getText().toString());
        workToEdit.setNote(note.getText().toString());
        workToEdit.setNumber(number.getText().toString());
        workToEdit.setRemindMe(labeledSwitch.isOn());
        String date = dateTime.getText().toString().trim();
        String[] arr = date.split(" {2}");
        workToEdit.setDate(arr[0]);
        workToEdit.setStartingTime(arr[1]);

        // create alarm if user wants to be reminded
        if(workToEdit.isRemindMe()){
            new TheAlarm(dateSelected.getTime(), workToEdit.getUid()).create(getApplicationContext());
        }
        Thread thread = new Thread(() -> TheDatabase.getInstance(getApplicationContext()).todoDao().update(workToEdit));
        thread.start();
        goBack();
    }

    private void addWork() {
        String taskName = name.getText().toString().trim();
        String cat = category.getText().toString().trim().toLowerCase();
        String dateStr, timeStr, date = dateTime.getText().toString().trim();
        String num = number.getText().toString().trim();
        String[] arr = date.split(" {2}");
        dateStr = arr[0];
        timeStr = arr[1];
        boolean isOk = true;
        if (taskName.isEmpty()){
            isOk = false;
            //show warnings
            inputLayout.setError("What are you planning?");
        }
        if (dateStr.isEmpty()){
            isOk = false;
            //show warnings
            dateTime.setCompoundDrawables(ContextCompat.getDrawable(this, R.drawable.add_calendar_today_24_error), null, null, null);
        }
        if (cat.isEmpty()){
            isOk = false;
            //show warnings
            categoryInput.setError("Select category");
        }

        if(cat.equalsIgnoreCase("call") && num.isEmpty()){
            isOk =  false;
            Drawable img = ContextCompat.getDrawable(this, R.drawable.add_local_phone_24_error);
            number.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        }

        if(isOk){
            createBtn.setClickable(false);
            TodoWork work = new TodoWork(taskName, dateStr, timeStr, dateSelected.getTime(),
                    false, labeledSwitch.isOn(), cat.toLowerCase());
            work.setNote(note.getText().toString().trim());
            if (cat.equalsIgnoreCase("call"))work.setNumber(num);

            addWorkToDatabase(work);

        }
    }

    private void goBack() {
        String cat = workToEdit != null ? workToEdit.getCategory() : from;
        Intent intent = new Intent(this, TodoActivity.class);
        intent.putExtra("category", cat);
        List<String> categories = Arrays.asList(getResources().getStringArray(R.array.categories));
        intent.putExtra("icon", _icons[categories.indexOf(StringUtils.capitalize(cat))]);
        startActivity(intent);
        finish();
    }

    private void showDateTimePicker() {
        SwitchDateTimeDialogFragment dateTimeDialogFragment = SwitchDateTimeDialogFragment.newInstance(
                "Select Date And Time",
                "OK",
                "Cancel"
        );

        dateTimeDialogFragment.set24HoursMode(true);
        dateTimeDialogFragment.setHighlightAMPMSelection(true);

        // Set listener
        dateTimeDialogFragment.setOnButtonClickListener(new SwitchDateTimeDialogFragment.OnButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Date date) {
                // Date is get on positive button click
                // Do something
                dateTime.setText(dateFormat.format(date));
                dateSelected = date;
            }

            @Override
            public void onNegativeButtonClick(Date date) {
                // Date is get on negative button click
            }
        });

// Show
        dateTimeDialogFragment.show(getSupportFragmentManager(), "dialog_time");
    }

    private void addWorkToDatabase(TodoWork todoWork){
        Thread thread = new Thread(() -> {
            final long id =  TheDatabase.getInstance(getApplicationContext()).todoDao().add(todoWork);
                // create alarm if user wants to be reminded
                if(todoWork.isRemindMe()){
                    Log.e(" "+id, "addWorkToDatabase: ");
                    todoWork.setUid(id);
                    new TheAlarm(todoWork.getTimeInMilli(), todoWork.getUid()).create(getApplicationContext());

                }
                goBack();
        });
        thread.start();
    }


    private void checkContactPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            selectContact();
        }else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)){
                showSnackBar();
            }else{
                contactRequestPermission.launch(Manifest.permission.READ_CONTACTS);
            }
        }
    }

    private void showSnackBar() {
        Snackbar.make(parentLayout, R.string.contact_reason, Snackbar.LENGTH_LONG)
                .setAction(R.string.grant_permission, v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    appSettingsIntent.launch(intent);
                }).show();
    }

    public void selectContact() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            contactSelectIntent.launch(intent);
        }else{
            Toast.makeText(this, "No app to handle this action", Toast.LENGTH_SHORT).show();
            Log.i("selectContact: ", "No app to handle this read contact intent");
        }
    }

    public static <T> List<T> removeFromList(List<T> list, int index){
        List<T> newList = new ArrayList<>();
        int count = 0;
        for (T s : list){
            if(index != count){
                newList.add(s);
            }
            count++;
        }
        return newList;
    }

    ActivityResultLauncher<String> contactRequestPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result) {
                    selectContact();
                } else {
                    showSnackBar();
                }
            }
    );

    ActivityResultLauncher<Intent> contactSelectIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri contactUri = data.getData();
                        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                        Cursor cursor = getContentResolver().query(contactUri, projection,
                                null, null, null);
                        // If the cursor returned is valid, get the phone number
                        if (cursor != null && cursor.moveToFirst()) {
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String num = cursor.getString(numberIndex);
                            number.setText(num);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                }
            }
    );

    ActivityResultLauncher<Intent> appSettingsIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkContactPermission()
    );
}