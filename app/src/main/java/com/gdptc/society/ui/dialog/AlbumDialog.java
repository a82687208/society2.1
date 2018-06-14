package com.gdptc.society.ui.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.tools.MaterialDesignCompat;
import com.gdptc.society.R;

import static com.gdptc.society.manager.ApplicationManager.displayMetrics;

/**
 * Created by hp on 2018/4/4.
 */

public class AlbumDialog implements View.OnClickListener {
    public final static int FIRST_SELECTOR_ID = R.id.item_lnrLyt_selector_camera;
    public final static int SECOND_SELECTOR_ID = R.id.item_lnrLyt_selector_album;

    private AlertDialog dialog = null;
    private View.OnClickListener onClickListener;

    public AlbumDialog(Context context) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.item_selector_dialog, null);
        View view = contentView.findViewById(R.id.item_lnrLyt_selector_camera);
        view.setOnClickListener(this);
        MaterialDesignCompat.addRipple(contentView.findViewById(R.id.item_lnrLyt_selector_camera), MaterialDesignCompat.DEFAULT_COLOR);

        view = contentView.findViewById(R.id.item_lnrLyt_selector_album);
        view.setOnClickListener(this);
        MaterialDesignCompat.addRipple(contentView.findViewById(R.id.item_lnrLyt_selector_album), MaterialDesignCompat.DEFAULT_COLOR);
        dialog = new AlertDialog.Builder(context, R.style.SelectorDialogTheme).setView(contentView).create();
    }

    public void show() {
        dialog.show();
        dialog.getWindow().setLayout((int) (displayMetrics.widthPixels / 1.5), LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public AlbumDialog setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        return this;
    }

    @Override
    public void onClick(View v) {
        if (onClickListener != null)
            onClickListener.onClick(v);
        dialog.dismiss();
    }
}
