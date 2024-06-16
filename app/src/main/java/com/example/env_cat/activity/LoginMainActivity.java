package com.example.env_cat.activity;

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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.env_cat.MyApplication;
import com.example.env_cat.database.DatabaseHelper;
import com.example.env_cat.MainActivity;
import com.example.env_cat.database.UserDBHelper;
import com.example.env_cat.enity.User;
import com.example.env_cat.mqtt.MqttManager;
import com.example.env_cat.R;
import com.example.env_cat.util.ViewUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class LoginMainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText et_telephone;
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
    private Button btn_login_register;
    private MyApplication app;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_main);

        app = MyApplication.getInstance();

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

        et_telephone = findViewById(R.id.et_telephone);
        et_password = findViewById(R.id.et_password);
        btn_forget = findViewById(R.id.btn_forget);
        ck_remember = findViewById(R.id.ck_remember);
        btn_login = findViewById(R.id.btn_login);
        btn_login_register = findViewById(R.id.btn_login_register);
        btn_login_register.setOnClickListener(this);

        et_telephone.addTextChangedListener(new HideTextWatcher(et_telephone, 11));
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
    @Override
    public void onClick(View v) {
        String telephone = et_telephone.getText().toString();
        String password = et_password.getText().toString();
        switch (v.getId()) {
            case R.id.btn_forget:
            {
                Intent intent = new Intent(this, LoginForgetActivity.class);
                intent.putExtra("phone", telephone);
                register.launch(intent);
                break;
            }
            case R.id.btn_login:
            {
                // 查询
                List<User> list =  app.getUserDB().queryByTelephone(telephone);
                int count = list.size();
                if (count > 0) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginMainActivity.this, "该账号不存在，请注册", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.btn_login_register:
            {
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
            }
        }
    }
    private void reload() {
        boolean isRemember = preferences.getBoolean("isRemember", false);
        if (isRemember) {
            String phone = preferences.getString("phone", "");
            et_telephone.setText(phone);
            String password = preferences.getString("password", "");
            et_password.setText(password);
            ck_remember.setChecked(true);
        }
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
                } else if (ReceiverStr != null && user_info.getString("STATUS").equals("False")) {
                    Toast.makeText(LoginMainActivity.this, "登录失败,请联系管理员!", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "解析服务器传来的JSon数据出错");
                throw new RuntimeException(e);
            }
        }
    };
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