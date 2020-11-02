package com.chenzhihui.mqtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.meituan.android.common.performance.utils.LogUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends AppCompatActivity {

    public static final int MQTT_STATE_UNKNOWN = -1;
    public static final int MQTT_STATE_CONNECTING = 0;
    public static final int MQTT_STATE_CONNECTED = 1;
    public static final int MQTT_STATE_DISCONNECTING = 2;
    public static final int MQTT_STATE_DISCONNECTED = 3;

    private MqttAndroidClient mClient;
    private MqttConnectOptions mOptions;
    private String mqttTimestamp = "";
    private String MQTT_URL = "tcp://192.168.1.154:1883";
    private static final int KEEP_ALIVE_INTERVAL = 60; // 60s
    private int currMqttState = MQTT_STATE_UNKNOWN;
    private static final String mTopic = "topicOne";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void initClient() {
        if (mClient == null) {
            if (TextUtils.isEmpty(mqttTimestamp)) {
                mqttTimestamp = String.valueOf(System.currentTimeMillis() / 1000);
            }
            mClient = new MqttAndroidClient(getApplicationContext(), MQTT_URL, getClientId());
        }
        mClient.setCallback(mMqttCallback);
    }

    private void initOptions() {
        mOptions = new MqttConnectOptions();
        mOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        mOptions.setAutomaticReconnect(false);
        mOptions.setCleanSession(true);
//        mOptions.setConnectionTimeout(CONNECTION_TIMEOUT);
        mOptions.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
        mOptions.setUserName("admin");
        mOptions.setPassword("admin".toCharArray());
        mOptions.setMaxInflight(100);
    }

    private MqttCallback mMqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.d("czhczh", "connectionLost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d("czhczh", "messageArrived" + topic);
            Toast.makeText(MainActivity.this, topic + "  " + message.toString() + "  " + message.getQos(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d("czhczh", "deliveryComplete");
        }
    };

    private IMqttActionListener mIMqttConnectListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d("czhczh", "Connect success");
            try {
                if (mClient != null && mClient.isConnected()) {
                    setCurrConnectState(MQTT_STATE_CONNECTED);

                    // 开始poll任务
                    // qos 0：至多一次 1：至少一次 2：只有一次
                    mClient.subscribe(mTopic, 0, MQTTApplication.getApplication(), mIMqttSubscribeListener);
                } else {
                    setCurrConnectState(MQTT_STATE_UNKNOWN);

                    if (mClient == null) {
                    } else {
                    }
                }
            } catch (MqttException e) {
                e.printStackTrace();
                setCurrConnectState(MQTT_STATE_UNKNOWN);
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.d("czhczh", "Connect failed");
            if (exception != null) {
                exception.printStackTrace();
            }

            setCurrConnectState(MQTT_STATE_UNKNOWN);
        }
    };

    private IMqttActionListener mIMqttSubscribeListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    };

    private String getClientId() {
        if (TextUtils.isEmpty(mqttTimestamp)) {
            mqttTimestamp = String.valueOf(System.currentTimeMillis() / 1000);
        }
        String clientId = DeviceUtil.getInstance().getRegCode() + "&&"
                + DeviceUtil.getInstance().getSerialNo() + "&&"
                + mqttTimestamp;
        return clientId;
    }

    public void initConnect(View view) {
        connect(true);
    }

    public void publishMsg(View view) {
        try {
            mClient.publish(mTopic, "publish msg".getBytes(), 0, false, MQTTApplication.getApplication(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("czhczh", "publishMsg success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                    Log.d("czhczh", "publishMsg failed");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect(final boolean retry) {
        initClient();
        initOptions();

        try {
            if (retry) {
                Random random = new Random();
                int sleepTime = random.nextInt(1000 * 60);
                Thread.sleep(sleepTime);
            }

            mClient.unregisterResources();
            mClient.connect(mOptions, MQTTApplication.getApplication(), mIMqttConnectListener);
        } catch (Exception e) {
            e.printStackTrace();
            setCurrConnectState(MQTT_STATE_UNKNOWN);
        }
    }

    public void setCurrConnectState(int state) {
        synchronized (MainActivity.class) {
            currMqttState = state;
        }
    }


}