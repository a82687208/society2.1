package com.gdptc.society.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.gdptc.society.R;

/**
 * Created by Administrator on 2017/12/12/012.
 */

public class LoadingDialog {
    private Dialog alertDialog = null;
    private WindowManager.LayoutParams layoutParams = null;
    private DisplayMetrics displayMetrics;
    private TextView tvMsg;

    public LoadingDialog(Activity activity) {
        displayMetrics = activity.getResources().getDisplayMetrics();
        alertDialog = new Dialog(activity, R.style.LoadingDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        tvMsg = view.findViewById(R.id.tv_login_content);
        alertDialog.setContentView(view);
    }

    public void show(boolean cancelable) {
        alertDialog.setCancelable(cancelable);
        alertDialog.show();
        if (layoutParams == null) {
            layoutParams = alertDialog.getWindow().getAttributes();
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            layoutParams.y = -(displayMetrics.heightPixels / 12);
        }
        alertDialog.getWindow().setAttributes(layoutParams);
    }

    public void setMessage(String msg) {
        tvMsg.setText(msg);
    }

    public boolean isShowing() {
        return alertDialog != null && alertDialog.isShowing();
    }

    public void dismiss() {
        alertDialog.dismiss();
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        alertDialog.setOnCancelListener(onCancelListener);
    }

}
