package com.example.env_cat.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.env_cat.dao.UserDao;
import com.example.env_cat.enity.UserInfo;

@Database(entities = {UserInfo.class}, version = 1, exportSchema = true)
public abstract class UserDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
