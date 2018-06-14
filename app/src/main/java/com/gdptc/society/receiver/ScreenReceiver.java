package com.gdptc.society.receiver;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.gdptc.society.service.CoreService;
import com.gdptc.society.service.MonitorService;
import com.gdptc.society.ui.activity.ScreenActivity;

import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.JOB_TIME;
import static com.gdptc.society.manager.ApplicationManager.activityManager;

/**
 * Created by Administrator on 2017/4/25/025.
 */

public class ScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = false;

        List<ActivityManager.RunningServiceInfo> infos = activityManager.getRunningServices(99); //最大值

        for(ActivityManager.RunningServiceInfo info : infos)
            if(info.service.getClassName().equals(CoreService.class.getName()))
                online = true;

        if (!online) {
            Intent service = new Intent();
            service.setClass(context, CoreService.class);
            context.startService(service);
            if (Build.VERSION.SDK_INT >= 21) {          //利用JobScheduler机制进行循环检查
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(context.getPackageName(), MonitorService.class.getName()));
                builder.setPeriodic(JOB_TIME);
                jobScheduler.schedule(builder.build());
            }
        }
        Log.e(ScreenReceiver.class.getSimpleName(), "off");
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {          //在关屏时启动全透明Activity保持前台状态，使ADJ降到应用最小值0
            //因为自身不采用多服务模式，主界面采用后台不退出，在另一进程启用本界面时将会把主界面拉起
            //而透明Activity所在的进程即推送进程的ADJ值将会被降低至0，主界面的进程将会降低至1
            Intent activity = new Intent();
            activity.setClass(context, ScreenActivity.class);
            activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activity);
        }
    }
}
