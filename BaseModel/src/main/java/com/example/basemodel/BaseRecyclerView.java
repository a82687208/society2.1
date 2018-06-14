package com.example.basemodel;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.EdgeEffect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/2/8/008.
 */

public class BaseRecyclerView extends RecyclerView {
    private int colorUI;

    public BaseRecyclerView(Context context) {
        this(context, null);
    }

    public BaseRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(new BaseContextWrapper(context,
                Value.getColorUI().toValue()), attrs, defStyle);
        colorUI = Value.getColorUI().toValue();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setEdgeEffectColor(colorUI);
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        setEdgeEffectColor(colorUI);
    }

    public void setEdgeEffectColor(int edgeEffectColor) {
        colorUI = edgeEffectColor;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((BaseContextWrapper) getContext()).setEdgeEffectColor(edgeEffectColor);
        else {
            Class<?> clazz = RecyclerView.class;
            try {
                Field fieldLeftGlow = clazz.getDeclaredField("mLeftGlow");
                Field fieldTopGlow = clazz.getDeclaredField("mTopGlow");
                Field fieldRightGlow = clazz.getDeclaredField("mRightGlow");
                Field fieldBottomGlow = clazz.getDeclaredField("mBottomGlow");

                fieldLeftGlow.setAccessible(true);
                fieldTopGlow.setAccessible(true);
                fieldRightGlow.setAccessible(true);
                fieldBottomGlow.setAccessible(true);

                Method methodLeftGlow = clazz.getDeclaredMethod("ensureLeftGlow");
                Method methodRightGlow = clazz.getDeclaredMethod("ensureRightGlow");
                Method methodTopGlow = clazz.getDeclaredMethod("ensureTopGlow");
                Method methodBottomGlow = clazz.getDeclaredMethod("ensureBottomGlow");

                methodLeftGlow.setAccessible(true);
                methodRightGlow.setAccessible(true);
                methodTopGlow.setAccessible(true);
                methodBottomGlow.setAccessible(true);

                methodLeftGlow.invoke(this);
                methodRightGlow.invoke(this);
                methodTopGlow.invoke(this);
                methodBottomGlow.invoke(this);

                initEdgeEffect((EdgeEffect) fieldLeftGlow.get(this), false);
                initEdgeEffect((EdgeEffect) fieldTopGlow.get(this), true);
                initEdgeEffect((EdgeEffect) fieldRightGlow.get(this), false);
                initEdgeEffect((EdgeEffect) fieldBottomGlow.get(this), true);

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initEdgeEffect(EdgeEffect edgeEffect, boolean isVertical) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getClipToPadding()) {
                if (isVertical)
                    edgeEffect.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
                else
                    edgeEffect.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            }
            else {
                if (isVertical)
                    edgeEffect.setSize(getMeasuredWidth(), getMeasuredHeight());
                else
                    edgeEffect.setSize(getMeasuredHeight(), getMeasuredWidth());
            }

            edgeEffect.setColor(colorUI);
        }
    }

}
