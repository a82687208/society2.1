package com.gdptc.society.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;

import com.example.basemodel.BaseRecyclerView;
import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;

/**
 * Created by Administrator on 2018/6/13/013.
 */

public class AddTalkMsg extends BaseActivity {
    private EditText edtTxtContent;
    private BaseRecyclerView rcyPic;
    private Button btnSubmit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_talk_msg);

        initUI();
    }

    private void initUI() {
        edtTxtContent = (EditText) findViewById(R.id.edtTxt_add_talk_msg);
        rcyPic = (BaseRecyclerView) findViewById(R.id.rcy_add_talk_msg_pic);
        btnSubmit = (Button) findViewById(R.id.btn_add_talk_msg);
    }
}
