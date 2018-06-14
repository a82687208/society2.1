package com.example.tools;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.widget.MaterialRippleLayout;
import com.example.widget.ShadowLayout;

/**
 * Created by Administrator on 2018/2/1/001.
 */

public class MaterialDesignCompat {
    public static final int DEFAULT_COLOR = 0x10000000;

    public static View addShadow(View child, float elevation) {
        return addShadow(child, 0, 0, elevation, true, true, true, true);
    }

    public static View addShadow(View child, int dx, int dy, float elevation) {
        if (child instanceof Button)
            return addShadow(child, dx, dy, elevation, true, true, true, true);

        return addShadow(child, dx, dy, elevation, false, false, false, true);
    }

    public static View addShadow(View child, int dx, int dy, float elevation, boolean justDrawBottom) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ShadowLayout shadowLayout;
            if (child instanceof Button)
                shadowLayout = (ShadowLayout) addShadow(child, dx, dy, elevation, true, true, true, true);
            else
                shadowLayout = (ShadowLayout) addShadow(child, dx, dy, elevation, false, false, false, true);

            shadowLayout.enableRadiusPadding(!justDrawBottom);
            return shadowLayout;
        }
        else
            return addShadow(child, dx, dy, elevation, false, false, false, true);
    }

    public static View addShadow(View child, int dx, int dy, float elevation, boolean paddingLeft,
                                    boolean paddingTop, boolean paddingRight, boolean paddingBottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            child.setTranslationZ(dy);
            if (elevation != -1)
                child.setElevation(elevation);
            return child;
        }
        else {
            ViewGroup.MarginLayoutParams childParams = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            ShadowLayout shadowLayout = new ShadowLayout(child.getContext());
            shadowLayout.setShadowOffset(dx, dy, paddingLeft, paddingTop, paddingRight, paddingBottom);
            if (elevation != -1)
                shadowLayout.setShadowRadius(elevation);
            ViewGroup parent = (ViewGroup) child.getParent();
            int index = 0;
            if (parent != null) {
                index = parent.indexOfChild(child);
                parent.removeView(child);
            }
            shadowLayout.setId(child.getId());
            shadowLayout.addView(child, new ViewGroup.LayoutParams(-1, -1));
            if (parent != null)
                parent.addView(shadowLayout, index, childParams);
            return shadowLayout;
        }
    }

    public static View addRipple(View view, int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return MaterialRippleLayout.on(view)
                    .rippleColor(color)
                    .rippleAlpha(0.2f)
                    .rippleHover(true)
                    .rippleOverlay(true)
                    .create();
        }
        else
            return view;
    }
}
