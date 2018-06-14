package com.gdptc.society.ui.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.bm.library.Info;
import com.bm.library.PhotoView;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.ui.holder.GalleryHolder;
import com.gdptc.society.ui.view.JazzyViewPager;
import com.gdptc.society.ui.view.ScaleCircleNavigator;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;

import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;


/**
 * Created by Administrator on 2017/6/22/022.
 */

public class PhotoViewBigActivity extends BaseActivity {
    private final int OFFSET_ITEM = 2;

    private Animation bgIn;
    private Animation bgOut;
    private ObjectAnimator naIn;
    private ObjectAnimator naOut;

    private View bg = null;
    private Info mInfo = null;
    private PhotoView photoViewBig = null;
    private boolean back = false;

    private MagicIndicator magicIndicator;

    private List<Object> galleryList;
    private JazzyViewPager viewPager;
    private Bitmap outBitmap;
    private Bitmap inBitmap;
    private int inPosition;

    private AsyncLoader asyncLoader;
    private Handler handler;
    private AnThread anThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoview_big);
        enabledScrollFinish(false);
        disableTransition();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().getAttributes().flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

        bgIn = new AlphaAnimation(0, 1.0f);
        bgOut = new AlphaAnimation(1.0f, 0);

        handler = new Handler();
        Intent data = getIntent();
        ApplicationManager applicationManager = (ApplicationManager) getApplication();
        asyncLoader = new AsyncLoader(this, OFFSET_ITEM + 1, AsyncLoader.QUALITY.FULL_SIZE);
        asyncLoader.setImageLoadListener(imageLoadListener);
        String key = data.getStringExtra(Public.CONTEXT);
        mInfo = (Info) applicationManager.dataMap.get(key);
        applicationManager.dataMap.remove(key);
        galleryList = (List<Object>) applicationManager.dataMap.get(Public.DATA);
        applicationManager.dataMap.remove(Public.DATA);

        if (galleryList == null) {
            galleryList = new ArrayList<>();

            String path = data.getStringExtra(Public.PATH);
            if (path != null)
                galleryList.add(path);
            else {
                int id = data.getIntExtra(Public.PATH, -1);
                if (id != -1)
                    galleryList.add(id);
            }
        }

        viewPager = (JazzyViewPager) findViewById(R.id.vPage_photoViewBig_list);
        viewPager.setOffscreenPageLimit(OFFSET_ITEM);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageMargin((int) (getResources().getDisplayMetrics().density * 15));
        viewPager.setTransitionEffect(JazzyViewPager.TransitionEffect.ZoomIn);

        if (galleryList.size() > 1) {
            viewPager.addOnPageChangeListener(pageChangeListener);
            initIndicator();
        }

        photoViewBig = (PhotoView) findViewById(R.id.photoView_photoViewBig_pic);
        bg = findViewById(R.id.view_photoViewBig_bg);

        inBitmap = (Bitmap) applicationManager.dataMap.get(Public.BITMAP);
        if (inBitmap != null) {
            photoViewBig.setImageBitmap(inBitmap);
            applicationManager.dataMap.remove(Public.BITMAP);
            inPosition = data.getIntExtra(Public.POSITION, 0);
            viewPager.setCurrentItem(inPosition);
            outBitmap = inBitmap;
            applicationManager.dataMap.remove(Public.BITMAP);
        }

        bgIn.setDuration(600);
        bgIn.setFillAfter(true);
        bgOut.setDuration(500);
        bgOut.setFillAfter(true);
        bgIn.setAnimationListener(animationListener);
        bgOut.setAnimationListener(animationListener);
        bg.startAnimation(bgIn);
        photoViewBig.animaFrom(mInfo);
    }

    private void initIndicator() {
        //类似桌面的滑动底部小圆角提示控件
        magicIndicator = (MagicIndicator) findViewById(R.id.mgIndicator_photoViewBig);
        ScaleCircleNavigator scaleCircleNavigator = new ScaleCircleNavigator(this);
        scaleCircleNavigator.setCircleCount(galleryList.size());
        scaleCircleNavigator.setNormalCircleColor(Color.LTGRAY);
        scaleCircleNavigator.setSelectedCircleColor(Color.DKGRAY);
        scaleCircleNavigator.setCircleClickListener(new ScaleCircleNavigator.OnCircleClickListener() {
            @Override
            public void onClick(int index) {
                viewPager.setCurrentItem(index);
            }
        });
        magicIndicator.setNavigator(scaleCircleNavigator);
        ViewPagerHelper.bind(magicIndicator, viewPager);
        naIn = ObjectAnimator.ofFloat(magicIndicator, "alpha", 0, 1);
        naOut = ObjectAnimator.ofFloat(magicIndicator, "alpha", 1, 0);
        naIn.setDuration(300);
        naOut.setDuration(300);
        naIn.addListener(animatorListener);
        naOut.addListener(animatorListener);
    }

    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            photoViewBig.setEnabled(false);
            bg.setEnabled(false);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (animation.equals(bgIn)) {
                photoViewBig.setVisibility(View.GONE);
                bg.setVisibility(View.GONE);
                viewPager.setVisibility(View.VISIBLE);
                if (galleryList.size() > 1) {
                    magicIndicator.setVisibility(View.VISIBLE);
                    anThread = new AnThread();
                    anThread.time = 4;
                    anThread.start();
                }
            }
            else if (galleryList.size() > 1)
                finish();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}
    };

    private Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            if (animation.equals(naIn))
                magicIndicator.setVisibility(View.VISIBLE);
            else
                magicIndicator.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };

    private AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            GalleryHolder holder = (GalleryHolder) ((View) parent).getTag();
            Bitmap bitmap = bitmapInfo.getBitmap(PhotoViewBigActivity.this, parent);
            holder.photoView.setImageBitmap(bitmap);
            holder.photoView.enable();
            holder.loading.setVisibility(View.GONE);
            holder.loadingBg.setVisibility(View.GONE);
            asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, mode);
            if (galleryList.size() == 1)
                outBitmap = bitmap;
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {

        }
    };

    private PagerAdapter pagerAdapter = new PagerAdapter() {
        List<View> list = new ArrayList<>();

        @Override
        public int getCount() {
            return galleryList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = list.size() > 0 ? list.remove(0) : null;
            GalleryHolder viewHolder;
            Object oj = galleryList.get(position);

            if (view == null) {
                view = LayoutInflater.from(PhotoViewBigActivity.this).inflate(R.layout.item_gallery_child, container, false);
                viewHolder = new GalleryHolder(view);
                view.setTag(viewHolder);
            }
            else
                viewHolder = (GalleryHolder) view.getTag();

            Bitmap bitmap;
            if (oj instanceof String)
                bitmap = asyncLoader.loadImgForLocal(viewHolder.photoView, (String) oj,
                        displayMetrics.widthPixels, displayMetrics.heightPixels);
            else
                bitmap = asyncLoader.loadImgForResource(viewHolder.photoView, (Integer) oj,
                        displayMetrics.widthPixels, displayMetrics.heightPixels);

            if (bitmap == null) {
                if (position == inPosition)
                    viewHolder.photoView.setImageBitmap(inBitmap);
                else
                    viewHolder.photoView.setImageResource(R.drawable.album);
                viewHolder.photoView.disenable();
                viewHolder.loading.setVisibility(View.VISIBLE);
                viewHolder.loadingBg.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.loading.setVisibility(View.GONE);
                viewHolder.loadingBg.setVisibility(View.GONE);
                viewHolder.photoView.setImageBitmap(bitmap);
                viewHolder.photoView.enable();
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

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int i, float v, int i1) {
            if (anThread == null || !anThread.isAlive()) {
                anThread = new AnThread();
                anThread.time = 4;
                anThread.start();
            }
            else
                anThread.time = 4;

            if (!naOut.isRunning() && magicIndicator.getVisibility() == View.GONE) {
                magicIndicator.setVisibility(View.VISIBLE);
                naIn.start();
            }
        }

        @Override
        public void onPageSelected(int i) {}

        @Override
        public void onPageScrollStateChanged(int i) {}
    };

    private class AnThread extends Thread {
        int time;

        @Override
        public void run() {
            try {
                while (time > 0) {
                    Thread.sleep(1000);
                    time -= 1;
                }
                handler.post(rubNaOut);
            }
            catch (Exception e) {}
        }
    }

    private Runnable rubNaOut = new Runnable() {
        @Override
        public void run() {
            naOut.start();
        }
    };

    @Override
    public void onBackPressed() {
        if (back)
            return;
        back = true;
        if (galleryList.size() == 1) {
            if (anThread != null && anThread.isAlive())
                anThread.interrupt();
            viewPager.setVisibility(View.GONE);
            photoViewBig.setVisibility(View.VISIBLE);
            photoViewBig.setImageBitmap(outBitmap);
            bg.startAnimation(bgOut);
            photoViewBig.animaTo(mInfo, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
        else
            findViewById(R.id.rltLyt_photoViewBig_mainLayout).startAnimation(bgOut);
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0,0);
        super.onPause();
    }
}
