package com.gdptc.society.base;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.transition.Transition;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.Visibility;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.example.basemodel.Value;
import com.gdptc.society.R;
import com.gdptc.society.manager.ApplicationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/12/9/009.
 */

public abstract class BaseActivity extends AppCompatActivity implements ApplicationManager.UIChangeCallBack {
    private GestureDetector mGestureDetector;
    private boolean mScrollFinish = true;
    private boolean mInterception = false;
    private float x, y;

    private boolean isFinish = false;
    private boolean transition = true;

    private List<View> mFilterViews;
    private List<PeriodicMonitoring> mMonitoring;

    public static abstract class PeriodicMonitoring {
        public void onStart() {}
        public void onReCreate() {}
        public void onResume() {}
        public void onReStart() {}
        public void onStop() {}
        public void finish() {}
        public void onDestroy() {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ApplicationManager) getApplication()).addActivity(this);
        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);
        mFilterViews = new ArrayList<>();
        mMonitoring = new ArrayList<>();
    }

    @Override
    public void recreate() {
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onReCreate();
        super.recreate();
    }

    @Override
    protected void onResume() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onResume();

        super.onResume();
    }

    @Override
    protected void onStart() {
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onStart();
        super.onStart();
    }

    @Override
    protected void onRestart() {
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onReStart();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onStop();
        super.onStop();
    }

    @Override
    public void finish() {
        if (!isFinish) {
            for (PeriodicMonitoring monitoring : mMonitoring)
                monitoring.finish();
            isFinish = true;
            mInterception = true;
            super.finish();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                overridePendingTransition(R.anim.finish_zoomin, R.anim.finish_zoomout);
            ((ApplicationManager) getApplication()).removeActivity(this);
            if (mFilterViews != null)
                mFilterViews.clear();
            mFilterViews = null;
            mGestureDetector = null;
        }
    }

    @Override
    protected void onDestroy() {
        for (PeriodicMonitoring monitoring : mMonitoring)
            monitoring.onDestroy();
        mMonitoring.clear();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mInterception || mGestureDetector == null)
            return true;
        else if (mScrollFinish && !isMoveInFilterView(ev) && mGestureDetector.onTouchEvent(ev)) {
            mInterception = true;
            onBackPressed();
            return true;
        }
        else
            return super.dispatchTouchEvent(ev);
    }

    public void resetInterception() {
        mInterception = false;
    }

    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return distanceX < -20 && distanceY < 10 && distanceY > -10;
        }
    };

    protected void enabledScrollFinish(boolean enable) {
        mScrollFinish = enable;
    }

    public void addFilterView(View view) {
        mFilterViews.add(view);
    }

    private boolean isMoveInFilterView(MotionEvent ev) {
        if (mFilterViews != null && mFilterViews.size() > 0) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = ev.getX();
                    y = ev.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    for (View view : mFilterViews) {
                        if (view.getVisibility() == View.VISIBLE) {
                            int[] location = new int[2];
                            view.getLocationInWindow(location);

                            if (x >= location[0] && x <= location[0] + view.getWidth() &&
                                    y >= location[1] && y <= location[1] + view.getHeight())
                                return true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    x = y = 0;
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    public void registerMonitoring(PeriodicMonitoring monitoring) {
        mMonitoring.add(monitoring);
    }

    public void unRegisterMonitoring(PeriodicMonitoring monitoring) {
        mMonitoring.remove(monitoring);
    }

    @Override
    public void onUIColorChange(Value.COLOR color) {}

    @Override
    public void startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (transition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //安卓6.0 ViewRoot构建流程有BUG
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)     //当前系统版本不是安卓6.0时启用转场特效
                super.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            else
                super.startActivity(intent);
        }
        else if (transition) {
            super.startActivity(intent);
            overridePendingTransition(R.anim.create_zoomin, R.anim.create_zoomout);
        }
        else
            super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (transition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //安卓6.0 ViewRoot构建流程有BUG
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)      //当前系统版本不是安卓6.0时启用转场特效
                super.startActivityForResult(intent, requestCode, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            else
                super.startActivityForResult(intent, requestCode);
        }
        else if (transition) {
            super.startActivityForResult(intent, requestCode);
            overridePendingTransition(R.anim.create_zoomin, R.anim.create_zoomout);
        }
        else
            super.startActivityForResult(intent, requestCode);
    }

    public void disableTransition() {
        transition = false;
    }

    public void enableTransition() {
        transition = true;
    }

}
