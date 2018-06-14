package com.gdptc.society.tools;

import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Administrator on 2017/11/6/006.
 */

public class BtnSendLimitUtil extends Thread {
    private Button sendBtn = null;
    private int time = 60;
    private String rclTxt, sendTxt, timeTxt;
    private boolean canSend = false, interrupted;
    private int rclColor = -1;
    private ColorStateList btnSourceColor;
    private PostTxtChange postTxtChange = new PostTxtChange();
    private EditText targetEdtTxt;

    public BtnSendLimitUtil(EditText targetEdtTxt, Button sendBtn, String rclTxt, String sendTxt) {
        this.sendBtn = sendBtn;
        this.rclTxt = rclTxt;
        this.sendTxt = sendTxt;
        this.targetEdtTxt = targetEdtTxt;
        timeTxt = rclTxt;
        btnSourceColor = sendBtn.getTextColors();
        if (targetEdtTxt != null)
            targetEdtTxt.addTextChangedListener(textWatcher);
    }

    public BtnSendLimitUtil(EditText targetEdtTxt,
                            Button sendBtn, String rclTxt, String sendTxt, int rclColor) {
        this(targetEdtTxt, sendBtn, rclTxt, sendTxt);
        this.rclColor = rclColor;
    }

    private class PostTxtChange implements Runnable {
        String txt;

        @Override
        public void run() {
            sendBtn.setText(txt);
        }
    }

    public boolean canSend() {
        return canSend;
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {
                canSend = false;
                if (isAlive()) {
                    sendBtn.setEnabled(false);
                    if (rclColor != -1)
                        sendBtn.setTextColor(rclColor);
                    sendBtn.setText(timeTxt);
                }
                else {
                    sendBtn.setText(rclTxt);
                    sendBtn.setTextColor(btnSourceColor);
                }
            }
            else {
                canSend = true;
                sendBtn.setText(sendTxt);
                sendBtn.setTextColor(btnSourceColor);
                sendBtn.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    @Override
    public void run() {
        sendBtn.post(new Runnable() {
            @Override
            public void run() {
                sendBtn.setEnabled(false);
                if (rclColor != -1)
                    sendBtn.setTextColor(rclColor);
            }
        });
        while (time != 0) {
            try {
                sleep(1000);
            }
            catch (InterruptedException e) {
                interrupted = true;
                sendBtn.post(new Runnable() {
                    @Override
                    public void run() {
                        canSend = false;
                        sendBtn.setEnabled(true);
                        sendBtn.setTextColor(btnSourceColor);
                        if (targetEdtTxt != null && targetEdtTxt.length() == 0)
                            sendBtn.setText(rclTxt);
                    }
                });
                return;
            }
            --time;
            timeTxt = rclTxt + "(" + time + "s)";
            if (!canSend) {
                postTxtChange.txt = timeTxt;
                sendBtn.post(postTxtChange);
            }
        }
        time = 60;
        if (sendBtn.length() > 0) {
            timeTxt = rclTxt;
            postTxtChange.txt = rclTxt;
            sendBtn.post(postTxtChange);
            sendBtn.post(new Runnable() {
                @Override
                public void run() {
                    sendBtn.setTextColor(btnSourceColor);
                    sendBtn.setEnabled(true);
                }
            });
        }
    }
}
