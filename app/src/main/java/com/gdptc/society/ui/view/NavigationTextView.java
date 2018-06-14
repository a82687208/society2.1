package com.gdptc.society.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.gdptc.society.R;
import com.gdptc.society.tools.TypedValueUtil;

/**
 * Created by Administrator on 2018/2/6/006.
 */

public class NavigationTextView extends View {
    /** 要显示的文字 */
    private String text;
    /** 文字的颜色 */
    private int textColor;
    /** 双重文字的颜色 */
    private int doubleColor = -1;
    /** 文字的大小 */
    private int textSize;
    /** 文字的方位 */
    private int textAlign;

    //	public static final int TEXT_ALIGN_CENTER            = 0x00000000;
    public static final int TEXT_ALIGN_LEFT              = 0x00000001;
    public static final int TEXT_ALIGN_RIGHT             = 0x00000010;
    public static final int TEXT_ALIGN_CENTER_VERTICAL   = 0x00000100;
    public static final int TEXT_ALIGN_CENTER_HORIZONTAL = 0x00001000;
    public static final int TEXT_ALIGN_TOP               = 0x00010000;
    public static final int TEXT_ALIGN_BOTTOM            = 0x00100000;

    /** 文本中轴线X坐标 */
    private float textCenterX;
    /** 文本baseline线Y坐标 */
    private float textBaselineY;

    /** 控件的宽度 */
    private int viewWidth;
    /** 控件的高度 */
    private int viewHeight;
    /** 控件画笔 */
    private Paint paint;

    private Paint.FontMetrics fm;
    /** 场景 */
    private Context context;

    public NavigationTextView(Context context) {
        this(context, null);
    }

    public NavigationTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.NavigationTextView, 0, 0);
        if (attr == null)
            return;

        try {
            text = attr.getString(R.styleable.NavigationTextView_text);
            textColor = attr.getColor(R.styleable.NavigationTextView_textColor, getResources().getColor(R.color.textColor));
            textSize = attr.getDimensionPixelSize(R.styleable.NavigationTextView_textSize, (int) TypedValueUtil.dip2px(context, 14));
        }
        finally {
            attr.recycle();
        }
        init();
    }

    /**
     * 变量初始化
     */
    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        //默认情况下文字居中显示
        textAlign = TEXT_ALIGN_CENTER_HORIZONTAL | TEXT_ALIGN_CENTER_VERTICAL;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        viewWidth = getWidth();
        viewHeight = getHeight();
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //绘制控件内容
        setTextLocation();
        canvas.drawText(text, textCenterX, textBaselineY, paint);
        if (doubleColor != -1) {
            paint.setColor(doubleColor);
            canvas.drawText(text, textCenterX, textBaselineY, paint);
        }
        super.onDraw(canvas);
    }

    /**
     * 定位文本绘制的位置
     */
    private void setTextLocation() {
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        fm = paint.getFontMetrics();
        //文本的宽度
        float textWidth = paint.measureText(text);
        float textCenterVerticalBaselineY = viewHeight / 2 - fm.descent + (fm.descent - fm.ascent) / 2;
        switch (textAlign) {
            case TEXT_ALIGN_CENTER_HORIZONTAL | TEXT_ALIGN_CENTER_VERTICAL:
                textCenterX = (float)viewWidth / 2;
                textBaselineY = textCenterVerticalBaselineY;
                break;
            case TEXT_ALIGN_LEFT | TEXT_ALIGN_CENTER_VERTICAL:
                textCenterX = textWidth / 2;
                textBaselineY = textCenterVerticalBaselineY;
                break;
            case TEXT_ALIGN_RIGHT | TEXT_ALIGN_CENTER_VERTICAL:
                textCenterX = viewWidth - textWidth / 2;
                textBaselineY = textCenterVerticalBaselineY;
                break;
            case TEXT_ALIGN_BOTTOM | TEXT_ALIGN_CENTER_HORIZONTAL:
                textCenterX = viewWidth / 2;
                textBaselineY = viewHeight - fm.bottom;
                break;
            case TEXT_ALIGN_TOP | TEXT_ALIGN_CENTER_HORIZONTAL:
                textCenterX = viewWidth / 2;
                textBaselineY = -fm.ascent;
                break;
            case TEXT_ALIGN_TOP | TEXT_ALIGN_LEFT:
                textCenterX = textWidth / 2;
                textBaselineY = -fm.ascent;
                break;
            case TEXT_ALIGN_BOTTOM | TEXT_ALIGN_LEFT:
                textCenterX = textWidth / 2;
                textBaselineY = viewHeight - fm.bottom;
                break;
            case TEXT_ALIGN_TOP | TEXT_ALIGN_RIGHT:
                textCenterX = viewWidth - textWidth / 2;
                textBaselineY = -fm.ascent;
                break;
            case TEXT_ALIGN_BOTTOM | TEXT_ALIGN_RIGHT:
                textCenterX = viewWidth - textWidth / 2;
                textBaselineY = viewHeight - fm.bottom;
                break;
        }
    }

    public void enableDoubleText(int color) {
        doubleColor = color;
        invalidate();
    }

    /**
     * 设置文本内容
     * @param text
     */
    public void setText(String text) {
        this.text = text;
        invalidate();
    }
    /**
     * 设置文本大小
     * @param textSizeSp 文本大小，单位是sp
     */
    public void setTextSize(int textSizeSp) {
        this.textSize = (int) TypedValueUtil.sp2px(context, textSizeSp);
        invalidate();
    }
    /**
     * 设置文本的方位
     */
    public void setTextAlign(int textAlign) {
        this.textAlign = textAlign;
        invalidate();
    }
    /**
     * 设置文本的颜色
     * @param textColor
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
        invalidate();
    }
}
