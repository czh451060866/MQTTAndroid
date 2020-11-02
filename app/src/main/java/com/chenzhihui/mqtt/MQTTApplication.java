package com.chenzhihui.mqtt;

import android.app.Application;

public class MQTTApplication extends Application {
    private static MQTTApplication myApplication = null;
    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        DeviceUtil.getInstance().initDevice();
    }

    public static Application getApplication(){
        return myApplication;
    }
}
