package com.example.env_cat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.env_cat.enity.UserInfo;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    void insert(UserInfo... user);

    @Delete
    void delete(UserInfo... user);

    @Query("DELETE FROM UserInfo")
    void deleteAll();

    @Update
    int update(UserInfo... user);

    @Query("SELECT * FROM UserInfo")
    List<UserInfo> queryAll();

    // 根据电话号码
    @Query("SELECT * FROM UserInfo WHERE telephone = :telephone ORDER BY uid DESC limit 1")
    UserInfo queryByName(String telephone);
}
