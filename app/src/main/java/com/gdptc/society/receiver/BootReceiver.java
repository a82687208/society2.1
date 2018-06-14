package com.gdptc.society.receiver;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.manager.DirectoryManager;
import com.gdptc.society.service.CoreService;
import com.gdptc.society.service.MonitorService;

/**
 * Created by Administrator on 2018/3/17/017.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences appSharedPreferences = context.getSharedPreferences(DirectoryManager.CFG_APP, Context.MODE_PRIVATE);
        SharedPreferences infoSharedPreferences = context.getSharedPreferences(DirectoryManager.CFG_INFO, Context.MODE_PRIVATE);
        if (appSharedPreferences.getBoolean(DirectoryManager.APP_BOOT, true)
                && infoSharedPreferences.getString(DirectoryManager.INFO_PHONE, null) != null
                && infoSharedPreferences.getString(DirectoryManager.INFO_PSW, null) != null) {
            Intent service = new Intent();
            service.setClass(context, CoreService.class);
            context.startService(service);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(context.getPackageName(), MonitorService.class.getName()));
                builder.setPeriodic(ApplicationManager.JOB_TIME);
                jobScheduler.schedule(builder.build());
            }
        }
    }
}
