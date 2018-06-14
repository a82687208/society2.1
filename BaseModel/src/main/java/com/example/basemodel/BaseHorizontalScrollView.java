package com.example.basemodel;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.EdgeEffect;
import android.widget.HorizontalScrollView;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018/3/8/008.
 */

public class BaseHorizontalScrollView extends HorizontalScrollView {
    public BaseHorizontalScrollView(Context context) {
        this(context, null);
    }

    public BaseHorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(new BaseContextWrapper(context), attrs, defStyleAttr);
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = HorizontalScrollView.class;
            try {
                Field fieldEdgeGlowLeft = clazz.getDeclaredField("mEdgeGlowLeft");
                Field fieldEdgeGlowRight = clazz.getDeclaredField("mEdgeGlowRight");
                fieldEdgeGlowLeft.setAccessible(true);
                fieldEdgeGlowRight.setAccessible(true);
                EdgeEffect mEdgeGlowLeft = (EdgeEffect) fieldEdgeGlowLeft.get(this);
                EdgeEffect mEdgeGlowRight = (EdgeEffect) fieldEdgeGlowRight.get(this);
                mEdgeGlowLeft.setColor(edgeEffectColor);
                mEdgeGlowRight.setColor(edgeEffectColor);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
