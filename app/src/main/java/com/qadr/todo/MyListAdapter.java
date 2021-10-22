package com.qadr.todo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.qadr.todo.R.string.already_exist;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.MyViewHolder> {
    private final ArrayList<String> categories;
    private final Context context;
    public static Integer[] _icons = {R.drawable.list_home_work_40,
            R.drawable.list_home_40, R.drawable.list_work_40, R.drawable.list_library_books_40,R.drawable.list_call_40,
            R.drawable.list_flight_40, R.drawable.list_fastfood_40, R.drawable.list_shopping_cart_40};
    public List<Integer> list_icons = new ArrayList<>(Arrays.asList(_icons));
    public ArrayList<String> customize_icons, customize_category;
    private final ArrayList<TodoWork.TaskNumber> taskNumbers;

    public MyListAdapter(ArrayList<String> categories, ArrayList<String> customize_category,
                         Context context, ArrayList<TodoWork.TaskNumber> taskNumbers, ArrayList<String> icons) {
        this.categories = categories;
        this.categories.addAll(1, customize_category);
        this.context = context;
        this.taskNumbers = taskNumbers;
        this.customize_icons = icons;
        this.customize_category = customize_category;
        // TODO: 2/24/2021 reverse this iteration
        Collections.reverse(icons);
        for(String ignored : icons)list_icons.add(1, null);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if(position == categories.size() - 1){
            holder.normalParent.setVisibility(View.GONE);
            holder.customizeParent.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> showDialog());
        }else{
            holder.customizeParent.setVisibility(View.GONE);
            final String text = categories.get(position);
            holder.name.setText(StringUtils.capitalize(text));
            if(existIn(customize_category, text)){
                int pos  = customize_category.indexOf(text);
//                String str = customize_icons.get(pos);
                holder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.customize_item));

            }else {
                holder.image.setImageDrawable(ContextCompat.getDrawable(context, list_icons.get(position)));
            }
            holder.number.setText(getTaskNumbers(categories.get(position)));
            holder.itemView.setOnClickListener(v -> {
                // go to list
                Intent intent = new Intent(this.context, TodoActivity.class);
                intent.putExtra("category", text.trim());
                int drawable = list_icons.get(position) == null ? R.drawable.customize_item : list_icons.get(position);
                intent.putExtra("icon", drawable);
                context.startActivity(intent);
                ((Activity) context).finish();
            });
        }
    }

    public void showDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edittext, null);
        TextInputEditText editText = view.findViewById(R.id.dialog_edit);
        TextInputLayout inputLayout = view.findViewById(R.id.outlinedTextField);
         new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.customize))
                .setView(view)
                 .setCancelable(false)
        .setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
        .setPositiveButton(context.getString(R.string.create), (dialog, which) -> {

            @NonNull String text  = editText.getText().toString().trim();
            if(!text.isEmpty()){
                if(!existIn(categories, text)){
                    categories.add(1, StringUtils.capitalize(text));
                    customize_category.add(0, StringUtils.capitalize(text));
                    customize_icons.add(0, null);
                    list_icons.add(1, 0);
                    notifyItemInserted(1);
                    notifyItemRangeChanged(1, getItemCount());
                    new Thread(() ->{
                        Gson gson = new Gson();

                        SharedPreferences  sharedPreferences = AlarmReceiver.SettingsSharedPreference.getInstance(context);
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
                    inputLayout.setError(context.getString(already_exist));
                }
            }
        })
        .show();
    }

    @Override
    public int getItemCount() { return categories.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        private final ImageView image;
        private final TextView name;
        private final TextView number;
        private final RelativeLayout normalParent, customizeParent;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.list_image);
            name = itemView.findViewById(R.id.list_category);
            number = itemView.findViewById(R.id.list_number);
            normalParent = itemView.findViewById(R.id.normal_parent);
            customizeParent = itemView.findViewById(R.id.customize_parent);
        }
    }

    //get number of tasks in each category
    private String getTaskNumbers(String category) {
        String task = context.getString(R.string.task);
        String pending = context.getString(R.string.pending);
        if(category.equalsIgnoreCase("all")){
            int total = 0;
            for (TodoWork.TaskNumber taskNumber : taskNumbers){
                total += taskNumber.task_number;
            }
            return total + " " + task;
        }else{
            for (TodoWork.TaskNumber taskNumber : taskNumbers) {
                if(taskNumber.category.equalsIgnoreCase(category)){
                    if(taskNumber.task_number == 0){
                        return 0 + " " + task;
                    }
                    return  taskNumber.task_number + " " + task + ", "
                            + taskNumber.getPending_tasks() +  " " + pending;
                }
            }
            return 0 + " " + task;
        }

    }

    public static boolean existIn(List<String> list, String cat){
        for (String c : list){
            if(c.equalsIgnoreCase(cat)){
                return true;
            }
        }
        return false;
    }
}
