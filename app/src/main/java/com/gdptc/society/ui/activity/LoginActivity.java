package com.gdptc.society.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.AccountInfo;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.manager.DirectoryManager;
import com.gdptc.society.tools.InputUtil;
import com.gdptc.society.tools.SecurityUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.ui.dialog.LoadingDialog;
import com.gdptc.society.ui.dialog.MergeDialog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.telephonyManager;

public class LoginActivity extends BaseActivity implements View.OnClickListener,
        TextWatcher, View.OnFocusChangeListener {

    private EditText edtTxtPhone, edtTxtPsw;
    private ImageView imgDeletePhone, imgDeletePsw, imgPswLook;
    private int focusId;
    private InputUtil inputUtil;
    private LoadingDialog loadingDialog;
    private List<EditText> editTextList;
    private DBManager.NetworkDBAdapter dbAdapter;
    private SecurityUtil securityUtil;
    private String toastMsg;
    private boolean pswVisit = false;
    private SharedPreferences.Editor editor;

    private MergeDialog remindDialog;

    private String cry;

    private ApiServer apiServer;

    private String[] errMsg = {"手机号", "密码"};

    private Runnable toastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private Runnable startRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableTransition();
        setContentView(R.layout.activity_login);

        securityUtil = new SecurityUtil();
        new TitleBarUtil(this, true).setTarget(findViewById(android.R.id.content)).init();
        inputUtil = new InputUtil(this);
        loadingDialog = new LoadingDialog(this);
        editTextList = new ArrayList<>();
        dbAdapter = DBManager.getInstance().openForNetWork(1, dbResultListener);
        dbAdapter.open();
        apiServer = ApiServer.getInstance();

        findViewById(android.R.id.content).setBackgroundResource(R.drawable.login_bg);

        initUI();
        setListener();

        addFilterView(edtTxtPhone);
        addFilterView(edtTxtPsw);

        SharedPreferences sharedPreferences = getSharedPreferences(DirectoryManager.CFG_INFO, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String num = sharedPreferences.getString(DirectoryManager.INFO_PHONE, null);
        cry = sharedPreferences.getString(DirectoryManager.INFO_PSW, null);

        if ((num != null && cry != null) || sharedPreferences.getString(DirectoryManager.INFO_QQ, null) != null) {
            try {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            } catch (Exception e) {
                cry = null;
                e.printStackTrace();
            }
        }

        /*if (num == null)
            num = telephonyManager.getLine1Number();

        if (num != null) {
            if (num.length() > 11)
                num = num.substring(num.length() - 12);
            edtTxtPhone.setText(num);
            edtTxtPhone.setSelection(edtTxtPhone.length());
            imgDeletePhone.setVisibility(View.VISIBLE);
        }*/


        if (getIntent().getBooleanExtra(Public.ERROR, false)) {
            remindDialog = new MergeDialog(this);
            remindDialog.setTitle("登录异常提醒");
            remindDialog.setMessage("您的账号在另一台设备上登录，如果不是您的操作，建议你及时修改密码");
            remindDialog.setCancelable(true);
            remindDialog.setPositiveButton("确定", this);
            remindDialog.show();
        }
    }

    private void initUI() {
        edtTxtPhone = (EditText) findViewById(R.id.edtTxt_login_phone);
        edtTxtPsw = (EditText) findViewById(R.id.edtTxt_login_psw);
        imgDeletePhone = (ImageView) findViewById(R.id.img_login_delete_phone);
        imgDeletePsw = (ImageView) findViewById(R.id.img_login_delete_psw);
        imgPswLook = (ImageView) findViewById(R.id.img_login_psw_look);

        //findViewById(R.id.cstLyt_login_mainLayout).setBackgroundColor(Value.getColorUI().toValue());

        editTextList.add(edtTxtPhone);
        editTextList.add(edtTxtPsw);
    }

    private void setListener() {
        findViewById(R.id.tv_login_register).setOnClickListener(this);
        findViewById(R.id.btn_login_submit).setOnClickListener(this);

        imgDeletePhone.setOnClickListener(this);
        imgPswLook.setOnClickListener(this);
        imgDeletePsw.setOnClickListener(this);

        edtTxtPhone.addTextChangedListener(this);
        edtTxtPsw.addTextChangedListener(this);

        edtTxtPhone.setOnFocusChangeListener(this);
        edtTxtPsw.setOnFocusChangeListener(this);

        InputFilter[] pswFilters = {inputUtil.spaceFilter};
        InputFilter[] phoneFilters = {inputUtil.spaceFilter, new InputFilter.LengthFilter(11)};
        edtTxtPsw.setFilters(pswFilters);
        edtTxtPhone.setFilters(phoneFilters);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        int oldFocusId;

        switch (v.getId()) {
            case R.id.tv_login_register:
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.alpha_in_activity, R.anim.alpha_out_activity);
                break;
            case R.id.img_login_delete_phone:
                oldFocusId = focusId;
                focusId = edtTxtPhone.getId();
                edtTxtPhone.setText(null);
                focusId = oldFocusId;
                break;
            case R.id.img_login_delete_psw:
                oldFocusId = focusId;
                focusId = edtTxtPsw.getId();
                edtTxtPsw.setText(null);
                focusId = oldFocusId;
                break;
            case R.id.img_login_psw_look:
                int selectIndex = edtTxtPsw.getSelectionStart();
                edtTxtPsw.setInputType(pswVisit ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                        : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                edtTxtPsw.setSelection(selectIndex);
                pswVisit = !pswVisit;
                imgPswLook.setImageResource(pswVisit ? R.drawable.show : R.drawable.hide);
                break;
            case R.id.btn_login_submit:
                if (inputUtil.checkNull(this, editTextList, errMsg)) {
                    if (dbAdapter.isOpen()) {
                        loadingDialog.show(true);
                        loadingDialog.setMessage("正在登入...");
                        apiServer.login(edtTxtPhone.getText().toString(), edtTxtPsw.getText().toString(), lgCallBack);
                    } else {
                        startRunnable = new Runnable() {
                            @Override
                            public void run() {
                                apiServer.login(edtTxtPhone.getText().toString(), edtTxtPsw.getText().toString(), lgCallBack);
                            }
                        };
                        dbAdapter.open();
                    }
                }
                break;
            case MergeDialog.POSITIVE_ID:
                remindDialog.dismiss();
                break;
            default:
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (focusId == edtTxtPhone.getId())
            imgDeletePhone.setVisibility(edtTxtPhone.length() > 0 ? View.VISIBLE : View.GONE);
        else
            imgDeletePsw.setVisibility(edtTxtPsw.length() > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus)
            focusId = v.getId();
    }

    private ApiServer.LoginCallBack lgCallBack = new ApiServer.LoginCallBack() {
        @Override
        public void onLogin(boolean isComplete) {
            loadingDialog.dismiss();
            editor.putString(DirectoryManager.INFO_PHONE, edtTxtPhone.getText().toString());
            String psw = cry == null ? edtTxtPsw.getText().toString() : cry;
            String ecp = null;
            try {
                ecp = securityUtil.AesEncrypt(psw);
            } catch (Exception e) {
                e.printStackTrace();
            }
            editor.putString(DirectoryManager.INFO_PSW, ecp == null ? psw : ecp).commit();
            Intent intent = new Intent(LoginActivity.this, isComplete ? MainActivity.class : CompleteInfoActivity.class);
            startActivity(intent);
            finish();
        }

        @Override
        public void onCancel() {
            loadingDialog.dismiss();
        }

        @Override
        public void onFailure(String msg) {
            toastMsg = msg;
            runOnUiThread(toastRunnable);
            if (loadingDialog.isShowing())
                loadingDialog.dismiss();
        }
    };

    private DBManager.ResultListener dbResultListener = new DBManager.ResultListener() {
        @Override
        public void dbOpen() {
            if (startRunnable != null)
                runOnUiThread(startRunnable);
        }

        @Override
        public void insert(Object o1, Object o2, int result) {
        }

        @Override
        public void query(Object o1, Object o2, ResultSet resultSet) {

        }

        @Override
        public void onFailure(Object o1, Object o2, Exception e) {
            if (!dbAdapter.isOpen()) {
                toastMsg = "连接数据库失败";
                runOnUiThread(toastRunnable);
            }
            e.printStackTrace();
        }
    };

    @Override
    public void finish() {
        dbAdapter.close();
        super.finish();
    }

}
