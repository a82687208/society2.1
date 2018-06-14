package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.tools.TypedValueUtil;
import com.gdptc.society.ui.view.ImageTextureView;
import com.gdptc.society.ui.view.SelectBorderView;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;

/**
 * Created by Administrator on 2017/9/15/015.
 */

public class PreViewActivity extends BaseActivity implements View.OnClickListener,
                                                        ImageTextureView.OnDrawDoneListener {
    private ImageView preView;
    private ImageTextureView contentView;
    private ImageView bitImg, normalImg, smallImg;
    private LinearLayout preLayout, bottomLayout;
    private Bitmap sourceBitmap, cutBitmap;
    private SelectBorderView selectBorderView;
    private Button mDoneBtn, mReCutBtn;
    private TextView mCutTv;
    //private float[] BitmapRect = new float[10];

    private TitleBarUtil titleBarUtil;
    private boolean showIcon;
    private Intent dataIntent;

    private int maxPicSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_img_change);
        enabledScrollFinish(false);
        dataIntent = getIntent();
        showIcon = dataIntent.getBooleanExtra(Public.ICON, false);
        maxPicSize = (int) TypedValueUtil.dip2px(this, 100);

        init();
        setListener();
    }

    private void init() {
        final String title = dataIntent.getStringExtra(Public.TITLE);
        titleBarUtil = new TitleBarUtil(this, true, true);
        titleBarUtil.setTitle(title == null ? "更换头像" : title).init();
        preLayout = (LinearLayout) findViewById(R.id.activity_user_img_preImgLayout);
        bottomLayout = (LinearLayout) findViewById(R.id.activity_user_img_bottomLayout);
        contentView = (ImageTextureView) findViewById(R.id.activity_user_img_change_img);
        preView = (ImageView) findViewById(R.id.activity_user_img_previewImg);
        mDoneBtn = (Button) findViewById(R.id.activity_user_img_change_done);
        mReCutBtn = (Button) findViewById(R.id.activity_user_img_change_reCut);
        bitImg = (ImageView) findViewById(R.id.activity_user_img_change_bitImg);
        normalImg = (ImageView) findViewById(R.id.activity_user_img_change_normalImg);
        smallImg = (ImageView) findViewById(R.id.activity_user_img_change_smallImg);
        selectBorderView = (SelectBorderView) findViewById(R.id.activity_user_img_drawView);
        mCutTv = (TextView) findViewById(R.id.activity_user_img_cutButton);

        Intent intent = getIntent();
        String path = intent.getStringExtra(Public.PATH);
        sourceBitmap = ImageUtil.getBitmap(displayMetrics.widthPixels, displayMetrics.heightPixels, path);
        contentView.setImageBitmap(sourceBitmap);
        contentView.setOnDrawDoneListener(this);
        selectBorderView.setPointSize(displayMetrics.widthPixels / 18);

        contentView.post(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mCutTv.getLayoutParams();
                layoutParams.topMargin = titleBarUtil.getSystemBarHeight();
            }
        });
    }

    private void setListener() {
        mReCutBtn.setOnClickListener(this);
        mDoneBtn.setOnClickListener(this);
        mCutTv.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_user_img_cutButton:
                Rect rectF = selectBorderView.getDstRect();
                int width = (int) ((rectF.right - rectF.left) / contentView.getImageScale());
                int height = (int) ((rectF.bottom - rectF.top) / contentView.getImageScale());
                Log.e("rect", rectF.left + "    " + rectF.top + "   " + width + "   " + height);
                cutBitmap = Bitmap.createBitmap(sourceBitmap, (int) (rectF.left / contentView.getImageScale()),
                        (int) (rectF.top / contentView.getImageScale()), width, height);
                preView.setImageBitmap(cutBitmap);
                preView.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
                selectBorderView.setVisibility(View.GONE);
                mCutTv.setVisibility(View.GONE);
                bottomLayout.setVisibility(View.VISIBLE);
                if (showIcon) {
                    Bitmap bmRound = ImageUtil.getRoundBitmap(cutBitmap);
                    ImageUtil.getBorderBitmap(bmRound, ImageUtil.MODE.ROUND_BORDER, maxPicSize / 18f, 0xffffffff);
                    preLayout.setVisibility(View.VISIBLE);
                    bitImg.setImageBitmap(bmRound);
                    normalImg.setImageBitmap(bmRound);
                    smallImg.setImageBitmap(bmRound);
                }
                break;
            case R.id.activity_user_img_change_done:
                String key = PreViewActivity.class.getSimpleName();
                ((ApplicationManager) getApplication()).dataMap.put(key, cutBitmap);
                Intent intent = new Intent();
                intent.putExtra(Public.CONTEXT, key);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.activity_user_img_change_reCut:
                preView.setVisibility(View.GONE);
                contentView.setVisibility(View.VISIBLE);
                selectBorderView.setVisibility(View.VISIBLE);
                mCutTv.setVisibility(View.VISIBLE);
                bottomLayout.setVisibility(View.GONE);
                preLayout.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawDone(Bitmap bitmap, RectF drawRect, int width, int height) {
        selectBorderView.setMaxSize(width, height);
        selectBorderView.setVisibility(View.VISIBLE);
        mReCutBtn.setClickable(true);
    }
}
