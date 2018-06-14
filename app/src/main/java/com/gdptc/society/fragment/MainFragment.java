package com.gdptc.society.fragment;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gdptc.society.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements View.OnClickListener {
    //  private Button mButton1, mButton2, mButton3;
    // private FrameLayout mDownMuneContainer;
    private View home;

    private Button mButton1;
    private Button mButton2;
    private Button mButton3;
    private int checkedbutton = -1;
    private FrameLayout mDownMuneContainer;
    //  private TextView mTextView;
    private Fragment mCurrentFragment;

    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        home = inflater.inflate(R.layout.fragment_main, container, false);
        initView();
        initData();
        initListener();
        return home;
    }

    private void initData() {

    }

    private void initView() {
//        AlertDialog
        mButton1 = (Button) home.findViewById(R.id.b1);
        mButton2 = (Button) home.findViewById(R.id.b2);
        mButton3 = (Button) home.findViewById(R.id.b3);
        // mTextView = (TextView) findViewById(R.id.tv_content);
        mDownMuneContainer = (FrameLayout) home.findViewById(R.id.down_menu_container);


    }

    public void setText(String item1, String item2) {
        closeDownMenu();
        // mTextView.setText(item1 + "   " + item2);
//        new ContentContainerCard(item1,item2);
    }


    private void initListener() {

        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        mButton3.setOnClickListener(this);
        FragmentManager fm = getFragmentManager();

        // 开启事务
        FragmentTransaction transaction = fm.beginTransaction();

        // 加载左侧
        //                transaction.replace(R.id.down_menu_container, MenuFragmentFactory.getFragment(id));
        /**-----------------------------解决闪烁的问题start--------------------------------------------------**/
        Fragment addressFragment = new AddressFragemnt();
        Fragment catalogueFragment = new CatalogueFragment();
        Fragment sortFragment = new SortFragment();
        transaction.add(R.id.down_menu_container, addressFragment, R.id.b1 + "");
        transaction.add(R.id.down_menu_container, catalogueFragment, R.id.b2 + "");
        transaction.add(R.id.down_menu_container, sortFragment, R.id.b3 + "");
        transaction.hide(addressFragment);
        transaction.hide(catalogueFragment);
        transaction.hide(sortFragment);
        transaction.commit();
        /**-----------------------------解决闪烁的问题end--------------------------------------------------**/
    }

    private void chooseFragmentById(int id) {
//        FragmentManager fm = getSupportFragmentManager();
//
//        // 开启事务
//        FragmentTransaction transaction = fm.beginTransaction();
//
//        // 加载左侧
//        transaction.replace(R.id.down_menu_container, MenuFragmentFactory.getFragment(id));
//
//        transaction.commit();
        /**------------------------闪屏解决------------------------------*/
        FragmentManager fm = getFragmentManager();

        // 开启事务
        FragmentTransaction transaction = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(id + "");

        boolean isShowOrAdd = true;

        if (fragment == null) {
            isShowOrAdd = false;
            switch (id) {
                case R.id.b1:
                    // 目录
                    fragment = new CatalogueFragment();
                    break;
                case R.id.b2:
                    // 地区
                    fragment = new AddressFragemnt();
                    break;
                case R.id.b3:
                    // 排序
                    fragment = new SortFragment();
                    break;
                default:
                    break;
            }
        }
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }
        if (isShowOrAdd) {
            transaction.show(fragment);
//            fragment.onResume();
        } else {
            transaction.add(R.id.down_menu_container, fragment, id + "");
        }
        mCurrentFragment = fragment;
        // 加载左侧
//        transaction.replace(R.id.down_menu_container, MenuFragmentFactory.getFragment(id));

        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        setButtonColor(v.getId());
        //连续点击同一个button;
        if (checkedbutton == v.getId()) {
            DismissAnimtion(mDownMuneContainer);
            checkedbutton = -1;
            return;
        } else {
            //显示mDownMuneContainer
            //可能是第一次显示，可能之前就显示过了 根据checkedbutton判断
            /**----------解决闪烁的问题----------**/
//            mDownMuneContainer.removeAllViews();
//            MenuFragmentFactory.removeFragment(v.getId());
            /**---------------------------------**/
            showAnimtion(mDownMuneContainer, checkedbutton);
            Log.d("mDownMuneContainer", "mDownMuneContainer");
            showItem(v.getId());


            checkedbutton = v.getId();
        }
    }

    private void showAnimtion(final View view, int checkedbutton) {
        Log.e("checkedbutton", checkedbutton + "");
        if (checkedbutton == -1) {
            AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
            //@integer/abc_config_activityShortDur"
            alpha.setDuration(300);
            alpha.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.startAnimation(alpha);
        }
    }

    private void DismissAnimtion(final View view) {
        AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
        //@integer/abc_config_activityShortDur"
        alpha.setDuration(300);
        alpha.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(alpha);
    }

    public void closeDownMenu() {
        setBtnClose();
        DismissAnimtion(mDownMuneContainer);
//        mDownMuneContainer.setVisibility(View.GONE);
        checkedbutton = -1;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public void isTure() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // getResources().getString(R.string.app_name);
                if (isAdded()) {
                    closeDownMenu();
                }

            }

        }.execute();
    }


    private void showItem(int id) {
        chooseFragmentById(id);
    }

    private void setButtonColor(int id) {
        switch (id) {
            case R.id.b1:
                setBtn1();
                break;
            case R.id.b2:
                setBtn2();
                break;
            case R.id.b3:
                setBtn3();
                break;
        }
    }

    public void setBtnClose() {
        mButton1.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        mButton2.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        mButton3.setTextColor(getResources().getColor(R.color.buttonNormalColor));
    }


    private void setBtn1() {
        if (checkedbutton != R.id.b1) {
            mButton1.setTextColor(getResources().getColor(R.color.buttonSelectColor));
            mButton2.setTextColor(getResources().getColor(R.color.buttonNormalColor));
            mButton3.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        } else {
            mButton1.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        }
    }

    private void setBtn2() {
        if (checkedbutton != R.id.b2) {
            mButton2.setTextColor(getResources().getColor(R.color.buttonSelectColor));
            mButton1.setTextColor(getResources().getColor(R.color.buttonNormalColor));
            mButton3.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        } else {
            mButton2.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        }
    }

    private void setBtn3() {
        if (checkedbutton != R.id.b3) {
            mButton3.setTextColor(getResources().getColor(R.color.buttonSelectColor));
            mButton2.setTextColor(getResources().getColor(R.color.buttonNormalColor));
            mButton1.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        } else {
            mButton3.setTextColor(getResources().getColor(R.color.buttonNormalColor));
        }
    }
}
