package com.gdptc.society.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.basemodel.Value;
import com.example.tools.MaterialDesignCompat;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.AccountInfo;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.apiServer.SchoolInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.manager.DirectoryManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.CameraUtil;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.InputUtil;
import com.gdptc.society.tools.NativeUtil;
import com.gdptc.society.tools.RandomUtil;
import com.gdptc.society.tools.SecurityUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.tools.TypedValueUtil;
import com.gdptc.society.ui.dialog.AlbumDialog;
import com.gdptc.society.ui.dialog.LoadingDialog;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.directoryManager;

public class CompleteInfoActivity extends BaseActivity implements View.OnClickListener {
    private final int REQUEST_ALBUM_CODE = 0x00000003;
    private final int REQUEST_IMG_CUT_CODE = 0x00000004;

    private final String[] errMsg = { "昵称", "电子邮件", "姓名", "证件号" };
    private List<EditText> editTextList = new ArrayList<>();

    private DBManager.NetworkDBAdapter dbAdapter;

    private EditText edtTxtUserName;
    private Button btnMan, btnGirl, btnSubmit;
    private ImageView imgUserPic;

    private RadioGroup rgSelector;
    private RadioButton rbStu, rbManager;

    private TextView tvSchoolName;

    private File filePic;

    private String userPicPath, selectSex = "男";

    private InputUtil inputUtil;
    private ApplicationManager applicationManager;
    private LoadingDialog loadingDialog;
    private AlbumDialog albumDialog;

    private AsyncLoader asyncLoader;

    private int UIColor, colorSearchBtn;

    private String toastMsg;
    private Drawable dwBtnPress;
    private Drawable dwBtnNoPress;

    private long schoolId = 0;
    private long userPidId;

    private AccountInfo accountInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_info);

        accountInfo = ApiServer.getAccountInfo();
        inputUtil = new InputUtil(this);
        new TitleBarUtil(this, true).setTitle("完善个人资料").init();
        applicationManager = (ApplicationManager) getApplication();

        dbAdapter = DBManager.getInstance().openForNetWork();
        asyncLoader = new AsyncLoader(this, 1, dbAdapter);
        dbAdapter.open();

        colorSearchBtn = getResources().getColor(R.color.colorSearchBtn);
        dwBtnNoPress = getResources().getDrawable(R.drawable.btn_search_nopress);
        initUI();
        setListener();
        onUIColorChange(Value.getColorUI());
    }

    private void initUI() {
        rgSelector = (RadioGroup) findViewById(R.id.rg_complete_info);
        rbStu = (RadioButton) findViewById(R.id.rb_complete_info_stu);
        rbManager = (RadioButton) findViewById(R.id.rb_complete_info_manager);
        edtTxtUserName = (EditText) findViewById(R.id.edtTxt_complete_info_userName);
        btnMan = (Button) findViewById(R.id.btn_complete_info_sex_man);
        btnGirl = (Button) findViewById(R.id.btn_complete_info_sex_girl);
        imgUserPic = (ImageView) findViewById(R.id.crlImg_complete_info_userPic);
        imgUserPic.setColorFilter(Value.getColorUI().toValue());
        tvSchoolName = (TextView) findViewById(R.id.tv_complete_info_school);
        btnSubmit = (Button) findViewById(R.id.btn_complete_info_submit);
        MaterialDesignCompat.addShadow(btnSubmit, 0, (int) TypedValueUtil.dip2px(this, 2),
                TypedValueUtil.dip2px(this, 3));

        imgUserPic.measure(0, 0);

        if (accountInfo.getUserPic() > 0) {
            imgUserPic.setTag(accountInfo.getUserPic());
            Bitmap bitmap = asyncLoader.loadImgForDB(imgUserPic, accountInfo.getUserPic(),
                    imgUserPic.getMeasuredWidth(), imgUserPic.getMeasuredHeight(), ImageUtil.MODE.ROUND);

            if (bitmap != null) {
                imgUserPic.setImageBitmap(bitmap);
                imgUserPic.setColorFilter(null);
            }
        }

        edtTxtUserName.setText(accountInfo.getName());
        if (accountInfo.getSex() != null) {
            if (accountInfo.getSex().equals("男"))
                changeBg(btnMan, btnGirl);
            else
                changeBg(btnGirl, btnMan);
        }

        loadingDialog = new LoadingDialog(this);
        albumDialog = new AlbumDialog(this);
    }

    private void setListener() {
        asyncLoader.setImageLoadListener(imageLoadListener);
        btnMan.setOnClickListener(this);
        btnGirl.setOnClickListener(this);
        imgUserPic.setOnClickListener(this);
        tvSchoolName.setOnClickListener(this);
        albumDialog.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
    }

    private ApiServer.VerificationCallBack vcCallBack = new ApiServer.VerificationCallBack() {
        @Override
        public void onSuccess() {
            SharedPreferences sharedPreferences = getSharedPreferences(DirectoryManager.CFG_INFO, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Intent data = getIntent();
            String phone = accountInfo.getPhone();
            String psw = data.getStringExtra(Public.PSW);

            if (psw != null && phone != null) {
                editor.putString(DirectoryManager.INFO_PHONE, phone);
                String ecp = null;
                try {
                    ecp = new SecurityUtil().AesEncrypt(psw);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                editor.putString(DirectoryManager.INFO_PSW, ecp == null ? psw : ecp).commit();
            }
            applicationManager.removeActivity(CompleteInfoActivity.this);
            applicationManager.removeAllActivity();
            Intent intent = new Intent(CompleteInfoActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        @Override
        public void onFailure(String msg) {
            toastMsg = msg;
            runOnUiThread(toastRub);
        }
    };

    private Runnable submitRub = new Runnable() {
        @Override
        public void run() {
            try {
                ContentValues contentValues = new ContentValues();
                userPidId = Long.valueOf(RandomUtil.getRandomId(DBManager.IMAGE_ID_LENGTH));
                dbAdapter.uploadImage(userPidId, userPicPath);
                contentValues.put(DBManager.ACCOUNT_PIC, userPidId);

                String name = edtTxtUserName.getText().toString();
                name = name.length() == 0 ? "用户: " + accountInfo.getPhone() : name;

                contentValues = new ContentValues();
                contentValues.put(DBManager.ACCOUNT_SCHOOL, schoolId);
                contentValues.put(DBManager.ACCOUNT_NAME, name);
                contentValues.put(DBManager.ACCOUNT_SEX, selectSex);
                //contentValues.put(DBManager.ACCOUNT_STU_ID, );
                if (accountInfo.getPhone() == null || accountInfo.getPhone().equals(""))
                    contentValues.put(DBManager.ACCOUNT_ADMIN, rgSelector.getCheckedRadioButtonId() == R.id.rb_complete_info_stu ? 0 : 1);
                ApiServer.getInstance().completeInfo(contentValues, vcCallBack);
            }
            catch (Exception e) {
                toastMsg = "提交错误，请联系运营商";
                runOnUiThread(toastRub);
                e.printStackTrace();
            }
        }
    };

    private Runnable toastRub = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(CompleteInfoActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
            if (loadingDialog.isShowing())
                loadingDialog.dismiss();
        }
    };

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(Public.PAUSE, false))
            applicationManager.removeAllActivity();
        super.onBackPressed();
    }

    private void changeBg(Button press, Button noPress) {
        selectSex = press.getText().toString().trim();
        press.setBackgroundDrawable(dwBtnPress);
        press.setTextColor(UIColor);
        noPress.setBackgroundDrawable(dwBtnNoPress);
        noPress.setTextColor(colorSearchBtn);
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.btn_complete_info_sex_man:
                changeBg(btnMan, btnGirl);
                break;
            case R.id.btn_complete_info_sex_girl:
                changeBg(btnGirl, btnMan);
                break;
            case R.id.crlImg_complete_info_userPic:
                albumDialog.show();
                break;
            case R.id.tv_complete_info_school:
                enableTransition();
                intent = new Intent(CompleteInfoActivity.this, SchoolSelectActivity.class);
                startActivityForResult(intent, 2);
                break;
            case R.id.btn_complete_info_submit:
                if (inputUtil.checkNull(this, editTextList, errMsg)) {
                    if (schoolId == 0)
                        Toast.makeText(this, "您尚未选择学校", Toast.LENGTH_SHORT).show();
                    else if (userPicPath == null && accountInfo.getUserPic() < 1)
                        Toast.makeText(this, "您尚未上传头像", Toast.LENGTH_SHORT).show();
                    else {
                        loadingDialog.show(false);
                        loadingDialog.setMessage("正在上传图片...");
                        new Thread(submitRub).start();
                    }
                }
                break;
            case AlbumDialog.FIRST_SELECTOR_ID:
                enableTransition();
                filePic = new File(directoryManager.getSafePicPath() + "/" + RandomUtil.getRandomString() + ".png");
                while (filePic.exists())
                    filePic = new File(RandomUtil.getRandomString());
                CameraUtil.takePicture(this, filePic);
                break;
            case AlbumDialog.SECOND_SELECTOR_ID:
                enableTransition();
                intent = new Intent(CompleteInfoActivity.this, AlbumGroupActivity.class);
                intent.putExtra(Public.RADIO, true);
                startActivityForResult(intent, REQUEST_ALBUM_CODE);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 2) {
            SchoolInfo schoolInfo = data.getParcelableExtra(Public.SELECT);
            schoolId = schoolInfo.getCollege_id();
            tvSchoolName.setText(schoolInfo.getName());
            tvSchoolName.setTextColor(Color.BLACK);
        }
        else if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMG_CUT_CODE:
                    String key = data.getStringExtra(Public.CONTEXT);
                    Bitmap bitmap = (Bitmap) applicationManager.dataMap.get(key);
                    File file = new File(directoryManager.getSafePicPath() + "/" + RandomUtil.getRandomString() + ".png");
                    while (file.exists())
                        file = new File(directoryManager.getSafePicPath() + "/" + RandomUtil.getRandomString() + ".png");

                    try {
                        NativeUtil.compressBitmap(bitmap, file.getPath(), true);
                        applicationManager.dataMap.remove(key);
                        userPicPath = file.getPath();
                        imgUserPic.setImageBitmap(ImageUtil.getRoundBitmap(bitmap));
                        imgUserPic.setColorFilter(null);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case REQUEST_ALBUM_CODE:
                    startPreViewActivity(data.getStringExtra(Public.PATH));
                    break;
                case CameraUtil.CAMERA_REQUEST_CODE:
                    startPreViewActivity(filePic.getPath());
                    break;
                default:
                    break;
            }
        }
    }

    private void startPreViewActivity(String path) {
        Intent intent = new Intent(CompleteInfoActivity.this, PreViewActivity.class);
        intent.putExtra(Public.PATH, path);
        intent.putExtra(Public.ICON, true);
        disableTransition();
        startActivityForResult(intent, REQUEST_IMG_CUT_CODE);
    }

    private AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            ImageView imageView = (ImageView) parent;
            if (imageView.getTag().equals(id))
                imageView.setImageBitmap(bitmapInfo.getBitmap(CompleteInfoActivity.this, imageView));

            asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, mode);
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {

        }
    };

    @Override
    public void onUIColorChange(Value.COLOR color) {
        UIColor = color.toValue();
        int[] c = { UIColor, UIColor };
        int[][] states = new int[2][];
        states[0] = new int[]{ android.R.attr.state_pressed };
        states[1] = new int[]{ android.R.attr.state_enabled };
        ColorStateList stateList = new ColorStateList(states, c);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rbStu.setButtonTintList(stateList);
            rbManager.setButtonTintList(stateList);
        }
        Resources resources = getResources();
        switch (color) {
            case BLUE:
                dwBtnPress = resources.getDrawable(R.drawable.btn_search_press_blue);
                btnSubmit.setBackgroundDrawable(resources.getDrawable(R.drawable.normal_button_blue));
                break;
            case PINK:
                dwBtnPress = resources.getDrawable(R.drawable.btn_search_press_pink);
                btnSubmit.setBackgroundDrawable(resources.getDrawable(R.drawable.normal_button_pink));
                break;
            case GREEN:
                dwBtnPress = resources.getDrawable(R.drawable.btn_search_press_green);
                btnSubmit.setBackgroundDrawable(resources.getDrawable(R.drawable.normal_button_green));
                break;
        }
        if (selectSex.equals("男"))
            changeBg(btnMan, btnGirl);
        else
            changeBg(btnGirl, btnMan);
    }

    @Override
    public void finish() {
        dbAdapter.close();
        super.finish();
    }
}
