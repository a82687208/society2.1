package com.gdptc.society.ui.dialog;

import android.content.Context;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.tools.MaterialDesignCompat;
import com.gdptc.society.R;
import com.gdptc.society.tools.TypedValueUtil;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;

/**
 * Created by Administrator on 2017/5/26/026.
 */

public class MergeDialog {
    public static final int POSITIVE_ID = R.id.item_merge_ok_button;
    public static final int NEGATIVE_ID = R.id.item_merge_cancel_button;

    private final View view;
    private final TextView title;
    private final TextView message;
    private final Button ok;
    private final Button cancel;
    private final View titleLine;
    private final View messageLine;
    private final View buttonLine;
    private final AlertDialog alertDialog;
    private final RelativeLayout bottomLayout;
    private Context context;

    public MergeDialog(Context context) {
        view = LayoutInflater.from(context).inflate(R.layout.item_merga_dialog, null);
        title = (TextView) view.findViewById(R.id.item_merge_title);
        message = (TextView) view.findViewById(R.id.item_merge_message);
        ok = (Button) view.findViewById(R.id.item_merge_ok_button);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ok.getLayoutParams();
            params.bottomMargin = 0;
            ok.setLayoutParams(params);
        }
        cancel = (Button) view.findViewById(R.id.item_merge_cancel_button);
        MaterialDesignCompat.addRipple(ok, MaterialDesignCompat.DEFAULT_COLOR);
        MaterialDesignCompat.addRipple(cancel, MaterialDesignCompat.DEFAULT_COLOR);
        titleLine = view.findViewById(R.id.item_merge_titleLine);
        messageLine = view.findViewById(R.id.item_merge_messageLine);
        buttonLine = view.findViewById(R.id.item_merge_buttonLine);
        bottomLayout = (RelativeLayout) view.findViewById(R.id.item_merge_bottonLayout);
        alertDialog = new AlertDialog.Builder(context, R.style.AlphaDialogTheme).setView(view).create();
        this.context = context;
    }


    public View getView() {
        return view;
    }

    public MergeDialog setCancelable(boolean cancelable) {
        alertDialog.setCancelable(cancelable);
        return this;
    }

    public MergeDialog setTitle(String title) {
        this.title.setText(title);
        return this;
    }

    public MergeDialog setTitleGravity(int gravity) {
        title.setGravity(gravity);
        return this;
    }

    public MergeDialog setMessage(String message) {
        this.message.setText(message);
        return this;
    }

    public MergeDialog setPositiveButton(String content, final View.OnClickListener onClickListener) {
        ok.setText(content);
        ok.setOnClickListener(onClickListener);
        return this;
    }

    public MergeDialog setNegativeButton(String content, final View.OnClickListener onClickListener) {
        buttonLine.setVisibility(View.VISIBLE);
        cancel.setVisibility(View.VISIBLE);
        bottomLayout.setPadding(0, 0, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) TypedValueUtil.dip2px(context, 45));
        layoutParams.setMargins(0, 0, 0, 0);
        layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.item_merge_cancel_button);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((View) ok.getParent()).setLayoutParams(layoutParams);
        else
            ok.setLayoutParams(layoutParams);
        ok.setBackgroundResource(R.drawable.dialog_right_button_selector);
        cancel.setText(content);
        cancel.setOnClickListener(onClickListener);
        return this;
    }

    public MergeDialog hideNagetiveButton() {
        buttonLine.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
        bottomLayout.setPadding(0, 0, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) TypedValueUtil.dip2px(context, 45),
                (int) TypedValueUtil.dip2px(context, 35));
        layoutParams.setMargins(0, 0, (int) TypedValueUtil.dip2px(context, 20), (int) TypedValueUtil.dip2px(context, 10));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((View) ok.getParent()).setLayoutParams(layoutParams);
        else
            ok.setLayoutParams(layoutParams);
        ok.setBackgroundResource(R.drawable.dialog_right_button_selector);

        return this;
    }

    public MergeDialog showTitleLine(boolean show) {
        titleLine.setVisibility(show ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) title.getLayoutParams();
        int margins[] = new int[2];
        margins[0] = (int) TypedValueUtil.dip2px(context, 20);
        margins[1] = (int) TypedValueUtil.dip2px(context, 10);
        layoutParams.setMargins(margins[0], margins[1], margins[0], margins[1]);
        return this;
    }

    public MergeDialog showMessageLine(boolean show) {
        messageLine.setVisibility(show ? View.VISIBLE : View.GONE);
        return this;
    }

    public void show() {
        alertDialog.show();
        alertDialog.getWindow().setLayout(messageLine.getVisibility() == View.VISIBLE ? (int) (displayMetrics.widthPixels / 1.5) :
                (int) (displayMetrics.widthPixels / 1.25), LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public void dismiss() {
        alertDialog.dismiss();
    }
}
