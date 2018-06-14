package com.example.basemodel;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.GridView;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018/2/8/008.
 */

public class BaseGridView extends GridView {
    public BaseGridView(Context context) {
        this(context, null);
    }

    public BaseGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(new BaseContextWrapper(context,
                Value.getColorUI().toValue()), attrs, defStyleAttr);
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = AbsListView.class;
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
