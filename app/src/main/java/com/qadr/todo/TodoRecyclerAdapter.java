package com.qadr.todo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.qadr.todo.databasefiles.TheDatabase;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static com.qadr.todo.MyListAdapter._icons;

public class TodoRecyclerAdapter extends RecyclerView.Adapter<TodoRecyclerAdapter.TheViewHolder> {
    private ArrayList<TodoWork> list;
    private final ArrayList<TodoWork> deletedWork = new ArrayList<>();
    private final Context context;
    private final RecyclerView recyclerView;
    private final List<String> categories;
    private Snackbar snackbar;


    public TodoRecyclerAdapter(ArrayList<TodoWork> list, Context context, RecyclerView recyclerView) {
        this.list = list;
        this.context = context;
        this.recyclerView = recyclerView;
        this.categories =  Arrays.asList(context.getResources().getStringArray(R.array.categories));
    }


    @NonNull
    @Override
    public TodoRecyclerAdapter.TheViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return  new TheViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.todo_item, null));
    }

    @Override
    public void onBindViewHolder(@NonNull TodoRecyclerAdapter.TheViewHolder holder, int position) {
        TodoWork todoWork = list.get(position);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(todoWork.getTimeInMilli());
        holder.positionInList = position;
        holder.todoName.setText(todoWork.getName());
        holder.todoTime.setText(todoWork.getDate().concat("  ".concat(todoWork.getStartingTime())));
        holder.remindMe = todoWork.isRemindMe();
        String note = todoWork.getNote();
        if (note.isEmpty()){
            holder.todoNote.setVisibility(View.GONE);
            holder.noteText.setVisibility(View.GONE);
        }else holder.todoNote.setText(todoWork.getNote());
        holder.categoryIcon.setImageDrawable(ContextCompat.getDrawable(context, getIcon(todoWork.getCategory())));
        //hide mark done button if task has been completed
        if (todoWork.isDone()){
            holder.markBtn.setVisibility(View.GONE);
            holder.markIcon.setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.mark));
            holder.markIcon.setVisibility(View.VISIBLE);
        }
    }

    Integer getIcon (String category) {
        int index = categories.indexOf(StringUtils.capitalize(category));
        return  (index == -1) ? R.drawable.customize_item: _icons[index];
    }

    public void setList(List<TodoWork> list) {
        this.list = (ArrayList<TodoWork>) list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class TheViewHolder extends RecyclerView.ViewHolder {
        private final TextView todoName, todoTime, todoNote, noteText;
        public boolean remindMe;
        private int positionInList;
        private final RelativeLayout overflowRelative;
        private final ImageView overflowIcon, categoryIcon, markIcon;
        private final Button markBtn;

        public TheViewHolder(@NonNull View itemView) {
            super(itemView);
            noteText = itemView.findViewById(R.id.note_text);
            todoName = itemView.findViewById(R.id.todo_name);
            todoTime = itemView.findViewById(R.id.todo_time);
            todoNote = itemView.findViewById(R.id.todo_note);
            overflowRelative = itemView.findViewById(R.id.expandCollapse);
            markIcon = itemView.findViewById(R.id.image_done);
            categoryIcon = itemView.findViewById(R.id.image_icon);
            overflowIcon = itemView.findViewById(R.id.image_overflow);
            overflowIcon.setOnClickListener(this::expandCollapse);
            markBtn = itemView.findViewById(R.id.todo_markBtn);
            Button deleteBtn = itemView.findViewById(R.id.todo_deleteBtn);
            deleteBtn.setOnClickListener( v -> showDeleteSnackBar(positionInList));
            markBtn.setOnClickListener(this::markDone);
            itemView.setOnClickListener(this::goEdit);
        }

        private void goEdit(View view) {
            Intent intent = new Intent(context, AddActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable("work", list.get(positionInList));
            intent.putExtra("bundle", bundle);
            context.startActivity(intent);
        }

        private void expandCollapse(View view) {
            if(overflowRelative.getVisibility() == View.VISIBLE) {
                overflowRelative.setVisibility(View.GONE);
                overflowIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.item_expand_more_24));
            }else {
                overflowRelative.setVisibility(View.VISIBLE);
                overflowIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.item_expand_less_24));
            }
        }


//        private void showPopupMenu(View v) {
//            PopupMenu popupMenu = new PopupMenu(context, v);
//            if (popupMenu.getMenu() instanceof MenuBuilder) {
//                //noinspection RestrictedApi
//                ((MenuBuilder) popupMenu.getMenu()).setOptionalIconsVisible(true);
//            }
//            popupMenu.getMenuInflater().inflate(R.menu.popup_menu_todo, popupMenu.getMenu());
//            popupMenu.setOnMenuItemClickListener(item -> {
//                if(item.getItemId() == R.id.edit_menu){
//                }else if (item.getItemId() == R.id.delete_menu){
//                    showDeleteSnackBar(item.getActionView());
//                }else if (item.getItemId() == R.id.mark_done){
//                    markDone(item.getActionView());
//                }
//                return true;
//            });
//            popupMenu.show();
//        }

        private void markDone(View v) {
            markWorKDone(context, list.get(positionInList).getUid());
        }
    }
    private synchronized void showDeleteSnackBar(int positionInList) {
        TodoWork work = list.get(positionInList);
        list.remove(positionInList);
        notifyItemRemoved(positionInList);
        notifyItemRangeChanged(positionInList, list.size());
        deletedWork.add(work);
        snackbar = Snackbar.make(recyclerView, context.getString(R.string.delete_task), Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> {
                    list.add(positionInList, work);
                    notifyItemInserted(positionInList);
                    notifyItemRangeChanged(positionInList, list.size());
                    deletedWork.remove(work);
                });
        snackbar.addCallback(new Snackbar.Callback(){
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if(event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT){
                    if (!deletedWork.isEmpty()){
                        deleteWork(context, deletedWork);
                    }
                }

            }
        }).show();
    }

    public static String timeText(int oldHour, int oldMinute) {
        String hour = ""+oldHour, min = ""+oldMinute;
        if(hour.length() == 1){ hour = "0" + hour; }
        if (min.length() == 1){min = "0" + min;}
        return hour + ":" + min;
    }


    private void markWorKDone(Context context, long workId){
        final Thread thread = new Thread(() -> TheDatabase.getInstance(context).todoDao().updateDone(workId, true));
        thread.start();
    }
    private void deleteWork(Context context, ArrayList<TodoWork> works){
        final Thread thread = new Thread(() -> TheDatabase.getInstance(context).todoDao().deleteAll(works));

        thread.start();
        for (TodoWork work : works){
            new TheAlarm(work.getTimeInMilli(), work.getUid()).cancel(context);
        }
    }
}
