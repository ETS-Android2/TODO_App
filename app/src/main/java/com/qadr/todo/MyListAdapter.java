package com.qadr.todo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.MyViewHolder> {
    private final ArrayList<String> categories;
    private final Context context;
    public static Integer[] _icons = {R.drawable.list_home_work_40,
            R.drawable.list_home_40, R.drawable.list_work_40, R.drawable.list_library_books_40,R.drawable.list_call_40,
            R.drawable.list_flight_40, R.drawable.list_fastfood_40, R.drawable.list_shopping_cart_40, R.drawable.ic_baseline_menu_40};
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
        final String text = categories.get(position);
        holder.name.setText(StringUtils.capitalize(text));
        if(existIn(customize_category, text)){
            int pos  = customize_category.indexOf(text);
//                String str = customize_icons.get(pos);
            holder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.customize_item));

        }else {
            Log.e( "onBindViewHolder: ", String.valueOf(position));
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

    void updateData(String text){
        categories.add(1, StringUtils.capitalize(text));
        customize_category.add(0, StringUtils.capitalize(text));
        customize_icons.add(0, null);
        list_icons.add(1, 0);
        notifyItemInserted(1);
        notifyItemRangeChanged(1, getItemCount());
    }



    @Override
    public int getItemCount() { return categories.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        private final ImageView image;
        private final TextView name;
        private final TextView number;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.list_image);
            name = itemView.findViewById(R.id.list_category);
            number = itemView.findViewById(R.id.list_number);
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
