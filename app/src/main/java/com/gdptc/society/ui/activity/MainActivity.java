package com.gdptc.society.ui.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.basemodel.BaseViewPager;
import com.example.basemodel.Value;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.SchoolInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.fragment.AddressFragemnt;
import com.gdptc.society.fragment.CatalogueFragment;
import com.gdptc.society.fragment.MainFragment;
import com.gdptc.society.fragment.SortFragment;
import com.gdptc.society.tools.BitmapToRound_Util;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.ui.view.NavigationTextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity implements View.OnClickListener {
    private Button mButton1;
    private Button mButton2;
    private Button mButton3;
    private int checkedbutton = -1;
    private FrameLayout mDownMuneContainer;
    private TextView mTextView;
    private Fragment mCurrentFragment;
    private ImageView main_menu;
    private TextView bt_school;
    private BitmapToRound_Util round_util = new BitmapToRound_Util();
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar_bar_main;

    private BaseViewPager mainViewPager;

    private int clickPosition = -1;
    private int colorUI;

    private List<LinearLayout> iconLayout;
    private List<ImageView> imgIcon;
    private List<NavigationTextView> tvIcon;
    private List<Bitmap> sourceIcon;
    private List<Bitmap> targetIcon;

    private List<AnimatorSet> anClickList;

    private List<View> viewList;

    private Fragment[] view = {new MainFragment()

    };
    private List<Fragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // new TitleBarUtil(this, (Toolbar) this.findViewById(R.id.toolbar_bar_main),null,false,false).init();
        // new TitleBarUtil(this, true, true).init();

        disableTransition();

        enabledScrollFinish(false);

        viewList = new ArrayList<>();
        imgIcon = new ArrayList<>();
        sourceIcon = new ArrayList<>();
        targetIcon = new ArrayList<>();
        tvIcon = new ArrayList<>();
        iconLayout = new ArrayList<>();
        anClickList = new ArrayList<>();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Resources resources = getResources();

        iconLayout.add((LinearLayout) findViewById(R.id.lnrLyt_main_menu_home));
        iconLayout.add((LinearLayout) findViewById(R.id.lnrLyt_main_menu_address));
        iconLayout.add((LinearLayout) findViewById(R.id.lnrLyt_main_menu_location));
        iconLayout.add((LinearLayout) findViewById(R.id.lnrLyt_main_menu_personal));

        imgIcon.add((ImageView) findViewById(R.id.activity_main_img_menu_home));
        imgIcon.add((ImageView) findViewById(R.id.activity_main_img_menu_address));
        imgIcon.add((ImageView) findViewById(R.id.activity_main_img_menu_location));
        imgIcon.add((ImageView) findViewById(R.id.activity_main_img_menu_personal));

        tvIcon.add((NavigationTextView) findViewById(R.id.tv_main_menu_home));
        tvIcon.add((NavigationTextView) findViewById(R.id.tv_main_menu_address));
        tvIcon.add((NavigationTextView) findViewById(R.id.tv_main_menu_location));
        tvIcon.add((NavigationTextView) findViewById(R.id.tv_main_menu_personal));

        sourceIcon.add(BitmapFactory.decodeResource(resources, R.drawable.internet));
        sourceIcon.add(BitmapFactory.decodeResource(resources, R.drawable.address));
        sourceIcon.add(BitmapFactory.decodeResource(resources, R.drawable.personal_location));
        sourceIcon.add(BitmapFactory.decodeResource(resources, R.drawable.personal));

        targetIcon.add(BitmapFactory.decodeResource(resources, R.drawable.internet_press));
        targetIcon.add(BitmapFactory.decodeResource(resources, R.drawable.address_press));
        targetIcon.add(BitmapFactory.decodeResource(resources, R.drawable.personal_location_press));
        targetIcon.add(BitmapFactory.decodeResource(resources, R.drawable.personal_press));

        for (ImageView img : imgIcon) {
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator animatorX = ObjectAnimator.ofFloat(img, "scaleX", 1, 0.5f, 1);
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(img, "scaleY", 1, 0.5f, 1);
            animatorSet.play(animatorX).with(animatorY);
            animatorSet.setDuration(200);
            anClickList.add(animatorSet);
        }

        colorUI = Value.getColorUI().toValue();

        imgIcon.get(0).setImageResource(R.drawable.internet_press);
        imgIcon.get(0).setColorFilter(colorUI, PorterDuff.Mode.SRC_ATOP);
        tvIcon.get(0).enableDoubleText(colorUI);

        final TitleBarUtil titleBarUtil = new TitleBarUtil(this, true, true).setTitle(getResources().getString(R.string.app_name));
        titleBarUtil.init();
        initView();
        initData();
        initListener();
        initViewPager();
        toolbar_bar_main.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout layout = (LinearLayout) findViewById(R.id.ll_top);
                ((ViewGroup.MarginLayoutParams) layout.getLayoutParams()).topMargin = titleBarUtil.getSystemBarHeight();
                ((ViewGroup.MarginLayoutParams) findViewById(R.id.rltLyt_main_content).getLayoutParams()).bottomMargin = titleBarUtil.getNavigationBarHeight();
            }
        });
    }

    private void initData() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        Bitmap bitmap1 = round_util.toRoundBitmap(bitmap);
        main_menu.setImageBitmap(bitmap1);

        View headerView = navigationView.getHeaderView(0);//获取头布局
        ImageView person = (ImageView) headerView.findViewById(R.id.person);
        if (person != null) {
            Bitmap per = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
            Bitmap mPer = round_util.toRoundBitmap(per);
            person.setImageBitmap(mPer);
        }


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //item.setChecked(true);
                Toast.makeText(MainActivity.this, item.getTitle().toString().trim(), Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });


        fragments = new ArrayList<>();
        for (int i = 0; i < view.length; i++) {
            fragments.add(view[i]);
            mainViewPager.setCurrentItem(i);
        }
    }

    private void initViewPager() {
        mainViewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
       /* for (int i =0 ; i < fragments.size(); i++) {
                vp.setCurrentItem(i);
        }*/

    }

    private void initView() {
//        AlertDialog


        mainViewPager = (BaseViewPager) findViewById(R.id.vp_main_content);

        navigationView = (NavigationView) findViewById(R.id.nav);
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_na);

        //viewList.add(home);

        main_menu = (ImageView) findViewById(R.id.main_menu);
        main_menu.setOnClickListener(this);
        bt_school = (TextView) findViewById(R.id.bt_school);
        bt_school.setOnClickListener(this);
        toolbar_bar_main = (Toolbar) findViewById(R.id.toolbar_bar);
        toolbar_bar_main.setOnClickListener(this);
    }

    public void setText(final String item1, final String item2) {
        //mTextView.setText(item1 + "   " + item2);
        // new MainFragment().closeDownMenu();

        if (item2.equals("社团介绍")) {
            startActivity(new Intent(MainActivity.this, Society_Activity.class));
        }

        Toast.makeText(getApplicationContext(), item1 + "" + item2, 0).show();
/*
        if (new MainFragment().isAdded()) {
            new MainFragment().closeDownMenu();
            Toast.makeText(getApplicationContext(), item1 + "" + item2, 0).show();
        }*/
    }


    private Bitmap getOffsetBitmap(Bitmap source, Bitmap target, float positionOffset) {
        positionOffset *= 2f;
        Bitmap bm = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        paint.setAlpha((int) (255 * positionOffset));
        canvas.drawBitmap(source, 0, 0, paint);
        paint.setAlpha((int) (255 * (1 - positionOffset)));
        canvas.drawBitmap(target, 0, 0, paint);

        return bm;
    }

    private void resetImage(int index) {
        for (int i = 0; i < imgIcon.size(); ++i) {
            LinearLayout layout = iconLayout.get(i);
            ImageView img = imgIcon.get(i);
            NavigationTextView tv = tvIcon.get(i);
            if (index == i) {
                img.setColorFilter(colorUI, PorterDuff.Mode.SRC_ATOP);
                img.setImageBitmap(targetIcon.get(index));
                tv.enableDoubleText(colorUI);
                layout.setEnabled(false);
            } else {
                img.setColorFilter(null);
                img.setImageBitmap(sourceIcon.get(i));
                tv.enableDoubleText(-1);
                layout.setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == 2) {
                // Intent i = getIntent();
                SchoolInfo s = data.getParcelableExtra(Public.SELECT);
                if (null == s) {
                    return;
                }
                String string1 = s.getName();

                // Bundle date = i.getBundleExtra("date");
                // String s = date.getParcelable(Public.SELECT);
                //String s = i.getStringExtra(Public.SELECT);
                bt_school.setText(string1);
                //bt_school.setBackgroundColor(Color.GREEN);
                bt_school.setTextColor(Color.BLACK);
                Toast.makeText(getApplicationContext(), string1, Toast.LENGTH_SHORT).show();
            }

        }


    }

    private void initListener() {
        for (LinearLayout layout : iconLayout)
            layout.setOnClickListener(this);
        findViewById(R.id.img_main_addWayBill).setOnClickListener(this);
        mainViewPager.setOnPageChangeListener(onPageChangeListener);
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        private int lastPosition = 0;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            ImageView leftIcon;
            ImageView rightIcon;
            NavigationTextView leftText;
            NavigationTextView rightText;

            if (clickPosition == -1 && positionOffset > 0) {
                leftIcon = imgIcon.get(position);
                rightIcon = imgIcon.get(position + 1);
                leftText = tvIcon.get(position);
                rightText = tvIcon.get(position + 1);

                Bitmap sourceBm, targetBm;
                int rgb = (colorUI & 0xffffff);
                int alpha = (int) (255 * positionOffset);
                int color = rgb ^ (alpha << 24);

                rightText.enableDoubleText(color);
                leftText.enableDoubleText(rgb ^ ((255 - alpha) << 24));
                alpha *= 2;

                if (alpha <= 255) {
                    color = rgb ^ (alpha << 24);
                    rightIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    sourceBm = sourceIcon.get(position);
                    targetBm = targetIcon.get(position);
                    leftIcon.setImageBitmap(getOffsetBitmap(sourceBm, targetBm, positionOffset));
                } else {
                    alpha = 255 - (alpha - 255);
                    color = rgb ^ (alpha << 24);
                    leftIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    position += 1;
                    sourceBm = sourceIcon.get(position);
                    targetBm = targetIcon.get(position);
                    rightIcon.setImageBitmap(getOffsetBitmap(targetBm, sourceBm, positionOffset));
                }
            } else if (positionOffset == 0) {
                clickPosition = -1;
                //stateChanged要慢于offset
                if (lastPosition != position)      //去除因回调密度导致的残影
                    resetImage(position);
                lastPosition = position;
            }
        }

        @Override
        public void onPageSelected(final int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lnrLyt_main_menu_home:
                mainViewPager.setCurrentItem(0);
                resetImage(0);
                clickPosition = 0;
                anClickList.get(0).start();
                break;
            case R.id.lnrLyt_main_menu_address:
                mainViewPager.setCurrentItem(1);
                resetImage(1);
                clickPosition = 1;
                anClickList.get(1).start();
                break;
            case R.id.lnrLyt_main_menu_location:
                mainViewPager.setCurrentItem(2);
                resetImage(2);
                clickPosition = 2;
                anClickList.get(2).start();
                break;
            case R.id.lnrLyt_main_menu_personal:
                mainViewPager.setCurrentItem(3);
                resetImage(3);
                clickPosition = 3;
                anClickList.get(3).start();
                break;
            case R.id.img_main_addWayBill:
                Intent intent = new Intent(MainActivity.this, AddSocietyActivity.class);
                startActivity(intent);
                break;
            case R.id.bt_school:
                Intent intent1 = new Intent(MainActivity.this, SchoolSelectActivity.class);
                startActivityForResult(intent1, 1);
                break;
            case R.id.main_menu:
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                }else drawerLayout.openDrawer(navigationView);
                break;
        }
    }

    private class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

}
