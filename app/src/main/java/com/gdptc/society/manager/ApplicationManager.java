package com.gdptc.society.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.example.basemodel.Value;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.base.BaseService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/12/9/009.
 */

public class ApplicationManager extends Application implements Thread.UncaughtExceptionHandler {
    public static DirectoryManager directoryManager;
    public static MemoryManager memoryManager;
    public static ActivityManager activityManager;
    public static TelephonyManager telephonyManager;
    public static DisplayMetrics displayMetrics;

    public static final int JOB_TIME = 15 * 60 * 1000;           //15分钟拉起一次JobService

    private List<BaseActivity> activityList;
    private List<BaseService> serviceList;
    private List<WeakReference<BaseActivity>> outStackActivity;
    private List<WeakReference<BaseService>> outStackService;

    public final Map<String, Object> dataMap = new HashMap<>();
    public static final String RECEIVER_PERMISSION = "com.gdptc.superbook.permission.READ_INFO";

    private List<WeakReference<UIChangeCallBack>> callBackList;
    private boolean pauseActivity = false;
    private Class<? extends Activity> filterActivity;

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
        removeAllActivity();
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        Log.e(ApplicationManager.class.getSimpleName(), writer.toString());
        Intent intent = new Intent(getPackageName() + Public.ERROR);
        intent.putExtra(Public.ERROR, writer.toString());
        intent.putExtra(Public.ID, android.os.Process.myPid());
        sendBroadcast(intent, ApplicationManager.RECEIVER_PERMISSION);
        System.exit(0);
    }

    public interface UIChangeCallBack {
        void onUIColorChange(Value.COLOR color);
    }

    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dsbUIChangeTask(intent.getStringExtra("color"));
        }
    };

    private BroadcastReceiver logOutErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            ApiServer.getInstance().logout(ApplicationManager.this);
//            getSharedPreferences(DirectoryManager.CFG_INFO, MODE_PRIVATE).edit().remove(DirectoryManager.INFO_PSW).commit();
//            Intent login = new Intent(ApplicationManager.this, LoginActivity.class);
//            login.putExtra(Public.ERROR, true);
//            login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(login);
        }
    };

    public void addCallBack(UIChangeCallBack uiChangeCallBack) {
        callBackList.add(new WeakReference<>(uiChangeCallBack));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Thread.setDefaultUncaughtExceptionHandler(this);
        activityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        activityList = new ArrayList<>();
        serviceList = new ArrayList<>();
        outStackActivity = new ArrayList<>();
        outStackService = new ArrayList<>();
        callBackList = new ArrayList<>();

        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();

        int myPid = android.os.Process.myPid();
        String packageName = getPackageName();
        String pushProcess = packageName + getResources().getString(R.string.app_push);

        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (myPid != info.pid)
                continue;

            if (info.processName.equals(packageName)) {
                telephonyManager = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
                directoryManager = new DirectoryManager(this);
                registerReceiver(logOutErrorReceiver, new IntentFilter(getPackageName() + ".logOut"), RECEIVER_PERMISSION, null);
            }
            else if (info.processName.equals(pushProcess)) {
                directoryManager = new DirectoryManager(getBaseContext());
                displayMetrics = getResources().getDisplayMetrics();
                new DBManager(this);
                if (getSharedPreferences(DirectoryManager.CFG_INFO, MODE_PRIVATE)
                        .getBoolean(DirectoryManager.INFO_EXIT, false)) {
                    Process.killProcess(myPid);                                     //暴力阻止百度自救
                }
                return;
            }

            if (!info.processName.equals(packageName))
                directoryManager = new DirectoryManager(this);
            displayMetrics = getResources().getDisplayMetrics();
            memoryManager = new MemoryManager(activityManager);
            new DBManager(this);
            registerReceiver(themeChangeReceiver, new IntentFilter(getPackageName() + ".theme"), RECEIVER_PERMISSION, null);
            SharedPreferences sp = getSharedPreferences(DirectoryManager.CFG_APP, MODE_PRIVATE);
            Value.setColorUI(sp.getString(DirectoryManager.APP_THEME, Value.BLUE));
        }
    }

    public void addActivity(BaseActivity activity) {
        if (pauseActivity && !activity.getClass().equals(filterActivity))
            activity.finish();
        else if (!activityList.contains(activity))
            activityList.add(activity);
    }

    public void removeActivity(BaseActivity activity) {
        activityList.remove(activity);
        outStackActivity.add(new WeakReference<>(activity));
    }

    public void addService(BaseService service) {
        if (!serviceList.contains(service))
            serviceList.add(service);
    }

    public void removeService(BaseService service) {
        serviceList.remove(service);
        outStackService.add(new WeakReference<>(service));
    }

    public void removeAllActivity() {
        while (activityList.size() != 0)
            activityList.remove(0).finish();

        while (outStackActivity.size() != 0) {
            Activity activity = outStackActivity.remove(0).get();
            if (activity != null && !activity.isFinishing())
                activity.finish();
        }

        outStackActivity.clear();
    }

    public void removeAllService() {
        getSharedPreferences(directoryManager.CFG_INFO, MODE_PRIVATE).edit()
                .putBoolean(directoryManager.INFO_EXIT, true).commit();
        Intent intent = new Intent(getPackageName() + Public.EXIT);
        sendBroadcast(intent, RECEIVER_PERMISSION);
        while (serviceList.size() != 0)
            serviceList.remove(0).stopSelf();

        while (outStackService.size() != 0) {
            Service service = outStackService.remove(0).get();
            if (service != null)
                service.stopSelf();
        }

        outStackService.clear();
    }

    public void exit() {
        removeAllActivity();
        removeAllService();
        DBManager.getInstance().close();
        Process.killProcess(Process.myPid());
    }

    public void changeUIColor(String themeName) {
        Intent intent = new Intent(getPackageName() + ".theme");
        intent.putExtra("color", themeName);
        sendBroadcast(intent, RECEIVER_PERMISSION);
    }

    void pauseActivity(Class<? extends Activity> filterActivity) {
        this.filterActivity = filterActivity;
        pauseActivity = true;
    }

    private void dsbUIChangeTask(String themeName) {
        SharedPreferences sp = getSharedPreferences(directoryManager.CFG_APP, MODE_PRIVATE);
        sp.edit().putString(directoryManager.APP_THEME, themeName).commit();

        Value.COLOR color;
        switch (themeName) {
            case Value.BLUE: default:
                color = Value.COLOR.BLUE;
                break;
            case Value.PINK:
                color = Value.COLOR.PINK;
                break;
            case Value.GREEN:
                color = Value.COLOR.GREEN;
                break;
        }

        Value.setColorUI(color);

        for (BaseActivity activity : activityList)
            activity.onUIColorChange(color);

        Iterator<WeakReference<BaseActivity>> iterator = outStackActivity.iterator();
        while (iterator.hasNext()) {
            WeakReference<BaseActivity> weakReference = iterator.next();
            BaseActivity activity = weakReference.get();
            if (activity != null && !activity.isFinishing())
                activity.onUIColorChange(color);
            else
                iterator.remove();
        }

        Iterator<WeakReference<UIChangeCallBack>> callBackIterator = callBackList.iterator();
        while (callBackIterator.hasNext()) {
            WeakReference<UIChangeCallBack> weakReference = callBackIterator.next();
            UIChangeCallBack callBack = weakReference.get();

            if (callBack != null)
                callBack.onUIColorChange(color);
            else
                callBackIterator.remove();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        DBManager.getInstance().close();
        super.finalize();
    }
}
