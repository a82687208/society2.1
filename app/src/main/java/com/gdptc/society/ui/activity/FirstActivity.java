package com.gdptc.society.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.basemodel.Value;
import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

/**
 * Created by Administrator on 2018/6/11/011.
 */

public class FirstActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_first);

        findViewById(R.id.btn_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(FirstActivity.this, LoginActivity.class);
                startActivity(intent1);
            }
        });

        requestPermission();
    }

    private void requestPermission() {
        List<PermissionItem> permissionItems = new ArrayList<PermissionItem>();
        permissionItems.add(new PermissionItem(Manifest.permission.READ_PHONE_STATE, "手机状态", R.drawable.permission_ic_phone));
        permissionItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE, "存储卡", R.drawable.permission_ic_storage));
//        permissionItems.add(new PermissionItem(Manifest.permission.RECEIVE_SMS, "短信", R.drawable.permission_ic_sms));
//        permissionItems.add(new PermissionItem(Manifest.permission.ACCESS_FINE_LOCATION, "定位", R.drawable.permission_ic_location));
        permissionItems.add(new PermissionItem(Manifest.permission.CAMERA, "相机", R.drawable.permission_ic_camera));
        HiPermission.create(this).animStyle(R.style.PermissionAnimScale)
                .filterColor(Value.getColorUI().toValue())
                .title("授予权限")
                .msg("为了正常使用" + getResources().getString(R.string.app_name) + "，需要以下访问权限")
                .style(R.style.PermissionDefaultGreenStyle)
                .permissions(permissionItems)
                .checkMutiPermission(permissionCallback);
    }

    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public void onClose() {

        }

        @Override
        public void onFinish() {

        }

        @Override
        public void onDeny(String permission, int position) {

        }

        @Override
        public void onGuarantee(String permission, int position) {

        }
    };

}
