package com.example.env_cat.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.env_cat.MyApplication;
import com.example.env_cat.R;
import com.example.env_cat.enity.User;
import com.example.env_cat.util.ToastUtil;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_register;
    private EditText et_reg_telephone , et_reg_password;
    private MyApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        app = MyApplication.getInstance();

        et_reg_telephone = findViewById(R.id.et_reg_telephone);
        et_reg_password = findViewById(R.id.et_reg_password);

        btn_register = findViewById(R.id.btn_register);
        btn_register.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String telephone = et_reg_telephone.getText().toString();
        String password = et_reg_password.getText().toString();
        User user = null;
        switch (v.getId()) {
            case R.id.btn_register:
            {
                user = new User(telephone,password);
                if (app.getUserDB().insert(user) > 0) {
                    ToastUtil.show(this, "添加成功");
                }
                break;
            }
        }
    }
}