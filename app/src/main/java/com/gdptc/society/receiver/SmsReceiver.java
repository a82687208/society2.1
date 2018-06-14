package com.gdptc.society.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.EditText;

/**
 * Created by Administrator on 2017/8/24/024.
 */

public class SmsReceiver extends BroadcastReceiver {
    private EditText target;
    private String logo = "【掌淘科技】超级图书";

    public SmsReceiver(EditText edtTxtVCode) {
        target = edtTxtVCode;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        SmsMessage[] smsMessage = new SmsMessage[pdus.length];

        for (int i = 0; i < pdus.length; i++)
            smsMessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

        String msg = smsMessage[pdus.length - 1].getMessageBody();

        //Log.e("TAG", phone.substring(17));

        if (msg.length() <= logo.length())
            return;
        if (msg.substring(0, logo.length()).equals(logo)) {
            int start;
            int end;
            for (start = 0; start < msg.length(); ++start) {
                if (msg.charAt(start) >= '0' && msg.charAt(start) <= '9')
                    break;
            }
            for (end = start + 1; end < msg.length(); ++end) {
                if (msg.charAt(end) < '0' || msg.charAt(end) > '9')
                    break;
            }
            target.setText(msg.substring(start, end));
            target.setSelection(target.length());
        }
    }
}
