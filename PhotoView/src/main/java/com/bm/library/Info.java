package com.bm.library;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by liuheng on 2015/8/19.
 */
public class Info {
    // 内部图片在整个手机界面的位置
    public final RectF mRect = new RectF();

    // 控件在窗口的位置
    public final RectF mImgRect = new RectF();

    public final RectF mWidgetRect = new RectF();

    public final RectF mBaseRect = new RectF();

    public final PointF mScreenCenter = new PointF();

    public float mScale;

    public float mDegrees;

    public ImageView.ScaleType mScaleType;

    public Info(RectF rect, RectF img, RectF widget, RectF base, PointF screenCenter, float scale, float degrees, ImageView.ScaleType scaleType) {
        mRect.set(rect);
        mImgRect.set(img);
        mWidgetRect.set(widget);
        mScale = scale;
        mScaleType = scaleType;
        mDegrees = degrees;
        mBaseRect.set(base);
        mScreenCenter.set(screenCenter);
    }
}
