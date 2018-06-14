package com.gdptc.society.ui.view;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.text.Editable;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.example.basemodel.Value;
import com.gdptc.society.tools.TypedValueUtil;

/**
 * Created by Administrator on 2018/3/19/019.
 */

public class LineSpaceCursorDrawable extends ShapeDrawable {
    private EditText view;

    public LineSpaceCursorDrawable(Context context, EditText view) {
        this.view = view;
        setDither(false);
        getPaint().setColor(Value.getColorUI().toValue());
        setIntrinsicWidth((int) TypedValueUtil.dip2px(context, 2));//res.getDimensionPixelSize(R.dimen.detail_notes_text_cursor_width));
    }

    public void setBounds(int left, int top, int right, int bottom) {
        Editable s = view.getText();
        ImageSpan[] imageSpans = s.getSpans(0, s.length(),
                ImageSpan.class);
        int selectionStart = view.getSelectionStart();
        for (ImageSpan span : imageSpans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            if (selectionStart >= start && selectionStart <= end)
            {
                super.setBounds(left, top, right, top - 1);
                return;
            }
        }
        super.setBounds(left, top, right, top + view.getLineHeight() - (int)view.getLineSpacingExtra());

    }
}
