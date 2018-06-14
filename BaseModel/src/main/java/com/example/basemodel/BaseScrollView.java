package com.example.basemodel;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.EdgeEffect;
import android.widget.ScrollView;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2018/2/8/008.
 */

public class BaseScrollView extends ScrollView {
    private OnScrollChangedListener onScrollChangedListener;

    public interface OnScrollChangedListener {
        void onScrollChanged(int x, int y);
    }

    public BaseScrollView(Context context) {
        this(context, null);
    }

    public BaseScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(new BaseContextWrapper(context,
                Value.getColorUI().toValue()), attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollChangedListener != null)
            onScrollChangedListener.onScrollChanged(l, t);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = ScrollView.class;
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
