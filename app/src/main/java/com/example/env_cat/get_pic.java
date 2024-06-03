package com.example.env_cat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

interface get_a_pic {
    byte[] get_now_pic(String url_add_esp32cam);  //获取当前实时图像
    /*
     *   get_now_pic() 返回当前图像的数据流
     *   url_add_esp32cam: esp32cam(esp32-arduino) 在路由器中的ip地址
     *   return: 返回实时图像的数据流,没获取到实时数据流则返回空
     * */

    Bitmap bytesToImageFile(byte[] bytes);   //当前实时图像数据流转图像
    /*
     *   bytesToImageFile() 将当前实时图像数据流转化为图像
     *   bytes: 实时图像的数据流
     *   return: 图片的位图对象
     * */
}

public abstract class get_pic implements get_a_pic {

    /* 在连接的路由器中查看 */
//    String url_add_esp32cam = "192.168.124.12";     // 摄像头 esp32-arduino ip地址

    public byte[] get_now_pic(String url_add_esp32cam) {
        String downloadUrl = "http://" + url_add_esp32cam + "/stream"; //这里ip具体到路由器管理页面去查看然后修改
//        String savePath = "/sdcard/pic.jpg";

//        File file = new File(savePath);
//        if (file.exists()) {
//            file.delete();
//        }

        BufferedInputStream bufferedInputStream = null;
//        FileOutputStream outputStream = null;
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
//                    StringBuffer stringBuffer = new StringBuffer();
                    int len;
                    byte[] buffer;

                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("Content-Type:")) {
                            line = bufferedReader.readLine();

                            len = Integer.parseInt(line.split(":")[1].trim());

                            bufferedInputStream = new BufferedInputStream(in);
                            buffer = new byte[len];

//                            int t = 0;
//                            while (t < len) {
//                                t += bufferedInputStream.read(buffer, t, len - t);
//                            }
                            return buffer;
//                            bytesToImageFile(buffer, "0A.jpg");
//                            final Bitmap bitmap = BitmapFactory.decodeFile("sdcard/0A.jpg");
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Bitmap bytesToImageFile(byte[] bytes) {
        // 数据流转图片文件，返回位图对象
//        try {
//            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName);
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(bytes, 0, bytes.length);
//            fos.flush();
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}

