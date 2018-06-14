package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bm.library.Info;
import com.bm.library.PhotoView;
import com.example.basemodel.Value;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ImgInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.ui.holder.GalleryHolder;
import com.gdptc.society.ui.view.JazzyViewPager;

import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;


/**
 * Created by Administrator on 2017/9/21/021.
 */

public class GalleryActivity extends BaseActivity implements View.OnClickListener {
    private final int OFFSET_CACHE_PAGE = 2;

    private JazzyViewPager viewPager;
    private int selectNum = 0;
    private int totalSelectNum;
    private ArrayList<ImgInfo> dataList;
    private CheckBox checkBox;
    private TextView title;

    private ColorStateList colorStateList;

    private Animation in = new AlphaAnimation(0, 1.0f);
    private Animation out = new AlphaAnimation(1.0f, 0);
    private View bg = null;
    private PhotoView photoViewBig = null;
    private RelativeLayout mainLayout;

    private AsyncLoader asyncLoader;
    private Bitmap bmPicLoad, bmPicFailure;

    private boolean animationRunning = false;
    private boolean mRadio;
    private int selectorPosition, cappedNum;
    private ApplicationManager applicationManager;

    private String key = GalleryActivity.class.getSimpleName();
    private Drawable dbSelect;

    private Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        enabledScrollFinish(false);
        disableTransition();
        asyncLoader = new AsyncLoader(this, OFFSET_CACHE_PAGE);   //开辟线程过多的话在同时解码6K等超大图时会超出系统负荷导致卡死
        asyncLoader.setImageLoadListener(imageLoadListener);
        mainLayout = (RelativeLayout) findViewById(R.id.activity_gallery_mainLayout);
        viewPager = (JazzyViewPager) findViewById(R.id.activity_gallery_viewPager);
        checkBox = (CheckBox) findViewById(R.id.activity_gallery_checkBox);
        dbSelect = getResources().getDrawable(R.drawable.gallery_checkbox);
        checkBox.setButtonDrawable(dbSelect);

        applicationManager = (ApplicationManager) getApplication();
        intent = getIntent();
        mRadio = intent.getBooleanExtra(Public.RADIO, false);
        cappedNum = intent.getIntExtra(Public.CAPPED, -1);

        Button send = (Button) findViewById(R.id.activity_gallery_send);
        send.setOnClickListener(this);

        String key = intent.getStringExtra(Public.CONTEXT);
        dataList = (ArrayList<ImgInfo>) applicationManager.dataMap.get(key);
        applicationManager.dataMap.remove(key);

        String titleStr;
        if (mRadio) {
            selectNum = intent.getIntExtra(Public.NUMBER, 0);
            send.setText("确定");
            titleStr = "选择图片";
        }
        else {
            for (ImgInfo imgInfo : dataList)
                if (imgInfo.selector)
                    ++selectNum;
            titleStr = "已选: " + totalSelectNum + "/" + (cappedNum == -1 ? dataList.size() : cappedNum);
            totalSelectNum -= selectNum;
            totalSelectNum = intent.getIntExtra(Public.NUMBER, 0);
        }

        selectorPosition = intent.getIntExtra(Public.ID, -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_bar);
        new TitleBarUtil(this, toolbar, titleStr, false, true).disableShadow().init();
        toolbar.setBackgroundColor(Color.argb(50, 0, 0,0));
        title = (TextView) toolbar.findViewById(R.id.tv_toolbar_title);
        ((Toolbar.LayoutParams) title.getLayoutParams()).gravity = Gravity.NO_GRAVITY;
        bmPicLoad = BitmapFactory.decodeResource(getResources(), R.drawable.album);
        bmPicFailure = BitmapFactory.decodeResource(getResources(), R.drawable.album_failure);

        int selection = intent.getIntExtra(Public.POSITION, 0);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageMargin((int) (getResources().getDisplayMetrics().density * 15));
        viewPager.setTransitionEffect(JazzyViewPager.TransitionEffect.ZoomIn);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        viewPager.setCurrentItem(selection);
        if (selection == 0)
            onPageChangeListener.onPageSelected(selection);
        viewPager.setOffscreenPageLimit(OFFSET_CACHE_PAGE);
        checkBox.setOnClickListener(this);

        Intent data = getIntent();
        ApplicationManager applicationManager = (ApplicationManager) getApplication();

        key = data.getStringExtra(Public.INFO);
        Info info = (Info) applicationManager.dataMap.get(key);
        applicationManager.dataMap.remove(key);

        photoViewBig = (PhotoView) findViewById(R.id.activity_gallery_photoView);
        bg = findViewById(R.id.activity_gallery_bg);
        photoViewBig.setImageBitmap((Bitmap) intent.getParcelableExtra(Public.DATA));

        in.setDuration(500);
        in.setFillAfter(true);
        out.setDuration(500);
        out.setFillAfter(true);
        in.setAnimationListener(animationListener);
        out.setAnimationListener(animationListener);
        bg.startAnimation(in);
        photoViewBig.animaFrom(info);

        onUIColorChange(Value.getColorUI());
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageSelected(int position) {
            checkBox.setChecked(mRadio ? position == selectorPosition : dataList.get(position).selector);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                checkBox.setButtonTintList(colorStateList);
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };

    private PagerAdapter pagerAdapter = new PagerAdapter() {
        List<View> list = new ArrayList<>();

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = list.size() > 0 ? list.remove(0) : null;
            GalleryHolder viewHolder;

            if (view == null) {
                view = LayoutInflater.from(GalleryActivity.this).inflate(R.layout.item_gallery_child, container, false);
                viewHolder = new GalleryHolder(view);
                view.setTag(viewHolder);
            }
            else
                viewHolder = (GalleryHolder) view.getTag();

            ImgInfo info = dataList.get(position);
            viewHolder.path = info.path;
            viewHolder.id = info.id;
            Bitmap bitmap = asyncLoader.loadImgForThumb(viewHolder.photoView, Long.valueOf(viewHolder.id));
            BitmapInfo pic = AsyncLoader.getImgCache(viewHolder.path, displayMetrics.widthPixels,
                    displayMetrics.heightPixels, ImageUtil.MODE.NORMAL, AsyncLoader.QUALITY.PROBABLY_SIZE);
            if (pic == null) {
                if (bitmap == null) {
                    viewHolder.photoView.setImageBitmap(bmPicLoad);
                }
                else {
                    viewHolder.photoView.setImageBitmap(bitmap);
                    asyncLoader.loadImgForLocal(viewHolder.photoView, viewHolder.path);
                }
                viewHolder.loading.setVisibility(View.VISIBLE);
                viewHolder.loadingBg.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.loading.setVisibility(View.GONE);
                viewHolder.loadingBg.setVisibility(View.GONE);
                viewHolder.photoView.setImageBitmap(pic.getBitmap(GalleryActivity.this, viewHolder.photoView));
            }
            container.addView(view);
            viewPager.setObjectForPosition(view, position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
            list.add(view);
            viewPager.delObjectForPosition(position);
        }

    };

    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            photoViewBig.setEnabled(false);
            bg.setEnabled(false);
            animationRunning = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            animationRunning = false;
            if (animation.equals(in)) {
                photoViewBig.setVisibility(View.GONE);
                photoViewBig.clearAnimation();
                mainLayout.setVisibility(View.VISIBLE);
            }
            else
                finish();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            ImageView imageView = (ImageView) parent;
            GalleryHolder viewHolder = (GalleryHolder) imageView.getTag();
            if (viewHolder.path.equals(id)) {
                imageView.setImageBitmap(bitmapInfo.getBitmap(GalleryActivity.this, imageView));
                imageView.setColorFilter(null);
                viewHolder.loading.setVisibility(View.GONE);
                viewHolder.loadingBg.setVisibility(View.GONE);
                asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, ImageUtil.MODE.NORMAL);
            }
            else if (viewHolder.id.equals(id.toString())) {
                Bitmap pic = asyncLoader.loadImgForLocal(imageView, viewHolder.path);
                if (pic == null) {
                    imageView.setColorFilter(null);
                    imageView.setImageBitmap(bitmapInfo.getBitmap(GalleryActivity.this, imageView));
                }
                else {
                    viewHolder.loading.setVisibility(View.GONE);
                    viewHolder.loadingBg.setVisibility(View.GONE);
                    imageView.setImageBitmap(pic);
                }
            }
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {
            ImageView imageView = (ImageView) parent;
            GalleryHolder viewHolder = (GalleryHolder) imageView.getTag();
            if (viewHolder.path.equals(id)) {
                imageView.setImageBitmap(bmPicFailure);
                viewHolder.loading.setVisibility(View.GONE);
                viewHolder.loadingBg.setVisibility(View.GONE);
            }
            if (e != null)
                e.printStackTrace();
        }
    };

    @Override
    public void onUIColorChange(Value.COLOR color) {
        dbSelect.setColorFilter(color.toValue(), PorterDuff.Mode.SRC_ATOP);
        viewPager.setEdgeEffectColor(color.toValue());
    }

    @Override
    public void onBackPressed() {
        if (!animationRunning) {
            Intent intent = new Intent();

            if (!mRadio) {
                applicationManager.dataMap.put(key, dataList);
                intent.putExtra(Public.CONTEXT, key);
            }
            if (selectorPosition == -1)
                setResult(RESULT_CANCELED);
            else {
                intent.putExtra(Public.NUMBER, selectNum);
                intent.putExtra(Public.ID, selectorPosition);
                setResult(RESULT_OK, intent);
            }
            findViewById(R.id.activity_gallery_parent).startAnimation(out);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_gallery_checkBox:
                CheckBox checkBox = (CheckBox) v;
                if (cappedNum == -1 || selectNum + totalSelectNum < cappedNum) {
                    if (!mRadio) {
                        dataList.get(viewPager.getCurrentItem()).selector = checkBox.isChecked();
                        selectNum += checkBox.isChecked() ? 1 : -1;
                        title.setText("已选: " + (selectNum + totalSelectNum) + "/" + (cappedNum == -1 ? dataList.size() : cappedNum));
                    }
                    else if (selectorPosition != viewPager.getCurrentItem()) {
                        selectorPosition = viewPager.getCurrentItem();
                        selectNum = 1;
                        checkBox.setChecked(true);
                    }
                    else {
                        selectorPosition = -1;
                        selectNum = 0;
                        checkBox.setChecked(false);
                    }
                }
                else if (!checkBox.isChecked()) {
                    dataList.get(viewPager.getCurrentItem()).selector = false;
                    --selectNum;
                    title.setText("已选: " + (selectNum + totalSelectNum) + "/" + (cappedNum == -1 ? dataList.size() : cappedNum));
                }
                else
                    checkBox.setChecked(false);
                break;
            case R.id.activity_gallery_send:
                if (selectNum == 0 && (mRadio || totalSelectNum == 0))
                    Toast.makeText(GalleryActivity.this, "您尚未选择图片哟", Toast.LENGTH_SHORT).show();
                else {
                    applicationManager.dataMap.put(key, dataList);

                    Intent intent = new Intent();
                    intent.putExtra(Public.UPLOAD, true);
                    intent.putExtra(Public.CONTEXT, key);
                    intent.putExtra(Public.NUMBER, selectNum);
                    if (mRadio && selectorPosition != -1)
                        intent.putExtra(Public.PATH, dataList.get(selectorPosition).path);
                    intent.putExtra(Public.ID, selectorPosition);
                    findViewById(R.id.activity_gallery_parent).startAnimation(out);
                    setResult(RESULT_OK, intent);
                    finish();
                }
        }
    }

    @Override
    protected void onDestroy() {
        asyncLoader.exitLoader();
        super.onDestroy();
    }
}
