package com.gdptc.society.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Administrator on 2018/1/3/003.
 */

public class CameraUtil {
    public static final int CAMERA_REQUEST_CODE = 0x00000001;

    public static void takePicture(Activity activity, File file) {
        int hasPermissions = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (hasPermissions != PackageManager.PERMISSION_GRANTED)
            Toast.makeText(activity, "请在应用管理中打开“相机”访问权限！", Toast.LENGTH_LONG).show();

        else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
            intent.putExtra("return-data", true);
            activity.startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }
}
