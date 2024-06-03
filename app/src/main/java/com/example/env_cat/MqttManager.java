package com.example.env_cat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.UUID;

public class MqttManager extends Service {

    public static final String TAG = "nice-code";
    public String HOST = "http://47.108.75.214/:1883";//服务器地址（协议+地址+端口号）
    public String USERNAME = "loss_expenience_android";//用户名
    public String PASSWORD = "123456";//密码
    public static String PUBLISH_TOPIC = "/sys/android/service/verify";//发布主题
    public static String SUBSCRIVE_TOPIC = "/sys/android/service/auth";//订阅主题

    @SuppressLint("MissingPermission")
    public String CLIENTID = UUID.randomUUID().toString().replaceAll("-", "");//客户端ID

    public static final String action = "com.nicecode.mymqttservice";//广播消息

    private static MqttAsyncClient mqttAndroidClient;
    private MqttConnectionOptions mMqttConnectOptions;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 订阅
     */
    public static void MQTT_Subscribe(String Subscribe_Topic) {
        Boolean retained = false;// 是否在服务器保留断开连接后的最后一条消息
        try {
            mqttAndroidClient.subscribe(Subscribe_Topic, 0);
            Log.d(TAG, "订阅主题" + Subscribe_Topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消订阅
     */
    public static void MQTT_UnSubscribe(String UnSubscribe_Topic) {
        Boolean retained = false;// 是否在服务器保留断开连接后的最后一条消息
        try {
            mqttAndroidClient.unsubscribe(UnSubscribe_Topic);
            Log.d(TAG, "取消订阅主题" + UnSubscribe_Topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布消息
     */
    public static void MQTT_Publish(String Publish_Topic, Integer qos, String message) {
        Boolean retained = false;// 是否在服务器保留断开连接后的最后一条消息
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(Publish_Topic, message.getBytes(), qos, retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化
     */
    private void init() {
        Log.d(TAG, "mqtt service init fun");
        String serverURI = HOST; //服务器地址（协议+地址+端口号）
        try {
            mqttAndroidClient = new MqttAsyncClient(serverURI, CLIENTID, new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        mqttAndroidClient.setCallback(mqttCallback);
        mMqttConnectOptions = new MqttConnectionOptions();
        mMqttConnectOptions.setCleanStart(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(10); //设置超时时间，单位：秒
        mMqttConnectOptions.setKeepAliveInterval(60); //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setUserName(USERNAME); //设置用户名
        mMqttConnectOptions.setPassword(PASSWORD.getBytes()); //设置密码

        // last will message
        boolean doConnect = true;
        String message = "last will message";
        String topic = PUBLISH_TOPIC;

        MqttMessage MqttMessage = new MqttMessage(message.getBytes());

        // 最后的遗嘱
        try {
            mMqttConnectOptions.setWill(topic, MqttMessage);
        } catch (Exception e) {
            Log.i(TAG, "Exception Occured", e);
            doConnect = false;
            iMqttActionListener.onFailure(null, e);
        }
        if (doConnect) {
            doClientConnection();
        }
    }

    /**
     * 初始化
     */
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {

        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            Log.i(TAG, "收到消息： " + new String(message.getPayload()));
            Intent intent = new Intent(action);
            Bundle bundle = new Bundle();
            bundle.putString("MQTT_RevMsg", new String(message.getPayload()));
            intent.putExtras(bundle);
            sendBroadcast(intent);
        }

        @Override
        public void deliveryComplete(IMqttToken token) {

        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {

        }
    };

    /**
     * MQTT是否连接成功的监听
     */
    private MqttActionListener iMqttActionListener = new MqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                mqttAndroidClient.subscribe(SUBSCRIVE_TOPIC, 2);//订阅主题，参数：主题、服务质量
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.i(TAG, "连接失败 ");
            doClientConnection();//连接失败，重连（可关闭服务器进行模拟）
        }
    };

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
                Log.d(TAG, "Connected to MQTT server.");
            } catch (MqttException me) {
                Log.d(TAG, "Connection failed: ");
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            }, 3000);
            return false;
        }
    }

    @Override
    public void onDestroy() {
        try {
            mqttAndroidClient.disconnect(); //断开连接
            Log.d(TAG, "断开 mqtt connect");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
