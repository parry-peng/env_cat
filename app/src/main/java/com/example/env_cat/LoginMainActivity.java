package com.example.env_cat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ViewUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.env_cat.util.ViewUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

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
    private String TAG = "Login-Panel";
    private static String action = "com.Login.ACTION_RECEIVE";//用于IntentFilter识别
    private static final String ACTION_PUBLISH = "com.Login.ACTION_PUBLISH";
    String ReceiverStr = null;//接收MQTT消息的变量

    DatabaseHelper dbHelper;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_main);

        /* 启动MQTT服务 */
        Intent intent = new Intent(getApplicationContext(), MqttManager.class);
        startService(intent);

        /*************************通过广播接收的方式从MqttService中接收数据*************************/
        IntentFilter filter = new IntentFilter(LoginMainActivity.action);
        getApplicationContext().registerReceiver(bc_Receiver, filter, Context.RECEIVER_EXPORTED);
        Log.d(TAG, "MainActivity start");
        /*************************通过广播接收的方式从MqttService中接收数据*************************/

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

    /**
     * 广播消息接收器
     */
    public BroadcastReceiver bc_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent msgintent) {
            // TODO Auto-generated method stub
            ReceiverStr = Objects.requireNonNull(msgintent.getExtras()).getString("MQTT_RevMsg");
            if (ReceiverStr == null) {
                return;
            }
            Log.i(TAG, "MQTT接收消息：" + ReceiverStr);

            try {
                JSONObject user_info = new JSONObject(ReceiverStr);
                if (ReceiverStr != null && user_info.getString("STATUS").equals("True")) {
                    // user_info在STATUS为空时，JSon中还有一个对象"RESULT"为查询到的数据
                    Toast.makeText(LoginMainActivity.this, "登录成功!", Toast.LENGTH_SHORT).show();
                    // 创建一个意图对象，准备跳到指定的活动页面
                    Intent intent_login2main = new Intent(LoginMainActivity.this, MainActivity.class);
                    // 设置启动标志：跳转到新页面时，栈中的原有实例都被清空，同时开辟新任务的活动栈
                    intent_login2main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent_login2main);
                }else{
                    Toast.makeText(LoginMainActivity.this, "登录失败,请联系管理员!", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "解析服务器传来的JSon数据出错");
                throw new RuntimeException(e);
            }
        }
    };

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
                            Toast.makeText(this, "本地校验失败,尝试查询服务器存储的用户中...", Toast.LENGTH_LONG).show();
                        } else {   //密码校验通过
                            Toast.makeText(this, "登录成功!", Toast.LENGTH_SHORT).show();
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
                } else {//本地没有查询到,查询云服务器上的账号
                    Toast.makeText(this, "本地校验失败,尝试查询服务器存储的用户中...", Toast.LENGTH_LONG).show();
                    Intent publishIntent = new Intent(ACTION_PUBLISH);//创建发布消息的Intent,用于连接MqttManager的接收器
                    try {
                        JSONObject JsonMESSAGE = new JSONObject();    //创建JSon字符串,用于封装账号信息
                        JsonMESSAGE.put("pwd", password);
                        JsonMESSAGE.put("phone", phone);
                        publishIntent.putExtra("TOPIC", "/sys/android/service/verify");
                        publishIntent.putExtra("MESSAGE", JsonMESSAGE.toString());
                        sendBroadcast(publishIntent);//向MqttManager发送广播
//                        publishIntent.getStringExtra("TOPIC");
                    } catch (JSONException e) {
                        Log.e("Login-Panel-JSONObject", "处理服务器返回的JSon字符串出错");
                        throw new RuntimeException(e);
                    }
                }

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