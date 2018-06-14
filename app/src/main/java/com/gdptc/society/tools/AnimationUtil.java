package com.gdptc.society.tools;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

/**
 * Created by Administrator on 2017/5/29/029.
 */

public class AnimationUtil {

    public static Animation getTranslateAnimation(float startX, float endX, float startY, float endY,
                                                  int type, int Duration, boolean fillAfter) {
        TranslateAnimation translateAnimation = new TranslateAnimation(type, startX, type, endX,
                type, startY, type, endY);
        translateAnimation.setDuration(Duration);
        translateAnimation.setFillAfter(fillAfter);

        return translateAnimation;
    }

    public static Animation getAlphaAnimation(float fromAlpha, float toAlpha, int Duration, boolean fillAfter) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
        alphaAnimation.setDuration(Duration);
        alphaAnimation.setFillAfter(fillAfter);

        return alphaAnimation;
    }

    public static Animation getScaleAnimation(float fromX, float toX, float fromY, float toY,
                                              float pivotX, float pivotY, int Duration, boolean fillAfter) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(fromX, toX, fromY, toY, pivotX, pivotY);
        scaleAnimation.setDuration(Duration);
        scaleAnimation.setFillAfter(fillAfter);

        return scaleAnimation;
    }

    public static Animation getRotateAnimation(float fromDegrees, float toDegrees, int RepeatCount,
                                               int type, int Duration, boolean fillAfter) {
        RotateAnimation rotateAnimation = new RotateAnimation(fromDegrees, toDegrees,
                type, 0.5f, type, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setRepeatCount(RepeatCount);
        rotateAnimation.setDuration(Duration);
        rotateAnimation.setFillAfter(fillAfter);

        return rotateAnimation;
    }
}
