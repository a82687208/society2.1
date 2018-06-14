package com.gdptc.society.tools;

import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * Created by Administrator on 2018/1/28/028.
 */

public class BitmapInfo {
    HashSet<WeakReference<Object>> parentReference;
    WeakReference<FragmentActivity> activity;
    Bitmap bitmap;
    int sourceHeight;
    int sourceWidth;
    int scale;
    Object id;

    public BitmapInfo() {
        parentReference = new HashSet<>();
    }

    BitmapInfo (Object id, Bitmap bitmap, int sourceWidth, int sourceHeight, int scale) {
        this();
        this.id = id;
        this.bitmap = bitmap;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    public int getSourceHeight() {
        return sourceHeight;
    }

    public int getSourceWidth() {
        return sourceWidth;
    }

    public Bitmap getBitmap(FragmentActivity activity, Object parent) {
        this.activity = new WeakReference<>(activity);
        parentReference.add(new WeakReference<>(parent));
        return bitmap;
    }

}