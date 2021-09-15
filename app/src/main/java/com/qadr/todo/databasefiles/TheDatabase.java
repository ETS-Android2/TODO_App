package com.qadr.todo.databasefiles;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.qadr.todo.TodoWork;

@Database(entities = {TodoWork.class}, version = 1)
public abstract class TheDatabase extends RoomDatabase {

    private static TheDatabase instance;
    public abstract TodoDao todoDao();

    public static synchronized TheDatabase getInstance(Context context) {
        if (null == instance){
            instance = Room.databaseBuilder(context,
                    TheDatabase.class, "call_scheduler")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
