package com.gdptc.society.tools;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gdptc.society.R;

/**
 * Created by Administrator on 2018/3/23/023.
 */

public class SearchBoxUtil {
    private RelativeLayout rltLytBox;
    private EditText edtTxtInput;
    private ImageView imgInputDel, imgSearchPic;
    private TextView tvSearchTitle;
    private LinearLayout lytSearchTitle;

    private Animation offsetAnimation;

    private String hintTxt;

    private View.OnFocusChangeListener listener;

    private InputUtil inputUtil;
    private Activity activity;

    private Intent intent;

    public SearchBoxUtil(Activity activity) {
        this.activity = activity;
        inputUtil = new InputUtil(activity);
        rltLytBox = (RelativeLayout) activity.findViewById(R.id.rltLyt_search_layout);
        edtTxtInput = (EditText) activity.findViewById(R.id.edtTxt_search_input);
        imgInputDel = (ImageView) activity.findViewById(R.id.img_search_delInput);
        imgSearchPic = (ImageView) activity.findViewById(R.id.img_item_search_box_searchLogo);
        lytSearchTitle = (LinearLayout) activity.findViewById(R.id.lnrLyt_item_search_box_content);
        tvSearchTitle = lytSearchTitle.findViewById(R.id.tv_item_search_box_searchTitle);

        edtTxtInput.setFilters(new InputFilter[] { inputUtil.spaceFilter });
        imgInputDel.setOnClickListener(onClickListener);

        edtTxtInput.setOnFocusChangeListener(focusChangeListener);
        edtTxtInput.addTextChangedListener(textWatcher);
        offsetAnimation = AnimationUtils.loadAnimation(activity, R.anim.search_title_offset);
        offsetAnimation.setAnimationListener(animationListener);
    }

    public void pauseSearchActivity(Intent intent) {
        this.intent = intent;
    }

    public void setTitle(String title) {
        tvSearchTitle.setText(title);
    }

    public void showTitle(boolean show) {
        tvSearchTitle.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showSearchPic(boolean show) {
        imgSearchPic.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setText(String s) {
        edtTxtInput.setText(s);
    }

    public void setHint(String s) {
        hintTxt = s;
        if (lytSearchTitle.getAnimation() != null)
            edtTxtInput.setHint(s);
    }

    public void setInputType(int type) {
        edtTxtInput.setInputType(type);
    }

    public Editable getText() {
        return edtTxtInput.getText();
    }

    public void addTextChangedListener(TextWatcher watcher) {
        edtTxtInput.addTextChangedListener(watcher);
    }

    public void setOnFocusChangeListener(View.OnFocusChangeListener focusChangeListener) {
        listener = focusChangeListener;
    }

    public void setVisibility(int visibility) {
        rltLytBox.setVisibility(visibility);
    }

    public int length() {
        return edtTxtInput.length();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (edtTxtInput.length() == 0)
                imgInputDel.setVisibility(View.GONE);
            else
                imgInputDel.setVisibility(View.VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                if (intent != null) {
                    edtTxtInput.setFocusable(false);
                    edtTxtInput.setFocusableInTouchMode(false);
                    activity.startActivity(intent);
                    edtTxtInput.setFocusable(true);
                    edtTxtInput.setFocusableInTouchMode(true);
                }
                else {
                    edtTxtInput.setFocusable(false);
                    edtTxtInput.setFocusableInTouchMode(false);
                    tvSearchTitle.setVisibility(View.GONE);
                    lytSearchTitle.startAnimation(offsetAnimation);
                }
            }

            if (listener != null)
                listener.onFocusChange(v, hasFocus);
        }
    };

    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (animation.equals(offsetAnimation)) {
                edtTxtInput.setFocusable(true);
                edtTxtInput.setFocusableInTouchMode(true);
                edtTxtInput.setOnFocusChangeListener(null);
                edtTxtInput.requestFocus();
                edtTxtInput.requestFocusFromTouch();
                edtTxtInput.setHint(hintTxt);
                inputUtil.openKeyBord(edtTxtInput);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == imgInputDel.getId())
                edtTxtInput.setText(null);
        }
    };

}
