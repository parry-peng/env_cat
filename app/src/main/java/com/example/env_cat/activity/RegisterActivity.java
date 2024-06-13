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
import androidx.room.Room;

import com.example.env_cat.R;
import com.example.env_cat.dao.UserDao;
import com.example.env_cat.database.UserDatabase;
import com.example.env_cat.enity.UserInfo;
import com.example.env_cat.util.ToastUtil;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_register;
    private EditText et_tel , et_password;
    private UserDao userDao;
    private UserDatabase userDatabase;

    public UserDatabase getUserDB() {
        return userDatabase;
    }

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

        et_tel = findViewById(R.id.et_tel);
        et_password = findViewById(R.id.et_password);

        btn_register = findViewById(R.id.btn_register);
        btn_register.setOnClickListener(this);

        // 构建用户数据库的实例
        userDatabase = Room.databaseBuilder(this, UserDatabase.class, "user_db")
                // 允许迁移数据库（发生数据库变更时，Room默认删除原数据库再创建新数据库。如此一来原来的记录会丢失，故而要改为迁移方式以便保存原有记录）
                .addMigrations()
                // 允许在主线程中操作数据库（Room默认不能在主线程中操作数据库）
                .allowMainThreadQueries()
                .build();

        userDao = userDatabase.userDao();
    }

    @Override
    public void onClick(View v) {
        String telephone = et_tel.getText().toString();
        String password = et_password.getText().toString();

        switch (v.getId()) {
            case R.id.btn_register:
            {
                UserInfo u1 = new UserInfo();
                u1.setTelephone(telephone);
                u1.setPassword(password);
                userDao.insert(u1);
                ToastUtil.show(this, "保存成功");
                break;
            }

        }
    }
}