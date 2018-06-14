package com.gdptc.society.base;

import android.app.Service;

import com.gdptc.society.manager.ApplicationManager;

/**
 * Created by Administrator on 2017/12/9/009.
 */

public abstract class BaseService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        ((ApplicationManager) getApplication()).addService(this);
    }

    @Override
    public void onDestroy() {
        ((ApplicationManager) getApplication()).removeService(this);
        super.onDestroy();
    }
}
