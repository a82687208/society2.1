package com.gdptc.society.apiServer;

import android.content.ContentValues;

import com.gdptc.society.manager.DBManager;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Administrator on 2018/6/11/011.
 */

public class ApiServer {
    private static ApiServer apiServer;
    private static AccountInfo accountInfo;

    public OkHttpClient okHttpClient;
    private Request.Builder requestBuilder;

    private DBManager.NetworkDBAdapter dbAdapter;

    private String phone, psw;

    private ContentValues contentValues;

    public interface CallBack<T> {
        void onFailure(Call call, Exception e);
        void onResponse(Call call, T object);
    }

    public interface LoginCallBack {
        void onLogin(boolean isComplete);
        void onCancel();
        void onFailure(String msg);
    }

    public interface RegisterCallBack {
        void onRegister();
        void onFailure(Throwable t, JSONObject js, String msg);
    }

    public interface VerificationCallBack {
        void onSuccess();
        void onFailure(String msg);
    }

    public interface ChangeAccountCallBack {
        void onVCodeSend(String msg);
        void onChange();
        void onFailure(Throwable t, JSONObject js, String msg);
    }

    private ApiServer() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(ApiParam.TIMEOUT, TimeUnit.SECONDS);
        okHttpClient = builder.build();
        requestBuilder = new Request.Builder();
        dbAdapter = DBManager.getInstance().openForNetWork(dbResultListener);
        dbAdapter.open();
    }

    public static ApiServer getInstance() {
        if (apiServer == null) {
            apiServer = new ApiServer();
        }
        return apiServer;
    }

    public static AccountInfo getAccountInfo() {
        return AccountInfo.copy(accountInfo);
    }

    public void login(final String phone, final String psw, LoginCallBack callBack) {
        this.phone = phone;
        this.psw = psw;
        dbAdapter.query(DBManager.TABLE.ACCOUNT, null, DBManager.ACCOUNT_PHONE + "=" + phone,
                null, null, null, null, "登入", callBack);
    }

    public void register(ContentValues contentValues, RegisterCallBack registerCallBack) {
        this.contentValues = contentValues;
        dbAdapter.query(DBManager.TABLE.ACCOUNT, new String[] { DBManager.ACCOUNT_PHONE },
                DBManager.ACCOUNT_PHONE + "=" + contentValues.getAsString(DBManager.ACCOUNT_PHONE),
                null, null, null, null, "注册", registerCallBack);
    }

    public void completeInfo(ContentValues contentValues, VerificationCallBack verificationCallBack) {
        this.contentValues = contentValues;
        dbAdapter.update(DBManager.TABLE.ACCOUNT, contentValues, DBManager.ACCOUNT_PHONE + "="
                + accountInfo.getPhone(), null, "完善账户信息", verificationCallBack);
    }

    public void logout() {
        accountInfo = null;
    }

    private DBManager.ResultListener dbResultListener = new DBManager.ResultListener() {
        @Override
        public void query(Object o1, Object o2, ResultSet resultSet) {
            switch ((String) o1) {
                case "登入":
                    try {
                        LoginCallBack loginCallBack = (LoginCallBack) o2;
                        if (resultSet.next()) {
                            if (resultSet.getString(DBManager.ACCOUNT_PSW).equals(psw)) {
                                accountInfo = new AccountInfo();
                                accountInfo.setPhone(phone);
                                accountInfo.setUserPic(resultSet.getLong(DBManager.ACCOUNT_PIC));
                                accountInfo.setStuId(resultSet.getLong(DBManager.ACCOUNT_STU_ID));
                                accountInfo.setSchoolId(resultSet.getLong(DBManager.ACCOUNT_SCHOOL));
                                accountInfo.setSex(resultSet.getString(DBManager.ACCOUNT_SEX));
                                accountInfo.setName(resultSet.getString(DBManager.ACCOUNT_NAME));
                                accountInfo.setAdmin(resultSet.getBoolean(DBManager.ACCOUNT_ADMIN));
                                accountInfo.setSocietyId(resultSet.getLong(DBManager.ACCOUNT_SOCIETY_ID));
                                loginCallBack.onLogin(accountInfo.getSchoolId() != 0);
                            }
                            else
                                loginCallBack.onFailure("密码错误");
                        }
                        else
                            loginCallBack.onFailure("账户不存在");
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                        ((LoginCallBack) o2).onFailure("登录失败");
                    }
                    phone = psw = null;
                    break;
                case "注册":
                    try {
                        if (resultSet.next()) {
                            ((RegisterCallBack) o2).onFailure(null, null, "账户已注册");
                            contentValues = null;
                        }
                        else
                            dbAdapter.insert(DBManager.TABLE.ACCOUNT, contentValues, "注册", o2);
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }

        @Override
        public void insert(Object o1, Object o2, int result) {
            switch ((String) o1) {
                case "注册":
                    RegisterCallBack registerCallBack = (RegisterCallBack) o2;
                    if (result > 0) {
                        accountInfo = new AccountInfo();
                        accountInfo.setPhone(phone);
                        phone = psw = null;
                        registerCallBack.onRegister();
                    }
                    else
                        registerCallBack.onFailure(null, null, "注册失败");
                    contentValues = null;
                    break;
            }
        }

        @Override
        public void update(Object o1, Object o2, int result) {
            switch ((String) o1) {
                case "完善账户信息":
                    VerificationCallBack verificationCallBack = (VerificationCallBack) o2;
                    if (result > 0) {
                        accountInfo.setName(contentValues.getAsString(DBManager.ACCOUNT_NAME));
                        accountInfo.setStuId(contentValues.getAsLong(DBManager.ACCOUNT_STU_ID));
                        accountInfo.setSex(contentValues.getAsString(DBManager.ACCOUNT_SEX));
                        accountInfo.setUserPic(contentValues.getAsLong(DBManager.ACCOUNT_PIC));
                        accountInfo.setAdmin(contentValues.getAsBoolean(DBManager.ACCOUNT_ADMIN));
                        accountInfo.setSchoolId(contentValues.getAsLong(DBManager.ACCOUNT_SCHOOL));
                        verificationCallBack.onSuccess();
                    }
                    else
                        verificationCallBack.onFailure("操作失败");
                    break;
            }
        }

        @Override
        public void onFailure(Object o1, Object o2, Exception e) {
            e.printStackTrace();
        }

    };

}
