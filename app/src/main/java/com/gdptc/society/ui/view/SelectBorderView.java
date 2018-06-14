package com.gdptc.society.ui.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Administrator on 2017/9/29/029.
 */

public class SelectBorderView extends View {
    private final int CLICK_LEFTTOP = 0;
    private final int CLICK_RIGHTTOP = 1;
    private final int CLICK_LEFTBOTTOM = 2;
    private final int CLICK_RIGHTBOTTOM = 3;
    private final int CLICK_CONTENT = 4;

    private Paint mPaint;
    private int mSize = 800;
    private int mPointSize = 50;
    private int mMaxWidth = mSize;
    private int mMaxHeight = mSize;
    private int mMaxBottom = 0, mMaxTop, mMaxLeft, mMaxRight;
    private RectF srcRect;
    private RectF dstRect;
    private RectF mLeftTopPoint;
    private RectF mRightTopPoint;
    private RectF mLeftBottomPoint;
    private RectF mRightBottomPoint;
    private Context context;

    private int pointCenter = mPointSize / 2;

    private int clickPoint;

    private float oldX, oldY;
    private PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    public SelectBorderView(Context context) {
        this(context, null);
    }

    public SelectBorderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectBorderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        this.context = context;
        //setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);           //关闭硬件加速
    }

    public void setMaxSize(int width, int height) {
        mMaxHeight = height;
        mMaxWidth = width;
    }

    public void setPointSize(int size) {
        mPointSize = size;
    }

    public void setBoundary(int left, int top, int right, int bottom) {
        mMaxLeft = left;
        mMaxTop = top;
        mMaxRight = right;
        mMaxBottom = bottom;
        Log.e("Boundary", left + "  " + top + " " + right + "   " + bottom);
    }

    public Rect getDstRect() {
        Rect rect = new Rect();
        rect.top = (int) (dstRect.top - mMaxTop);
        rect.left = (int) (dstRect.left - mMaxLeft);
        rect.bottom = (int) (dstRect.bottom - mMaxTop);
        rect.right = (int) (dstRect.right - mMaxLeft);
        return rect;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float offsetX = x - oldX;
        float offsetY = y - oldY;
        float offset = getMoveOffset(x - oldX, y - oldY);
        float offset2 = getMoveOffset2(x - oldX, y - oldY);

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                centerMove();
                break;
            case MotionEvent.ACTION_DOWN:
                if (mLeftTopPoint.contains(x, y))
                    clickPoint = CLICK_LEFTTOP;
                else if (mRightTopPoint.contains(x, y))
                    clickPoint = CLICK_RIGHTTOP;
                else if (mLeftBottomPoint.contains(x, y))
                    clickPoint = CLICK_LEFTBOTTOM;
                else if (mRightBottomPoint.contains(x, y))
                    clickPoint = CLICK_RIGHTBOTTOM;
                else if (dstRect.contains(x, y))
                    clickPoint = CLICK_CONTENT;
                else
                    clickPoint = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                switch (clickPoint) {
                    case CLICK_CONTENT:
                        dstRect.left += offsetX;
                        dstRect.top += offsetY;
                        dstRect.right += offsetX;
                        dstRect.bottom += offsetY;
                        centerMove();
                        break;
                    case CLICK_LEFTTOP:
                        dstRect.left += offset;
                        dstRect.top += offset;
                        if (offsetX < 0 && offsetY < 0 || checkSize()) {
                            moveLeftTop();
                            moveRightTop();
                            moveLeftBottom();
                        }
                        else {
                            dstRect.left = dstRect.right - mSize / 2;
                            dstRect.top = dstRect.bottom - mSize / 2;
                            centerMove();
                        }
                        if (!checkBoundary())
                            moveRightBottom();
                        else {
                            dstRect.left -= offset;                                 //因误差而产生抖动效果
                            dstRect.top -= offset;
                        }
                        break;
                    case CLICK_RIGHTTOP:
                        dstRect.right += offset2;
                        dstRect.top -= offset2;
                        if (offsetX > 0 && offsetY < 0 || checkSize()) {
                            moveLeftTop();
                            moveRightTop();
                            moveRightBottom();
                        }
                        else {
                            dstRect.right = dstRect.left + mSize / 2;
                            dstRect.top = dstRect.bottom - mSize / 2;
                            centerMove();
                        }
                        if (!checkBoundary())
                            moveLeftBottom();
                        else {
                            dstRect.right -= offset2;
                            dstRect.top += offset2;
                        }
                        break;
                    case CLICK_LEFTBOTTOM:
                        dstRect.left += offset2;
                        dstRect.bottom -= offset2;
                        if (offset2 < 0 && offset2 > 0 || checkSize()) {
                            moveLeftTop();
                            moveRightBottom();
                            moveLeftBottom();
                        }
                        else {
                            dstRect.left = dstRect.right - mSize / 2;
                            dstRect.bottom = dstRect.top + mSize / 2;
                            centerMove();
                        }
                        if (!checkBoundary())
                            moveRightTop();
                        else {
                            dstRect.left -= offset2;
                            dstRect.bottom += offset2;
                        }
                        break;
                    case CLICK_RIGHTBOTTOM:
                        dstRect.right += offset;
                        dstRect.bottom += offset;
                        if (offsetX > 0 && offsetY > 0 || checkSize()) {
                            moveRightTop();
                            moveRightBottom();
                            moveLeftBottom();
                        }
                        else {
                            dstRect.right = dstRect.left + mSize / 2;
                            dstRect.bottom = dstRect.top + mSize / 2;
                            centerMove();
                        }
                        if (!checkBoundary())
                            moveLeftTop();
                        else {
                            dstRect.right -= offset;
                            dstRect.bottom -= offset;
                        }
                        break;
                }
                postInvalidate();
                break;
        }

        oldX = x;
        oldY = y;
        return true;
    }

    private float getMoveOffset(float offsetX, float offsetY) {
        return offsetX / 2 + offsetY / 2;
    }

    private float getMoveOffset2(float offsetX, float offsetY) {
        if (offsetX <= 0)
            return offsetX / 2 - offsetY / 2;
        else
            return offsetX / 2 + Math.abs(offsetY / 2);
    }

    private boolean checkSize() {
        return dstRect.right - dstRect.left > mSize / 2 || dstRect.bottom - dstRect.top > mSize / 2;
    }

    private boolean checkBoundary() {
        boolean isBoundary = false;

        if (dstRect.right - dstRect.left < mMaxWidth && dstRect.bottom - dstRect.top < mMaxHeight) {
            if (dstRect.left < mMaxLeft) {
                if (dstRect.right < mMaxRight)
                    dstRect.right += mMaxLeft - dstRect.left;
                dstRect.left = mMaxLeft;
                isBoundary = false;
            }
            if (dstRect.right > mMaxRight) {
                if (dstRect.left > mMaxLeft)
                    dstRect.left -= dstRect.right - mMaxRight;
                dstRect.right = mMaxRight;
                isBoundary = false;
            }
            if (dstRect.top < mMaxTop) {
                if (dstRect.bottom < mMaxBottom)
                    dstRect.bottom += mMaxTop - dstRect.top;
                dstRect.top = mMaxTop;
                isBoundary = false;
            }
            if (dstRect.bottom > mMaxBottom) {
                if (dstRect.top > mMaxTop)
                    dstRect.top -= dstRect.bottom - mMaxBottom;
                dstRect.bottom = mMaxBottom;
                isBoundary = false;
            }
        }
        else
            isBoundary = true;

        return isBoundary;
    }

    private void centerMove() {
        checkBoundary();

        moveLeftTop();
        moveRightTop();
        moveLeftBottom();
        moveRightBottom();
    }

    private void moveLeftTop() {
        mLeftTopPoint.left = dstRect.left - pointCenter;
        mLeftTopPoint.top = dstRect.top - pointCenter;
        mLeftTopPoint.right = dstRect.left + pointCenter;
        mLeftTopPoint.bottom = dstRect.top + pointCenter;
    }

    private void moveLeftBottom() {
        mLeftBottomPoint.left = dstRect.left - pointCenter;
        mLeftBottomPoint.top = dstRect.bottom - pointCenter;
        mLeftBottomPoint.right = dstRect.left + pointCenter;
        mLeftBottomPoint.bottom = dstRect.bottom + pointCenter;
    }

    private void moveRightTop() {
        mRightTopPoint.left = dstRect.right - pointCenter;
        mRightTopPoint.top = dstRect.top - pointCenter;
        mRightTopPoint.right = dstRect.right + pointCenter;
        mRightTopPoint.bottom = dstRect.top + pointCenter;
    }

    private void moveRightBottom() {
        mRightBottomPoint.left = dstRect.right - pointCenter;
        mRightBottomPoint.top = dstRect.bottom - pointCenter;
        mRightBottomPoint.right = dstRect.right + pointCenter;
        mRightBottomPoint.bottom = dstRect.bottom + pointCenter;
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        if (srcRect == null) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            pointCenter = mPointSize / 2;
            srcRect = new RectF(0, 0, width, height);
            dstRect = new RectF();
            mLeftTopPoint = new RectF();
            mRightTopPoint = new RectF();
            mLeftBottomPoint = new RectF();
            mRightBottomPoint = new RectF();

            mSize = mMaxWidth >= mMaxHeight ? (int) (mMaxHeight / 1.5f) : (int) (mMaxWidth / 1.5f);

            dstRect.left = centerX - mSize / 2;
            dstRect.top = centerY - mSize / 2;
            dstRect.right = centerX + mSize / 2;
            dstRect.bottom = centerY + mSize / 2;

            if (dstRect.right - mPointSize - (dstRect.left + mPointSize) < 50) {
                Toast.makeText(context, "该图片不符合要求, 请选择其它图片", Toast.LENGTH_SHORT).show();
                ((Activity) context).onBackPressed();
                return;
            }


            if (mMaxBottom == 0)
                setBoundary(centerX - mMaxWidth/ 2, centerY - mMaxHeight / 2, centerX + mMaxWidth / 2, centerY + mMaxHeight / 2);

            centerMove();
        }
        int saveCount = canvas.saveLayer(srcRect, mPaint, Canvas.ALL_SAVE_FLAG);           //保存离散图层，兼容硬件加速

        mPaint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(srcRect, mPaint);
        mPaint.setColor(Color.argb(255, 0, 0, 0));
        mPaint.setXfermode(porterDuffXfermode);
        canvas.drawRect(dstRect, mPaint);
        canvas.restoreToCount(saveCount);

        mPaint.setXfermode(null);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);

        canvas.drawOval(mLeftTopPoint, mPaint);
        canvas.drawOval(mRightTopPoint, mPaint);
        canvas.drawOval(mLeftBottomPoint, mPaint);
        canvas.drawOval(mRightBottomPoint, mPaint);
        canvas.drawLine(dstRect.left, dstRect.top, dstRect.left, dstRect.bottom, mPaint);
        canvas.drawLine(dstRect.left, dstRect.top, dstRect.right, dstRect.top, mPaint);
        canvas.drawLine(dstRect.right, dstRect.top, dstRect.right, dstRect.bottom, mPaint);
        canvas.drawLine(dstRect.right, dstRect.bottom, dstRect.left, dstRect.bottom, mPaint);
    }
}
