package com.gdptc.society.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.tools.BtnSendLimitUtil;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.InputUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.ui.dialog.LoadingDialog;

import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.Public.PAUSE;
import static com.gdptc.society.manager.ApplicationManager.telephonyManager;

public class RegisterActivity extends BaseActivity implements View.OnClickListener {
    private TitleBarUtil titleBarUtil;
    private InputUtil inputUtil;
    private EditText edtTxtPhone, edtTxtPsw, edtTxtRePsw;
    private Button btnSubmit;
    private List<EditText> edtTxtList = new ArrayList<>();
    private String[] errMsg = { "手机号码", "密码", "密码" };

    private LoadingDialog loadingDialog;

    private String toastMsg;

    private ApplicationManager applicationManager;
    private ApiServer apiServer;

    private Runnable toastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(RegisterActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableTransition();
        setContentView(R.layout.activity_register);

        findViewById(android.R.id.content).setBackgroundResource(R.drawable.login_bg);

        apiServer = ApiServer.getInstance();
        applicationManager = (ApplicationManager) getApplication();

        titleBarUtil = new TitleBarUtil(this, true, true).setTitle("注册");
        titleBarUtil.init(false);

        inputUtil = new InputUtil(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.constraintLyt_content_register)
                        .setPadding(0, titleBarUtil.getSystemBarHeight(), 0, 0);
            }
        });

        initUI();
        setFilter();
        btnSubmit.setOnClickListener(this);

        String phone = telephonyManager.getLine1Number();
        if (phone != null) {
            edtTxtPhone.setText(phone);
            edtTxtPhone.setSelection(phone.length());
        }
    }

    private void initUI() {
        edtTxtPhone = (EditText) findViewById(R.id.edtTxt_phone_register);
        edtTxtPsw = (EditText) findViewById(R.id.edtTxt_psw_register);
        edtTxtRePsw = (EditText) findViewById(R.id.edtTxt_rePsw_register);
        btnSubmit = (Button) findViewById(R.id.btn_submit_register);

        edtTxtList.add(edtTxtPhone);
        edtTxtList.add(edtTxtPsw);
        edtTxtList.add(edtTxtPsw);

        for (View view : edtTxtList)
            addFilterView(view);

        loadingDialog = new LoadingDialog(this);
    }

    private void setFilter() {
        InputFilter[] inputFilters = new InputFilter[] { inputUtil.spaceFilter };
        edtTxtPhone.setFilters(inputFilters);
        edtTxtPsw.setFilters(inputFilters);
        edtTxtRePsw.setFilters(inputFilters);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.alpha_in_activity, R.anim.alpha_out_activity);
    }

    @Override
    public void onClick(View view) {
        if (inputUtil.checkNull(this, edtTxtList, errMsg)) {
            if (edtTxtPsw.getText().toString().equals(edtTxtRePsw.getText().toString())) {
                loadingDialog.show(false);
                ContentValues contentValues = new ContentValues();
                contentValues.put(DBManager.ACCOUNT_PHONE, edtTxtPhone.getText().toString());
                contentValues.put(DBManager.ACCOUNT_PSW, edtTxtRePsw.getText().toString());
                apiServer.register(contentValues, registerCallBack);
            }
            else
                Toast.makeText(this, "密码不一致", Toast.LENGTH_SHORT).show();
        }
    }

    private ApiServer.RegisterCallBack registerCallBack = new ApiServer.RegisterCallBack() {

        @Override
        public void onRegister() {
            applicationManager.removeActivity(RegisterActivity.this);
            applicationManager.removeAllActivity();
            Intent intent = new Intent(RegisterActivity.this, CompleteInfoActivity.class);
            startActivity(intent);
            loadingDialog.dismiss();
            finish();
        }

        @Override
        public void onFailure(Throwable t, JSONObject js, String msg) {
            toastMsg = msg;
            runOnUiThread(toastRunnable);
            if (loadingDialog.isShowing())
                loadingDialog.dismiss();
        }

    };

}
