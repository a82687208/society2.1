package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;

/**
 * Created by Administrator on 2018/3/10/010.
 */

public class AlarmActivity extends BaseActivity {
    private boolean exit;
    private CardView cardView;

    private Animation in;
    private Animation out;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        Intent date = getIntent();
        exit = date.getBooleanExtra(Public.EXIT, false);
        cardView = (CardView) findViewById(R.id.cv_alarm_mainLayout);
        TextView textView = (TextView) findViewById(R.id.tv_alarm_title);
        textView.setText(date.getStringExtra(Public.TITLE));
        textView = (TextView) findViewById(R.id.tv_alarm_msg);
        textView.setText(date.getStringExtra(Public.MSG));
        findViewById(R.id.tv_alarm_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        in = AnimationUtils.loadAnimation(this, R.anim.alarm_modal_in);
        out = AnimationUtils.loadAnimation(this, R.anim.alarm_modal_out);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (exit)
                    ApiServer.getInstance().logout();
                else
                    finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        cardView.startAnimation(in);
    }

    @Override
    public void onBackPressed() {
        cardView.startAnimation(out);
    }

}
