package com.gdptc.society.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * 脱离主线程绘制，减轻绘制4K以上大图时CPU负担和提高主线程刷新帧数
 * 最大可提高40%性能
 * 默认缩放算法是fitCenter
 * Created by Administrator on 2017/10/8/008.
 */

public class ImageTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private float mScale = 1;
    private Bitmap mImg;
    private Paint mPaint;
    private OnDrawDoneListener mOnDrawDoneListener;

    public interface OnDrawDoneListener {
        void drawDone(Bitmap bitmap, RectF drawRect, int width, int height);
    }

    public ImageTextureView(Context context) {
        this(context, null);
    }

    public ImageTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setColor(Color.BLACK);
        setSurfaceTextureListener(this);
    }

    public void setOnDrawDoneListener(OnDrawDoneListener onDrawDoneListener) {
        mOnDrawDoneListener = onDrawDoneListener;
    }

    public Bitmap getBitmap() {
        return mImg;
    }

    public void setImageBitmap(Bitmap mImg) {
        this.mImg = mImg;
    }

    public float getImageScale() {
        return mScale;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        float scaleX = (float) width / mImg.getWidth();
        float scaleY = (float) height / mImg.getHeight();

        if (mImg.getHeight() > height && mImg.getWidth() > width || mImg.getHeight() < height && mImg.getWidth() < width)
            mScale = scaleX < scaleY ? scaleX : scaleY;
        else
            mScale = mImg.getHeight() > height && mImg.getWidth() < width ?
                    (float) height / mImg.getHeight() : (float) width / mImg.getWidth();

        float imgWidth = mImg.getWidth() * mScale;
        float imgHeight = mImg.getHeight() * mScale;

        float left = imgWidth < width ? (width - imgWidth) / 2.0f : 0;
        float top = imgHeight < height ? (height - imgHeight) / 2.0f : 0;
        float right = imgWidth + left;
        float bottom = imgHeight + top;

        Canvas canvas = lockCanvas();
        canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), mPaint);

        RectF rectF = new RectF(left, top, right, bottom);
        canvas.drawBitmap(mImg, null, rectF, mPaint);
        unlockCanvasAndPost(canvas);

        if (mOnDrawDoneListener != null)
            mOnDrawDoneListener.drawDone(mImg, rectF, (int) imgWidth, (int) imgHeight);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

}