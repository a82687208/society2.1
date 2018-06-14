package com.gdptc.society.tools;

import android.content.Context;

/**
 * Created by yt on 2017/12/8.
 */

public class TypedValueUtil {
    public static float dip2px(Context context, int dip) {
        return dip * context.getResources().getDisplayMetrics().density;
    }

    public static float px2dip(Context context, int px) {
        return px / context.getResources().getDisplayMetrics().density * (px >= 0 ? 1 : -1);
    }

    public static float sp2px(Context context, float spValue) {
        return spValue * context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static float px2sp(Context context, float pxValue) {
        return pxValue / context.getResources().getDisplayMetrics().scaledDensity;
    }
}
