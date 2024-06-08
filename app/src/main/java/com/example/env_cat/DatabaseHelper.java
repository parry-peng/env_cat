package com.example.env_cat;

/*
 *  数据库帮助类,用于数据库的创建和升级,使用SQLite数据库
 *  数据库名: UserManager.db
 *  表名: users
 *  用户账号: user_id
 *  账号密码: user_psw
 *  用户角色: user_name (管理员or普通用户)
 * */

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    // 数据库版本
    private static final int DATABASE_VERSION = 1;

    // 数据库名称
    private static final String DATABASE_NAME = "UserManager.db";

    // 用户表名
    private static final String TABLE_USERS = "users";

    // 用户表列名
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_USER_PSW = "user_psw";

    // 创建用户表的SQL语句
    private static final String CREATE_TABLE_USERS = "CREATE TABLE "
            + TABLE_USERS + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_NAME + " TEXT,"
            + COLUMN_USER_PSW + " TEXT" + ")";

    // 调用父类的构造方法(便于之后进行初始化赋值)
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("sqlite_____", "create Database");     // 创建数据库
        db.execSQL(CREATE_TABLE_USERS);
        Log.i("sqlite_____", "Finished Database");// 完成数据库的创建

        //插入初始数据
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, "13110119120");
        values.put(COLUMN_USER_NAME, "Admin");
        values.put(COLUMN_USER_PSW, "123456");
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单起见，我们只是删除旧表并重新创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
