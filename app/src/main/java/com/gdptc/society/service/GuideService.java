package com.gdptc.society.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.gdptc.society.base.BaseService;

/**
 * Created by Administrator on 2017/3/27/027.
 */

public class GuideService extends BaseService {
    private static Notification a = new Notification();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void bindNotification(Service context) {
        context.startForeground(1, a);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindNotification(this);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
}
