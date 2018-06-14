package com.gdptc.society.ui.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

/**
 * 修复全屏沉浸状态下adjustResize属性失效问题
 * Created by Administrator on 2018/1/3/003.
 */

public class FixImmersedRelativeLayout extends RelativeLayout {

    public FixImmersedRelativeLayout(Context context) {
        this(context, null);
    }

    public FixImmersedRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixImmersedRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFitsSystemWindows(true);
    }

    @Override
    public WindowInsets computeSystemWindowInsets(WindowInsets in, Rect outLocalInsets) {
        outLocalInsets.left = 0;
        outLocalInsets.top = 0;
        outLocalInsets.right = 0;

        return super.computeSystemWindowInsets(in, outLocalInsets);
    }

    @Override
    protected final boolean fitSystemWindows(@NonNull Rect insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            insets.left = 0;
            insets.top = 0;
            insets.right = 0;
        }

        return super.fitSystemWindows(insets);
    }
}
