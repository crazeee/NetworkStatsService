package com.mobile2345.ads.loader.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.mobile2345.ads.loader.App;
import com.mobile2345.ads.loader.BuildConfig;
import com.mobile2345.ads.loader.config.Constant;
import com.mobile2345.ads.loader.statistic.DeviceHelper;
import com.mobile2345.ads.loader.statistic.StatisticConstant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

/**
 * @author hepengcheng
 * @package_name com.mobile2345.ads.app.utils
 * @date 2017/12/29
 */

public class DeviceUtils {

    //USB状态值
    public static final int DEFAULT = 0;
    public static final int NOT_CHARGING = 1;
    public static final int USB_CHARGING = 2;
    public static final int AC_CHARGING = 3;
    private static String sNum = "";

    private DeviceUtils() {
    }

    /**
     * 获取Imei值
     * 首次获取记录在本地
     * 后续获取先从本地获取
     *
     * @return
     */
    public static String getImei() {
        String imei = "";
        if (App.sContext == null) {
            return "";
        }
        if (!PermissionUtils.checkReadPhoneStatePermission()) {
            return "";
        }

        SpUtils spUtils = new SpUtils(Constant.DEVICE_DATA, App.sContext);
        imei = spUtils.getString(Constant.IMEI, "");
        if (TextUtils.isEmpty(imei)) {
            try {
                imei = ((TelephonyManager) App.sContext.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                if (!TextUtils.isEmpty(imei)) {
                    imei = imei.trim();
                    spUtils.putStringByCommit(Constant.IMEI, imei);
                } else {
                    imei = "";
                }
            } catch (Exception e) {
                imei = "";
            }
        }
        return imei;
    }

    /**
     * 返回系统类型
     *
     * @return
     */
    public static String getOs() {
        return "Android";
    }

    /**
     * 获取网络类型 WIFI/3G/4G/2G
     *
     * @return 1-wifi 2-2g 3-3g 4-4g
     */
    public static int getNetworkType() {
        int result = 0;
        try {
            if (App.sContext == null) {
                return result;
            }
            ConnectivityManager cm = (ConnectivityManager) App.sContext.getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            if (cm == null) {
                return result;
            }
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                int type = ni.getType();
                switch (type) {
                    case ConnectivityManager.TYPE_MOBILE:
                        if (ni.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS || ni.getSubtype()
                                == TelephonyManager.NETWORK_TYPE_CDMA
                                || ni.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE) {
                            result = 2;
                        } else if (ni.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE) {
                            result = 4;
                        } else {
                            result = 3;
                        }
                        break;
                    case ConnectivityManager.TYPE_WIFI:
                        result = 1;
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 获取设备厂商
     */
    public static String getBrand() {
        return Build.BRAND;
    }

    /**
     * 获取手机型号
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }


    /**
     * 获取设备分辨率
     */
    public static String getResolution() {
        if (App.sContext == null) {
            return "*";
        }
        String result = "";
        try {
            SpUtils spUtils = new SpUtils(Constant.DEVICE_DATA, App.sContext);
            result = spUtils.getString(Constant.RESOLUTION, "");
            if (TextUtils.isEmpty(result)) {
                DisplayMetrics dm = App.sContext.getResources().getDisplayMetrics();
                result = dm.widthPixels + "*" + dm.heightPixels;
                spUtils.putStringByCommit(Constant.RESOLUTION, result);
                return result;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取ip地址
     *
     * @param context
     * @return
     */
    public static String getIPAddress(Context context) {
        if (context == null) {
            return "";
        }
        try {
            NetworkInfo info = ((ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                //当前使用2G/3G/4G网络
                if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    try {
                        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                            NetworkInterface networkInterface = en.nextElement();
                            for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                                InetAddress inetAddress = inetAddresses.nextElement();
                                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                    return inetAddress.getHostAddress();
                                }
                            }
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    //当前使用无线网络
                } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    try {
                        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        //得到IPV4地址
                        String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
                        return ipAddress;
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    /**
     * 获取经度和纬度
     * 必须在主线程调用
     *
     * @param context
     * @return
     */
    public static String[] getLngAndLat(Context context) {
        double latitude = 0.0;
        double longitude = 0.0;
        String latAndLng = "";
        String[] result = new String[2];
        DeviceHelper deviceHelper = new DeviceHelper();
        if (context == null || !PermissionUtils.checkLocationLatAndLonPermission()) {
            result[0] = latitude + "";
            result[1] = longitude + "";
            return result;
        }
        try {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //从gps获取经纬度
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        try {
                            locationManager.removeUpdates(this);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    latAndLng = latitude + "," + longitude;
                    result = latAndLng.split(",");
                    return result;
                } else {//当GPS信号弱没获取到位置的时候又从网络获取
                    return deviceHelper.getLngAndLatWithNetwork();
                }
            } else {    //从网络获取经纬度
                return deviceHelper.getLngAndLatWithNetwork();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        result[0] = latitude + "";
        result[1] = longitude + "";
        return result;
    }

    /**
     * 获取操作系统版本号
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取操作系统版本号
     */
    public static int getOsVersionCode() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 判断是平板还是手机
     *
     * @param context
     * @return 1-手机, 2-平板 -1:未知
     */
    public static int isPad(Context context) {
        int result;
        if (context == null) {
            return -1;
        }
        if ((context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            result = 2;
        } else {
            result = 1;
        }

        return result;
    }

    /**
     * 判断当前屏幕是横屏还是竖屏
     *
     * @return 1-竖屏 2-横屏
     */
    public static int getOrientation() {
        int result = 0;
        if (App.sContext == null) {
            return result;
        }
        try {
            Configuration configuration = App.sContext.getResources().getConfiguration(); //获取设置的配置信息
            int ori = configuration.orientation; //获取屏幕方向
            if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
                result = 2;
            } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                //竖屏
                result = 1;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getPhoneRatio() {
        if (App.sContext == null) {
            return "";
        }
        String deviceWidth = String.valueOf(DensityUtils.getScreenWidth(App.sContext));
        String deviceHeight = String.valueOf(DensityUtils.getScreenHeight(App.sContext));
        return deviceWidth + "*" + deviceHeight;
    }

    public static String getAndroidId() {
        try {
            return Settings.System.getString(App.sContext.getContentResolver(), Settings.System.ANDROID_ID);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 功能：获取手机SIM卡串号
     * 说明：如果imsi为空，则返回""，该方法仅用在uuid获取，与武林榜一致
     */
    public static String getImsi() {
        try {
            TelephonyManager tm = (TelephonyManager) App.sContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                return "";
            }
            String imsi = tm.getSubscriberId();
            if (TextUtils.isEmpty(imsi)) {
                String simOperator = tm.getSimOperator();
                if (!TextUtils.isEmpty(simOperator)) {
                    imsi = simOperator + "@" + getAndroidId();
                } else {
                    imsi = "";
                }
            }
            return imsi;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取网络类型 WIFI/3G/4G/2G
     *
     * @return wifi 2g 3g 4g
     */
    public static String getAccessType() {
        String result = "";
        try {
            if (App.sContext == null) {
                return result;
            }
            ConnectivityManager cm = (ConnectivityManager) App.sContext.getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            if (cm == null) {
                return result;
            }
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                int type = ni.getType();
                switch (type) {
                    case ConnectivityManager.TYPE_MOBILE:
                        if (ni.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS || ni.getSubtype()
                                == TelephonyManager.NETWORK_TYPE_CDMA
                                || ni.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE) {
                            result = "2g";
                        } else if (ni.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE) {
                            result = "4g";
                        } else {
                            result = "3g";
                        }
                        break;
                    case ConnectivityManager.TYPE_WIFI:
                        result = "WiFi";
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取电池电量
     *
     * @return
     */
    public static int getBattery() {
        int result = -1;
        try {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatusIntent = App.sContext.registerReceiver(null, iFilter);
            if (batteryStatusIntent != null) {
                int level = batteryStatusIntent.getIntExtra("level", -1);
                int scale = batteryStatusIntent.getIntExtra("scale", 100);
                result = 100 * level / scale;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取手机内部总的存储空间
     */
    public static long getTotalInternalMemorySize() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize / 1024 / 1024;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获得系统总内存大小
     */
    public static long getSystemTotalMemorySize(Context context) {
        long mTotal = -1;
        if (context == null) {
            return mTotal;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memoryInfo);
                    mTotal = memoryInfo.totalMem;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return mTotal;
            }
        } else {
            String path = "/proc/meminfo";
            String content = null;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(path), 8);
                String line;
                if ((line = br.readLine()) != null) {
                    content = line;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                if (TextUtils.isEmpty(content)) {
                    return mTotal;
                } else {
                    int begin = content.indexOf(':');
                    int end = content.indexOf('k');

                    content = content.substring(begin + 1, end).trim();
                    mTotal = Integer.parseInt(content) * 1024;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return mTotal;
            }
        }
        if (mTotal >= 0) {
            return mTotal / 1024 / 1024;
        }
        return -1;
    }

    public static String getGEO(Context context) {
        if (context == null) {
            return "";
        }
        SpUtils mSp = new SpUtils(Constant.LOCATION_DATA, context);
        return mSp.getString(Constant.LOCATION_GEO, "");
    }

    /**
     * app初始化时，请求定位
     *
     * @param context
     */
    public static void requestLocation(Context context) {
        if (context == null) {
            return;
        }
        final SpUtils mSp = new SpUtils(Constant.LOCATION_DATA, context);
        mSp.clear();
        try {
            final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            final LocationListener sLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    try {
                        locationManager.removeUpdates(this);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }


                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 1, sLocationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    mSp.putStringByCommit(Constant.LOCATION_GEO, "" + location.getLatitude() + "," + location.getLongitude());

                } else {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        mSp.putStringByCommit(Constant.LOCATION_GEO, "" + location.getLatitude() + "," + location.getLongitude());
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String getNum(Context context) {
        if (context == null) {
            return "";
        }
        String result = "";
        String num = "";
        try {
            SpUtils spUtils = new SpUtils(Constant.USER_CENTER, context);
            num = spUtils.getString(Constant.USER_CENTER_UNION_PHONE, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(num) && num.length() > 7) {
            result = num.substring(0, 7);
        } else if (!TextUtils.isEmpty(sNum) && sNum.length() > 7) {
            result = sNum.substring(0, 7);
        }
        return result;
    }

    public static void setNum(String num) {
        if (!TextUtils.isEmpty(num) && num.length() > 7) {
            sNum = num;
        }
    }

    public static int getChargeStatus(Context context) {
        int result = DEFAULT;
        if (context == null) {
            return result;
        }
        try {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatusIntent = context.registerReceiver(null, iFilter);
            if (batteryStatusIntent != null) {
                int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                int chargePlug = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
                if (isCharging) {
                    if (usbCharge) {
                        result = USB_CHARGING;
                    } else if (acCharge) {
                        result = AC_CHARGING;
                    }
                } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    result = NOT_CHARGING;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取蓝牙mac地址
     *
     * @return
     */
    public static String getBlueToothAddress(Context context) {
        if (context == null) {
            return "";
        }
        String result = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContentResolver contentResolver = context.getContentResolver();
                if (contentResolver != null) {
                    result = Settings.Secure.getString(contentResolver, "bluetooth_address");
                }
            } else {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    result = bluetoothAdapter.getAddress();
                }
            }

            if (TextUtils.isEmpty(result)) {
                return "";
            } else {
                result = result.replaceAll(":", "").replaceAll("\\.", "").toLowerCase(Locale.US);
                result = result.trim();
            }
        } catch (Exception e) {
            return "";
        }
        return result;
    }

    /**
     * 获取手机设置亮度
     */
    public static int getBrightness(Context context) {
        if (context == null) {
            return -1;
        }
        int systemBrightness = -1;
        try {
            systemBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    /**
     * 获得系统可用内存信息
     */
    public static long getSystemAvailableMemorySize(Context context) {
        if (context == null) {
            return -1;
        }
        ActivityManager.MemoryInfo memoryInfo = null;
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            memoryInfo = new ActivityManager.MemoryInfo();
            if (activityManager != null) {
                activityManager.getMemoryInfo(memoryInfo);
            }
            if (memoryInfo.availMem >= 0) {
                return memoryInfo.availMem / 1024 / 1024;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 获取手机内部剩余存储空间
     */
    public static long getAvailableInternalMemorySize() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize / 1024 / 1024;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获取qq或者wechat文件修改时间
     * 精确到秒 10位时间戳
     *
     * @return
     */
    public static long getFileModifyTime(Context context, String appName) {
        if (context == null || TextUtils.isEmpty(appName)) {
            return -1;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                return -1;
            }
        }

        String path = "";
        switch (appName) {
            case Constant.MOBLIE_QQ:
                path = Environment.getExternalStorageDirectory() + File.separator + "Android" + File.separator + "data" + File.separator
                        + "com.tencent.mobileqq" + File.separator + "files" + File.separator + "tbslog" + File.separator + "tbslog.txt";
                break;
            case Constant.WECHAT:
                path = Environment.getExternalStorageDirectory() + File.separator + "Android" + File.separator + "data" + File.separator
                        + "com.tencent.mm" + File.separator + "files" + File.separator + "tbslog" + File.separator + "tbslog.txt";
                break;
            default:
                break;
        }

        try {
            File file = new File(path);
            if (!TextUtils.isEmpty(path) && file.exists()) {
                return file.lastModified() / 1000;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取武林榜sdk存本地的激活天数
     *
     * @param context
     * @return
     */
    public static int getLocalDay(Context context) {
        if (context == null) {
            return 0;
        }
        int result = 0;
        try {
            SpUtils spUtils = new SpUtils(Constant.TONG_JI, context);
            result = spUtils.getInt(Constant.LOCAL_DAY_COUNT, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取武林榜sdk存本地的app激活状态
     *
     * @param context
     * @return
     */
    public static int getActivate(Context context) {
        if (context == null) {
            return 0;
        }
        int result = 0;
        try {
            SpUtils spUtils = new SpUtils(Constant.TONG_JI, context);
            result = spUtils.getInt(Constant.PARAM_ACTIVATE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取武林榜存本地的是否启动的状态
     *
     * @param context
     * @return
     */
    public static int getStart(Context context) {
        if (context == null) {
            return 0;
        }
        int result = 0;
        try {
            SpUtils spUtils = new SpUtils(Constant.TONG_JI, context);
            result = spUtils.getInt(Constant.PARAM_RESPCODE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获得系统音量
     */
    public static String getSystemVolume(Context context) {
        if (context == null) {
            return "";
        }
        String result = "";
        try {
            AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager != null) {
                result = "" + mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) + "/" +
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取流量
     *
     * @param context
     * @return
     */
    public static long getTrafficUsed(Context context) {
        if (context == null) {
            return -1;
        }
        long traffic = -1;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo == null) {
                return -1;
            }
            int uid = packageInfo.applicationInfo.uid;
            traffic = getTraffic(uid);
            if (traffic == 0 || traffic == -1) {
                traffic = getTrafficApi25(uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return traffic;
    }

    /**
     * 获取指定应用的流量消耗
     *
     * @param context
     * @param packageName
     * @return
     */
    public static long getTargetTrafficUsed(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return -1L;
        }
        long traffic = -1;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo == null) {
                return -1;
            }
            int uid = packageInfo.applicationInfo.uid;
            traffic = getTraffic(uid);
            if (traffic == 0 || traffic == -1) {
                traffic = getTrafficApi25(uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return traffic;
    }

    /**
     * 指定uid获取流量
     *
     * @param uid
     * @return
     */
    private static long getTraffic(int uid) {
        long traffic = -1;
        try {
            traffic = (TrafficStats.getUidTxBytes(uid) / 1024) + (TrafficStats.getUidRxBytes(uid) / 1024);
            if (traffic == -1 || traffic == 0) {
                traffic = getTrafficUsedByFile(uid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return traffic;
    }

    /**
     * 针对MIUI8读取流量数据失败，直接读取系统文件
     */
    private static long getTrafficUsedByFile(int uid) {
        long receive = -1;
        File receiveFile = new File(String.format("/proc/uid_stat/%s/tcp_rcv", uid));
        BufferedReader buffer = null;
        if (receiveFile.exists()) {
            try {
                buffer = new BufferedReader(new FileReader(receiveFile));
                receive = Long.valueOf(buffer.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long send = -1;
        File sendFile = new File(String.format("/proc/uid_stat/%s/tcp_snd", String.valueOf(uid)));
        if (sendFile.exists()) {
            try {
                buffer = new BufferedReader(new FileReader(sendFile));
                send = Long.valueOf(buffer.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (receive == -1 && send == -1) {
            return -1;
        }

        receive = (receive == -1) ? 0 : receive;
        send = (send == -1) ? 0 : send;
        return (receive + send) / 1024;
    }

    /**
     * 高版本流量获取
     *
     * @param uid
     * @return
     */
    private static long getTrafficApi25(int uid) {
        long traffic = 0;
        try {
            Method methodGetStatsService = TrafficStats.class.getDeclaredMethod("getStatsService");
            Class classINetworkStatsService = Class.forName("android.net.INetworkStatsService");
            methodGetStatsService.setAccessible(true);
            Object iNetworkStatsService = methodGetStatsService.invoke(null);
            Object netWorkStats = null;
            for (Method method : classINetworkStatsService.getDeclaredMethods()) {
                if ("getDataLayerSnapshotForUid".equals(method.getName())) {
                    method.setAccessible(true);
                    netWorkStats = method.invoke(iNetworkStatsService, uid);
                    break;
                }
            }
            Class classNetWorkStats = Class.forName("android.net.NetworkStats");
            Field[] fields = classNetWorkStats.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                if ("rxBytes".equals(f.getName())) {
                    long[] rxBytes = (long[]) f.get(netWorkStats);
                    for (int i = 0; i < rxBytes.length; i++) {
                        traffic = traffic + rxBytes[i];
                    }
                } else if ("txBytes".equals(f.getName())) {
                    long[] txBytes = (long[]) f.get(netWorkStats);
                    for (int i = 0; i < txBytes.length; i++) {
                        traffic = traffic + txBytes[i];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            traffic = -1;
        }
        return traffic / 1024;
    }

    public static String getChannel() {
        try {
            if (App.sContext != null) {
                Properties p = getAssetsProperties(App.sContext, "appConfig");
                if (p != null) {
                    String channel = p.getProperty("channelId");
                    if (channel != null && !TextUtils.equals(channel, "default_channel")) {
                        return channel;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return BuildConfig.CHANNEL;
    }

    public static Properties getAssetsProperties(Context c, String name) {
        Properties props = new Properties();
        try {
            InputStream in = c.getAssets().open(name);
            //方法二：通过class获取setting.properties的FileInputStream
            //InputStream in = PropertiesUtill.class.getResourceAsStream("/assets/  setting.properties "));
            props.load(in);
            return props;
        } catch (Throwable e1) {
            e1.printStackTrace();
        }
        return null;
    }

    //是否是亮屏
    public static boolean isScreeOn() {
        if (App.sContext == null) {
            return true;
        }
        try {
            PowerManager powerManager = (PowerManager) App.sContext
                    .getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isScreenOn();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 获取屏幕像素密度
     *
     * @return
     */
    private float getDensity() {
        if (App.sContext == null) {
            return 0;
        }
        DisplayMetrics dm = App.sContext.getResources().getDisplayMetrics();
        if (dm != null) {
            return dm.density;
        }
        return 0;
    }


    /**
     * 获取mac 带:
     *
     * @return
     */
    public static String getMacAd(Context context) {
        if (context == null) {
            return "";
        }
        SpUtils spUtils = new SpUtils(Constant.DEVICE_DATA, context);
        String mac = spUtils.getString(StatisticConstant.SpfKey.MAC_AD, "");
        try {
            if (TextUtils.isEmpty(mac)) {
                WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    WifiInfo info = wifi.getConnectionInfo();
                    if (info != null) {
                        mac = info.getMacAddress();
                    }
                }
                if (mac != null) {
                    mac = mac.replaceAll("\\.", "").toLowerCase(Locale.US);
                    mac = mac.trim();
                    spUtils.putStringByCommit(StatisticConstant.SpfKey.MAC_AD, mac);
                } else {
                    mac = "";
                }
            }
        } catch (Exception e) {
            mac = "";
        }

        if ("02:00:00:00:00:00".equals(mac)) {
            mac = getMacByNetworkInterfaceAd();
        }
        return mac == null ? "" : mac.trim();
    }


    /**
     * 功能：android 6.0获取mac地址 带 :
     */
    private static String getMacByNetworkInterfaceAd() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            SpUtils spUtils = new SpUtils(Constant.DEVICE_DATA, App.sContext);
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iF = interfaces.nextElement();

                if (!"wlan0".equals(iF.getName())) {
                    continue;
                }

                byte[] addr = iF.getHardwareAddress();
                if (addr == null || addr.length == 0) {
                    continue;
                }

                StringBuilder buf = new StringBuilder();
                for (byte b : addr) {
                    buf.append(String.format("%02X", b));
                }
                String mac = buf.toString().toLowerCase(Locale.US).trim();
                if (!TextUtils.isEmpty(mac)) {
                    spUtils.putString(StatisticConstant.SpfKey.MAC_AD, mac);
                }
                return mac;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
