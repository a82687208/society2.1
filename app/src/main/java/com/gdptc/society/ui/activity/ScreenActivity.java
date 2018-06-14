package com.gdptc.society.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.gdptc.society.Public;

/**
 * 该Activity只起到降低ADJ优先值作用，不参与UI界面设计
 * Created by Administrator on 2017/4/25/025.
 */

public class ScreenActivity extends Activity {
    private MyReceiver myReceiver = null;

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.e(ScreenActivity.class.getSimpleName(), "finish");
            sendBroadcast(new Intent(getPackageName() + Public.HIDE));
            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);
        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));        //亮屏时关闭该Activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }
}
