package com.example.env_cat;

import android.app.Application;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.example.env_cat.database.UserDBHelper;

import java.util.HashMap;

public class MyApplication extends Application {
    private static MyApplication mApp;
    // 声明一个公共的信息映射对象，可当作全局变量使用
    public HashMap<String, String> infoMap = new HashMap<>();
    private UserDBHelper userDBHelper;
    public static MyApplication getInstance() {
        return mApp;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        userDBHelper = UserDBHelper.getInstance(this);
        userDBHelper.openReadLink();
        userDBHelper.openWriteLink();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public UserDBHelper getUserDB() {
        return userDBHelper;
    }
}
