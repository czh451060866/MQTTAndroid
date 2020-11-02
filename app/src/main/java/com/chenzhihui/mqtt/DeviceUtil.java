/*
 * Copyright (c) 2016 Meituan Inc.
 *
 *   The right to copy, distribute, modify, or otherwise make use
 *   of this software may be licensed only pursuant to the terms
 *   of an applicable Meituan license agreement.
 */

package com.chenzhihui.mqtt;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;


import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class DeviceUtil extends com.meituan.android.common.performance.serialize.Environment {
    private String facNo = "";
    private String modelNo = "";
    private String serialNo = "";

    public DeviceUtil() {
        super(MQTTApplication.getApplication());
    }

    private static class SingletonFactory {
        private static DeviceUtil instance = new DeviceUtil();
    }

    public static DeviceUtil getInstance() {
        return DeviceUtil.SingletonFactory.instance;
    }

    public void initDevice() {
        facNo = Build.MANUFACTURER;
        if (Build.MODEL.equals("APOS A8") || Build.MODEL.equals("APOS A7") || Build.MODEL.equals("N900")) {
            //联迪A7、A8和新大陆N900是用的最早的联迪的CloudService版本，所以Model就是传的Model
            modelNo = Build.MODEL;
        } else {
            modelNo = Build.MANUFACTURER + " " + Build.MODEL;
        }
        if (Build.MODEL.equals("D1") || Build.MODEL.equals("t1host")) {
            serialNo = getSnByReflection();
        } else {
            serialNo = Build.SERIAL;
        }
    }

    @Override
    public String getUuid() {
        return serialNo;
    }

    @Override
    public String getCh() {
        return "meituan";
    }

    @Override
    public String getToken() {
        return "5a4d9c2740d8d1750a5416b1";
    }

    public String getFacNo() {
        return facNo;
    }

    public String getModelNo() {
        return modelNo;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getRegCode() {
        String channel = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            channel = (String) c.getMethod("get", String.class).invoke(c, "ro.regcode");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channel;
    }

    /**
     * 获取系统编译时间UTC
     */
    public String getBuildTimeUTC() {
        String buildTime = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            buildTime = (String) c.getMethod("get", String.class).invoke(c, "ro.build.date.utc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buildTime;
    }

    /**
     * 获取ROM版本号
     */
    public String getRomVersion() {
        String osVersion = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
//            systemVersionCode = (String) c.getMethod("get", new Class[]{String.class}).invoke(c, "ro.build.display.id");
            osVersion = (String) c.getMethod("get", String.class).invoke(c, "ro.product.version");
            int index = osVersion.lastIndexOf(".");
            osVersion = osVersion.substring(0, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return osVersion;
    }

    public String getMacAddress() {
        String mac;
        mac = getEthMac();
        if (TextUtils.isEmpty(mac)) {
            mac = getIMEI();
            if (TextUtils.isEmpty(mac)) {
                mac = getWifiMac();
            }
        }
        return mac;
    }

    private String getWifiMac() {
        String mac;
        WifiManager wifiManager = (WifiManager) MQTTApplication.getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mac = tryToGetWifiMAC(wifiManager);
//        if (!TextUtils.isEmpty(mac)) {
//            return mac;
//        }

        //获取失败，尝试打开wifi获取
//        if (wifiManager != null) {
//            int state = wifiManager.getWifiState();
//            if (state != WifiManager.WIFI_STATE_ENABLED && state != WifiManager.WIFI_STATE_ENABLING) {
//                wifiManager.setWifiEnabled(true);
//            }
//        }
//
//        for (int index = 0; index < 3; index++) {
//            //假设第一次没有成功，第二次做100毫秒的延迟。
//            if (index != 0) {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            mac = tryToGetWifiMAC(wifiManager);
//            if (!TextUtils.isEmpty(mac)) {
//                break;
//            }
//        }
//
//        //尝试关闭wifi
//        if (wifiManager != null) {
//            wifiManager.setWifiEnabled(false);
//        }
        return mac;
    }

    private String getEthMac() {
        //使用命令行获取mac地址
        String macSerial = "";
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim().toLowerCase();
                    break;
                }
            }
        } catch (Exception ex) {
            // 赋予默认值
            ex.printStackTrace();
            macSerial = "";
        }
        return macSerial;
    }

    private String getIMEI() {
        String imei;
        try {
            //实例化TelephonyManager对象
            TelephonyManager telephonyManager = (TelephonyManager) MQTTApplication.getApplication().getSystemService(Context.TELEPHONY_SERVICE);
            //获取IMEI号
            imei = telephonyManager.getDeviceId();
            if (imei == null) {
                imei = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            imei =  "";
        }
        return imei;
    }

    private String getSnByReflection() {
        String serial = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            serial = (String) c.getMethod("get", String.class).invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    private String tryToGetWifiMAC(WifiManager wifiManager) {
        //使用命令行获取mac地址
        String macSerial = "";
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim().toLowerCase();
                    break;
                }
            }
        } catch (Exception ex) {
            // 赋予默认值
            ex.printStackTrace();
            macSerial = "";
        }
        return macSerial;
    }
}
