package com.gdptc.society.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.app.FragmentActivity;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2017/7/17/017.
 */

public class ImageUtil {
    private static PaintFlagsDrawFilter drawFilter;
    private static Paint paint;
    private static PorterDuffXfermode clipFerMode;

    public enum MODE {
        ROUND(0), NORMAL(1), ROUND_BORDER(2), NORMAL_BORDER(3);

        private int value;

        MODE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    static {
        drawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);     //Bitmap与画笔都开启抗锯齿
        paint = new Paint();
        clipFerMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    }

    public static Bitmap doBlur(Context context, Bitmap bitmap, int radius) {
        if (radius > 25)
            radius = 25;

        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);

        Type t = allocation.getType();

        //Create allocation with the same type
        Allocation blurredAllocation = Allocation.createTyped(rs, t);

        //Create blur script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(blurredAllocation);

        //Copy script result into bitmap
        blurredAllocation.copyTo(bitmap);

        //Destroy everything to free memory
        allocation.destroy();
        blurredAllocation.destroy();
        blurScript.destroy();
        t.destroy();
        rs.destroy();
        bitmap.getConfig();
        return bitmap;
    }

    public static int getScale(BitmapFactory.Options options, float width, float height) {
        int scale = 1;
        int targetSize;

        if (width != -1 || height != -1) {
            targetSize = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;

            while (targetSize / scale > width && targetSize / scale > height)
                scale *= 2;         //系统底层强制采用2的次幂，非规则的数系统将会自动向下采样接近2的次幂的缩放因数
        }

        return scale;
    }

    public static int getScale(int sourceWidth, int sourceHeight, float targetWidth, float targetHeight) {
        int scale = 1;
        int targetSize;

        if (targetWidth != -1 || targetHeight != -1) {
            targetSize = sourceWidth > sourceHeight ? sourceWidth : sourceHeight;

            while (targetSize / scale > targetWidth && targetSize / scale > targetHeight)
                scale *= 2;
        }

        return scale;
    }

    public static Bitmap getBitmap(float width, float height, String bitmapPath) {
        return getBitmap(width, height, null, bitmapPath, null, null, null);
    }

    public static Bitmap getBitmap(float width, float height, String bitmapPath, BitmapInfo bitmapInfo, FragmentActivity activity, Object parent) {
        return getBitmap(width, height, null, bitmapPath, bitmapInfo, activity, parent);
    }

    public static Bitmap getBitmap(float width, float height, Bitmap inBitmap, String bitmapPath, BitmapInfo bitmapInfo, FragmentActivity activity, Object parent) {
        File file = new File(bitmapPath);
        Bitmap bitmap = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            bitmap = getBitmap(width, height, inBitmap, inputStream.getFD(), bitmapInfo, activity, parent);
            inputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap getBitmap(float width, float height, FileDescriptor fileDescriptor) {
        return getBitmap(width, height, null, fileDescriptor, null, null, null);
    }

    public static Bitmap getBitmap(float width, float height, FileDescriptor fileDescriptor, BitmapInfo bitmapInfo, FragmentActivity activity, Object parent) {
        return getBitmap(width, height, null, fileDescriptor, bitmapInfo, activity, parent);
    }

    public static Bitmap getBitmap(float width, float height, Bitmap inBitmap, FileDescriptor fileDescriptor, BitmapInfo bitmapInfo, FragmentActivity activity, Object parent) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = null;
        int sourceWidth = -1, sourceHeight = -1;

        options.inJustDecodeBounds = true;
        options.inMutable = true;
        options.inScaled = false;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);   //只获取到尺寸信息

        if (options.outHeight != 0 && options.outWidth != 0) {
            sourceWidth = options.outWidth;
            sourceHeight = options.outHeight;
            if (inBitmap != null)
                options.inBitmap = inBitmap;
            options.inSampleSize = getScale(options, width, height);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        }

        if (bitmapInfo != null) {
            bitmapInfo.bitmap = bitmap;
            bitmapInfo.sourceWidth = sourceWidth;
            bitmapInfo.sourceHeight = sourceHeight;
            if (activity != null)
                bitmapInfo.activity = new WeakReference<>(activity);
            if (parent != null)
                bitmapInfo.parentReference.add(new WeakReference<>(parent));
        }

        return bitmap;
    }

    public static Bitmap getRoundBitmap(Bitmap bitmap) {
        return getRoundBitmap(bitmap, bitmap.getByteCount());
    }

    public static Bitmap getRoundBitmap(Bitmap bitmap, int roundPx) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.setDrawFilter(drawFilter);
        paint.reset();
        Rect rect = new Rect(0, 0, output.getWidth(), output.getHeight());
        RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(clipFerMode);
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode) {
        return getBorderBitmap(bitmap, mode, bitmap.getWidth() / 10f);
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, float borderSize) {
        return getBorderBitmap(bitmap, mode, borderSize, 0xffffffff);
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, int color) {
        return getBorderBitmap(bitmap, mode, bitmap.getWidth() / 10f, color);
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, float borderSize, int color) {
        return getBorderBitmap(bitmap, mode, new float[] { borderSize }, new int[] { color });
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, float borderSize, int color, boolean copy) {
        return getBorderBitmap(bitmap, mode, new float[] { borderSize }, new int[] { color }, copy);
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, float[] borderSize, int[] color) {
        return getBorderBitmap(bitmap, mode, borderSize, color, false);
    }

    public static Bitmap getBorderBitmap(Bitmap bitmap, MODE mode, float[] borderSize, int[] color, boolean copy) {
        Bitmap output;
        Rect dst = cutBitmap(bitmap);
        if (dst != null)
            output = Bitmap.createBitmap(bitmap, dst.left, dst.top,
                    dst.right - dst.left, dst.bottom - dst.top, null, true);
        else {
            if (!bitmap.isMutable() || copy)
                output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
            else
                output = bitmap;
        }
        Canvas canvas = new Canvas(output);
        canvas.setDrawFilter(drawFilter);
        paint.reset();
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        if (dst != null) {
            int dx = dst.left;
            int dy = dst.top;
            dst.left -= dx;
            dst.top -= dy;
            dst.right -= dx;
            dst.bottom -= dy;
        }
        else if (bitmap != output)
            canvas.drawBitmap(bitmap, src, src, paint);

        if (borderSize != null && color != null) {
            RectF rectF = new RectF();

            if (dst != null)
                src = dst;

            for (int i = 0; i < borderSize.length; ++i)
                drawBorder(canvas, src, mode, borderSize[i], color[i], rectF);
        }
        else
            drawBorder(canvas, mode);
        return output;
    }

    public static void drawBorder(Canvas canvas, MODE mode) {
        drawBorder(canvas, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()),
                mode, canvas.getWidth() / 10f, 0xffffffff, new RectF());
    }

    public static void drawBorder(Canvas canvas, MODE mode, float borderSize) {
        drawBorder(canvas, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()),
                mode, borderSize, 0xffffffff, new RectF());
    }

    public static void drawBorder(Canvas canvas, MODE mode, float borderSize, int color) {
        drawBorder(canvas, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()),
                mode, borderSize, color, new RectF());
    }

    private static void drawBorder(Canvas canvas, Rect rect, MODE mode, float borderSize, int color, RectF rectF) {
        paint.setXfermode(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(borderSize);
        borderSize /= 2f;       //路径描边
        rectF.set(rect.left + borderSize, rect.top + borderSize,
                rect.right - borderSize, rect.bottom - borderSize);
        switch (mode) {
            case NORMAL: case NORMAL_BORDER:
                canvas.drawRect(rectF, paint);
                break;
            case ROUND: case ROUND_BORDER:
                canvas.drawArc(rectF, 0, 360, false, paint);
                break;
        }
    }

    private static Rect cutBitmap(Bitmap bitmap) {
        if (bitmap.getHeight() == bitmap.getWidth())            //符合规范则不进行裁剪
            return null;

        int point;
        int length;
        Rect rect = new Rect();

        if (bitmap.getHeight() > bitmap.getWidth()) {
            length = bitmap.getWidth();
            point = bitmap.getHeight() / 2;
            rect.set(0, point - length / 2, length, point + length / 2);
        }
        else {
            length = bitmap.getHeight();
            point = bitmap.getWidth() / 2;
            rect.set(point - length / 2, 0, point + length / 2, length);
        }

        return rect;        //返回中心裁剪区域
    }

}
