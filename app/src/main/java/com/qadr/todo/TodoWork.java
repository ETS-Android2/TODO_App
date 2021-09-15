package com.qadr.todo;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "todo_works")
public class TodoWork implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    @ColumnInfo
    private String name;
    @ColumnInfo
    private String date;
    @ColumnInfo
    private String startingTime;
    @ColumnInfo
    private long timeInMilli;
    @ColumnInfo
    private boolean isDone;
    @ColumnInfo
    private boolean remindMe;
    @ColumnInfo
    private String category;

    @ColumnInfo
    private String note="";

    @ColumnInfo
    private String number = "";


    public String getNumber(){ return number; }
    public void setNumber(String no){number = no;}

    public String getCategory() {
        return category;
    }



    public boolean isRemindMe() {
        return remindMe;
    }

    public void setRemindMe( boolean remindMe) {
        this.remindMe = remindMe;
    }

    public TodoWork(String name, String date, String startingTime, long timeInMilli, boolean isDone, boolean remindMe, String category){
       this.name =  name;
       this.date = date;
       this.startingTime = startingTime;
       this.timeInMilli = timeInMilli;
       this.isDone = isDone;
       this.remindMe = remindMe;
        this.category = category;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public String getStartingTime() {
        return startingTime;
    }


    public long getTimeInMilli() {
        return timeInMilli;
    }


    @Override
    public String toString() {
        return "TodoWork{" +
                "uid=" + uid +
                ", name='" + name + '\'' +
                ", date='" + date + '\'' +
                ", startingTime='" + startingTime + '\'' +
                ", timeInMilli=" + timeInMilli +
                ", isDone=" + isDone +
                ", remindMe=" + remindMe +
                '}';
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
    public void setDate(String s) {
        date = s;
    }

    public void setStartingTime(String s) {
        startingTime = s;
    }
    // parcelable implementations follows

    @Ignore
    public TodoWork(Parcel source) {
        this.uid = source.readLong();
        this.name = source.readString();
        this.date = source.readString();
        this.startingTime = source.readString();
        this.timeInMilli = source.readLong();
        this.isDone = source.readInt() == 1;
        this.remindMe = source.readInt() == 1;
        this.category = source.readString();
        this.number = source.readString();
        this.note = source.readString();
    }
    @Ignore
    @Override
    public int describeContents() {
        return 0;
    }

    @Ignore
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.uid);
        dest.writeString(this.name);
        dest.writeString(this.date);
        dest.writeString(this.startingTime);
        dest.writeLong(this.timeInMilli);
        dest.writeInt(this.isDone? 1 : 0);
        dest.writeInt(this.remindMe ? 1 : 0);
        dest.writeString(this.category);
        dest.writeString(this.number);
        dest.writeString(this.note);
    }

    @Ignore
    public static final Creator<TodoWork> CREATOR = new Creator<TodoWork>() {
        @Override
        public TodoWork createFromParcel(Parcel source) {
            return new TodoWork(source);
        }

        @Override
        public TodoWork[] newArray(int size) {
            return new TodoWork[size];
        }
    };



    public static class TaskNumber {
        public int task_number;

        @ColumnInfo
        public String category;

       @Ignore
        private int pending_tasks;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public int getPending_tasks() {
            return pending_tasks;
        }

        public void setPending_tasks(int pending_tasks) {
            this.pending_tasks = pending_tasks;
        }
    }
}
