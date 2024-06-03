package com.example.env_cat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ViewUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.env_cat.util.ViewUtil;

public class LoginMainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_phone;
    private EditText et_password;
    private CheckBox ck_remember;
    private Button btn_forget;
    private ActivityResultLauncher<Intent> register;
    private String mPassword = "123456";
    private Button btn_login;
    private SharedPreferences preferences;
    private Intent mIntent;
    private String TAG = "s-bran";
    private static String action = "/sys/android/service/auth";

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_main);

        // 创建 DatabaseHelper 对象,利用构造方法进行初始化
        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        et_phone = findViewById(R.id.et_phone);
        et_password = findViewById(R.id.et_password);
        btn_forget = findViewById(R.id.btn_forget);
        ck_remember = findViewById(R.id.ck_remember);
        btn_login = findViewById(R.id.btn_login);

        et_phone.addTextChangedListener(new HideTextWatcher(et_phone, 11));
        et_password.addTextChangedListener(new HideTextWatcher(et_password, 6));
        btn_forget.setOnClickListener(this);
        btn_login.setOnClickListener(this);

        register = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Intent intent = result.getData();
                if (intent != null && result.getResultCode() == Activity.RESULT_OK) {
                    // 用户密码已改为新密码，故更新密码变量
                    mPassword = intent.getStringExtra("new_password");
                }
            }
        });

        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        reload();
    }

    private void reload() {
        boolean isRemember = preferences.getBoolean("isRemember", false);
        if (isRemember) {
            String phone = preferences.getString("phone", "");
            et_phone.setText(phone);

            String password = preferences.getString("password", "");
            et_password.setText(password);
            ck_remember.setChecked(true);
        }
    }

    @Override
    public void onClick(View v) {
        String phone = et_phone.getText().toString();
        String password = et_password.getText().toString();
        if (phone.length() != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.btn_forget:
                Intent intent = new Intent(this, LoginForgetActivity.class);
                intent.putExtra("phone", phone);
                register.launch(intent);
                break;
            case R.id.btn_login:
                SQLiteDatabase db = dbHelper.getReadableDatabase(); //create or open a database
                Cursor cursor = db.query("users", new String[]{"user_id", "user_psw"}, "user_id=?", new String[]{phone}, null, null, null, "0,1");
                if (!cursor.isBeforeFirst() || !cursor.moveToFirst()) {
                    Toast.makeText(this, "请输入正确的用户名和密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 游标移动进行校验
                if (cursor.moveToNext()) {
                    String dbPassword = "";
                    try {
                        int columnIndex = cursor.getColumnIndexOrThrow("user_psw");
                        dbPassword = cursor.getString(columnIndex);
//                    String dbPassword = cursor.getString(cursor.getColumnIndex("user_psw"));// 从数据库获取密码进行校验
                        cursor.close();// 关闭游标
                        if (!dbPassword.equals(password)) { //密码校验未通过
                            Toast.makeText(this, "请输入正确的用户名和密码", Toast.LENGTH_SHORT).show();
                        } else {   //密码校验通过
                            // 创建一个意图对象，准备跳到指定的活动页面
                            Intent intent_login2main = new Intent(this, MainActivity.class);
                            // 设置启动标志：跳转到新页面时，栈中的原有实例都被清空，同时开辟新任务的活动栈
                            intent_login2main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent_login2main);
                        }
                    } catch (IllegalArgumentException e) {
                        // 处理列名不在结果中的情况
                        Log.e("DatabaseError", "Column 'user_psw' not found in the query result.", e);
                    }
                    return;
                }
                cursor.close();// 游标校验失败也要关闭

/*
                if (ck_remember.isChecked()) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("phone", et_phone.getText().toString());
                    editor.putString("password", et_password.getText().toString());
                    editor.putBoolean("isRemember", ck_remember.isChecked());
                    editor.commit();
                }
                break;
                */
        }
    }

    private class HideTextWatcher implements TextWatcher {
        private EditText mView;
        private int mMaxLength;

        public HideTextWatcher(EditText v, int maxLength) {
            this.mView = v;
            this.mMaxLength = maxLength;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().length() == mMaxLength)
                ViewUtil.hideOneInputMethod(LoginMainActivity.this, mView);
        }
    }
}