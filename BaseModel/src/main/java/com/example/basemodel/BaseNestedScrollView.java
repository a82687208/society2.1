package com.example.basemodel;

import android.content.Context;
import android.os.Build;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.widget.EdgeEffect;
import android.widget.ScrollView;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018/3/28/028.
 */

public class BaseNestedScrollView extends NestedScrollView {

    public BaseNestedScrollView(Context context) {
        this(context, null);
    }

    public BaseNestedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(new BaseContextWrapper(context,
                Value.getColorUI().toValue()), attrs, defStyleAttr);
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = NestedScrollView.class;
            try {
                Field fieldEdgeGlowTop = clazz.getDeclaredField("mEdgeGlowTop");
                Field fieldEdgeGlowBottom = clazz.getDeclaredField("mEdgeGlowBottom");
                fieldEdgeGlowTop.setAccessible(true);
                fieldEdgeGlowBottom.setAccessible(true);
                EdgeEffect mEdgeGlowTop = (EdgeEffect) fieldEdgeGlowTop.get(this);
                EdgeEffect mEdgeGlowBottom = (EdgeEffect) fieldEdgeGlowBottom.get(this);
                mEdgeGlowTop.setColor(edgeEffectColor);
                mEdgeGlowBottom.setColor(edgeEffectColor);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
