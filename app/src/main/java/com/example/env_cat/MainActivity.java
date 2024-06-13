package com.example.env_cat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity::";

    private HandlerThread handlerThread;
    private Handler handler;
    private ImageView imageView;

    private final int DOWNDLOAD = 1;
    private final int REGISTER = 2;

    static boolean ctrl_num = true; //true代表无更新温湿度及重力数据请求

    private static final int WRITE_PERMISSION = 0x01;

    String weight_num, wendu_num, shidu_num;
    TextView tv_temp, tv_humiVal, tv_weightVal;

    /* 在连接的路由器中查看 */
    String url_address = "192.168.124.56";  // 温湿度 ESP ip地址
    String url_add_esp32cam = "192.168.124.63";     // 摄像头 esp32-arduino ip地址
    private Button btn_connVid;
    private Button btn_updateEnvInfo;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.img);
        handlerThread = new HandlerThread("http");
        handlerThread.start();
        handler = new HttpHandler(handlerThread.getLooper());

        btn_connVid = findViewById(R.id.btn_connVid);
        btn_updateEnvInfo = findViewById(R.id.btn_updateEnvInfo);
        btn_connVid.setOnClickListener(this);
        btn_updateEnvInfo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connVid:
                handler.sendEmptyMessage(DOWNDLOAD);
                break;
            case R.id.btn_updateEnvInfo:
                handler.sendEmptyMessage(REGISTER);
                ctrl_num = false;   //有更新温湿度重力数据请求
                break;
            default:
                break;
        }
    }

    private class HttpHandler extends Handler {
        public HttpHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REGISTER:
                    dht_hx_data();
                    break;
                case DOWNDLOAD:
                    downloadFile();
                    break;
                default:
                    break;
            }
        }
    }

    private void dht_hx_data() {
        try {
            System.out.println("向ESP01S发送get请求中...");
            String json_data = sendGet(url_address);
            parseJson_display(json_data);
            ctrl_num = true;    //温湿度重力数据更新完毕
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadFile() {
        if (!isGrantExternalRW(this)) {   //动态权限
            return;
        }
        String downloadUrl = "http://" + url_add_esp32cam + "/stream"; //这里ip具体到路由器管理页面去查看然后修改
        String rootPath = getSDPath(this);
        String savePath = rootPath + "/pic.jpg";

        File file = new File(savePath);
        if (file.exists()) {
            file.delete();
        }

        if (!isGrantExternalRW(this)) {
            return;
        }

        BufferedInputStream bufferedInputStream = null;
        FileOutputStream outputStream = null;
        try {
            URL url = new URL(downloadUrl);

            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(1000 * 5);
                httpURLConnection.setReadTimeout(1000 * 5);
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                if (httpURLConnection.getResponseCode() == 200) {
                    InputStream in = httpURLConnection.getInputStream();

                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(isr);

                    String line;
                    StringBuffer stringBuffer = new StringBuffer();

                    int i = 0;

                    int len;
                    byte[] buffer;

                    while ((line = bufferedReader.readLine()) != null && ctrl_num) {
//                        dht_hx_data();
                        if (line.contains("Content-Type:")) {
                            line = bufferedReader.readLine();

                            len = Integer.parseInt(line.split(":")[1].trim());

                            bufferedInputStream = new BufferedInputStream(in);
                            buffer = new byte[len];

                            int t = 0;
                            while (t < len) {
                                t += bufferedInputStream.read(buffer, t, len - t);
                            }

                            bytesToImageFile(buffer, "pic.jpg");

                            final Bitmap bitmap = BitmapFactory.decodeFile(rootPath + "/pic.jpg");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //动态申请权限
    public static boolean isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
            return false;
        }
        return true;
    }

    public static String getSDPath(Context context) {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);// 判断sd卡是否存在
        if (sdCardExist) {
            if (Build.VERSION.SDK_INT >= 29) {
                //Android10之后
                sdDir = context.getExternalFilesDir(null);
            } else {
                sdDir = Environment.getExternalStorageDirectory();// 获取SD卡根目录
            }
        } else {
            sdDir = Environment.getRootDirectory();// 获取跟目录
        }
        return sdDir.toString();
    }

    private void bytesToImageFile(byte[] bytes, String fileName) {
        try {
            File file = new File(getSDPath(this) + "/" + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseJson_display(String json) {   //解析json
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            System.out.println("JSON对象化错误");
            e.printStackTrace();
        }
        try {
            System.out.println("解析JSON...");
            weight_num = jsonObject.getString("weight");
            wendu_num = jsonObject.getString("wendu");
            shidu_num = jsonObject.getString("shidu");

            tv_temp = (TextView) findViewById(R.id.tv_temp);
            tv_humiVal = (TextView) findViewById(R.id.tv_humiVal);
            tv_weightVal = (TextView) findViewById(R.id.tv_weightVal);


            float wendu_float = Float.parseFloat(wendu_num);
            float shidu_float = Float.parseFloat(shidu_num);
            if (wendu_float > 40.0) {
                tv_temp.setText(String.format("%s温度偏高,请再等一会...", wendu_num));
            } else tv_temp.setText(wendu_num);
            if (shidu_float > 65.0) {
                tv_humiVal.setText(String.format("%s       !", shidu_num));
            } else tv_humiVal.setText(shidu_num);
            tv_weightVal.setText(weight_num);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String sendGet(String httpUrl) {
        System.out.println("开始HTTP请求...");
        //链接
        HttpURLConnection connection = null;
        InputStream is = null;
        BufferedReader br = null;
        StringBuffer result = new StringBuffer();

        try {
            //创建连接
            URL url = new URL("http://" + httpUrl);
            connection = (HttpURLConnection) url.openConnection();
            //设置请求方式
            connection.setRequestMethod("GET");
            //设置连接超时时间
            connection.setReadTimeout(3000);
            //开始连接
            connection.connect();
            //获取响应数据
            if (connection.getResponseCode() == 200) {
                //获取返回的数据
                is = connection.getInputStream();
                if (null != is) {
                    br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String temp = null;
                    while (null != (temp = br.readLine())) {
                        result.append(temp);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "如果长时间获取数据不成功,请离路由器近一点并尝试复位ESP01", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else ctrl_num = false;    //返回错误重新允许视频输入
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭远程连接
            connection.disconnect();
        }
        System.out.println(result);
        return result.toString();
    }

    /*描述: 读取输入流中的字符串
     * @param inStream
     * @return 输入流中的字节数组*/
    private static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }
}