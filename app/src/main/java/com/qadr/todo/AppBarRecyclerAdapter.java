package com.qadr.todo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AppBarRecyclerAdapter extends RecyclerView.Adapter<AppBarRecyclerAdapter.DateViewHolder> {
    private final Context context;
    private final ArrayList<ADate> dates;
    private final OnDateClicked onDateClicked;
    private final RecyclerView recyclerView;
    private int currentItemsPosition;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM dd yyyy", Locale.getDefault());

    public AppBarRecyclerAdapter(Context context, ArrayList<ADate> dates, RecyclerView recyclerView, int position){
         this.context = context;
         onDateClicked = (OnDateClicked) context;
         this.dates = dates;
         currentItemsPosition = position;
         this.recyclerView = recyclerView;
     }

     public int getCurrentItemsPosition(){ return currentItemsPosition; }

     public  void setCurrentItemsPosition(int position) {
        this.currentItemsPosition = position;
     }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DateViewHolder(LayoutInflater.from(context).inflate(R.layout.appbar_item, null));
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        ADate aDate = dates.get(position);
        holder.dateBtn.setText(aDate.getDayNumber());
        holder.weekDay.setText(aDate.getDayOfTheWeek());
        holder.aDate = aDate;
        holder.position = position;
        if (getCurrentItemsPosition() == position) {
            holder.isClicked = true;
        }
        if(dateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()).equals(dateFormat.format(aDate.getDate()))){
            holder.dateBtn.setText(context.getString(R.string.today_text));
        }

        if (holder.isClicked && getCurrentItemsPosition() == position){
            holder.dateBtn.setBackgroundColor(context.getResources().getColor(R.color.white));
        }else{
            holder.isClicked = false;
            holder.dateBtn.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public class DateViewHolder extends RecyclerView.ViewHolder{
        private final Button dateBtn;
        private final TextView weekDay;
        private ADate aDate;
        private int position;
        private boolean isClicked = false;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            dateBtn = itemView.findViewById(R.id.dayNumber);
            weekDay = itemView.findViewById(R.id.weekDay);
            dateBtn.setOnClickListener(v -> {
                if (!isClicked) {
                    changeActiveBackground(position);
                    onDateClicked.onFilterDate(aDate.getDate());
                    isClicked = true;
                }
            });
        }
    }

    public void changeActiveBackground(int position){
        setCurrentItemsPosition(position);
        notifyDataSetChanged();
    }

    public void checkDate(Date date){
        int count = 0;
        for (ADate aDate : dates) {
            if(dateFormat.format(aDate.getDate()).equals(dateFormat.format(date))){
                changeActiveBackground(count);
                recyclerView.scrollToPosition(count);
                return;
            }
            changeActiveBackground(-1);
            count++;
        }
    }

    public static class ADate{
        final SimpleDateFormat dateFormat = new SimpleDateFormat("E dd", Locale.getDefault());
        private final Date date;
        private final String dayNumber, dayOfTheWeek;

        public ADate(Date date){
            this.date = date;
            String[] dateStr = dateFormat.format(date).split(" ");
            dayOfTheWeek = dateStr[0];
            dayNumber = dateStr[1];
        }

        public String getDayNumber() {
            return dayNumber;
        }

        public String getDayOfTheWeek() {
            return dayOfTheWeek;
        }

        public Date getDate() {
            return date;
        }
    }

    public interface OnDateClicked{
        void onFilterDate(Date date);
    }
}
