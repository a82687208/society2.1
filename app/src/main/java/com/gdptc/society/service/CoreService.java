package com.gdptc.society.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.AccountInfo;
import com.gdptc.society.apiServer.AidlService;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseService;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.manager.DirectoryManager;
import com.gdptc.society.receiver.ScreenReceiver;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.SecurityUtil;
import com.gdptc.society.tools.TimeUtil;
import com.gdptc.society.ui.activity.FirstActivity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import static com.gdptc.society.manager.ApplicationManager.JOB_TIME;


/**
 * Created by Administrator on 2018/1/2/002.
 */

public class CoreService extends BaseService {
    private ServiceBinder serviceBinder = new ServiceBinder();

    private SecurityUtil securityUtil;
    private Intent intent;
    private Handler handler;

    private NotificationManager notificationManager;

    private String appName;
    private Bitmap bmIcon;

    private ScreenReceiver screenReceiver;
    private AlarmManager alarmManager;

    private static AccountInfo accountInfo;

    private PendingIntent pushBookIntent;
    private long pushTime = 1000 * 60 * 1;         //30分钟推送一次书籍

    private boolean tryLogin = false;

    private TimeUtil timeUtil;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder.asBinder();
    }

    private BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + Public.ERROR)) {
                Toast.makeText(context, "很抱歉，超级图书馆异常正在重启，请稍后..", Toast.LENGTH_LONG).show();
                final String err = intent.getStringExtra(Public.ERROR);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DBManager.getInstance().uploadException(Build.BRAND, Build.MODEL,
                                    Build.VERSION.RELEASE, Build.MANUFACTURER, err);
                        }
                        catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                int id = intent.getIntExtra(Public.ID, -1);
                if (id != -1)
                    Process.killProcess(id);
                Intent app = new Intent(context, FirstActivity.class);
                app.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(app);
            }
        }
    };

    private BroadcastReceiver exitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + Public.EXIT)) {
                if (Build.VERSION.SDK_INT >= 21) {
                    JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.cancelAll();
                }
                alarmManager.cancel(pushBookIntent);
                notificationManager.cancelAll();
                DBManager.getInstance().close();
                Process.killProcess(Process.myPid());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        timeUtil = new TimeUtil();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Resources resources = getResources();
        appName = resources.getString(R.string.app_name);
        bmIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher);
        handler = new Handler();
        screenReceiver = new ScreenReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, intentFilter);
        registerReceiver(exitReceiver, new IntentFilter(getPackageName() + Public.EXIT), ApplicationManager.RECEIVER_PERMISSION, null);
        registerReceiver(errorReceiver, new IntentFilter(getPackageName() + Public.ERROR), ApplicationManager.RECEIVER_PERMISSION, null);
        GuideService.bindNotification(this);
        Intent intent = new Intent(this, GuideService.class);
        startService(intent);

        if (Build.VERSION.SDK_INT >= 21) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(getPackageName(), MonitorService.class.getName()));
            builder.setPeriodic(JOB_TIME);
            jobScheduler.schedule(builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPreferences = getSharedPreferences(DirectoryManager.CFG_APP, MODE_PRIVATE);
        this.intent = intent;
        handler.postDelayed(rubLoadAccount, 2000);
        return START_NOT_STICKY;
    }

    private Runnable rubLoadAccount = new Runnable() {
        @Override
        public void run() {
            if (accountInfo == null && !tryLogin) {
                Log.e(CoreService.class.getSimpleName(), "startLogin");
                securityUtil = new SecurityUtil();
                String phone, psw;
                SharedPreferences sharedPreferences = getSharedPreferences(DirectoryManager.CFG_INFO, MODE_PRIVATE);
                phone = sharedPreferences.getString(DirectoryManager.INFO_PHONE, null);
                psw = sharedPreferences.getString(DirectoryManager.INFO_PSW, null);

                if (phone != null && psw != null) {
                    try {
                        psw = securityUtil.AesDesEncrypt(psw);
                        tryLogin = true;
                        //ApiServer.getInstance().login(phone, psw, lgCallBack);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private class ServiceBinder extends AidlService.Stub {
        @Override
        public void bindAccountInfo(AccountInfo accountInfo) throws RemoteException {
            Log.e(CoreService.class.getSimpleName(), "bindSuccess");
            CoreService.accountInfo = accountInfo;
        }

        @Override
        public IBinder asBinder() {
            return new ServiceBinder();
        }
    }

    private void buildNotification(Context context, String title, String msg, Intent intent, int id, Bitmap largeIcon,
                                   int num, ContentValues contentValues) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(largeIcon);
        builder.setContentTitle(title);
        builder.setContentText(msg);
        builder.setNumber(num);
        builder.setTicker(msg);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (intent != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        builder.setOngoing(false);
        builder.setWhen(System.currentTimeMillis());
        //builder.setFullScreenIntent(null, false);
        Intent post = new Intent(context.getPackageName() + Public.POST);
        post.putExtra(Public.DATA, contentValues);
        context.sendBroadcast(post, ApplicationManager.RECEIVER_PERMISSION);
        Notification notification = builder.build();
        notificationManager.notify(id, notification);
    }

    private class Info {
        Object info;
        Object callBack;

        Info(Object info, Object callBack) {
            this.info = info;
            this.callBack = callBack;
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
}
