package com.gdptc.society.ui.activity;

import android.os.Bundle;
import android.widget.ScrollView;

import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.tools.TitleBarUtil;

public class Society_Activity extends BaseActivity {
    ScrollView svContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society);
        new TitleBarUtil(this, true, true).setTitle("社团介绍").init();

        svContent = (ScrollView) findViewById(R.id.sv_society_content);
        //svContent.smoothScrollTo();
    }
}
