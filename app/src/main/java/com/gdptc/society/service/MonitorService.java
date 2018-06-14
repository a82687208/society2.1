package com.gdptc.society.service;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.activityManager;

/**
 * 利用JobScheduler机制拉起PushService
 * Created by Administrator on 2017/4/21/021.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MonitorService extends JobService {
    private boolean online = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        List<ActivityManager.RunningServiceInfo> infos = activityManager.getRunningServices(99); //最大值

        for(ActivityManager.RunningServiceInfo info : infos)
            if(info.service.getClassName().equals(CoreService.class.getName()))
                online = true;

        if (!online) {
            Intent intent = new Intent();
            intent.setClass(MonitorService.this, CoreService.class);
            startService(intent);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobFinished(params, false);
        return false;
    }
}
