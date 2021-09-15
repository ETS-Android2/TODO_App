package com.qadr.todo.databasefiles;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.qadr.todo.TodoWork;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface TodoDao {

    @Query("SELECT * FROM todo_works")
    LiveData<List<TodoWork>> getAllLiveData();

    @Query("SELECT * FROM todo_works")
    List<TodoWork> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long add(TodoWork todoWork);

    @Delete
    void deleteAll(ArrayList<TodoWork> todoWorks);

    @Update
    void update(TodoWork todoWork);

    @Query("select count(uid) as task_number, category from todo_works group by category")
    List<TodoWork.TaskNumber> getTaskNumber();

    @Query("select count(uid) as task_number, category from todo_works where isDone=0 group by category")
    List<TodoWork.TaskNumber> getPendingTaskNumber();

    @Query("select * from todo_works WHERE uid=:id")
    TodoWork getWork(long id);

    @Query("UPDATE todo_works SET isDone = :done WHERE uid=:id")
    void updateDone(long id, boolean done);

    @Query("SELECT * FROM todo_works WHERE category LIKE :category")
    LiveData<List<TodoWork>> getTasksByCategory(String category);

    @Query("delete from todo_works")
    void clearAll();

    @Query("update todo_works set remindMe='false' where uid=:id")
    void updateRemindMe(int id);
}
