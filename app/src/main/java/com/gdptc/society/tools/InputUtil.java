package com.gdptc.society.tools;

import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gdptc.society.ui.view.LineSpaceCursorDrawable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Administrator on 2017/12/9/009.
 */

public class InputUtil {
    private Context context;
    private InputMethodManager imm;

    public InputUtil(Context context) {
        this.context = context;
        imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void openKeyBord(EditText editText) {
        editText.requestFocus();
        editText.requestFocusFromTouch();
        imm.showSoftInput(editText, InputMethodManager.RESULT_SHOWN);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void closeKeyBord(EditText editText) {
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static boolean checkNull(Context context, List<EditText> editTextList, String[] errMsg) {
        for (int i = 0; i < editTextList.size(); ++i) {
            if (editTextList.get(i).length() == 0) {
                Toast.makeText(context, "您尚未填写" + errMsg[i], Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    public static void setTextCursor(Context context, EditText editText) {
        try {
            Class<?> cls = TextView.class;
            Method createEditorIfNeeded = cls.getDeclaredMethod("createEditorIfNeeded");
            if (createEditorIfNeeded != null) {
                createEditorIfNeeded.setAccessible(true);
                createEditorIfNeeded.invoke(editText, new Object[0]);
                Field editor = cls.getDeclaredField("mEditor");
                if (editor != null) {
                    editor.setAccessible(true);
                    Field cursorDrawable = Class.forName("android.widget.Editor").getDeclaredField("mCursorDrawable");
                    if (cursorDrawable != null) {
                        cursorDrawable.setAccessible(true);
                        Array.set(cursorDrawable.get(editor.get(editText)), 0,
                                new LineSpaceCursorDrawable(context, editText));
                        Array.set(cursorDrawable.get(editor.get(editText)), 1,
                                new LineSpaceCursorDrawable(context, editText));
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float getNumberTextWidth(Paint paint) {
        float[] widths = new float[1];
        paint.getTextWidths("0", widths);
        return widths[0];
    }

    public static float getLineSpacingExtra(Context context, TextView view){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return view.getLineSpacingExtra();
        }
        else{
            return TypedValueUtil.dip2px(context, 8);
        }
    }

    public final static InputFilter spaceFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            return source.toString().equals(" ") ? "" : null;
        }
    };
}
