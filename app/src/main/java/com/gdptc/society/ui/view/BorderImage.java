package com.gdptc.society.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by Administrator on 2017/9/20/020.
 */

public class BorderImage extends AppCompatImageView {
    private Paint mPaint;
    private int borderSize = -1;

    public BorderImage(Context context) {
        this(context, null);
    }

    public BorderImage(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BorderImage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);     //设置空心
    }

    public void setBorderSize(int size) {
        borderSize = size;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect rect = new Rect();
        mPaint.setColor(Color.WHITE);
        if (borderSize == -1)
            borderSize = getWidth() / 20;

        mPaint.setStrokeWidth(borderSize);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(rect, mPaint);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(1);

        rect.set(1, 1, getWidth() + 1, getHeight() + 1);
        canvas.drawRect(rect, mPaint);
    }
}
