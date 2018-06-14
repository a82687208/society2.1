package com.gdptc.society.manager;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;

/**
 * Created by Administrator on 2017/5/16/016.
 */

public class MemoryManager {
    private boolean readyGC = false;
    private ActivityManager activityManager;

    MemoryManager(ActivityManager activityManager) {
        this.activityManager = activityManager;
    }

    public long getMaxMemory() {
        return activityManager.getMemoryClass() * 1024;         //采用KB单位
    }

    //返回单位字节
    public long getImgMemoryUse(int width, int height, int bit) {
        return width * height * (bit / 8);
    }

    //返回单位KB
    public long getProcessFreeMemory() {
        long total = getMaxMemory() - getProcessMemoryUse();

        //如果剩余内存小于一张该手机的分辨率的图片所占的内存大小，ARGB_8888=32bit
        if (total <= getImgMemoryUse(displayMetrics.widthPixels, displayMetrics.heightPixels, 32))
            gcNow();
        total = getMaxMemory() - getProcessMemoryUse();

        return Math.abs(total);
    }

    public long getProcessMemoryUse() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.getTotalPrivateDirty() + (Debug.getNativeHeapAllocatedSize() / 1024);  //memoryUse返回原本就为KB单位
    }

    public long getSystemTotalMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return Build.VERSION.SDK_INT >= 16 ? memoryInfo.totalMem / 1024 / 1024 : 0;
    }

    public long getAvailMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem / 1024 / 1024;
    }

    public void callGC() {
        if (readyGC)
            return;
        readyGC = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gcNow();
                readyGC = false;
            }
        }).start();
    }

    private void gcNow() {
        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();
    }
}
