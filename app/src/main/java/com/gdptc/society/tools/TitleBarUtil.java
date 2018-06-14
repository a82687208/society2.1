package com.gdptc.society.tools;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.basemodel.Value;
import com.example.tools.MaterialDesignCompat;
import com.gdptc.society.R;
import com.gdptc.society.manager.ApplicationManager;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2017/8/26/026.
 */

public class TitleBarUtil implements ApplicationManager.UIChangeCallBack {
    private View target;
    private AppCompatActivity activity;
    private String title;
    private Window window;
    private WindowManager.LayoutParams layoutParams;
    private int systemBarHeight;
    private int navigationBarHeight;

    private boolean shouldChangeNavigation = false;
    private boolean shouldSetNavigation = false;
    private boolean systemBarColorChange = false;
    private boolean shadow = true;
    private boolean firstLoad = true;
    private boolean init = false;

    public TitleBarUtil(AppCompatActivity activity) {
        this(activity, false);
    }

    public TitleBarUtil(AppCompatActivity activity, boolean shouldChangeNavigation) {
        this(activity, shouldChangeNavigation, false);
    }

    public TitleBarUtil(AppCompatActivity activity, boolean shouldChangeNavigation, boolean shouldSetNavigation) {
        this(activity, (Toolbar) activity.findViewById(R.id.toolbar_bar),
                null, shouldChangeNavigation, shouldSetNavigation);
    }

    public TitleBarUtil(AppCompatActivity activity, View target, String title, boolean shouldChangeNavigation, boolean shouldSetNavigation) {
        this.activity = activity;
        this.target = target;
        this.title = title;
        this.shouldChangeNavigation = shouldChangeNavigation;
        this.shouldSetNavigation = shouldSetNavigation;
        ((ApplicationManager) activity.getApplication()).addCallBack(this);
    }

    public TitleBarUtil setTarget(View target) {
        if (!init)
            this.target = target;
        return this;
    }

    public TitleBarUtil setTitle(String title) {
        if (!init)
            this.title = title;
        return this;
    }

    public TitleBarUtil disableShadow() {
        shadow = false;
        return this;
    }

    public void init() {
        init(true);
    }

    public void init(boolean applyTheme) {
        if (!init) {
            init = true;
            if (target instanceof Toolbar)
                activity.setSupportActionBar((Toolbar) target);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && shouldChangeNavigation)
                    shouldChangeNavigation = false;
                window = activity.getWindow();
                layoutParams = window.getAttributes();
                layoutParams.flags = ((shouldChangeNavigation ?
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION : WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                        | layoutParams.flags);
                activity.runOnUiThread(new Runnable() {
                    @SuppressLint("NewApi")
                    @Override
                    public void run() {
                        android.content.res.Resources resources = activity.getResources();
                        DisplayMetrics metrics = new DisplayMetrics();
                        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                        int[] sc = new int[2];
                        target.getLocationOnScreen(sc);      //为了多机型适配性，采用控件在屏幕上的坐标进行判断

                        if (sc[1] == 0) {                                                                             //判断是否成功沉浸
                            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
                            if (resourceId > 0)
                                systemBarHeight = resources.getDimensionPixelSize(resourceId);
                            else
                                systemBarHeight = (int) TypedValueUtil.dip2px(activity, 25);                          //默认系统状态栏25dp

                            if (shouldChangeNavigation && isNavigationBarShow()) {                                   //判断是否显示虚拟按键
                                int id = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                                navigationBarHeight = resources.getDimensionPixelSize(id);
                                ((ViewGroup) activity.findViewById(android.R.id.content))
                                        .getChildAt(0).setPadding(0, 0, 0, navigationBarHeight);
                            }

                            if (target != null && target instanceof Toolbar) {
                                ViewGroup.LayoutParams layoutParams = target.getLayoutParams();
                                layoutParams.height += systemBarHeight;
                                float dpx = TypedValueUtil.dip2px(activity, 10);
                                target.setPadding(0, (int) (systemBarHeight / 2 + dpx), 0, 0);
                            }
                        }
                    }
                });
            }
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                if (shouldSetNavigation) {
                    actionBar.setHomeButtonEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                actionBar.setDisplayShowTitleEnabled(false);
            }
            if (target != null && target instanceof Toolbar) {
                Toolbar toolbar = (Toolbar) target;
                ((TextView) toolbar.findViewById(R.id.tv_toolbar_title)).setText(title);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.onBackPressed();
                    }
                });
                if (applyTheme) {
                    if (shadow) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                            MaterialDesignCompat.addShadow(toolbar, 0, (int) TypedValueUtil.dip2px(target.getContext(), 1), -1, true);
                    }
                    else
                        ViewCompat.setElevation(toolbar, 0f);
                    onUIColorChange(Value.getColorUI());
                }
            }

            firstLoad = false;
        }
    }

    public int getSystemBarHeight() {
        return systemBarHeight;
    }

    public int getNavigationBarHeight() {
        return navigationBarHeight;
    }

    public int getTargetHeight() {
        if (target.getMeasuredHeight() == 0 && target.getMeasuredWidth() == 0)
            target.measure(0, 0);
        return target.getMeasuredHeight();
    }

    public int getTargetWidth() {
        if (target.getMeasuredHeight() == 0 && target.getMeasuredWidth() == 0)
            target.measure(0, 0);
        return target.getMeasuredWidth();
    }

    public TitleBarUtil changeSystemIconColor() {
        if (Build.BRAND.equals("Xiaomi")) {
            Class<? extends Window> clazz = window.getClass();
            try {
                Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                java.lang.reflect.Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                int darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                extraFlagField.invoke(activity.getWindow(), systemBarColorChange ? 0 : darkModeFlag, darkModeFlag);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = activity.getWindow().getDecorView();
            int ui = decor.getSystemUiVisibility();
            if (!systemBarColorChange)
                ui |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else
                ui ^= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(ui);
        }
        else if (firstLoad)
            shouldChangeNavigation = false;
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            layoutParams.flags ^= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            window.setAttributes(layoutParams);
        }

        systemBarColorChange = !systemBarColorChange;
        return this;
    }

    private boolean isNavigationBarShow(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);

            return realSize.y != size.y;
        }
        else {
            boolean menu = ViewConfiguration.get(activity).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

            return (menu || back);
        }
    }

    @Override
    public void onUIColorChange(Value.COLOR color) {
        if (target instanceof Toolbar) {
            Toolbar toolbar = (Toolbar) target;
            switch (color) {
                case BLUE: default:
                    toolbar.setBackgroundResource(R.drawable.title_bar_blue);
                    break;
                case PINK:
                    toolbar.setBackgroundResource(R.drawable.title_bar_pink);
                    break;
                case GREEN:
                    toolbar.setBackgroundResource(R.drawable.title_bar_green);
                    break;
            }
        }
    }

}
