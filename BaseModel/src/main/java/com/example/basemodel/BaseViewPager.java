package com.example.basemodel;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.EdgeEffect;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018/2/8/008.
 */

public class BaseViewPager extends ViewPager {

    public BaseViewPager(Context context) {
        this(context, null);
    }

    public BaseViewPager(Context context, AttributeSet attrs) {
        super(new BaseContextWrapper(context,
                Value.getColorUI().toValue()), attrs);
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = ViewPager.class;
            try {
                Field fieldLeftEdge = clazz.getDeclaredField("mLeftEdge");
                Field fieldRightEdge = clazz.getDeclaredField("mRightEdge");
                fieldLeftEdge.setAccessible(true);
                fieldRightEdge.setAccessible(true);
                EdgeEffect mLeftEdge = (EdgeEffect) fieldLeftEdge.get(this);
                EdgeEffect mRightEdge = (EdgeEffect) fieldRightEdge.get(this);
                mLeftEdge.setColor(edgeEffectColor);
                mRightEdge.setColor(edgeEffectColor);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
