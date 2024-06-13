package com.example.env_cat.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.env_cat.R;

import java.util.Random;

public class LoginForgetActivity extends AppCompatActivity implements View.OnClickListener {

    private String mPhone;
    private String mVerifyCode;
    private EditText et_password_first;
    private EditText et_password_second;
    private EditText et_verifycode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_forget);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mPhone = getIntent().getStringExtra("phone");
        Log.i("mPhone", "mPhone= " + mPhone);

        et_verifycode = findViewById(R.id.et_verifycode);
        et_password_first = findViewById(R.id.et_password_first);
        et_password_second = findViewById(R.id.et_password_second);

        findViewById(R.id.btn_verifycode).setOnClickListener(this);
        findViewById(R.id.btn_confirm).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_verifycode:
                // 点击了“获取验证码”按钮
                // 生成六位随机数字的验证码
                mVerifyCode = String.format("%06d", new Random().nextInt(999999));
                // 以下弹出提醒对话框，提示用户记住六位验证码数字
                AlertDialog.Builder buider = new AlertDialog.Builder(this);
                buider.setTitle("请记住验证码");
                buider.setMessage("手机号" + mPhone + ",本次验证码是" + mVerifyCode + ",请输入验证码");
                buider.setPositiveButton("好的", null);
                AlertDialog dialog = buider.create();
                dialog.show();
                break;

            case R.id.btn_confirm:
                // 点击了“确定”按钮
                Log.i("btn_confirm", "1");
                String password_first = et_password_first.getText().toString();
                String password_second = et_password_second.getText().toString();
                Log.i("btn_confirm", "11");
                if (password_first.length() < 6) {
                    Toast.makeText(this, "请输入正确的密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("btn_confirm", "2");

                if (!password_first.equals(password_second)) {
                    Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("btn_confirm", "3");

                if (!mVerifyCode.equals(et_verifycode.getText().toString())) {
                    Toast.makeText(this, "请输入正确的验证码", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.i("btn_confirm", "4");
                Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
                // 以下把修改好的新密码返回给上一个页面
                Intent intent = new Intent();
                intent.putExtra("new_password", password_first);
                setResult(Activity.RESULT_OK, intent);
                finish();
                Log.i("btn_confirm", "5");
                break;
        }
    }
}