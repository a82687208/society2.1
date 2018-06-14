package com.gdptc.society.tools;

import java.util.Random;

/**
 * Created by Administrator on 2018/1/3/003.
 */

public class RandomUtil {

    public static String getRandomString() {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < base.length(); ++i)
            sb.append(base.charAt(random.nextInt(base.length())));
        return sb.toString();
    }

    public static String getRandomId(int digits) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder(digits);

        for (int i = 0; i < digits; ++i)
            builder.append(random.nextInt(9));
        return builder.toString();
    }
}
